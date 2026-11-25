package id.ac.loopy.data.repository

import id.ac.loopy.data.mapper.toDomain
import id.ac.loopy.data.remote.api.RewardApi
import id.ac.loopy.data.remote.dto.RedeemRequestDto
import id.ac.loopy.domain.model.RedemptionResult
import id.ac.loopy.domain.repository.RewardRepository
import kotlinx.coroutines.flow.map

class RewardRepositoryImpl(private val api: RewardApi, private val auth: AuthRepositoryImpl) : RewardRepository {
    override val userId = auth.session.map { it?.user?.takeIf { user -> user.role == "USER" }?.userId }
    override suspend fun balance() = auth.withBearer { api.balance(it).balance }
    override suspend fun catalog() = auth.withBearer { api.catalog(it).items.map { item -> item.toDomain() } }
    override suspend fun history() = auth.withBearer { api.history(it).items.map { item -> item.toDomain() } }
    override suspend fun redeem(rewardId: String, requestKey: String): RedemptionResult = auth.withBearer {
        val result = api.redeem(it, requestKey, RedeemRequestDto(rewardId))
        RedemptionResult(result.redemption.toDomain(), result.remainingBalance, result.replayed)
    }
}
