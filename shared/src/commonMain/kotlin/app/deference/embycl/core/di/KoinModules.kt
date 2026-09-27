package app.deference.embycl.core.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("app.deference.embycl.core")
class CoreModule

@Module
@ComponentScan("app.deference.embycl.data")
class DataModule

@Module
@ComponentScan("app.deference.embycl.domain")
class DomainModule

@Module
@ComponentScan("app.deference.embycl.ui")
class UiModule