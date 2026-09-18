package app.deference.embcl.core.networking

import app.deference.embcl.core.networking.ResponseHandler.toDataState
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException

object ApiResponseHandler {
	
	suspend fun <T> safeApiCall(call: suspend () -> T): T = try {
		call()
	} catch (e: Exception) {
		e.printStackTrace()
		val message = "Error: ${e.message ?: e.stackTrace}"
		throw IOException(message, e)
	} catch (e: IOException) {
		throw IOException(e.message ?: "Unable to connect to Emby server. Check address.", e)
	}
	/*	suspend inline fun <T> safeDataState(
			crossinline apiCall: suspend () -> DataState<T>
		): DataState<T> {
			return runCatching {
				apiCall()
			}.getOrElse { e ->
				handleException(e)
			}
		}*/
	suspend inline fun <reified T> safeDataState(
		crossinline call: suspend () -> HttpResponse,
	): DataState<T> {
		return runCatching {
			call().toDataState<T>()
		}.getOrElse { e ->
			handleException(e)
		}
	}
	
	fun handleException(e: Throwable): DataState.Error {
		e.printStackTrace()
		return when (e) {
			is kotlinx.io.IOException -> DataState.Error("Network request failed: ${e.message}")
			is TimeoutCancellationException -> DataState.Error("Request timed out")
			is SerializationException -> DataState.Error("Invalid response from server")
			else -> DataState.Error("Unhandled Exception: ${e.message}")
		}
	}
}
