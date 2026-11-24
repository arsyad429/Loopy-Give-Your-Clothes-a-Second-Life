package id.ac.loopy.domain.model

enum class AiResult {
    POTENTIALLY_ELIGIBLE,
    NEEDS_RECHECK,
    NOT_RECOMMENDED
}

data class ClothingCheck(
    val checkId: String,
    val userId: String,
    val clothingPhoto: String,
    val clothingType: ClothingType,
    val aiResult: AiResult,
    val aiDescription: String,
    val actionRecommendation: String? = null,
    val createdAt: String
)
