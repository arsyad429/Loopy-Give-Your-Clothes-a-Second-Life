package id.ac.loopy.feature.rewards

import id.ac.loopy.domain.model.Reward
import id.ac.loopy.domain.model.Redemption

data class RewardsUiState(
    val balance: Int? = null,
    val rewards: List<Reward> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val loaded: Boolean = false,
    val selected: Reward? = null,
    val pending: Boolean = false,
    val error: String? = null,
    val redeemError: String? = null,
    val message: String? = null,
)
