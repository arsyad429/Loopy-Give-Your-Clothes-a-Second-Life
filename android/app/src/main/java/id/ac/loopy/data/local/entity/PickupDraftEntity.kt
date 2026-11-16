package id.ac.loopy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Draft perangkat ini saja; bukan pengajuan resmi di backend. Jangan simpan PII demo. */
@Entity(tableName = "pickup_drafts")
data class PickupDraftEntity(
    @PrimaryKey val draftId: String,
    val note: String,
    val updatedAt: Long,
)
