package id.ac.loopy

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.loopy.data.local.LoopyDatabase
import id.ac.loopy.data.local.entity.PickupDraftEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
