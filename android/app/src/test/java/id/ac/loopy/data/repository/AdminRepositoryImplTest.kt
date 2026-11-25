package id.ac.loopy.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AdminRepositoryImplTest {
    @Test
    fun scheduleUsesJakartaDateAfterUtcMidnightBoundaryAndSendsVersion() {
        val at = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 25, 18, 45)
        }.timeInMillis

        val request = scheduleDonationRequest(at, expectedVersion = 7)

        assertEquals("2026-11-26", request.scheduledDate)
        assertEquals("01:45", request.scheduledTime)
        assertEquals("Asia/Jakarta", request.timezone)
        assertEquals(7L, request.expectedVersion)
    }
}
