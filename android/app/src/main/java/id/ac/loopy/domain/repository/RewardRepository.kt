package id.ac.loopy.domain.repository

import id.ac.loopy.domain.model.Reward
import id.ac.loopy.domain.model.Redemption
import id.ac.loopy.domain.model.RedemptionResult
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    val userId: Flow<String?>
    suspend fun balance(): Int
    suspend fun catalog(): List<Reward>
    suspend fun history(): List<Redemption>
    suspend fun redeem(rewardId: String, requestKey: String): RedemptionResult
}
