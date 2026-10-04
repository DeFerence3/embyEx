package app.deference.embycl.core.networking

import com.sun.net.httpserver.HttpServer
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdaterHttpClientTest {
    private fun withServer(block: (HttpServer, String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.start()
        try {
            block(server, "http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }

    @Test(timeout = 10_000)
    fun followsRedirectsAndFallsBackToAnotherDnsAddress() = withServer { server, _ ->
        server.createContext("/latest") {
            it.responseHeaders.add("Location", "/manifest")
            it.sendResponseHeaders(302, -1)
            it.close()
        }
        server.createContext("/manifest") {
            val body = "version: 1.2.3".toByteArray()
            it.sendResponseHeaders(200, body.size.toLong())
            it.responseBody.use { output -> output.write(body) }
        }
        val transport = OkHttpClient.Builder().dns(object : Dns {
            override fun lookup(hostname: String) = listOf(
                InetAddress.getByName("127.0.0.2"), // No server on the first DNS address.
                InetAddress.getByName("127.0.0.1"),
            )
        }).connectTimeout(1, TimeUnit.SECONDS).build()
        UpdaterHttpClient(transport).use { client ->
            val response = client.send(
                HttpRequest.newBuilder(URI("http://updates.test:${server.address.port}/latest")).build(),
                BodyHandlers.ofString(),
            )
            assertEquals(200, response.statusCode())
            assertEquals("version: 1.2.3", response.body())
            assertEquals("/manifest", response.uri().path)
        }
    }

    @Test(timeout = 10_000)
    fun preservesRangeHeadersAndBinaryResponses() = withServer { server, base ->
        val bytes = byteArrayOf(0, 1, -1, 127)
        var range: String? = null
        server.createContext("/installer") {
            range = it.requestHeaders.getFirst("Range")
            it.responseHeaders.add("Content-Range", "bytes 0-3/100")
            it.sendResponseHeaders(206, bytes.size.toLong())
            it.responseBody.use { output -> output.write(bytes) }
        }
        UpdaterHttpClient().use { client ->
            val response = client.send(
                HttpRequest.newBuilder(URI("$base/installer")).header("Range", "bytes=0-3").build(),
                BodyHandlers.ofByteArray(),
            )
            assertEquals("bytes=0-3", range)
            assertEquals(206, response.statusCode())
            assertEquals("bytes 0-3/100", response.headers().firstValue("Content-Range").orElseThrow())
            assertContentEquals(bytes, response.body())
        }
    }

    @Test(timeout = 10_000)
    fun returnsInstallerStreamBeforeDownloadCompletes() = withServer { server, base ->
        val continueDownload = CountDownLatch(1)
        val bytes = ByteArray(1024 * 1024) { (it % 251).toByte() }
        server.createContext("/installer") {
            it.sendResponseHeaders(200, bytes.size.toLong())
            it.responseBody.use { output ->
                output.write(bytes, 0, 8192)
                output.flush()
                if (continueDownload.await(5, TimeUnit.SECONDS)) {
                    output.write(bytes, 8192, bytes.size - 8192)
                }
            }
        }
        try {
            UpdaterHttpClient().use { client ->
                val response = client.send(HttpRequest.newBuilder(URI("$base/installer")).build(), BodyHandlers.ofInputStream())
                response.body().use { stream ->
                    val first = stream.readNBytes(8192)
                    assertContentEquals(bytes.copyOfRange(0, 8192), first)
                    assertEquals(1L, continueDownload.count)
                    continueDownload.countDown()
                    assertContentEquals(bytes.copyOfRange(8192, bytes.size), stream.readAllBytes())
                }
            }
        } finally {
            continueDownload.countDown()
        }
    }

    @Test(timeout = 10_000)
    fun preservesHttpErrorsForNucleusToHandle() = withServer { server, base ->
        server.createContext("/missing") {
            it.sendResponseHeaders(404, -1)
            it.close()
        }
        UpdaterHttpClient().use { client ->
            val response = client.send(HttpRequest.newBuilder(URI("$base/missing")).build(), BodyHandlers.ofByteArray())
            assertEquals(404, response.statusCode())
            assertTrue(response.body().isEmpty())
        }
    }
}
