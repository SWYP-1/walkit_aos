package team.swyp.sdu.ui.record.friendrecord

import team.swyp.sdu.domain.model.FollowerWalkRecord

/**
 * 친구 기록 UI 상태
 */
sealed interface FriendRecordUiState {

    /**
     * 로딩 중
     */
    data object Loading : FriendRecordUiState

    /**
     * 성공 (데이터 로드됨)
     */
    data class Success(
        val data: FollowerWalkRecord,
        val like: LikeUiState,
        val processedLottieJson: String? = null, // Lottie 캐릭터 JSON 추가
    ) : FriendRecordUiState

    /**
     * 팔로우하지 않은 사용자
     */
    data class NotFollowing(val message: String) : FriendRecordUiState

    /**
     * 산책 기록 없음
     */
    data class NoRecords(val message: String) : FriendRecordUiState

    /**
     * 에러 발생
     */
    data class Error(val message: String?) : FriendRecordUiState
}

/**
 * 좋아요 UI 상태
 */
data class LikeUiState(
    val count: Int = 0,
    val isLiked: Boolean = false,
)