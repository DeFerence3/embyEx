package app.deference.embycl.core.networking

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.IOException
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.SubmissionPublisher
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession

/**
 * Nucleus requires a JDK HttpClient, but its default transport can get stuck on one
 * unreachable GitHub CDN address. OkHttp tries alternate DNS addresses, as on Android.
 * This adapter supports the synchronous GET requests used by Nucleus, including ranges
 * and streaming bodies, so Nucleus still owns checksum verification and installation.
 */
internal class UpdaterHttpClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .build(),
) : HttpClient() {
    override fun <T> send(request: HttpRequest, handler: HttpResponse.BodyHandler<T>): HttpResponse<T> {
        require(request.method() == "GET" && request.bodyPublisher().isEmpty) {
            "The updater transport only supports GET requests without a body."
        }
        val builder = Request.Builder().url(request.uri().toString())
        request.headers().map().forEach { (name, values) ->
            values.forEach { builder.addHeader(name, it) }
        }
        val call = client.newCall(builder.build())
        request.timeout().ifPresent { call.timeout().timeout(it.toMillis(), TimeUnit.MILLISECONDS) }
        val response = call.execute()
        val headers = HttpHeaders.of(response.headers.toMultimap()) { _, _ -> true }
        val version = if (response.protocol == Protocol.HTTP_2) Version.HTTP_2 else Version.HTTP_1_1
        val publisher = SubmissionPublisher<List<ByteBuffer>>(bodyExecutor, 1)
        try {
            val subscriber = handler.apply(object : HttpResponse.ResponseInfo {
                override fun statusCode() = response.code
                override fun headers() = headers
                override fun version() = version
            })
            publisher.subscribe(object : Flow.Subscriber<List<ByteBuffer>> {
                override fun onSubscribe(subscription: Flow.Subscription) {
                    subscriber.onSubscribe(object : Flow.Subscription {
                        override fun request(n: Long) = subscription.request(n)
                        override fun cancel() {
                            subscription.cancel()
                            call.cancel()
                            response.close()
                        }
                    })
                }
                override fun onNext(item: List<ByteBuffer>) = subscriber.onNext(item)
                override fun onError(error: Throwable) = subscriber.onError(error)
                override fun onComplete() = subscriber.onComplete()
            })
            // BodyHandlers.ofInputStream returns before EOF. Pump on another thread and
            // keep a bounded queue so large installers are never buffered in memory.
            bodyExecutor.execute {
                try {
                    response.use {
                        val input = it.body.byteStream()
                        while (publisher.hasSubscribers()) {
                            val bytes = ByteArray(8192)
                            val count = input.read(bytes)
                            if (count == -1) break
                            publisher.submit(listOf(ByteBuffer.wrap(bytes, 0, count)))
                        }
                    }
                    publisher.close()
                } catch (error: Exception) {
                    publisher.closeExceptionally(error)
                }
            }
            val body = subscriber.body.toCompletableFuture().get()
            return object : HttpResponse<T> {
                override fun statusCode() = response.code
                override fun request() = request
                override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
                override fun headers() = headers
                override fun body() = body
                override fun sslSession(): Optional<SSLSession> = Optional.empty()
                override fun uri() = response.request.url.toUri()
                override fun version() = version
            }
        } catch (error: Exception) {
            call.cancel()
            response.close()
            publisher.closeExceptionally(error)
            if (error is ExecutionException) throw IOException("Could not read update response", error.cause)
            throw error
        }
    }

    override fun <T> sendAsync(request: HttpRequest, handler: HttpResponse.BodyHandler<T>): CompletableFuture<HttpResponse<T>> =
        throw UnsupportedOperationException("Nucleus uses synchronous update requests.")

    override fun <T> sendAsync(request: HttpRequest, handler: HttpResponse.BodyHandler<T>, pushHandler: HttpResponse.PushPromiseHandler<T>): CompletableFuture<HttpResponse<T>> =
        throw UnsupportedOperationException("Nucleus uses synchronous update requests.")

    override fun cookieHandler(): Optional<CookieHandler> = Optional.empty()
    override fun connectTimeout(): Optional<Duration> = Optional.of(Duration.ofMillis(client.connectTimeoutMillis.toLong()))
    override fun followRedirects() = Redirect.NORMAL
    override fun proxy(): Optional<ProxySelector> = Optional.of(client.proxySelector)
    override fun sslContext(): SSLContext = SSLContext.getDefault()
    override fun sslParameters(): SSLParameters = sslContext().defaultSSLParameters
    override fun authenticator(): Optional<Authenticator> = Optional.empty()
    override fun version() = Version.HTTP_2
    override fun executor(): Optional<Executor> = Optional.of(bodyExecutor)

    override fun close() {
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private companion object {
        val bodyExecutor = Executors.newCachedThreadPool { task ->
            Thread(task, "updater-response").apply { isDaemon = true }
        }
    }
}
