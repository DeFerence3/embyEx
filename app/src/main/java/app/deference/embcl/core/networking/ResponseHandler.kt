package app.deference.embcl.core.networking

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ResponseHandler {
	
	val HttpResponse.isSuccess get() = (this.status.value in 200..299)
	
	suspend inline fun <reified T> HttpResponse.toDataState(): DataState<T> {
		if (T::class == Unit::class) {
			return if (this.isSuccess) {
				DataState.Success(Unit as T)
			} else {
				handleErrorResponse(this) as DataState<T>
			}
		}
		return decodeAndWrap()
	}
	
	suspend inline fun <reified R> HttpResponse.decodeAndWrap(): DataState<R> {
		return if (isSuccess) {
			try {
				val data = this.body<R>()
				DataState.Success(data)
			} catch (e: Exception) {
				e.printStackTrace()
				DataState.Error(e.message ?: "Unknown error")
			}
		} else {
			handleErrorResponse(this)
		}
	}
	
	suspend fun handleErrorResponse(
		response: HttpResponse,
	): DataState.Error {
		val error = when (response.contentType()) {
			ContentType.Application.Json -> {
				val errorBody = runCatching {
					response.bodyAsText()
				}.getOrDefault("")
				val responseJson = runCatching {
					if (errorBody.isNotBlank()) {
						response.body<JsonObject>()
					} else {
						null
					}
				}.getOrNull()
				responseJson?.let {
					getApiErrors(it, response.status.value, response.status.description)
				} ?: response.status.run {
					"$value : ${displayDescription()}"
				}
			}
			
			ContentType.Text.Plain -> response.bodyAsText()
			else -> {
				response.status.run {
					"$value : ${displayDescription()}"
				}
			}
		}
		
		return DataState.Error(error)
	}
	
	private fun HttpStatusCode.displayDescription(): String {
		return description.ifBlank {
			HttpStatusCode.fromValue(value).description
		}
	}
	
	private fun HttpStatusCode.displayError(): String {
		return "$value : ${displayDescription()}"
	}
	
	fun getApiErrors(json: JsonObject, statusCode: Int, statusMessage: String): String {
		return try {
			if (json["errors"] != null) {
				val errorsObject = json["errors"] !!.jsonObject
				val errorMessages = mutableListOf<String>()
				errorsObject.forEach { (field, value) ->
					value.jsonArray.forEach { error ->
						errorMessages.add(
							"$field: ${error.jsonPrimitive.content}"
						)
					}
				}
				errorMessages.joinToString("\n")
			} else if (json["error"] != null) {
				val errorsObject = json["error"]?.jsonPrimitive
				errorsObject?.content ?: ""
			} else if (json["title"] != null) {
				json["title"] !!.jsonPrimitive.content
			} else if (json["message"] != null) {
				val message = json["message"] !!.jsonPrimitive.content
				val statusDescription =
					json["statusDescription"]
						?.jsonPrimitive
						?.content
				if (statusDescription != null) {
					"$message: $statusDescription"
				} else {
					message
				}
			} else {
				"Error: HTTP $statusCode: $statusMessage"
			}
		} catch (e: Exception) {
			e.printStackTrace()
			"Unable to parse error response: HTTP $statusCode: $statusMessage"
		}
	}
}
