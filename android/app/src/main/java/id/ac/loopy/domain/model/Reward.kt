package id.ac.loopy.domain.model

data class Reward(
    val rewardId: String, val name: String, val description: String, val icon: String,
    val requiredPoints: Int, val type: String, val isActive: Boolean,
)
