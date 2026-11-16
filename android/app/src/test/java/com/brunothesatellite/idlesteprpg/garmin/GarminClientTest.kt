package com.brunothesatellite.idlesteprpg.garmin

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Base64

class GarminClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client() = GarminClient(server.url("/").toString().trimEnd('/'))

    private fun jwt(exp: Long): String {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"exp":$exp}""".toByteArray())
        return "header.$payload.signature"
    }

    @Test
    fun tokenIsReturnedAndExpiryParsed() = runBlocking {
        val exp = System.currentTimeMillis() / 1000L + 3_600L
        val token = jwt(exp)
        server.enqueue(MockResponse().setBody(token))
        val result = client().obtainToken("GCDC_test")
        assertEquals(token, result.value)
        assertTrue(result.expiresAtEpochSeconds >= exp)
        val request = server.takeRequest()
        assertEquals("ApiKey GCDC_test", request.getHeader("Authorization"))
        assertTrue(request.path!!.contains("personal-api-key/token"))
    }

    @Test
    fun invalidKeyThrows401() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))
        val error = runCatching { client().obtainToken("bad") }.exceptionOrNull()
        assertTrue(error is GarminException)
        assertEquals(401, (error as GarminException).code)
    }

    @Test
    fun stepsAreParsed() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"calendarDate":"2026-11-16","totalSteps":5000,"totalDistance":4200.0}]"""
            )
        )
        val days = client().steps(
            "token",
            LocalDate.of(2026, 11, 10),
            LocalDate.of(2026, 11, 16)
        )
        assertEquals(1, days.size)
        assertEquals("2026-11-16", days.first().date)
        assertEquals(5000, days.first().steps)
        assertTrue(server.takeRequest().path!!.contains("2026-11-10/2026-11-16"))
    }

    @Test
    fun emptyStepsReturnEmptyList() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        val days = client().steps("token", LocalDate.now(), LocalDate.now())
        assertTrue(days.isEmpty())
    }

    @Test
    fun rateLimitCarriesRetryAfter() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "120")
                .setBody("too many")
        )
        val error = runCatching {
            client().steps("token", LocalDate.now(), LocalDate.now())
        }.exceptionOrNull()
        assertTrue(error is GarminException)
        assertEquals(429, (error as GarminException).code)
        assertEquals(120L, error.retryAfterSeconds)
    }

    @Test
    fun malformedResponseIsRejected() = runBlocking {
        server.enqueue(MockResponse().setBody("not json"))
        val error = runCatching {
            client().steps("token", LocalDate.now(), LocalDate.now())
        }.exceptionOrNull()
        assertTrue(error is GarminException)
    }
}
