package team.swyp.sdu.presentation.viewmodel

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
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.map
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.repository.MissionRepository
import team.swyp.sdu.utils.CalenderUtils.dayRange
import team.swyp.sdu.utils.CalenderUtils.monthRange
import team.swyp.sdu.utils.CalenderUtils.weekRange
import team.swyp.sdu.utils.WalkingTestData
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import timber.log.Timber

/**
 * 캘린더 화면용 ViewModel
 * - 더미 데이터 삽입 (11월~12월 초)
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val missionRepository: MissionRepository,
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
                walkingSessionRepository.getSessionsBetween(start, end)
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
                walkingSessionRepository.getSessionsBetween(start, end)
            }.catch { e ->
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(e, "주간 세션 로드 실패")
                emit(emptyList()) // 오류 발생 시 빈 리스트 반환
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val daySessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = dayRange(date)
                Timber.d("📅 CalendarViewModel - daySessions 쿼리: date=$date, start=$start (${java.time.Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())}), end=$end (${java.time.Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault())})")
                walkingSessionRepository.getSessionsBetween(start, end)
                    .onEach { sessions ->
                        Timber.d("📅 CalendarViewModel - daySessions 결과: ${sessions.size}개 세션")
                        sessions.forEachIndexed { index, session ->
                            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Timber.d("📅   세션[$index]: id=${session.id}, startTime=${session.startTime}, sessionDate=$sessionDate")
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
    fun deleteSessionNote(id: String){
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
     * 특정 날짜로 설정
     */
    fun setDate(date: LocalDate) {
        Timber.d("📅 CalendarViewModel - setDate 호출: $date")
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

    fun prevWeek() {
        today.value = today.value.minusWeeks(1)
    }

    fun nextWeek() {
        today.value = today.value.plusWeeks(1)
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


