package id.ac.loopy.server.storage

import id.ac.loopy.server.auth.LoopyPrincipal
import id.ac.loopy.server.auth.UserRole
import id.ac.loopy.server.checks.MAX_GROQ_JPEG_BYTES
import id.ac.loopy.server.database.queryOne
import id.ac.loopy.server.database.read
import id.ac.loopy.server.database.update
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class PhotoStorage(private val database: DataSource) {
    internal fun insert(db: Connection, checkId: UUID, jpeg: ByteArray) {
        require(jpeg.size in 3..MAX_GROQ_JPEG_BYTES) { "Ukuran JPEG tidak valid." }
        require(jpeg[0] == 0xFF.toByte() && jpeg[1] == 0xD8.toByte() && jpeg[2] == 0xFF.toByte()) {
            "Format foto harus JPEG."
        }

        db.update(
            "INSERT INTO clothing_check_photos (check_id, jpeg) VALUES (?, ?)",
            checkId, jpeg,
        )
    }

    suspend fun findFor(actor: LoopyPrincipal, checkId: UUID): ByteArray? =
        database.read { db ->
            db.queryOne(
                """
                SELECT p.jpeg
                FROM clothing_check_photos p
                JOIN clothing_checks c ON c.check_id = p.check_id
                WHERE c.check_id = ? AND (? OR c.user_id = ?)
                """.trimIndent(),
                checkId, actor.role == UserRole.ADMIN, actor.userId,
            ) { row -> row.getBytes("jpeg") }
        }
}
