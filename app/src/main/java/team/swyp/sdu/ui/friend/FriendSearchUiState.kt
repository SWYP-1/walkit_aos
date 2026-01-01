package team.swyp.sdu.ui.friend

import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.UserSummary

/**
 * 친구 검색 상세 화면 UI 상태
 */
sealed interface FriendSearchUiState {
    /**
     * 로딩 중
     */
    data object Loading : FriendSearchUiState

    /**
     * 성공 상태
     */
    data class Success(
        val data: UserSummary,
    ) : FriendSearchUiState

    /**
     * 에러 상태
     */
    data class Error(
        val message: String? = null,
    ) : FriendSearchUiState
}





