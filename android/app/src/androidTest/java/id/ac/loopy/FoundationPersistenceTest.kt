package id.ac.loopy

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.loopy.data.local.LoopyDatabase
import id.ac.loopy.data.local.entity.AdminPickupScheduleDraftEntity
import id.ac.loopy.data.local.entity.PickupDraftEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationPersistenceTest {
    @Test fun draftSurvivesDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "foundation-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, LoopyDatabase::class.java, name).build()
        try {
            val first = open()
            try { first.pickupDraftDao().upsert(PickupDraftEntity("test", "no PII", 1L)) }
            finally { first.close() }
            val second = open()
            try { assertEquals("test", second.pickupDraftDao().observeDrafts().first().single().draftId) }
            finally { second.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun upgradePreservesLegacyDraftsAndPersistsAdminSchedulesSeparately() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "admin-schedule-draft-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, LoopyDatabase::class.java, name).build()
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL("CREATE TABLE pickup_drafts (draftId TEXT NOT NULL PRIMARY KEY, note TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO pickup_drafts VALUES ('legacy', 'Draft demo lama', 1)")
                db.version = 1
            }

            val dateOnly = AdminPickupScheduleDraftEntity("admin-a", "donation-a", 1_800_000_000_000L, null)
            val timeOnly = AdminPickupScheduleDraftEntity("admin-b", "donation-a", null, 570)
            val first = open()
            try {
                assertEquals("legacy", first.pickupDraftDao().observeDrafts().first().single().draftId)
                first.adminPickupScheduleDraftDao().save(dateOnly)
                first.adminPickupScheduleDraftDao().save(timeOnly)
            } finally {
                first.close()
            }

            val second = open()
            try {
                val drafts = second.adminPickupScheduleDraftDao()
                assertNull(drafts.find("admin-a", "donation-b"))
                assertEquals(dateOnly, drafts.find("admin-a", "donation-a"))
                assertEquals(timeOnly, drafts.find("admin-b", "donation-a"))

                val complete = dateOnly.copy(minutesOfDay = 630)
                drafts.save(complete)
                assertEquals(complete, drafts.find("admin-a", "donation-a"))
                drafts.delete("admin-a", "donation-a")
                assertNull(drafts.find("admin-a", "donation-a"))
                assertEquals(timeOnly, drafts.find("admin-b", "donation-a"))
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
