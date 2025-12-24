package team.swyp.sdu.ui.record.friendrecord

import team.swyp.sdu.domain.model.FollowerWalkRecord

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
        val walkId: Long? = null, // 좋아요 기능을 위한 walkId (별도 API로 받아올 예정)
        val like: LikeUiState = LikeUiState.EMPTY,
    ) : FriendRecordUiState

    /**
     * 에러 상태
     */
    data class Error(
        val message: String? = null,
    ) : FriendRecordUiState
}

