package id.ac.loopy.data.remote.dto

import id.ac.loopy.domain.model.AiResult
import id.ac.loopy.domain.model.ClothingCheck
import id.ac.loopy.domain.model.ClothingType

data class ClothingCheckDto(
    val checkId: String,
    val userId: String,
    val clothingPhoto: String,
    val clothingType: String,
    val aiResult: String,
    val aiDescription: String,
    val actionRecommendation: String?,
    val createdAt: String,
) {
    fun toDomain() = ClothingCheck(
        checkId = checkId,
        userId = userId,
        clothingPhoto = clothingPhoto,
        clothingType = ClothingType.entries.first { it.backendValue == clothingType },
        aiResult = AiResult.valueOf(aiResult),
        aiDescription = aiDescription,
        actionRecommendation = actionRecommendation,
        createdAt = createdAt,
    )
}
