package id.ac.loopy.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Separate debug database: never used as the authoritative balance of a signed-in account. */
@Entity(tableName = "demo_donations")
data class DemoDonation(
    @PrimaryKey val id: String,
    val quantity: Int,
    val status: String,
    val schedule: String,
    val address: String = "Jl. Contoh No. 12, Malang · alamat ilustrasi.",
    val updatedAt: Long,
)

@Entity(tableName = "demo_points")
data class DemoPointEntry(@PrimaryKey val reference: String, val points: Int)

@Entity(tableName = "demo_redemptions")
data class DemoRedemption(
    @PrimaryKey val requestKey: String,
    val rewardId: String,
    val name: String,
    val description: String,
    val icon: String,
    val pointsUsed: Int,
    val redeemedAt: Long,
)

@Dao
interface CompletionDemoDao {
    @Query("SELECT * FROM demo_donations ORDER BY updatedAt DESC, id")
    fun observeDonations(): Flow<List<DemoDonation>>
    @Query("SELECT COALESCE(SUM(points), 0) FROM demo_points")
    fun observeBalance(): Flow<Int>
    @Query("SELECT * FROM demo_redemptions ORDER BY redeemedAt DESC, requestKey")
    fun observeRedemptions(): Flow<List<DemoRedemption>>
    @Query("SELECT COUNT(*) FROM demo_donations")
    suspend fun donationCount(): Int
    @Query("SELECT * FROM demo_donations WHERE id=:id")
    suspend fun donation(id: String): DemoDonation?
    @Query("SELECT COALESCE(SUM(points), 0) FROM demo_points")
    suspend fun balance(): Int
    @Query("SELECT * FROM demo_redemptions WHERE requestKey=:key")
    suspend fun redemption(key: String): DemoRedemption?
    @Insert suspend fun insertDonation(item: DemoDonation)
    @Insert suspend fun insertPoints(item: DemoPointEntry)
    @Insert suspend fun insertRedemption(item: DemoRedemption)
    @Query("UPDATE demo_donations SET status=:target, updatedAt=:at WHERE id=:id AND status=:expected")
    suspend fun advance(id: String, expected: String, target: String, at: Long): Int
}

@Database(entities = [DemoDonation::class, DemoPointEntry::class, DemoRedemption::class], version = 1, exportSchema = true)
abstract class CompletionDemoDatabase : RoomDatabase() {
    abstract fun completionDao(): CompletionDemoDao
}
