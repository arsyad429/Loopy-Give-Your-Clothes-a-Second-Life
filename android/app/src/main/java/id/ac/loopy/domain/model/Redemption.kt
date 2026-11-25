package id.ac.loopy.domain.model

data class Redemption(
    val redemptionId: String, val rewardId: String, val rewardName: String,
    val description: String, val icon: String, val type: String,
    val pointsUsed: Int, val redeemedAt: String,
)

data class RedemptionResult(val redemption: Redemption, val remainingBalance: Int, val replayed: Boolean)
