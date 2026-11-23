package id.ac.loopy.server.checks

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import id.ac.loopy.server.auth.SessionStore
import id.ac.loopy.server.backendModule
import id.ac.loopy.server.database.migrateDatabase
import id.ac.loopy.server.database.read
import id.ac.loopy.server.database.update
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClothingCheckRoutesTest {
    @Test
    fun createsReplaysAndProtectsCheckAndPhoto() = runBlocking {
        val url = System.getenv("LOOPY_B2_TEST_JDBC_URL").orEmpty()
        assumeTrue("Set LOOPY_B2_TEST_JDBC_URL untuk tes B3.", url.isNotBlank())

        val live = System.getenv("LOOPY_B3_LIVE") == "1"
        val image = if (live) {
            val path = checkNotNull(System.getenv("GROQ_SMOKE_IMAGE")) { "Set GROQ_SMOKE_IMAGE." }
            File(path).readBytes()
        } else {
            fixtureJpeg()
        }

        val clothingType = if (live) {
            System.getenv("GROQ_SMOKE_CLOTHING_TYPE") ?: "TSHIRT"
        } else {
            "TSHIRT"
        }
        val changedType = if (clothingType == "SHIRT") "TSHIRT" else "SHIRT"
        val calls = AtomicInteger()

        HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = System.getenv("LOOPY_B2_TEST_DB_USER")
            password = System.getenv("LOOPY_B2_TEST_DB_PASSWORD")
        }).use { database ->
            migrateDatabase(database)

            val ownerId = UUID.randomUUID()
            val otherId = UUID.randomUUID()
            val users = listOf(ownerId, otherId)
            val sessions = SessionStore(database)

            try {
                database.read { db ->
                    for (id in users) {
                        db.update(
                            "INSERT INTO users (user_id, name, email) VALUES (?, ?, ?)",
                            id, "Loopy test", "$id@example.invalid",
                        )
                    }
                }

                val ownerToken = sessions.issue(ownerId)
                val otherToken = sessions.issue(otherId)

                val providerClient = if (live) createGroqHttpClient() else null
                providerClient.use { groqClient ->
                    val real = groqClient?.let { GroqVisionGateway.fromEnvironment(it) }
                    val gateway = object : AiVisionGateway {
                        override suspend fun analyze(jpeg: ByteArray, clothingType: String): AiVisionResult {
                            calls.incrementAndGet()
                            return real?.analyze(jpeg, clothingType)
                                ?: AiVisionResult(AiResult.POTENTIALLY_ELIGIBLE, "Pakaian terlihat utuh.", null)
                        }
                    }

                    testApplication(parentCoroutineContext = Dispatchers.Default) {
                        application { backendModule(database, gateway) }

                        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/checks/${UUID.randomUUID()}").status)

                        val invalid = client.upload(ownerToken, "invalid-jpeg", clothingType, byteArrayOf(1, 2, 3))
                        assertEquals(HttpStatusCode.UnprocessableEntity, invalid.status)
                        assertEquals(0, calls.get())

                        val created = client.upload(ownerToken, "check-b3", clothingType, image)
                        assertEquals(HttpStatusCode.Created, created.status)

                        val json = Json.parseToJsonElement(created.bodyAsText()).jsonObject
                        val id = json.getValue("checkId").jsonPrimitive.content
                        val photo = json.getValue("clothingPhoto").jsonPrimitive.content
                        val category = json.getValue("aiResult").jsonPrimitive.content
                        assertTrue(AiResult.entries.any { it.name == category })
                        assertEquals(clothingType, json.getValue("clothingType").jsonPrimitive.content)

                        val afterCreate = calls.get()
                        assertTrue(afterCreate >= 1)

                        val replay = client.upload(ownerToken, "check-b3", clothingType, image)
                        assertEquals(HttpStatusCode.OK, replay.status)
                        val replayId = Json.parseToJsonElement(replay.bodyAsText())
                            .jsonObject.getValue("checkId").jsonPrimitive.content
                        assertEquals(id, replayId)
                        assertEquals(afterCreate, calls.get())

                        assertEquals(
                            HttpStatusCode.Conflict,
                            client.upload(ownerToken, "check-b3", changedType, image).status,
                        )
                        assertEquals(HttpStatusCode.OK, client.get("/v1/checks/$id") { bearerAuth(ownerToken) }.status)
                        assertEquals(HttpStatusCode.NotFound, client.get("/v1/checks/$id") { bearerAuth(otherToken) }.status)
                        assertEquals(HttpStatusCode.OK, client.get(photo) { bearerAuth(ownerToken) }.status)
                        assertEquals(HttpStatusCode.NotFound, client.get(photo) { bearerAuth(otherToken) }.status)

                        if (live) println("B3 menyimpan kategori dari Groq: $category")
                    }
                }
            } finally {
                database.read { db ->
                    for (id in users) db.update("DELETE FROM users WHERE user_id = ?", id)
                }
            }
        }
    }

    private suspend fun HttpClient.upload(token: String, key: String, type: String, bytes: ByteArray) =
        submitFormWithBinaryData(
            url = "/v1/checks",
            formData = formData {
                append("clothingType", type)
                append("image", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"check.jpg\"")
                })
            },
        ) {
            bearerAuth(token)
            header("Idempotency-Key", key)
        }

    private fun fixtureJpeg(): ByteArray {
        val image = BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB)
        return ByteArrayOutputStream().use { output ->
            check(ImageIO.write(image, "jpeg", output))
            output.toByteArray()
        }
    }
}
