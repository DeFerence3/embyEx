package app.deference.embcl.core.di

import app.deference.embcl.core.networking.EmbyMdnsDiscovery
import app.deference.embcl.core.networking.EmbyUdpDiscovery
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.data.repository.EmbyRepositoryImpl
import app.deference.embcl.domain.repository.EmbyRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
  single { EmbySessionStore(androidContext()) }
  single { EmbyUdpDiscovery(androidContext()) }
  single { EmbyMdnsDiscovery(androidContext()) }
  single<EmbyRepository> { EmbyRepositoryImpl(get(), get(), get(), get(), get()) }
}
