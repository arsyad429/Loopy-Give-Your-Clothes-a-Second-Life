package id.ac.loopy.data.remote.dto

data class RewardDto(
    val rewardId: String, val name: String, val description: String, val icon: String,
    val requiredPoints: Int, val type: String, val isActive: Boolean,
)
data class RedemptionDto(
    val redemptionId: String, val rewardId: String, val rewardName: String,
    val description: String, val icon: String, val type: String,
    val pointsUsed: Int, val redeemedAt: String,
)
data class RewardListDto(val items: List<RewardDto>)
data class RedemptionListDto(val items: List<RedemptionDto>)
data class PointsBalanceDto(val balance: Int)
data class RedeemRequestDto(val rewardId: String)
data class RedemptionResultDto(val redemption: RedemptionDto, val remainingBalance: Int, val replayed: Boolean)
