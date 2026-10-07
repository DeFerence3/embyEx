package app.deference.embycl.core.networking

import app.deference.embycl.BuildConfig
import app.deference.embycl.core.session.Session
import app.deference.embycl.core.session.Session.deviceId
import app.deference.embycl.core.utils.JsonUtils
import app.deference.embycl.core.utils.toHttpUrl
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException

val DONT_INTERCEPT = AttributeKey<Boolean>("DONT_INTERCEPT")

/**
 * Don't intercept this request for setting server url,
 * instead uses provided host and port
 */
fun HttpRequestBuilder.dontIntercept(
	host: String,
	port: Int?,
	protocol: URLProtocol? = null
) {
	url {
		this.host = host
		if (port != null) {
			this.port = port
		}
		if (protocol != null) {
			this.protocol = protocol
		}
	}
	attributes[DONT_INTERCEPT] = true
}

fun HttpClient.invalidateAuthTokens() {
	authProvider<BearerAuthProvider>()?.clearToken()
}

fun HttpClient.urlInterceptor() {
	plugin(HttpSend).intercept { request ->
		val dontIntercept = request.attributes.getOrNull(DONT_INTERCEPT)
		val proccessedRequest = if (dontIntercept == true) {
			request
		} else {
			val dynamicBase = Session.serverUrl.toHttpUrl()
			request.url.host = dynamicBase.host
			request.url.port = dynamicBase.port
			request
		}
		execute(proccessedRequest)
	}
}

fun HttpClientConfig<*>.configureLogging() {
	install(Loggin) {
		logRequest = false
		logResponse = false
		logOnError = true
	}
}

fun HttpClientConfig<*>.configureDefaultRequest(userAgent: UserAgentProvider) {
	install(DefaultRequest) {
		contentType(ContentType.Application.Json)
		accept(ContentType.Application.Json)
		userAgent(userAgent.getUserAgent())
		headers {
			val authHeader = buildString {
				append("Emby Client=\"mpvEx\", Device=\"Android\", DeviceId=\"")
				append(deviceId)
				append("\", Version=\"")
				append(BuildConfig.VERSION_NAME)
				append('"')
				append(", UserId=\"${Session.userId}\"")
				Session.accessToken?.let { append(", Token=\"$it\"") }
			}
			
			append("Emby-Client", "mpvEx")
			append("Device", "Android")
			append("DeviceId", deviceId)
			append("Version", BuildConfig.VERSION_NAME)
			append("UserId", Session.userId)
			append("X-Emby-Authorization", authHeader)
			Session.accessToken?.let { append("X-Emby-Token", it) }
		}
	}
	
	install(HttpTimeout) {
		socketTimeoutMillis = 10000
		requestTimeoutMillis = 10000
		connectTimeoutMillis = 10000
	}
	
	HttpResponseValidator {
		handleResponseExceptionWithRequest { cause, request ->
			when (cause) {
				is ConnectTimeoutException,
				is SocketTimeoutException,
				is HttpRequestTimeoutException,
				is TimeoutCancellationException -> {
					Session.setConnected(false)
					throw cause
				}
				
				is IOException -> {
					if (cause.message?.contains("timeout", ignoreCase = true) == true ||
						cause.message?.contains("timed out", ignoreCase = true) == true) {
						Session.setConnected(false)
					}
					throw cause
				}
				
				else -> throw cause
			}
		}
	}
}

fun HttpClientConfig<*>.configureContentNegotiation() {
	install(ContentNegotiation) {
		json(
			json = JsonUtils.json,
			contentType = ContentType.Application.Json
		)
	}
}

fun HttpClientConfig<*>.configureValidation() {
	HttpResponseValidator {
		validateResponse { response: HttpResponse ->
			if (response.status.value == 401) {
				Session.logout()
				response.call.client.invalidateAuthTokens()
			}
		}
	}
}

fun HttpClientConfig<*>.configureAuth() {
	install(Auth) {
		bearer {
			loadTokens {
				val token = Session.accessToken
				token?.let {
					BearerTokens(it, null)
				}
			}
		}
	}
}
