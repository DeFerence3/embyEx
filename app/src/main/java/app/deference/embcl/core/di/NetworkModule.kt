package app.deference.embcl.core.di

import app.deference.embcl.core.networking.UserAgentProvider
import app.deference.embcl.core.networking.configureAuth
import app.deference.embcl.core.networking.configureContentNegotiation
import app.deference.embcl.core.networking.configureDefaultRequest
import app.deference.embcl.core.networking.configureLogging
import app.deference.embcl.core.networking.configureValidation
import app.deference.embcl.core.networking.urlInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class NetworkModule {

	@Single
	fun provideHttpClient(
		userAgentProvider: UserAgentProvider
	): HttpClient {
		return HttpClient(OkHttp) {
			configureValidation()
			configureDefaultRequest(userAgentProvider)
			configureContentNegotiation()
			configureAuth()
			configureLogging()
		}.apply {
			urlInterceptor()
		}
	}
}
