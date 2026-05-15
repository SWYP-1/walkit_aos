package swyp.team.walkit.domain.model

/**
 * 미션 타입별 진행도 모델
 * - Steps / Attendance: 막대그래프로 표현
 * - PhotoColor: 달성/미달성 뱃지로 표현
 */
sealed class MissionProgress {

    data class Steps(
        val current: Int,
        val target: Int,
        val missionName: String = "",
        val rewardPoints: Int = 0,
        val userWeeklyMissionId: Long? = null,
        val isReadyForClaim: Boolean = false,
    ) : MissionProgress() {
        val percentage: Float =
            if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    }

    data class Attendance(
        val current: Int,
        val target: Int,
        val missionName: String = "",
        val rewardPoints: Int = 0,
        val userWeeklyMissionId: Long? = null,
        val isReadyForClaim: Boolean = false,
    ) : MissionProgress() {
        val percentage: Float =
            if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    }

    data class PhotoColor(
        val targetColor: String,
        val isCompleted: Boolean,
        val rewardPoints: Int = 0,
        val userWeeklyMissionId: Long? = null,
        val isReadyForClaim: Boolean = false,
    ) : MissionProgress()

    data object None : MissionProgress()
}
