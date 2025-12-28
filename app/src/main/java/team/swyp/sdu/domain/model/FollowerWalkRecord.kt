package team.swyp.sdu.domain.model

/**
 * 팔로워 산책 기록 Domain 모델
 */
data class FollowerWalkRecord(
    val character: Character,
    val walkProgressPercentage: String,
    val walkId: Long,
    val createdDate: String,
    val stepCount: Int = 0,
    val totalDistance: Double = 0.0,
    val likeCount: Int = 0,
    val liked: Boolean = false
)



