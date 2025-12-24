package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.domain.repository.MissionRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * 미션 화면의 상태
 */
data class MissionUiState(
    val weeklyMissions: List<WeeklyMission> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * 미션 ViewModel
 */
@HiltViewModel
class MissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyMissions()
    }

    /**
     * 주간 미션 목록 로드
     */
    private fun loadWeeklyMissions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = missionRepository.getWeeklyMissions()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        weeklyMissions = result.data,
                        isLoading = false,
                    )
                }
                is Result.Error -> {
                    Timber.e(result.exception, "주간 미션 로드 실패")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "미션 목록을 불러오는데 실패했습니다.",
                    )
                }

                Result.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = true,
                    )
                }
            }
        }
    }
}

