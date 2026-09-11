package app.deference.embcl.core.di

import org.koin.dsl.KoinAppDeclaration
import org.koin.plugin.module.dsl.startKoin

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin<KoinApp> {
	appDeclaration()
	modules(networkModule)
}
