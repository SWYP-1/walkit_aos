package swyp.team.walkit.ui.friend

import swyp.team.walkit.domain.model.FollowerWalkRecord
import swyp.team.walkit.domain.model.UserSummary

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
        val processedLottieJson: String? = null, // Lottie 캐릭터 JSON 추가
    ) : FriendSearchUiState

    /**
     * 에러 상태
     */
    data class Error(
        val message: String? = null,
    ) : FriendSearchUiState
}





