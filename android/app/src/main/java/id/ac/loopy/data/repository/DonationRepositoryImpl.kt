package id.ac.loopy.data.repository

import id.ac.loopy.data.mapper.toDomain
import id.ac.loopy.data.mapper.toDto
import id.ac.loopy.data.remote.api.DonationApi
import id.ac.loopy.domain.model.CreateDonationSubmission
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import id.ac.loopy.domain.repository.DonationRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class DonationRepositoryImpl(
    private val api: DonationApi,
    private val auth: AuthRepositoryImpl,
) : DonationRepository {
    private val submitMutex = Mutex()
    private var pending: PendingSubmission? = null

    private data class PendingSubmission(
        val userId: String,
        val submission: CreateDonationSubmission,
        val key: String = UUID.randomUUID().toString(),
    )

    override suspend fun createDonation(submission: CreateDonationSubmission): DonationRequest =
        submitMutex.withLock {
            val userId = auth.session.value?.user?.userId ?: error("Silakan masuk terlebih dahulu.")
            val previous = pending
            val current = if (previous != null && previous.userId == userId && previous.submission == submission) {
                previous
            } else {
                PendingSubmission(userId, submission)
            }
            pending = current

            auth.withBearer { bearer ->
                api.create(bearer, current.key, submission.toDto()).toDomain()
            }.also { pending = null }
        }

    override suspend fun listMyDonations(
        status: DonationStatus?,
        query: String?,
    ): List<DonationSummaryItem> = auth.withBearer { bearer ->
        api.listOwn(
            bearer = bearer,
            status = status?.name,
            query = query?.takeIf { it.isNotBlank() },
        ).items.map { it.toDomain() }
    }

    override suspend fun getDonationDetail(donationId: String): DonationRequest =
        auth.withBearer { bearer ->
            api.detail(bearer, donationId).toDomain()
        }
}
