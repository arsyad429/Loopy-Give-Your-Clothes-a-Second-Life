package id.ac.loopy.data.mapper

import id.ac.loopy.data.remote.dto.RewardDto
import id.ac.loopy.data.remote.dto.RedemptionDto
import id.ac.loopy.domain.model.Reward
import id.ac.loopy.domain.model.Redemption

fun RewardDto.toDomain() = Reward(rewardId, name, description, icon, requiredPoints, type, isActive)
fun RedemptionDto.toDomain() = Redemption(redemptionId, rewardId, rewardName, description, icon, type, pointsUsed, redeemedAt)
