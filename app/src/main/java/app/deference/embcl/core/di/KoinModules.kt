package app.deference.embcl.core.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("app.deference.embcl.core")
class CoreModule

@Module
@ComponentScan("app.deference.embcl.data")
class DataModule

@Module
@ComponentScan("app.deference.embcl.domain")
class DomainModule

@Module
@ComponentScan("app.deference.embcl.ui")
class UiModule