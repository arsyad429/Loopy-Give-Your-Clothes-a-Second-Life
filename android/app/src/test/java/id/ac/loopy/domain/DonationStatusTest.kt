package id.ac.loopy.domain

import id.ac.loopy.domain.model.DonationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DonationStatusTest {
    @Test fun `PRD permits exactly four transitions`() {
        val allowed = setOf(
            DonationStatus.PENDING_REVIEW to DonationStatus.PICKUP_SCHEDULED,
            DonationStatus.PENDING_REVIEW to DonationStatus.REJECTED,
            DonationStatus.PICKUP_SCHEDULED to DonationStatus.PICKED_UP,
            DonationStatus.PICKED_UP to DonationStatus.COMPLETED,
        )
        DonationStatus.entries.forEach { from ->
            DonationStatus.entries.forEach { to ->
                assertEquals("$from -> $to", (from to to) in allowed, from.canTransitionTo(to))
            }
        }
    }
}
