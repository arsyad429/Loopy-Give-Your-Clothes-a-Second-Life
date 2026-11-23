package id.ac.loopy.server.auth

import id.ac.loopy.server.database.queryOne
import id.ac.loopy.server.database.read
import id.ac.loopy.server.database.update
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.sql.DataSource

class SessionStore(private val database: DataSource) {
    private val random = SecureRandom()
    private val tokenFormat = Regex("[A-Za-z0-9_-]{43}")

    /** Call after the account has been authenticated. */
    suspend fun issue(userId: UUID): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        database.read { db ->
            db.update(
                """
                INSERT INTO user_sessions (token_hash, user_id, expires_at)
                VALUES (?, ?, clock_timestamp() + INTERVAL '30 minutes')
                """.trimIndent(),
                hash(token), userId,
            )
        }
        return token
    }

    suspend fun verify(token: String): LoopyPrincipal? {
        if (!tokenFormat.matches(token)) return null

        return database.read { db ->
            db.queryOne(
                """
                SELECT u.user_id, u.role
                FROM user_sessions s
                JOIN users u ON u.user_id = s.user_id
                WHERE s.token_hash = ? AND s.expires_at > clock_timestamp()
                """.trimIndent(),
                hash(token),
            ) { row ->
                LoopyPrincipal(
                    row.getObject("user_id", UUID::class.java),
                    UserRole.valueOf(row.getString("role")),
                )
            }
        }
    }

    suspend fun revoke(token: String): Boolean {
        if (!tokenFormat.matches(token)) return false
        return database.read { db ->
            db.update("DELETE FROM user_sessions WHERE token_hash = ?", hash(token)) == 1
        }
    }

    private fun hash(token: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.US_ASCII))
}
