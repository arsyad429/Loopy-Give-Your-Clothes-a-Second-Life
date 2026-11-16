package id.ac.loopy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import id.ac.loopy.data.local.dao.PickupDraftDao
import id.ac.loopy.data.local.entity.PickupDraftEntity

@Database(entities = [PickupDraftEntity::class], version = 1, exportSchema = true)
abstract class LoopyDatabase : RoomDatabase() {
    abstract fun pickupDraftDao(): PickupDraftDao
    // TODO: tambahkan cache per user + migration saat skema berubah, hapus cache saat logout.
}
