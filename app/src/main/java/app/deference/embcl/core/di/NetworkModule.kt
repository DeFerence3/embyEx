package app.deference.embcl.core.di

import app.deference.embcl.core.networking.AuthInterceptor
import app.deference.embcl.core.networking.HostSelectionInterceptor
import app.deference.embcl.data.remote.EmbyApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
	single { HostSelectionInterceptor(get()) }
	single { AuthInterceptor(get()) }
	single {
		HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY
		}
	}
	single {
		OkHttpClient.Builder()
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.addInterceptor(get<HostSelectionInterceptor>())
			.addInterceptor(get<AuthInterceptor>())
			.addInterceptor(get<HttpLoggingInterceptor>())
			.build()
	}
	single {
		val json = Json {
			ignoreUnknownKeys = true
			encodeDefaults = true
			isLenient = true
			coerceInputValues = true
		}
		Retrofit.Builder()
			.baseUrl("http://localhost:8096/")
			.client(get())
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.build()
	}
	single<EmbyApiService> {
		get<Retrofit>().create(EmbyApiService::class.java)
	}
}
