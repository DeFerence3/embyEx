package app.deference.embcl.core.di

import app.deference.embcl.core.networking.configureAuth
import app.deference.embcl.core.networking.configureContentNegotiation
import app.deference.embcl.core.networking.configureDefaultRequest
import app.deference.embcl.core.networking.configureLogging
import app.deference.embcl.core.networking.configureValidation
import app.deference.embcl.core.networking.urlInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

val networkModule = module {
	single<HttpClientEngine> { OkHttp.create() }
	
	single<HttpClient> {
		HttpClient(get()) {
			configureValidation()
			configureDefaultRequest(get())
			configureContentNegotiation()
			configureAuth()
			configureLogging()
		}.apply {
			urlInterceptor()
		}
	}
}
