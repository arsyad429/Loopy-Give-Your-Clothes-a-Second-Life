package id.ac.loopy.data.repository

import id.ac.loopy.data.mapper.toDomain
import id.ac.loopy.data.remote.api.AdminApi
import id.ac.loopy.data.remote.dto.AdvanceDonationRequest
import id.ac.loopy.data.remote.dto.RejectDonationRequest
import id.ac.loopy.data.remote.dto.ScheduleDonationRequest
import id.ac.loopy.domain.model.DonationRequest
import id.ac.loopy.domain.model.DonationStatus
import id.ac.loopy.domain.model.DonationSummaryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class AdminDonationPage(
    val items: List<DonationSummaryItem>,
    val nextCursor: String?,
)

internal class AdminRepositoryImpl(
    private val api: AdminApi,
    private val auth: AuthRepositoryImpl,
) {
    suspend fun summary() = auth.withBearer(api::summary)

    suspend fun list(status: DonationStatus, query: String, cursor: String?): AdminDonationPage =
        auth.withBearer { bearer ->
            val response = api.list(bearer, status.name, query.trim().ifBlank { null }, cursor)
            AdminDonationPage(response.items.map { it.toDomain() }, response.nextCursor)
        }

    suspend fun detail(id: String): DonationRequest =
        auth.withBearer { api.detail(it, id).toDomain() }

    suspend fun schedule(donation: DonationRequest, at: Long): DonationRequest {
        val request = scheduleDonationRequest(at, donation.version)
        return auth.withBearer { api.schedule(it, donation.donationId, request).toDomain() }
    }

    suspend fun reject(donation: DonationRequest, reason: String): DonationRequest =
        auth.withBearer {
            api.reject(it, donation.donationId, RejectDonationRequest(reason.trim(), donation.version)).toDomain()
        }

    suspend fun advance(donation: DonationRequest, status: DonationStatus): DonationRequest =
        auth.withBearer {
            api.advance(it, donation.donationId, AdvanceDonationRequest(status.name, donation.version)).toDomain()
        }
}

internal fun scheduleDonationRequest(at: Long, expectedVersion: Long): ScheduleDonationRequest {
    val timezone = TimeZone.getTimeZone("Asia/Jakarta")
    fun format(pattern: String) = SimpleDateFormat(pattern, Locale.ROOT).apply {
        timeZone = timezone
    }.format(Date(at))
    return ScheduleDonationRequest(format("yyyy-MM-dd"), format("HH:mm"), timezone.id, expectedVersion)
}
