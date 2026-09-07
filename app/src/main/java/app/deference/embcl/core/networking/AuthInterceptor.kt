package app.deference.embcl.core.networking

import app.deference.embcl.BuildConfig
import app.deference.embcl.core.session.EmbySessionStore
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(
	private val sessionStore: EmbySessionStore,
) : Interceptor {
	
	@Throws(IOException::class)
	override fun intercept(chain: Interceptor.Chain): Response {
		val originalRequest = chain.request()
		val requestBuilder = originalRequest.newBuilder()
			.method(originalRequest.method, originalRequest.body)
			.addHeader("Content-Type", "application/json")
			.addHeader("User-Agent", "mpvEx")
		val session = sessionStore.session.value
		val deviceId = sessionStore.deviceId()
		val authHeader = buildString {
			append("Emby Client=\"mpvEx\", Device=\"Android\", DeviceId=\"")
			append(deviceId)
			append("\", Version=\"")
			append(BuildConfig.VERSION_NAME)
			append('"')
			session?.userId?.let { append(", UserId=\"$it\"") }
			session?.accessToken?.let { append(", Token=\"$it\"") }
		}
		
		if (originalRequest.header("X-Emby-Authorization") == null) {
			requestBuilder.addHeader("X-Emby-Authorization", authHeader)
		}
		
		if (session != null && originalRequest.header("X-Emby-Token") == null) {
			requestBuilder.addHeader("X-Emby-Token", session.accessToken)
		}
		val response = chain.proceed(requestBuilder.build())
		
		if (response.code == 401 && session != null) {
			sessionStore.clear()
		}
		
		return response
	}
}
