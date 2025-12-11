package team.swyp.sdu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import java.time.LocalDate
import java.time.ZoneId

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val sessionsThisWeek: List<WalkingSession>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            walkingSessionRepository
                .getAllSessions()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }
                .collect { sessions ->
                    _uiState.value = HomeUiState.Success(sessions.filterThisWeek())
                }
        }
    }

    private fun List<WalkingSession>.filterThisWeek(): List<WalkingSession> {
        val today = LocalDate.now()
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
        val endOfWeek = startOfWeek.plusDays(6)
        return filter { session ->
            val date =
                java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }.sortedByDescending { it.startTime }
    }
}

