package id.ac.loopy.domain.model

enum class DonationStatus {
    PENDING_REVIEW, PICKUP_SCHEDULED, PICKED_UP, COMPLETED, REJECTED;

    /** Bantuan UI; backend tetap wajib memvalidasi transisi dan role di dalam transaksi. */
    fun canTransitionTo(next: DonationStatus): Boolean = when (this) {
        PENDING_REVIEW -> next == PICKUP_SCHEDULED || next == REJECTED
        PICKUP_SCHEDULED -> next == PICKED_UP
        PICKED_UP -> next == COMPLETED
        COMPLETED, REJECTED -> false
    }
}
