package com.novastore.novalicense

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class HttpUrlConnectionTransportTest {

    private lateinit var server: HttpServer
    private val capturedBody = AtomicReference<String>()

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/validate") { exchange ->
            capturedBody.set(String(exchange.requestBody.readBytes(), Charsets.UTF_8))
            val body = """{"data":{"valid":true}}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/api-invalid/validate") { exchange ->
            val body = """{"data":{"valid":false}}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/api-down/validate") { exchange ->
            exchange.sendResponseHeaders(500, -1L)
            exchange.close()
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun transport(): HttpUrlConnectionTransport = HttpUrlConnectionTransport()

    @Test
    fun validTrueFromServer() {
        val outcome = transport().isValid(
            "com.demo.game", "dev1", null, "http://127.0.0.1:${server.address.port}/api", 5_000,
        )
        assertEquals(true, (outcome as LicenseTransport.Outcome.Ok).valid)
    }

    @Test
    fun validFalseFromServer() {
        val outcome = transport().isValid(
            "com.demo.game", "dev1", null, "http://127.0.0.1:${server.address.port}/api-invalid", 5_000,
        )
        assertEquals(false, (outcome as LicenseTransport.Outcome.Ok).valid)
    }

    @Test
    fun serverErrorFailsWithoutThrowing() {
        val outcome = transport().isValid(
            "com.demo.game", "dev1", null, "http://127.0.0.1:${server.address.port}/api-down", 5_000,
        )
        assertTrue(outcome is LicenseTransport.Outcome.Failed)
    }

    @Test
    fun postsPackageNameDeviceIdAndApkSha1() {
        transport().isValid(
            "com.demo.game", "dev1", "AA:BB:00", "http://127.0.0.1:${server.address.port}/api", 5_000,
        )
        val body = capturedBody.get()
        assertTrue(body.contains("\"package_name\":\"com.demo.game\""))
        assertTrue(body.contains("\"device_id\":\"dev1\""))
        assertTrue(body.contains("\"apk_sha1\":\"AA:BB:00\""))
    }

    @Test
    fun apkSha1OmittedWhenEmpty() {
        transport().isValid(
            "com.demo.game", "dev1", null, "http://127.0.0.1:${server.address.port}/api", 5_000,
        )
        assertTrue(!capturedBody.get().contains("apk_sha1"))
    }

    @Test
    fun unreachableServerFails() {
        val outcome = transport().isValid(
            "com.demo.game", "dev1", null, "http://127.0.0.1:1/api/v1", 500,
        )
        assertTrue(outcome is LicenseTransport.Outcome.Failed)
    }
}