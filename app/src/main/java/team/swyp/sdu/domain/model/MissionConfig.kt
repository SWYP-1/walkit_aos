package team.swyp.sdu.domain.model

import kotlinx.serialization.Serializable

/**
 * 미션 설정 sealed class
 */
@Serializable
sealed class MissionConfig {
    /**
     * 챌린지 걸음 수 설정
     */
    @Serializable
    data class ChallengeStepsConfig(
        val weeklyGoalSteps: Int,
    ) : MissionConfig()

    /**
     * 챌린지 출석 설정
     */
    @Serializable
    data class ChallengeAttendanceConfig(
        val requiredAttendanceDays: Int,
    ) : MissionConfig()

    /**
     * 사진 색상 설정
     */
    @Serializable
    data class PhotoColorConfig(
        val color: String,
    ) : MissionConfig()
}
