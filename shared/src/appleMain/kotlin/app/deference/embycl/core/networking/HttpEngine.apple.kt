package app.deference.embycl.core.networking

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual val clientEngine: HttpClientEngineFactory<*>
	get() = Darwin