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
//	single { AuthInterceptor(get()) }
	/*single {
		HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BASIC
		}
	}
	factory { OkHttpClient.Builder() }*/
	
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
	/*single {
		get<OkHttpClient.Builder>()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.addInterceptor(get<HostSelectionInterceptor>())
			.addInterceptor(get<AuthInterceptor>())
			.addInterceptor(get<HttpLoggingInterceptor>())
			.build()
	}
	single {
		val sessionStore = get<EmbySessionStore>()
		val url = sessionStore.getLastServerUrl() ?: "http://localhost:8096/"
		val json = Json {
			ignoreUnknownKeys = true
			encodeDefaults = true
			isLenient = true
			coerceInputValues = true
		}
		Retrofit.Builder()
			.baseUrl(url)
			.client(get())
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.build()
	}
	single<EmbyApiService> {
		get<Retrofit>().create(EmbyApiService::class.java)
	}*/
}
