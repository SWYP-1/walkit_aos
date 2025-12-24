package team.swyp.sdu.domain.model

/**
 * 팔로워 산책 기록 Domain 모델
 */
data class FollowerWalkRecord(
    val character: Character,
    val walkProgressPercentage: String? = null,
    val createdDate: String? = null,
    val stepCount: Int = 0,
    val totalDistance: Int = 0,
)



