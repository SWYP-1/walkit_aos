package swyp.team.walkit.domain.model

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
        fun toDomain(dto: swyp.team.walkit.data.remote.walking.dto.WalkSaveResponse): WalkSaveResult {
            return WalkSaveResult(
                id = dto.id,
                imageUrl = dto.imageUrl,
                createdAt = dto.createdAt
            )
        }
    }
}
