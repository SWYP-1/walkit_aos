package team.swyp.sdu.domain.model

/**
 * 산책 저장 결과 도메인 모델
 */
data class WalkSaveResult(
    val id: String,
    val imageUrl: String? = null,
    val createdAt: String? = null
) {
    companion object {
        /**
         * API DTO WalkSaveResponse를 도메인 WalkSaveResult로 변환
         */
        fun fromApiResponse(apiResponse: team.swyp.sdu.data.remote.walking.dto.WalkSaveResponse): WalkSaveResult {
            return WalkSaveResult(
                id = apiResponse.id,
                imageUrl = apiResponse.imageUrl,
                createdAt = apiResponse.createdAt
            )
        }
    }
}
