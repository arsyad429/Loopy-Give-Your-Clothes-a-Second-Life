package id.ac.loopy.domain.repository

import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem

interface DonationRepository {
    suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest
    suspend fun listMyDonations(status: DonationStatus? = null, query: String? = null): List<DonationSummaryItem>
    suspend fun getDonationDetail(donationId: String): DonationRequest
}
