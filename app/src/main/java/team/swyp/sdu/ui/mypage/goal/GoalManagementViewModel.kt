package team.swyp.sdu.ui.mypage.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.ui.mypage.goal.model.GoalState
import timber.log.Timber
import javax.inject.Inject



/**
 * 목표 관리 ViewModel
 */
@HiltViewModel
class GoalManagementViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow(GoalState())
    val uiState: StateFlow<GoalState> = _uiState.asStateFlow()

    init {
        // 서버 캐시 구독
        viewModelScope.launch {
            goalRepository.goalFlow.collectLatest { cachedGoal ->
                if (cachedGoal != null) {
                    // 캐시가 있으면 UI에 바로 반영
                    _uiState.value = _uiState.value.copy(
                        targetSteps = cachedGoal.targetStepCount,
                        walkFrequency = cachedGoal.targetWalkCount
                    )
                } else {
                    // 캐시가 없으면 서버에서 새로 가져오기
                    refreshGoal()
                }
            }
        }
    }

    /**
     * 서버에서 최신 목표 정보 가져오기
     */
    fun refreshGoal() {
        viewModelScope.launch {
            goalRepository.refreshGoal().onError { t, msg ->
                Timber.e(t, "목표 갱신 실패: $msg")
            }
        }
    }

    /**
     * 목표 업데이트 (Optimistic Update + 서버 반영)
     */
    fun updateGoal(targetSteps: Int, walkFrequency: Int) {
        viewModelScope.launch {
            // 1. UI 상태 바로 업데이트 (Optimistic)
            _uiState.value = GoalState(targetSteps, walkFrequency)

            // 2. 서버 업데이트
            val goal = Goal(targetStepCount = targetSteps, targetWalkCount = walkFrequency)
            goalRepository.updateGoal(goal).onError { t, msg ->
                Timber.e(t, "목표 업데이트 실패: $msg")
                // 실패 시 서버에서 다시 가져와 UI 동기화
                refreshGoal()
            }
        }
    }

    /**
     * 목표 초기화 (UI만 리셋, 서버 저장하지 않음)
     */
    fun resetGoal() {
        val defaultState = GoalState()
        // UI 상태만 초기화 (서버 저장하지 않음)
        _uiState.value = defaultState
        Timber.d("목표 UI 초기화됨: 걸음 수=${defaultState.targetSteps}, 빈도=${defaultState.walkFrequency}회")
    }

    /**
     * 현재 목표 상태 반환 (UI에서 로컬 상태 초기화에 사용)
     */
    fun getCurrentGoalState(): GoalState = _uiState.value
}
