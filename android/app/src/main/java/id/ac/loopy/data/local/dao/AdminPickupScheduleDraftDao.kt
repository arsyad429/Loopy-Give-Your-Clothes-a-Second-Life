package id.ac.loopy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import id.ac.loopy.data.local.entity.AdminPickupScheduleDraftEntity

@Dao
internal interface AdminPickupScheduleDraftDao {
    @Query("SELECT * FROM admin_pickup_schedule_drafts WHERE adminId = :adminId AND donationId = :donationId")
    suspend fun find(adminId: String, donationId: String): AdminPickupScheduleDraftEntity?

    @Upsert
    suspend fun save(draft: AdminPickupScheduleDraftEntity)

    @Query("DELETE FROM admin_pickup_schedule_drafts WHERE adminId = :adminId AND donationId = :donationId")
    suspend fun delete(adminId: String, donationId: String)
}
