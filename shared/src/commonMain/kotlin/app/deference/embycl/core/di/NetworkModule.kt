package app.deference.embycl.core.di

import app.deference.embycl.core.networking.UserAgentProvider
import app.deference.embycl.core.networking.clientEngine
import app.deference.embycl.core.networking.configureAuth
import app.deference.embycl.core.networking.configureContentNegotiation
import app.deference.embycl.core.networking.configureDefaultRequest
import app.deference.embycl.core.networking.configureLogging
import app.deference.embycl.core.networking.configureValidation
import app.deference.embycl.core.networking.urlInterceptor
import app.deference.embycl.core.session.Session
import app.deference.embycl.core.utils.Log
import coil3.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.userAgent
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
class NetworkModule {

	@Single
	fun provideHttpClient(
		userAgentProvider: UserAgentProvider
	): HttpClient {
		return HttpClient(engineFactory = clientEngine) {
			configureValidation()
			configureDefaultRequest(userAgentProvider)
			configureContentNegotiation()
			configureAuth()
			configureLogging()
		}.apply {
			urlInterceptor()
		}
	}
	
	@Single
	@Named("image_client")
	fun provideImageHttpClient(
		userAgentProvider: UserAgentProvider
	): HttpClient {
		return HttpClient {
			// ONLY append the auth headers Emby needs to serve the image.
			// Do NOT install ContentNegotiation, Auth, or your custom Loggin plugin.
			// Do NOT set accept(ContentType.Application.Json).
			install(DefaultRequest) {
				userAgent(userAgentProvider.getUserAgent())
				header("X-Emby-Token", Session.accessToken ?: "")
			}
		}.apply {
			urlInterceptor() // Keep if this appends API keys to the URL
		}
	}
	
	@Single
	fun provideCoilLogger(): Logger = object : Logger {
		override var minLevel: Logger.Level = Logger.Level.Debug
		override fun log(tag: String, level: Logger.Level, message: String?, throwable: Throwable?) {
			Log.i("Coil-Image") { "($level): $message" }
			throwable?.printStackTrace()
		}
	}
	
}
