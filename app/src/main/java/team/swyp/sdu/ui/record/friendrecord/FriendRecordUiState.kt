package team.swyp.sdu.ui.record.friendrecord

import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.core.Result



/**
 * 좋아요 UI 상태
 */
data class LikeUiState(
    val count: Int,
    val isLiked: Boolean,
) {
    companion object {
        val EMPTY = LikeUiState(0, false)
    }
}

/**
 * 친구 기록 화면 UI 상태
 */
sealed interface FriendRecordUiState {
    /**
     * 로딩 중
     */
    data object Loading : FriendRecordUiState

    /**
     * 성공 상태
     */
    data class Success(
        val data: FollowerWalkRecord,
        val like: LikeUiState,
    ) : FriendRecordUiState

    /**
     * 팔로우하지 않은 사용자
     */
    data class NotFollowing(
        val message: String,
    ) : FriendRecordUiState

    /**
     * 산책 기록이 없는 경우
     */
    data class NoRecords(
        val message: String,
    ) : FriendRecordUiState

    /**
     * 에러 상태
     */
    data class Error(
        val message: String? = null,
    ) : FriendRecordUiState
}

