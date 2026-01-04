package team.swyp.sdu.data.mapper

import kotlinx.coroutines.flow.first
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.model.MissionConfig
import team.swyp.sdu.domain.model.MissionStatus
import team.swyp.sdu.domain.model.MissionType
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.ui.mission.model.MissionCardState
import timber.log.Timber
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
        Timber.d("mapToCardState 시작: missionId=${mission.missionId}, status=${mission.status}, isActive=$isActive")

        // 비활성화된 미션은 INACTIVE 상태로 반환
        if (!isActive) {
            Timber.d("mapToCardState 결과: INACTIVE (비활성화된 미션)")
            return MissionCardState.INACTIVE
        }

        val result = when (mission.status) {
            MissionStatus.COMPLETED -> {
                // completedAt이 있으면 보상을 이미 받은 상태
                if (mission.completedAt != null) {
                    Timber.d("mapToCardState 결과: COMPLETED (보상 완료)")
                    MissionCardState.COMPLETED
                } else {
                    Timber.d("mapToCardState 결과: READY_FOR_CLAIM (보상 받기 가능)")
                    MissionCardState.READY_FOR_CLAIM
                }
            }
            MissionStatus.FAILED -> {
                Timber.d("mapToCardState 결과: INACTIVE (FAILED 상태)")
                MissionCardState.INACTIVE
            }
            MissionStatus.IN_PROGRESS -> {
                // 진행 중인 경우 미션 조건 체크
                val conditionMet = checkMissionCondition(mission)
                val state = if (conditionMet) {
                    Timber.d("mapToCardState 결과: READY_FOR_CLAIM (IN_PROGRESS + 조건 충족)")
                    MissionCardState.READY_FOR_CLAIM
                } else {
                    Timber.d("mapToCardState 결과: ACTIVE_CHALLENGE (IN_PROGRESS + 조건 미충족)")
                    MissionCardState.ACTIVE_CHALLENGE
                }
                state
            }
            else -> {
                Timber.d("mapToCardState 결과: INACTIVE (알 수 없는 상태: ${mission.status})")
                MissionCardState.INACTIVE
            }
        }

        Timber.d("mapToCardState 최종 결과: $result")
        return result
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
            } catch (t: Throwable) {
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
        Timber.d("미션 목표 걸음 수: $requiredSteps")

        // 오늘 걸음 수 조회 (WalkingSessionRepository에서 오늘 세션들의 걸음 수 합계)
        val todaySteps = getTodaySteps()
        Timber.d("오늘 실제 걸음 수: $todaySteps")

        val conditionMet = todaySteps >= requiredSteps
        Timber.d("미션 조건 충족 여부: $conditionMet ($todaySteps >= $requiredSteps)")

        return conditionMet
    }

    /**
     * 오늘 걸음 수 조회
     *
     * @return 오늘 걸음 수 합계
     */
    private suspend fun getTodaySteps(): Int {
        // System.currentTimeMillis()와 동일한 방식으로 오늘 범위 계산
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // 24시간 후

        Timber.d("오늘 날짜 범위 (System.currentTimeMillis 기준): $startOfDay ~ $endOfDay")

        val todaySessions = walkingSessionRepository.getSessionsBetween(startOfDay, endOfDay).first()
        Timber.d("오늘 walking sessions 개수: ${todaySessions.size}")
        todaySessions.forEach { session ->
            Timber.d("Session: id=${session.id}, stepCount=${session.stepCount}, startTime=${session.startTime}")
        }

        val totalSteps = todaySessions.sumOf { it.stepCount }
        Timber.d("총 걸음 수 합계: $totalSteps")

        return totalSteps
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
    } catch (t: Throwable) {
        0L
    }
}
