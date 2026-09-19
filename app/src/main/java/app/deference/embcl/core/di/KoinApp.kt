package app.deference.embcl.core.di

import org.koin.core.annotation.KoinApplication

@KoinApplication(
	modules = [
		DataModule::class,
		DomainModule::class,
		UiModule::class,
		NetworkModule::class,
		CoreModule::class
	]
)
class KoinApp
