package app.deference.embycl.core.utils

import app.deference.embycl.core.networking.DataState
import app.deference.embycl.core.networking.ResponseHandler.toDataState
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

object NetworkUtils {
	
	suspend fun <T> safeApiCall(call: suspend () -> T): T = try {
		call()
	} catch (e: Exception) {
		e.printStackTrace()
		val message = "Error: ${e.message ?: e.stackTraceToString()}"
		throw IOException(message, e)
	} catch (e: IOException) {
		throw IOException(e.message ?: "Unable to connect to Emby server. Check address.", e)
	}
	
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
		if (e is TimeoutCancellationException || (e is IOException && e.message?.contains("timeout", ignoreCase = true) == true)) {
			app.deference.embycl.core.session.Session.setConnected(false)
		}
		return when (e) {
			is IOException -> DataState.Error("Network request failed: ${e.message}")
			is TimeoutCancellationException -> DataState.Error("Request timed out")
			is SerializationException -> DataState.Error("Invalid response from server")
			else -> DataState.Error("Unhandled Exception: ${e.message}")
		}
	}
}