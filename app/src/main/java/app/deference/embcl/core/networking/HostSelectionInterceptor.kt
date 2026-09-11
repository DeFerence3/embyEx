package app.deference.embcl.core.networking

import app.deference.embcl.core.session.EmbySessionStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class HostSelectionInterceptor(
  private val sessionStore: EmbySessionStore,
) : Interceptor {

  @Volatile
  var hostUrl: String? = null

  override fun intercept(chain: Interceptor.Chain): Response {
    var request = chain.request()
    val currentServer = hostUrl ?: sessionStore.session.value?.serverUrl
    if (!currentServer.isNullOrBlank()) {
      val newUrl = currentServer.toHttpUrlOrNull()
      if (newUrl != null) {
        val updatedUrl = request.url.newBuilder()
          .scheme(newUrl.scheme)
          .host(newUrl.host) 
          .port(newUrl.port)
          .build()
        request = request.newBuilder().url(updatedUrl).build()
      }
    }
    return chain.proceed(request)
  }
}
