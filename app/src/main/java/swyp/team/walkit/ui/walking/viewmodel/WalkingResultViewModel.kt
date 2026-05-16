package swyp.team.walkit.ui.walking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.repository.WalkingSessionRepository
import java.time.LocalDate
import java.time.ZoneId

sealed interface WalkingResultUiState {
    data object Loading : WalkingResultUiState
    data class Success(
        val sessionsThisWeek: List<WalkingSession>,
        val syncedSessionsThisWeek: List<WalkingSession> // SYNCED 상태인 세션만
    ) : WalkingResultUiState
    data class Error(val message: String) : WalkingResultUiState
}

@HiltViewModel
class WalkingResultViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalkingResultUiState>(WalkingResultUiState.Loading)
    val uiState: StateFlow<WalkingResultUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            // 일요일 기준 이번 주 범위 계산 (앱 전체 기준과 통일)
            val today = LocalDate.now()
            val daysFromSunday = (today.dayOfWeek.value % 7).toLong()
            val startDate = today.minusDays(daysFromSunday)
            val endDate = startDate.plusDays(7)
            val weekStartMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val weekEndMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

            // repository는 현재 사용자 기준 + SYNCED 상태만 반환
            walkingSessionRepository.getSessionsBetween(weekStartMillis, weekEndMillis)
                .map { syncedSessions ->
                    WalkingResultUiState.Success(
                        sessionsThisWeek = syncedSessions,
                        syncedSessionsThisWeek = syncedSessions
                    )
                }
                .catch { e ->
                    _uiState.value = WalkingResultUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
}



