package swyp.team.walkit.domain.model

/**
 * 아이템 착용 상태를 나타내는 sealed class
 */
sealed class WearState {

    /**
     * 착용 중인 상태 (코스메틱 아이템)
     */
    data class Worn(val itemId: Int) : WearState()

    /**
     * 미착용 상태 (투명 PNG로 표시)
     * 착용 해제 시 이 상태가 되어 완전히 빈 상태로 보임
     */
    data object Unworn : WearState()

    /**
     * 캐릭터 기본값 상태 (초기 로드 시)
     * 캐릭터의 원래 아이템이 표시됨
     */
    data object Default : WearState()
}
