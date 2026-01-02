package team.swyp.sdu.ui.walking.viewmodel

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
import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.data.local.mapper.WalkingSessionMapper
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import java.time.DayOfWeek
import java.time.Instant
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
    private val walkingSessionDao: WalkingSessionDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalkingResultUiState>(WalkingResultUiState.Loading)
    val uiState: StateFlow<WalkingResultUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            // 이번 주 시작/끝 시간 계산
            val today = LocalDate.now()
            val startOfWeek = today.with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault())
            val endOfWeek = startOfWeek.plusDays(6).plusHours(23).plusMinutes(59).plusSeconds(59)

            val weekStartMillis = startOfWeek.toInstant().toEpochMilli()
            val weekEndMillis = endOfWeek.toInstant().toEpochMilli()

            walkingSessionDao
                .getSessionsThisWeek(weekStartMillis, weekEndMillis)
                .map { entities ->
                    // Entity에서 Domain으로 변환하고 SYNCED 필터링
                    val syncedSessions = entities
                        .filter { it.syncState == SyncState.SYNCED }
                        .map { WalkingSessionMapper.toDomain(it) }

                    val allSessions = entities.map { WalkingSessionMapper.toDomain(it) }

                    WalkingResultUiState.Success(
                        sessionsThisWeek = allSessions,
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

    private fun List<WalkingSession>.filterThisWeek(): List<WalkingSession> {
        val today = LocalDate.now()
        val startOfWeek = today.with(DayOfWeek.MONDAY)
        val endOfWeek = startOfWeek.plusDays(6)
        return filter { session ->
            val date =
                Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }.sortedByDescending { it.startTime }
    }
}



