package id.ac.loopy.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import id.ac.loopy.data.local.dao.AdminPickupScheduleDraftDao
import id.ac.loopy.data.local.dao.PickupDraftDao
import id.ac.loopy.data.local.entity.AdminPickupScheduleDraftEntity
import id.ac.loopy.data.local.entity.PickupDraftEntity

@Database(
    entities = [PickupDraftEntity::class, AdminPickupScheduleDraftEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class LoopyDatabase : RoomDatabase() {
    abstract fun pickupDraftDao(): PickupDraftDao
    internal abstract fun adminPickupScheduleDraftDao(): AdminPickupScheduleDraftDao
}
