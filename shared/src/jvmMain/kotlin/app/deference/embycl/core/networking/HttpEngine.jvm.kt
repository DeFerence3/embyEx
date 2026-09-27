package app.deference.embycl.core.networking

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual val clientEngine: HttpClientEngineFactory<*>
	get() = OkHttp