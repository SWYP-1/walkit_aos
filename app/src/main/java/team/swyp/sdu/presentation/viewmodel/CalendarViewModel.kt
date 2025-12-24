package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
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
) : ViewModel() {

    private val _dummyMessage = MutableStateFlow<String?>(null)
    val dummyMessage: StateFlow<String?> = _dummyMessage.asStateFlow()

    data class WalkAggregate(
        val steps: Int = 0,
        val durationMillis: Long = 0L,
    ) {
        val durationHours: Int get() = (durationMillis / (1000 * 60 * 60)).toInt()
        val durationMinutesRemainder: Int get() = ((durationMillis / (1000 * 60)) % 60).toInt()
    }

    private val today = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = today.asStateFlow()

    val allSessions: StateFlow<List<WalkingSession>> =
        walkingSessionRepository
            .getAllSessions()
            .stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val dayStats: StateFlow<WalkAggregate> =
        today
            .flatMapLatest { date ->
                val (start, end) = dayRange(date)
                walkingSessionRepository.getSessionsBetween(start, end).map { it.aggregate() }
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
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val weekSessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = weekRange(date)
                walkingSessionRepository.getSessionsBetween(start, end)
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val daySessions: StateFlow<List<WalkingSession>> =
        today
            .flatMapLatest { date ->
                val (start, end) = dayRange(date)
                walkingSessionRepository.getSessionsBetween(start, end)
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun generateDummyData() {
        Timber.d("CalendarViewModel.generateDummyData() called")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val current = walkingSessionRepository.getAllSessions().first()
                    val hasNovDec =
                        current.any { session ->
                            val date =
                                java.time.Instant.ofEpochMilli(session.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            (date.monthValue == 12 && date.dayOfMonth <= 16)
                        }
                    if (hasNovDec) {
                        Timber.d("Dummy data skipped: early-December data already exists")
                    
                    } else {
                        val decemberRange = WalkingTestData.generateDecemberRangeSessions()
                        val todaySession = WalkingTestData.generateSessionForDate(LocalDate.now())
                        val all = decemberRange + todaySession
                        Timber.d("Dummy data generating: decRange=${decemberRange.size}, today=1")
                        all.forEach { walkingSessionRepository.saveSession(it) }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Dummy data generation failed")
                    "실패: ${e.message ?: "알 수 없는 오류"}"
                }
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
        today.value = date
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

    private fun dayRange(date: LocalDate): Pair<Long, Long> {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun weekRange(date: LocalDate): Pair<Long, Long> {
        val startDate = date.with(DayOfWeek.MONDAY)
        val endDate = startDate.plusDays(7)
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun monthRange(date: LocalDate): Pair<Long, Long> {
        val startDate = date.withDayOfMonth(1)
        val endDate = startDate.plusMonths(1)
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return start to end
    }
}


