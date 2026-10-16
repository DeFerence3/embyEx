package app.deference.embycl.core.networking

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

/** No global Session, URL interception, automatic logout, logging, or redirect following. */
fun HttpClientConfig<*>.configureMediaDownloads() {
    followRedirects = false
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
        // Payload transfers may take hours; metadata/probes set a request timeout separately.
    }
}
