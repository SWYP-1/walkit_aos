package team.swyp.sdu.data.mapper

import kotlinx.coroutines.flow.first
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.model.MissionConfig
import team.swyp.sdu.domain.model.MissionStatus
import team.swyp.sdu.domain.model.MissionType
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.ui.mission.model.MissionCardState
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 미션 카드 상태 매핑 클래스
 *
 * 미션 데이터를 받아 MissionCardState로 매핑하는 로직을 담당합니다.
 */
@Singleton
class MissionCardStateMapper @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
) {

    /**
     * 미션 데이터를 MissionCardState로 매핑
     *
     * @param mission 매핑할 미션 데이터
     * @param isActive 해당 미션이 활성화된 미션인지 여부
     * @return 매핑된 MissionCardState
     */
    suspend fun mapToCardState(mission: WeeklyMission, isActive: Boolean): MissionCardState {
        // 비활성화된 미션은 INACTIVE 상태로 반환
        if (!isActive) return MissionCardState.INACTIVE

        return when (mission.status) {
            MissionStatus.COMPLETED -> MissionCardState.READY_FOR_CLAIM
            MissionStatus.FAILED -> MissionCardState.INACTIVE
            MissionStatus.IN_PROGRESS -> {
                // 진행 중인 경우 미션 조건 체크
                if (checkMissionCondition(mission)) {
                    MissionCardState.READY_FOR_CLAIM
                } else {
                    MissionCardState.ACTIVE_CHALLENGE
                }
            }
            else -> MissionCardState.INACTIVE
        }
    }

    /**
     * 미션 조건 충족 여부를 확인
     *
     * @param mission 확인할 미션
     * @return 조건 충족 여부
     */
    private suspend fun checkMissionCondition(mission: WeeklyMission): Boolean {
        val missionConfig = mission.getMissionConfig() ?: return false

        return when (mission.type) {
            MissionType.CHALLENGE_ATTENDANCE -> {
                checkAttendanceCondition(mission, missionConfig)
            }
            MissionType.CHALLENGE_STEPS -> {
                checkStepsCondition(missionConfig)
            }
            else -> false
        }
    }

    /**
     * 출석 미션 조건 확인
     *
     * @param mission 미션 데이터
     * @param missionConfig 미션 설정
     * @return 조건 충족 여부
     */
    private suspend fun checkAttendanceCondition(
        mission: WeeklyMission,
        missionConfig: MissionConfig
    ): Boolean {
        if (missionConfig !is MissionConfig.ChallengeAttendanceConfig) return false

        val requiredDays = missionConfig.requiredAttendanceDays
        val weekStart = mission.weekStart ?: return false
        val weekEnd = mission.weekEnd ?: return false

        // 주간 범위 내 세션 조회
        val sessions = walkingSessionRepository.getSessionsBetween(
            startMillis = weekStart.toMillis(),
            endMillis = weekEnd.toMillis()
        ).first()

        // 세션들의 날짜 추출 및 중복 제거
        val sessionDates = sessions.mapNotNull { session ->
            try {
                val instant = java.time.Instant.ofEpochMilli(session.startTime)
                instant.atZone(ZoneId.systemDefault()).toLocalDate()
            } catch (e: Exception) {
                null
            }
        }.distinct().sorted()

        // 연속 출석일 체크
        return hasConsecutiveDays(sessionDates, requiredDays)
    }

    /**
     * 걸음 수 미션 조건 확인
     *
     * @param missionConfig 미션 설정
     * @return 조건 충족 여부
     */
    private suspend fun checkStepsCondition(missionConfig: MissionConfig): Boolean {
        if (missionConfig !is MissionConfig.ChallengeStepsConfig) return false

        val requiredSteps = missionConfig.weeklyGoalSteps

        // 오늘 걸음 수 조회 (WalkingSessionRepository에서 오늘 세션들의 걸음 수 합계)
        val todaySteps = getTodaySteps()

        return todaySteps >= requiredSteps
    }

    /**
     * 오늘 걸음 수 조회
     *
     * @return 오늘 걸음 수 합계
     */
    private suspend fun getTodaySteps(): Int {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val todaySessions = walkingSessionRepository.getSessionsBetween(startOfDay, endOfDay).first()

        return todaySessions.sumOf { it.stepCount }
    }

    /**
     * 연속된 날짜가 요구 일수 이상인지 확인
     *
     * @param dates 정렬된 날짜 목록
     * @param requiredDays 요구되는 최소 연속 일수
     * @return 연속 조건 충족 여부
     */
    private fun hasConsecutiveDays(dates: List<LocalDate>, requiredDays: Int): Boolean {
        if (dates.size < requiredDays) return false

        var maxConsecutive = 1
        var currentConsecutive = 1

        for (i in 1 until dates.size) {
            val prevDate = dates[i - 1]
            val currentDate = dates[i]

            // 이전 날짜 + 1일이 현재 날짜와 같으면 연속
            if (prevDate.plusDays(1) == currentDate) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 1
            }
        }

        return maxConsecutive >= requiredDays
    }
}

/**
 * String을 LocalDate로 변환하는 확장 함수
 */
private fun String.toMillis(): Long {
    return try {
        val localDate = LocalDate.parse(this)
        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        0L
    }
}
