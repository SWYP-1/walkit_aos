package swyp.team.walkit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.map
import swyp.team.walkit.data.mapper.MissionCardStateMapper
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.domain.model.MissionProgress
import swyp.team.walkit.domain.repository.MissionRepository
import swyp.team.walkit.utils.CalenderUtils.dayRange
import swyp.team.walkit.utils.CalenderUtils.monthRange
import swyp.team.walkit.utils.CalenderUtils.weekRange
import swyp.team.walkit.utils.WalkingTestData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import timber.log.Timber

/**
 * 캘린더 화면용 ViewModel
 * - 더미 데이터 삽입 (11월~12월 초)
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val missionRepository: MissionRepository,
    private val missionCardStateMapper: MissionCardStateMapper,
) : ViewModel() {


    data class WalkAggregate(
        val steps: Int = 0,
        val durationMillis: Long = 0L,
    ) {
        val durationHours: Int get() = (durationMillis / (1000 * 60 * 60)).toInt()
        val durationMinutesRemainder: Int get() = ((durationMillis / (1000 * 60)) % 60).toInt()

        companion object {
            fun empty(): WalkAggregate {
                return WalkAggregate(steps = 0, durationMillis = 0L)
            }
        }
    }

    private val today = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = today.asStateFlow()


    // 데이터 로딩 상태
    private val _isLoadingDaySessions = MutableStateFlow(true)
    val isLoadingDaySessions: StateFlow<Boolean> = _isLoadingDaySessions.asStateFlow()

    val dayStats: StateFlow<WalkAggregate> =
        today
            .flatMapLatest { date ->
                val (start, end) = dayRange(date)
                walkingSessionRepository.getSessionsBetween(start, end).map { it.aggregate() }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "일일 통계 로드 실패")
                emit(WalkAggregate()) // 오류 발생 시 빈 통계 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = WalkAggregate(),
            )

    val weekStats: StateFlow<WalkAggregate> =
        today
            .flatMapLatest { date ->
                val (start, end) = weekRange(date)
                walkingSessionRepository.getSessionsBetween(start, end).map { it.aggregate() }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "주간 통계 로드 실패")
                emit(WalkAggregate()) // 오류 발생 시 빈 통계 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = WalkAggregate(),
            )

    val monthStats: StateFlow<WalkAggregate> =
        today
            .flatMapLatest { date ->
                val (start, end) = monthRange(date)
                walkingSessionRepository.getSessionsBetween(start, end).map { it.aggregate() }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "월간 통계 로드 실패")
                emit(WalkAggregate()) // 오류 발생 시 빈 통계 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = WalkAggregate(),
            )

    val monthSessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = monthRange(date)
                Timber.d("📅 CalendarViewModel - monthSessions 쿼리: date=$date, start=$start (${java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())}), end=$end (${java.time.Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())})")
                walkingSessionRepository.getSessionsBetween(start, end)
                    .onEach { sessions ->
                        Timber.d("📅 CalendarViewModel - monthSessions 결과: ${sessions.size}개 세션")
                        sessions.forEachIndexed { index, session ->
                            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Timber.d("📅   월간 세션[$index]: id=${session.id}, startTime=${session.startTime}, sessionDate=$sessionDate, 걸음수=${session.stepCount}, 사용자ID=${session.userId}")
                        }
                    }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "월간 세션 로드 실패")
                emit(emptyList()) // 오류 발생 시 빈 리스트 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val monthMissionsCompleted: StateFlow<List<String>> =
        today
            .flatMapLatest { date ->
                val year = date.year
                val month = date.monthValue
                val result = missionRepository.getMonthlyCompletedMissions(year, month)

                flowOf(
                    when (result) {
                        is Result.Success -> result.data
                        is Result.Error -> {
                            Timber.e("월간 미션 완료 데이터 로드 실패: ${result.message}")
                            emptyList()
                        }

                        Result.Loading -> {
                            emptyList()
                        }
                    }
                )
            }

            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )


    val weekSessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = weekRange(date)
                Timber.d("📅 CalendarViewModel - weekSessions 쿼리: date=$date, start=$start (${java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())}), end=$end (${java.time.Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())})")
                walkingSessionRepository.getSessionsBetween(start, end)
                    .onEach { sessions ->
                        Timber.d("📅 CalendarViewModel - weekSessions 결과: ${sessions.size}개 세션")
                        sessions.forEachIndexed { index, session ->
                            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Timber.d("📅   주간 세션[$index]: id=${session.id}, startTime=${session.startTime}, sessionDate=$sessionDate, 걸음수=${session.stepCount}, 사용자ID=${session.userId}")
                        }
                    }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "주간 세션 로드 실패")
                emit(emptyList()) // 오류 발생 시 빈 리스트 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // 주간 미션 진행도 — 세션 변화 시 자동 갱신
    val missionProgress: StateFlow<MissionProgress> =
        today
            .flatMapLatest { date ->
                val (start, end) = weekRange(date)
                val missionResult = missionRepository.getActiveWeeklyMission()
                val mission = (missionResult as? Result.Success)?.data?.firstOrNull()
                    ?: return@flatMapLatest flowOf(MissionProgress.None)
                walkingSessionRepository.getSessionsBetween(start, end)
                    .map { sessions ->
                        val progress = missionCardStateMapper.calculateProgress(mission, sessions)
                        // 주간 총 걸음수 기준으로 달성 여부 판단 (오늘 걸음수만 보던 버그 수정)
                        val isReadyForClaim = when (progress) {
                            is MissionProgress.Steps -> progress.current >= progress.target
                            is MissionProgress.Attendance -> progress.current >= progress.target
                            is MissionProgress.PhotoColor -> progress.isCompleted
                            is MissionProgress.None -> false
                        }
                        when (progress) {
                            is MissionProgress.Steps -> progress.copy(isReadyForClaim = isReadyForClaim)
                            is MissionProgress.Attendance -> progress.copy(isReadyForClaim = isReadyForClaim)
                            is MissionProgress.PhotoColor -> progress.copy(isReadyForClaim = isReadyForClaim)
                            is MissionProgress.None -> progress
                        }
                    }
            }
            .catch { e ->
                Timber.e(e, "미션 진행도 로드 실패")
                emit(MissionProgress.None)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MissionProgress.None,
            )

    val daySessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = dayRange(date)
                Timber.d(
                    "📅 CalendarViewModel - daySessions 쿼리: date=$date, start=$start (${
                        java.time.Instant.ofEpochMilli(
                            start
                        ).atZone(ZoneId.systemDefault())
                    }), end=$end (${
                        java.time.Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())
                    })"
                )
                walkingSessionRepository.getSessionsBetween(start, end)
                    .onEach { sessions ->
                        Timber.d("📅 CalendarViewModel - daySessions 결과: ${sessions.size}개 세션")
                        sessions.forEachIndexed { index, session ->
                            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Timber.d("📅   세션[$index]: id=${session.id}, startTime=${session.startTime}, sessionDate=$sessionDate, 걸음수=${session.stepCount}, 거리=${String.format("%.2f", session.totalDistance)}km, 사용자ID=${session.userId}")
                        }
                    }
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
                Timber.e(e, "일일 세션 로드 실패")
                _isLoadingDaySessions.value = false
                emit(emptyList()) // 오류 발생 시 빈 리스트 반환
            }.onEach { sessions ->
                // 데이터 로드 완료 시 로딩 상태 해제
                _isLoadingDaySessions.value = false
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

//    fun generateDummyData() {
//        Timber.d("CalendarViewModel.generateDummyData() called")
//        viewModelScope.launch {
//            val result = withContext(Dispatchers.IO) {
//                try {
//                    val current = walkingSessionRepository.getAllSessions().first()
//                    val hasNovDec =
//                        current.any { session ->
//                            val date =
//                                java.time.Instant.ofEpochMilli(session.startTime)
//                                    .atZone(ZoneId.systemDefault())
//                                    .toLocalDate()
//                            (date.monthValue == 12 && date.dayOfMonth <= 16)
//                        }
//                    if (hasNovDec) {
//                        Timber.d("Dummy data skipped: early-December data already exists")
//
//                    } else {
//                        // 현재 사용자 ID 가져오기
//                        val currentUserId = walkingSessionRepository.getCurrentUserId()
//                        Timber.d("📋 캘린더 더미 데이터 생성 - 현재 사용자 ID: $currentUserId")
//
//                        val decemberRange = WalkingTestData.generateDecemberRangeSessions(userId = currentUserId) // ✅ userId 전달
//                        val todaySession = WalkingTestData.generateSessionForDate(LocalDate.now(), userId = currentUserId) // ✅ userId 전달
//                        val all = decemberRange + todaySession
//                        Timber.d("Dummy data generating: decRange=${decemberRange.size}, today=1, userId=$currentUserId")
//                        all.forEach { session ->
//                            Timber.d("💾 캘린더 세션 저장: userId=${session.userId}")
//                            walkingSessionRepository.saveSession(session)
//                        }
//                    }
//                } catch (e: Throwable) {
//                    // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
//                    Timber.e(e, "Dummy data generation failed")
//                    "실패: ${e.message ?: "알 수 없는 오류"}"
//                }
//            }
//        }
//    }

    fun updateSessionNote(id: String, note: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                walkingSessionRepository.updateSessionNote(id, note)
            }
        }
    }

    fun deleteSessionNote(id: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    walkingSessionRepository.updateSessionNote(id, "")
                }
                Timber.d("세션 노트 삭제 완료: $id")
                // 삭제 성공 시 UI 즉시 업데이트를 위해 로딩 상태 토글
                _isLoadingDaySessions.value = true
                _isLoadingDaySessions.value = false
            } catch (e: Throwable) {
                // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
                Timber.e(e, "세션 노트 삭제 실패: $id")
                // UI에 에러 표시를 위해서는 추가 구현 필요
            }
        }
    }

    /**
     * 외부에서 오늘 날짜를 강제로 새로고침할 수 있게
     */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    /**
     * 디버깅용: 모든 세션 상태 확인
     */
    fun debugAllSessions() {
        viewModelScope.launch {
            try {
                walkingSessionRepository.debugAllSessions()
            } catch (e: Throwable) {
                Timber.e(e, "🔍 [DEBUG] 세션 디버깅 실패")
            }
        }
    }

    /**
     * 특정 날짜로 설정
     */
    fun setDate(date: LocalDate) {
        today.value = date
        // 날짜 변경 시 로딩 상태로 설정
        _isLoadingDaySessions.value = true
    }

    fun prevDay() {
        today.value = today.value.minusDays(1)
    }

    fun nextDay() {
        today.value = today.value.plusDays(1)
    }

// CalendarViewModel.kt 수정 부분


// CalendarViewModel.kt
    fun nextWeek() {
        val current = today.value
        val currentWeekStart = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val nextWeekStart = currentWeekStart.plusWeeks(1)

        today.value = nextWeekStart
        _isLoadingDaySessions.value = true
    }

    fun prevWeek() {
        val current = today.value
        val currentWeekStart = current.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val prevWeekStart = currentWeekStart.minusWeeks(1)

        today.value = prevWeekStart
        _isLoadingDaySessions.value = true
    }

    /**
     * 주간 미션 보상 수령
     */
    fun claimMissionReward(userWeeklyMissionId: Long) {
        viewModelScope.launch {
            when (val result = missionRepository.verifyWeeklyMissionReward(userWeeklyMissionId)) {
                is Result.Success -> Timber.d("미션 보상 수령 성공: $userWeeklyMissionId")
                is Result.Error -> Timber.e(result.exception, "미션 보상 수령 실패: $userWeeklyMissionId")
                Result.Loading -> Unit
            }
        }
    }

    private fun List<WalkingSession>.aggregate(): WalkAggregate {
        var steps = 0
        var duration = 0L
        for (s in this) {
            steps += s.stepCount
            val d = s.endTime - s.startTime
            if (d > 0) duration += d
        }
        return WalkAggregate(steps, duration)
    }

}


