package app.deference.embycl.core.di

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin<KoinApp> {
	appDeclaration()
	
	SingletonImageLoader.setSafe { context ->
		ImageLoader.Builder(context)
			.components {
				add(
					KtorNetworkFetcherFactory(
						httpClient = { KoinPlatformTools.defaultContext().get().get<HttpClient>(named("image_client")) }
					)
				)
			}
			.build()
	}
}
