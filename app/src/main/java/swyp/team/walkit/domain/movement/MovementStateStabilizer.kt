package swyp.team.walkit.domain.movement

import swyp.team.walkit.domain.service.MovementState
import javax.inject.Inject

/**
 * MovementState 스무딩 전담 클래스
 *
 * MovementState 변경 시 n초 이상 유지되어야만 상태 확정
 * ViewModel에서 pending / stable 상태 제거
 */
class MovementStateStabilizer(
    private val stableDurationMs: Long = 3000L,
) {
    private var pendingState: MovementState? = null
    private var lastChangeTime: Long = 0L
    private var stableState: MovementState? = null

    /**
     * 움직임 상태 업데이트
     *
     * @param detectedState 감지된 상태
     * @param timestamp 현재 시간 (밀리초)
     * @return 확정된 상태 (안정화된 상태 또는 감지된 상태)
     */
    fun update(
        detectedState: MovementState,
        timestamp: Long,
    ): MovementState {
        val currentStableState = stableState ?: detectedState

        if (detectedState != currentStableState) {
            // 상태가 변경되었음
            if (pendingState != detectedState) {
                // 새로운 상태로 대기 시작
                pendingState = detectedState
                lastChangeTime = timestamp
            } else {
                // 같은 상태로 계속 대기 중
                val elapsedTime = timestamp - lastChangeTime
                if (elapsedTime >= stableDurationMs) {
                    // 충분한 시간 동안 같은 상태 유지됨 -> 상태 변경 확정
                    stableState = detectedState
                    pendingState = null
                }
            }
        } else {
            // 상태가 변경되지 않았음 -> 대기 중인 상태 초기화
            if (pendingState != null) {
                pendingState = null
                lastChangeTime = 0L
            }
        }

        // 확정된 상태 반환 (대기 중이면 이전 상태 유지)
        return stableState ?: detectedState
    }

    /**
     * 상태 초기화
     */
    fun reset() {
        pendingState = null
        lastChangeTime = 0L
        stableState = null
    }
}

