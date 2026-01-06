package swyp.team.walkit.domain.model

data class Friend(
    val id: String,
    val nickname: String,
    val avatarUrl: String? = null,
)

/**
 * 사용자 검색 결과 도메인 모델
 */
data class UserSearchResult(
    val userId: Long,
    val imageName: String?,
    val nickname: String,
    val followStatus: FollowStatus,
)

/**
 * 사용자 요약 정보 도메인 모델
 */
data class UserSummary(
    val character: Character,
    val walkSummary: WalkSummary,
    val followStatus: FollowStatus = FollowStatus.EMPTY,
)


/**
 * 산책 요약 정보 도메인 모델
 */
data class WalkSummary(
    val totalWalkCount: Int,
    val totalWalkTimeMillis: Long,
)










