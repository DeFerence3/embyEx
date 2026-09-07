package app.deference.embcl.core.networking

import retrofit2.HttpException
import java.io.IOException

object ApiResponseHandler {

  suspend fun <T> safeApiCall(call: suspend () -> T): T = try {
    call()
  } catch (e: HttpException) {
    val message = when (e.code()) {
      401 -> "Invalid username or password."
      403 -> "Access forbidden for this account."
      404 -> "Emby server endpoint not found."
      500 -> "Emby internal server error."
      else -> "Emby server returned code ${e.code()}."
    }
    throw IOException(message, e)
  } catch (e: IOException) {
    throw IOException(e.message ?: "Unable to connect to Emby server. Check address.", e)
  }
}
