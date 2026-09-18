package app.deference.embcl.core.networking

import app.deference.embcl.core.networking.ResponseHandler.isSuccess
import app.deference.embcl.core.utils.JsonUtils
import app.deference.embcl.core.utils.Log
import io.ktor.client.call.replaceResponse
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.isSaved
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.util.AttributeKey
import io.ktor.util.GZipEncoder
import io.ktor.util.appendAll
import io.ktor.util.appendIfNameAbsent
import io.ktor.util.split
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.MalformedInputException
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.serialization.serializer

val LOG_RESPONSE = AttributeKey<Boolean>("LOG_RESPONSE")
val LOG_REQUEST = AttributeKey<Boolean>("LOG_REQUEST")

fun HttpRequestBuilder.log() {
	attributes[LOG_RESPONSE] = true
	attributes[LOG_REQUEST] = true
}

fun HttpRequestBuilder.logRequest() {
	attributes[LOG_REQUEST] = true
}

fun HttpRequestBuilder.logResponse() {
	attributes[LOG_RESPONSE] = true
}

@OptIn(InternalAPI::class)
val DFLogger: ClientPlugin<MyLogginConf> = createClientPlugin("KtorLoggerDf", ::MyLogginConf) {
	val logAll = pluginConfig.logAll
	val logRequest = pluginConfig.logRequest
	val logResponse = pluginConfig.logResponse
	val logOnError = pluginConfig.logOnError
	
	suspend fun detectIfBinary(
		body: ByteReadChannel,
		contentLength: Long?,
		contentType: ContentType?,
		headers: Headers,
	): Triple<Boolean, Long?, ByteReadChannel> {
		if (headers.contains(HttpHeaders.ContentEncoding)) {
			return Triple(true, contentLength, body)
		}
		val charset = if (contentType != null) {
			contentType.charset() ?: Charsets.UTF_8
		} else {
			Charsets.UTF_8
		}
		var isBinary = false
		val firstChunk = ByteArray(1024)
		val firstReadSize = body.readAvailable(firstChunk)
		
		if (firstReadSize < 1) {
			return Triple(false, 0L, body)
		}
		val buffer = Buffer().apply { writeFully(firstChunk, 0, firstReadSize) }
		val firstChunkText = try {
			charset.newDecoder().decode(buffer)
		} catch (_: MalformedInputException) {
			isBinary = true
			""
		}
		
		if (! isBinary) {
			var lastCharIndex = - 1
			for (ch in firstChunkText) {
				lastCharIndex += 1
			}
			
			for ((i, ch) in firstChunkText.withIndex()) {
				if (ch == '\ufffd' && i != lastCharIndex) {
					isBinary = true
					break
				}
			}
		}
		
		if (! isBinary) {
			val channel = ByteChannel()
			val copied = client.async {
				channel.writeFully(firstChunk, 0, firstReadSize)
				val copied = body.copyTo(channel)
				channel.flushAndClose()
				copied
			}.await()
			
			return Triple(false, copied + firstReadSize, channel)
		}
		
		return Triple(true, contentLength, body)
	}
	
	suspend fun logRequestBody(
		content: OutgoingContent,
		contentLength: Long?,
		headers: Headers,
		method: HttpMethod,
		logLines: MutableList<String>,
		body: ByteReadChannel,
	) {
		val (isBinary, size, newBody) = detectIfBinary(body, contentLength, content.contentType, headers)
		
		if (! isBinary) {
			val contentType = content.contentType
			val charset = if (contentType != null) {
				contentType.charset() ?: Charsets.UTF_8
			} else {
				Charsets.UTF_8
			}
			logLines.add(JsonUtils.prettifyJsonString(newBody.readRemaining().readText(charset = charset)))
			logLines.add("--> END ${method.value} ($size-byte body)")
		} else {
			var type = "binary"
			if (headers.contains(HttpHeaders.ContentEncoding)) {
				type = "encoded"
			}
			
			if (size != null) {
				logLines.add("--> END ${method.value} ($type $size-byte body omitted)")
			} else {
				logLines.add("--> END ${method.value} ($type body omitted)")
			}
		}
	}
	
	suspend fun logOutgoingContent(
		content: OutgoingContent,
		method: HttpMethod,
		headers: Headers,
		logLines: MutableList<String>,
		process: (ByteReadChannel) -> ByteReadChannel = { it },
	) {
		when (content) {
			is OutgoingContent.ByteArrayContent -> {
				val bytes = content.bytes()
				logRequestBody(content, bytes.size.toLong(), headers, method, logLines, ByteReadChannel(bytes))
			}
			
			is OutgoingContent.ContentWrapper -> {
				logOutgoingContent(content.delegate(), method, headers, logLines, process)
			}
			
			is OutgoingContent.NoContent -> {
				logLines.add("--> END ${method.value}")
			}
			
			is OutgoingContent.ProtocolUpgrade -> {
				logLines.add("--> END ${method.value}")
			}
			
			is OutgoingContent.ReadChannelContent -> {
				val (origChannel, newChannel) = content.readFrom().split(client)
				logRequestBody(content, content.contentLength, headers, method, logLines, newChannel)
			}
			
			is OutgoingContent.WriteChannelContent -> {
				val channel = ByteChannel()
				
				client.launch {
					content.writeTo(channel)
					channel.close()
				}
				
				val (origChannel, newChannel) = channel.split(client)
				logRequestBody(content, content.contentLength, headers, method, logLines, newChannel)
			}
		}
	}
	
	suspend fun logRequest(request: HttpRequestBuilder, logLines: MutableList<String>) {
		val uri = request.url.toString()
		val body = request.body
		val headers = HeadersBuilder().apply {
			if (body is OutgoingContent &&
				request.method != HttpMethod.Get &&
				request.method != HttpMethod.Head &&
				body !is EmptyContent
			) {
				body.contentType?.let {
					appendIfNameAbsent(HttpHeaders.ContentType, it.toString())
				}
				body.contentLength?.let {
					appendIfNameAbsent(HttpHeaders.ContentLength, it.toString())
				}
			}
			appendAll(request.headers)
		}.build()
		val contentLength = headers[HttpHeaders.ContentLength]?.toLongOrNull()
		val startLine = when {
			(request.method == HttpMethod.Get) ||
					(request.method == HttpMethod.Head) ||
					(contentLength == null) ||
					headers.contains(HttpHeaders.ContentEncoding) -> "--> ${request.method.value} $uri"
			
			else -> "--> ${request.method.value} $uri ($contentLength-byte body)"
		}
		
		logLines.add(startLine)
		
		for ((name, values) in headers.entries()) {
			logLines.add("$name: ${values.joinToString(", ")}")
		}
		
		if (request.method == HttpMethod.Get || request.method == HttpMethod.Head) {
			logLines.add("--> END ${request.method.value}")
			return
		}
		
		logLines.add("")
		// ✅ CASE 1: body is a Kotlin object (not yet serialized)
		if (body !is OutgoingContent) {
			try {
				val serializer = request.bodyType?.kotlinType?.let {
					JsonUtils.json.serializersModule.serializer(it)
				} ?: request.bodyType?.kotlinType?.let { JsonUtils.json.serializersModule.serializer(it) }
				val jsonString = if (serializer != null) JsonUtils.json.encodeToString(serializer, body) else ""
				logLines.add(JsonUtils.prettifyJsonString(jsonString))
				logLines.add("--> END ${request.method.value}")
			} catch (_: Exception) {
				logLines.add("[Unserializable body: ${body::class.simpleName}]")
				logLines.add("--> END ${request.method.value}")
			}
			return
		}
		// ✅ CASE 2: body is already OutgoingContent (your original path)
		if (request.headers[HttpHeaders.ContentEncoding] == "gzip") {
			logOutgoingContent(body, request.method, headers, logLines) { channel ->
				GZipEncoder.decode(channel)
			}
		} else {
			logOutgoingContent(body, request.method, headers, logLines)
		}
	}
	
	suspend fun logResponseBody(response: HttpResponse, body: ByteReadChannel, logLines: MutableList<String>) {
		logLines.add("")
		
		val (isBinary, size, newBody) = detectIfBinary(
			body,
			response.contentLength(),
			response.contentType(),
			response.headers
		)
		val duration = response.responseTime.timestamp - response.requestTime.timestamp
		
		if (size == 0L) {
			logLines.add("<-- END HTTP (${duration}ms, $size-byte body)")
			return
		}
		
		if (! isBinary) {
			val contentType = response.contentType()
			val charset = if (contentType != null) {
				contentType.charset() ?: Charsets.UTF_8
			} else {
				Charsets.UTF_8
			}
			
			if (contentType == ContentType.Application.Json) {
				logLines.add(JsonUtils.prettifyJsonString(newBody.readRemaining().readText(charset = charset)))
			} else {
				logLines.add(newBody.readRemaining().readText(charset = charset))
			}
			logLines.add("<-- END HTTP (${duration}ms, $size-byte body)")
		} else {
			var type = "binary"
			if (response.headers.contains(HttpHeaders.ContentEncoding)) {
				type = "encoded"
			}
			
			if (size != null) {
				logLines.add("<-- END HTTP (${duration}ms, $type $size-byte body omitted)")
			} else {
				logLines.add("<-- END HTTP (${duration}ms, $type body omitted)")
			}
		}
	}
	
	suspend fun logResponse(response: HttpResponse, logLines: MutableList<String>): HttpResponse {
		val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
		val request = response.request
		val duration = response.responseTime.timestamp - response.requestTime.timestamp
		val startLine = when {
			response.headers[HttpHeaders.TransferEncoding] == "chunked" ->
				"<-- ${response.status} ${request.url} (${duration}ms, unknown-byte body)"
			
			contentLength != null ->
				"<-- ${response.status} ${request.url} (${duration}ms, $contentLength-byte body)"
			
			else -> "<-- ${response.status} ${request.url} (${duration}ms)"
		}
		
		logLines.add(startLine)
		
		
		for ((name, values) in response.headers.entries()) {
			logLines.add("$name: ${values.joinToString(separator = ", ")}")
		}
		
		
		if (contentLength != null && contentLength == 0L) {
			logLines.add("<-- END HTTP (${duration}ms, $contentLength-byte body)")
			return response
		}
		
		if (response.contentType() == ContentType.Text.EventStream) {
			logLines.add("<-- END HTTP (streaming)")
			return response
		}
		
		if (response.isSaved) {
			logResponseBody(response, response.rawContent, logLines)
			return response
		}
		
		val (origChannel, newChannel) = response.rawContent.split(response)
		
		logResponseBody(response, newChannel, logLines)
		val call = response.call.replaceResponse { origChannel }
		return call.response
	}
	
	onRequest { request, _ ->
		if (request.attributes.getOrNull(LOG_REQUEST) == true || logAll || logRequest) {
			val requestLogLines = mutableListOf<String>()
			logRequest(request, requestLogLines)
			
			if (requestLogLines.isNotEmpty()) {
				val logsList = listOf("<-------------- Request -------------->") + requestLogLines
				Log.raw { logsList.joinToString(separator = "\n") }
			}
		}
	}
	
	onResponse { response ->
		val logOnError = logOnError && ! response.isSuccess
		if (response.call.attributes.getOrNull(LOG_RESPONSE) == true || logAll || logResponse || logOnError) {
			val responseLogLines = mutableListOf<String>()
			logResponse(response, responseLogLines)
			
			if (responseLogLines.isNotEmpty()) {
				val logsList = listOf("<-------------- Response -------------->") + responseLogLines
				Log.raw { logsList.joinToString(separator = "\n") }
			}
		}
	}
}

private fun computeRequestBodySize(content: Any): Long {
	check(content is OutgoingContent)
	return when (content) {
		is OutgoingContent.ByteArrayContent -> content.bytes().size.toLong()
		is OutgoingContent.ContentWrapper -> computeRequestBodySize(content.delegate())
		is OutgoingContent.NoContent -> 0
		is OutgoingContent.ProtocolUpgrade -> 0
		else -> error("Unable to calculate the size for type ${content::class.simpleName}")
	}
}

class MyLogginConf {
	
	internal var logAll: Boolean = false
	internal var logRequest: Boolean = false
	internal var logResponse: Boolean = false
	internal var logOnError: Boolean = false
}