package app.deference.embycl.core.networking

import io.ktor.client.engine.HttpClientEngineFactory

expect val clientEngine: HttpClientEngineFactory<*>