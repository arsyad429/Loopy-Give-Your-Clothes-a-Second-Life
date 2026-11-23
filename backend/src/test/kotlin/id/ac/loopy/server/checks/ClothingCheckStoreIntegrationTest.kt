package id.ac.loopy.server.checks

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import id.ac.loopy.server.auth.SessionStore
import id.ac.loopy.server.database.migrateDatabase
import id.ac.loopy.server.database.queryOne
import id.ac.loopy.server.database.read
import id.ac.loopy.server.database.update
import id.ac.loopy.server.storage.PhotoStorage
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClothingCheckStoreIntegrationTest {
    @Test
    fun sessionsOwnershipAndAtomicSave() = runBlocking {
        val url = System.getenv("LOOPY_B2_TEST_JDBC_URL").orEmpty()
        assumeTrue("Set LOOPY_B2_TEST_JDBC_URL untuk memakai PostgreSQL uji.", url.isNotBlank())

        HikariDataSource(HikariConfig().apply {
            jdbcUrl = url
            username = System.getenv("LOOPY_B2_TEST_DB_USER")
            password = System.getenv("LOOPY_B2_TEST_DB_PASSWORD")
        }).use { database ->
            migrateDatabase(database)

            val ownerId = UUID.randomUUID()
            val otherId = UUID.randomUUID()
            val adminId = UUID.randomUUID()
            val users = listOf(ownerId to "USER", otherId to "USER", adminId to "ADMIN")
            val sessions = SessionStore(database)
            val store = ClothingCheckStore(database)
            val photos = PhotoStorage(database)

            try {
                database.read { db ->
                    for ((id, role) in users) {
                        db.update(
                            "INSERT INTO users (user_id, name, email, role) VALUES (?, ?, ?, ?)",
                            id, "Loopy test", "$id@example.invalid", role,
                        )
                    }
                }

                val token = sessions.issue(ownerId)
                val owner = assertNotNull(sessions.verify(token))
                val other = assertNotNull(sessions.verify(sessions.issue(otherId)))
                val admin = assertNotNull(sessions.verify(sessions.issue(adminId)))
                assertNull(sessions.verify("token-palsu"))

                val hash = MessageDigest.getInstance("SHA-256").digest("payload-A".toByteArray())
                val key = "b2-test-key"
                val claim = assertIs<CheckClaim.Acquired>(store.claim(owner, key, hash))
                assertEquals(CheckClaim.InProgress, store.claim(owner, key, hash))

                val analysis = AiVisionResult(AiResult.POTENTIALLY_ELIGIBLE, "Terlihat utuh.", null)
                assertFailsWith<IllegalArgumentException> {
                    store.complete(claim, "TSHIRT", analysis, byteArrayOf())
                }
                val remaining = database.read { db ->
                    db.queryOne(
                        "SELECT count(*) FROM clothing_checks WHERE user_id = ?", ownerId,
                    ) { row -> row.getLong(1) }
                }
                assertEquals(0L, remaining)

                // Fixture penyimpanan. B3 menguji dekode gambar sungguhan.
                val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
                val checkId = store.complete(claim, "TSHIRT", analysis, jpeg)

                assertEquals(CheckClaim.Completed(checkId), store.claim(owner, key, hash))
                assertEquals(CheckClaim.PayloadMismatch, store.claim(owner, key, ByteArray(32)))
                assertEquals(analysis, store.findFor(owner, checkId)?.analysis)
                assertNotNull(store.findFor(admin, checkId))
                assertNull(store.findFor(other, checkId))
                assertContentEquals(jpeg, photos.findFor(owner, checkId))
                assertNull(photos.findFor(other, checkId))

                assertTrue(sessions.revoke(token))
                assertNull(sessions.verify(token))
            } finally {
                database.read { db ->
                    for ((id, _) in users) db.update("DELETE FROM users WHERE user_id = ?", id)
                }
            }
        }
    }
}
