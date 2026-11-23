package id.ac.loopy.server.checks

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AiVisionGatewayTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val model = "qwen/qwen3.8-27b"

    @Test
    fun sendsOneImageAndReadsAllThreeCategories() = runBlocking {
        for (category in AiResult.entries) {
            val recommendation = if (category == AiResult.NOT_RECOMMENDED) {
                "Pertimbangkan perbaikan atau daur ulang."
            } else {
                null
            }
            val engine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("https://api.groq.com/openai/v1/chat/completions", request.url.toString())
                assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])

                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals(model, body.getValue("model").jsonPrimitive.content)

                val format = body.getValue("response_format").jsonObject
                assertEquals("json_schema", format.getValue("type").jsonPrimitive.content)
                val schema = format.getValue("json_schema").jsonObject
                assertEquals("true", schema.getValue("strict").jsonPrimitive.content)

                val content = body.getValue("messages").jsonArray.single()
                    .jsonObject.getValue("content").jsonArray
                assertEquals(2, content.size)
                assertTrue(content[0].jsonObject.getValue("text").jsonPrimitive.content.contains("TSHIRT"))
                val imageUrl = content[1].jsonObject.getValue("image_url")
                    .jsonObject.getValue("url").jsonPrimitive.content
                assertTrue(imageUrl.startsWith("data:image/jpeg;base64,"))

                respond(
                    envelope(resultJson(category.name, recommendation)),
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

            HttpClient(engine).use { client ->
                val result = GroqVisionGateway(client, "test-key", model).analyze(jpeg, "TSHIRT")
                assertEquals(category, result.aiResult)
                assertEquals(recommendation, result.actionRecommendation)
            }
        }
    }

    @Test
    fun rejectsInvalidOutputInJsonObjectMode() = runBlocking {
        val cases = listOf(
            "bukan JSON",
            envelope("{"),
            envelope(resultJson("UNKNOWN")),
            envelope(resultJson("POTENTIALLY_ELIGIBLE", description = " ")),
            envelope(resultJson("NOT_RECOMMENDED")),
            envelope(resultJson("NEEDS_RECHECK"), finishReason = "length"),
        )

        for (response in cases) {
            val engine = MockEngine { request ->
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                val format = body.getValue("response_format").jsonObject
                assertEquals("json_object", format.getValue("type").jsonPrimitive.content)
                respond(response)
            }

            HttpClient(engine).use { client ->
                val gateway = GroqVisionGateway(client, "test-key", model, GroqResponseMode.JSON_OBJECT)
                val error = assertFailsWith<AiVisionException> { gateway.analyze(jpeg, "TSHIRT") }
                assertEquals(AiVisionFailure.INVALID_RESPONSE, error.failure)
            }
        }
    }

    @Test
    fun rateLimitReturnsRetryAfterWithoutResending() = runBlocking {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond(
                content = "{}",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.RetryAfter, "3"),
            )
        }

        HttpClient(engine).use { client ->
            val gateway = GroqVisionGateway(client, "test-key", model)
            val error = assertFailsWith<AiVisionException> { gateway.analyze(jpeg, "TSHIRT") }
            assertEquals(AiVisionFailure.RATE_LIMITED, error.failure)
            assertEquals(429, error.providerStatus)
            assertEquals(3L, error.retryAfterSeconds)
            assertEquals(1, requests)
        }
    }

    @Test
    fun requestTimeoutReturnsTypedError() = runBlocking {
        val engine = MockEngine { request -> throw HttpRequestTimeoutException(request) }

        HttpClient(engine).use { client ->
            val gateway = GroqVisionGateway(client, "test-key", model)
            val error = assertFailsWith<AiVisionException> { gateway.analyze(jpeg, "TSHIRT") }
            assertEquals(AiVisionFailure.TIMEOUT, error.failure)
        }
    }

    private fun resultJson(
        category: String,
        recommendation: String? = null,
        description: String = "Kondisi pakaian terlihat pada foto.",
    ): String = buildJsonObject {
        put("aiResult", category)
        put("aiDescription", description)
        put("actionRecommendation", recommendation?.let { JsonPrimitive(it) } ?: JsonNull)
    }.toString()

    private fun envelope(content: String, finishReason: String = "stop"): String =
        buildJsonObject {
            putJsonArray("choices") {
                addJsonObject {
                    put("finish_reason", finishReason)
                    putJsonObject("message") { put("content", content) }
                }
            }
        }.toString()
}

class GroqVisionSmokeTest {
    @Test
    fun analyzesOneLocalJpeg() = runBlocking {
        val path = System.getenv("GROQ_SMOKE_IMAGE").orEmpty()
        assumeTrue("Set GROQ_SMOKE_IMAGE untuk menjalankan tes langsung.", path.isNotBlank())

        val photo = File(path)
        require(photo.isFile && photo.length() in 3L..MAX_GROQ_JPEG_BYTES.toLong()) {
            "Gunakan file JPEG lokal berukuran maksimal 2 MiB."
        }
        val clothingType = System.getenv("GROQ_SMOKE_CLOTHING_TYPE")
            ?.trim()?.takeIf { it.isNotEmpty() } ?: "TSHIRT"

        createGroqHttpClient().use { client ->
            val result = GroqVisionGateway.fromEnvironment(client).analyze(photo.readBytes(), clothingType)
            assertTrue(result.aiDescription.isNotBlank())
            println("Groq berhasil mengembalikan kategori ${result.aiResult}.")
        }
    }
}
