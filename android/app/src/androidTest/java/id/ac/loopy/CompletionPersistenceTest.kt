package id.ac.loopy

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import id.ac.loopy.data.local.CompletionDemoDatabase
import id.ac.loopy.data.repository.CompletionDemoRepository
import id.ac.loopy.domain.model.DonationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CompletionPersistenceTest {
    @Test fun completionCreditsOnceAndRedemptionSurvivesReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "completion-test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, CompletionDemoDatabase::class.java, name).build()
        var db = open()
        try {
            var repo = CompletionDemoRepository(db)
            repo.prepare()
            assertEquals(20, db.completionDao().balance())
            repo.advance("LP021", DonationStatus.PICKED_UP)
            assertEquals(20, db.completionDao().balance())
            repo.advance("LP021", DonationStatus.COMPLETED)
            assertEquals(30, db.completionDao().balance())
            assertTrue(runCatching { repo.advance("LP021", DonationStatus.COMPLETED) }.isFailure)
            repo.redeem("voucher-demo", "persistent-request")
            repo.redeem("voucher-demo", "persistent-request")
            assertEquals(0, db.completionDao().balance())
            assertTrue(runCatching { repo.redeem("eco-supporter-badge", "insufficient-request") }.isFailure)
            db.close()
            db = open()
            repo = CompletionDemoRepository(db)
            repo.prepare()
            assertEquals(0, db.completionDao().balance())
            assertEquals("COMPLETED", db.completionDao().donation("LP021")?.status)
            assertEquals("voucher-demo", db.completionDao().redemption("persistent-request")?.rewardId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun failedLedgerInsertRollsBackCompletionAndRedemption() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, CompletionDemoDatabase::class.java).build()
        try {
            val repo = CompletionDemoRepository(db)
            repo.prepare()
            repo.advance("LP021", DonationStatus.PICKED_UP)
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_points BEFORE INSERT ON demo_points BEGIN SELECT RAISE(ABORT, 'test ledger failure'); END")
            assertTrue(runCatching { repo.advance("LP021", DonationStatus.COMPLETED) }.isFailure)
            assertEquals("PICKED_UP", db.completionDao().donation("LP021")?.status)
            assertEquals(20, db.completionDao().balance())
            assertTrue(runCatching { repo.redeem("eco-supporter-badge", "rollback-request") }.isFailure)
            assertNull(db.completionDao().redemption("rollback-request"))
            assertEquals(20, db.completionDao().balance())
        } finally { db.close() }
    }
}
