package id.ac.loopy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.ac.loopy.data.local.entity.PickupDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PickupDraftDao {
    @Query("SELECT * FROM pickup_drafts ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<PickupDraftEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: PickupDraftEntity)
    @Query("DELETE FROM pickup_drafts")
    suspend fun clear()
}
