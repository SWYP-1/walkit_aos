package team.swyp.sdu.ui.dressroom

import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem

/**
 * DressingRoom UI 상태
 */
sealed interface DressingRoomUiState {

    /**
     * 로딩 중
     */
    data object Loading : DressingRoomUiState

    /**
     * 성공 (아이템 목록 로드됨)
     *
     * @param items 코스메틱 아이템 목록
     * @param selectedItemId 현재 선택된 아이템 ID
     * @param currentPosition 현재 필터링된 위치 (HEAD, BODY, FEET, null이면 전체)
     * @param availablePositions 사용 가능한 위치 목록
     */
    data class Success(
        val character: Character? = null,
        val items: List<CosmeticItem> = emptyList(),
        val selectedItemId: Int? = null,
        val selectedItemIdSet : LinkedHashSet<Int> = LinkedHashSet(),
        val currentPosition: String? = null,
        val myPoint : Int = 0,
        val processedLottieJson: String? = null,  // Lottie 미리보기 JSON
        val showOwnedOnly: Boolean = false,  // 보유한 아이템만 보기 필터 상태
    ) : DressingRoomUiState

    /**
     * 에러 발생
     *
     * @param message 에러 메시지
     */
    data class Error(val message: String) : DressingRoomUiState
}


