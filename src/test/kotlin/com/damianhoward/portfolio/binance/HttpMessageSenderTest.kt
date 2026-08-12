package com.damianhoward.portfolio.binance

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

/**
 * Exercises the real Apache HttpClient against a loopback `HttpServer`, so the
 * request building, response handling, the `HttpRequestFailed` error path, and
 * the default-client factory are all covered without mocking HttpClient internals.
 */
class HttpMessageSenderTest {
    private lateinit var server: HttpServer
    private var baseUrl = ""

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange ->
            val bytes = "{\"result\":\"ok\"}".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/unauthorized") { exchange ->
            val bytes = "denied".toByteArray()
            exchange.sendResponseHeaders(401, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @AfterEach
    fun tearDown() = server.stop(0)

    @Test
    fun `GET returns the response body on success`() {
        HttpMessageSender().use { sender ->
            assertEquals("{\"result\":\"ok\"}", sender.sendGetMessage("$baseUrl/ok", "key123"))
        }
    }

    @Test
    fun `non-2xx status throws HttpRequestFailed with the query-stripped endpoint`() {
        HttpMessageSender().use { sender ->
            val ex =
                assertThrows(HttpRequestFailed::class.java) {
                    sender.sendGetMessage("$baseUrl/unauthorized?signature=secret", "key123")
                }
            assertEquals(401, ex.status)
            assertEquals("127.0.0.1/unauthorized", ex.endpoint)
        }
    }
}
