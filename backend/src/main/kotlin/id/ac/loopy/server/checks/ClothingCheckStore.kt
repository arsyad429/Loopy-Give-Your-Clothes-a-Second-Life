package id.ac.loopy.server.checks

import id.ac.loopy.server.auth.LoopyPrincipal
import id.ac.loopy.server.auth.UserRole
import id.ac.loopy.server.database.queryOne
import id.ac.loopy.server.database.read
import id.ac.loopy.server.database.transaction
import id.ac.loopy.server.database.update
import id.ac.loopy.server.storage.PhotoStorage
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

sealed interface CheckClaim {
    data class Acquired(
        val ownerId: UUID,
        val requestKey: String,
        val claimId: UUID,
    ) : CheckClaim

    data class Completed(val checkId: UUID) : CheckClaim
    data object InProgress : CheckClaim
    data object PayloadMismatch : CheckClaim
}

class CheckClaimLostException : IllegalStateException("Reservasi pemeriksaan sudah berakhir.")

data class StoredClothingCheck(
    val checkId: UUID,
    val ownerId: UUID,
    val clothingType: String,
    val analysis: AiVisionResult,
    val createdAt: Instant,
)

class ClothingCheckStore(private val database: DataSource) {
    private val photos = PhotoStorage(database)
    private val keyFormat = Regex("[A-Za-z0-9._-]{1,128}")

    suspend fun claim(actor: LoopyPrincipal, key: String, hash: ByteArray): CheckClaim {
        require(keyFormat.matches(key)) { "Format Idempotency-Key tidak valid." }
        require(hash.size == 32) { "Request hash harus berupa SHA-256." }
        val claimId = UUID.randomUUID()

        return database.transaction { db ->
            val acquired = db.queryOne(
                """
                INSERT INTO clothing_check_requests AS r
                    (user_id, request_key, request_hash, claim_id, claimed_until)
                VALUES (?, ?, ?, ?, clock_timestamp() + INTERVAL '2 minutes')
                ON CONFLICT (user_id, request_key) DO UPDATE
                SET claim_id = EXCLUDED.claim_id,
                    claimed_until = clock_timestamp() + INTERVAL '2 minutes'
                WHERE r.request_hash = EXCLUDED.request_hash
                  AND r.check_id IS NULL
                  AND (r.claimed_until IS NULL OR r.claimed_until <= clock_timestamp())
                RETURNING claim_id
                """.trimIndent(),
                actor.userId, key, hash, claimId,
            ) { row -> row.getObject("claim_id", UUID::class.java) }

            if (acquired != null) {
                return@transaction CheckClaim.Acquired(actor.userId, key, acquired)
            }

            checkNotNull(db.queryOne(
                """
                SELECT request_hash, check_id
                FROM clothing_check_requests
                WHERE user_id = ? AND request_key = ?
                """.trimIndent(),
                actor.userId, key,
            ) { row ->
                val existingId = row.getObject("check_id", UUID::class.java)
                when {
                    !row.getBytes("request_hash").contentEquals(hash) -> CheckClaim.PayloadMismatch
                    existingId != null -> CheckClaim.Completed(existingId)
                    else -> CheckClaim.InProgress
                }
            })
        }
    }

    suspend fun complete(
        claim: CheckClaim.Acquired,
        clothingType: String,
        analysis: AiVisionResult,
        jpeg: ByteArray,
    ): UUID = database.transaction { db ->
        val checkId = UUID.randomUUID()

        db.update(
            """
            INSERT INTO clothing_checks
                (check_id, user_id, clothing_type, ai_result, ai_description, action_recommendation)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            checkId, claim.ownerId, clothingType, analysis.aiResult.name,
            analysis.aiDescription, analysis.actionRecommendation,
        )
        photos.insert(db, checkId, jpeg)

        val updated = db.update(
            """
            UPDATE clothing_check_requests
            SET check_id = ?, claim_id = NULL, claimed_until = NULL
            WHERE user_id = ? AND request_key = ? AND claim_id = ?
              AND check_id IS NULL AND claimed_until > clock_timestamp()
            """.trimIndent(),
            checkId, claim.ownerId, claim.requestKey, claim.claimId,
        )
        if (updated != 1) throw CheckClaimLostException()

        checkId
    }

    suspend fun release(claim: CheckClaim.Acquired) {
        database.read { db ->
            db.update(
                """
                UPDATE clothing_check_requests
                SET claim_id = NULL, claimed_until = NULL
                WHERE user_id = ? AND request_key = ? AND claim_id = ? AND check_id IS NULL
                """.trimIndent(),
                claim.ownerId, claim.requestKey, claim.claimId,
            )
        }
    }

    suspend fun findFor(actor: LoopyPrincipal, checkId: UUID): StoredClothingCheck? =
        database.read { db ->
            db.queryOne(
                """
                SELECT check_id, user_id, clothing_type, ai_result,
                       ai_description, action_recommendation, created_at
                FROM clothing_checks
                WHERE check_id = ? AND (? OR user_id = ?)
                """.trimIndent(),
                checkId, actor.role == UserRole.ADMIN, actor.userId,
            ) { row ->
                StoredClothingCheck(
                    checkId = row.getObject("check_id", UUID::class.java),
                    ownerId = row.getObject("user_id", UUID::class.java),
                    clothingType = row.getString("clothing_type"),
                    analysis = AiVisionResult(
                        AiResult.valueOf(row.getString("ai_result")),
                        row.getString("ai_description"),
                        row.getString("action_recommendation"),
                    ),
                    createdAt = row.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                )
            }
        }
}
