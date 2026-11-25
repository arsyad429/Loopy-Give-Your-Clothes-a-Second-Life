package id.ac.loopy.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "admin_pickup_schedule_drafts",
    primaryKeys = ["adminId", "donationId"],
)
internal data class AdminPickupScheduleDraftEntity(
    val adminId: String,
    val donationId: String,
    val dateMillis: Long?,
    val minutesOfDay: Int?,
)
