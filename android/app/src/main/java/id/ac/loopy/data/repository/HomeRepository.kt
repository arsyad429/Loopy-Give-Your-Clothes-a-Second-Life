package id.ac.loopy.data.repository

import id.ac.loopy.data.mapper.toDomain
import id.ac.loopy.data.remote.api.HomeApi
import id.ac.loopy.domain.model.DonationSummaryItem

internal interface HomeRepository {
    suspend fun getPoints(): Int
    suspend fun getLatestDonation(): DonationSummaryItem?
}

internal class RemoteHomeRepository(
    private val api: HomeApi,
    private val auth: AuthRepositoryImpl,
) : HomeRepository {
    override suspend fun getPoints(): Int =
        auth.withBearer { api.points(it).balance }

    override suspend fun getLatestDonation(): DonationSummaryItem? =
        auth.withBearer { api.latestDonation(it).item?.toDomain() }
}
