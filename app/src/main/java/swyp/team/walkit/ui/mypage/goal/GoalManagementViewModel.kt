package swyp.team.walkit.ui.mypage.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.onError
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.ui.mypage.goal.model.GoalState
import timber.log.Timber
import javax.inject.Inject

/**
 * 목표 관리 에러 타입
 */
sealed class GoalError {
    data object UpdateNotAllowed : GoalError()
    data object SaveFailed : GoalError()
}



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

    // 에러 상태
    private val _goalError = MutableStateFlow<GoalError?>(null)
    val goalError: StateFlow<GoalError?> = _goalError.asStateFlow()

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
            val result = goalRepository.updateGoal(goal)

            when (result) {
                is Result.Success -> {
                    Timber.d("목표 업데이트 성공: 걸음=${targetSteps}, 빈도=${walkFrequency}회")
                }
                is Result.Error -> {
                    val exception = result.exception
                    // CancellationException은 정상적인 취소이므로 에러 처리하지 않음
                    if (exception is kotlinx.coroutines.CancellationException) {
                        Timber.d("목표 업데이트가 취소되었습니다")
                    } else {
                        // 특정 에러 코드에 따른 처리
                        val goalError = when (exception.message) {
                            "GOAL_UPDATE_NOT_ALLOWED" -> GoalError.UpdateNotAllowed
                            else -> GoalError.SaveFailed
                        }
                        Timber.e(exception, "목표 업데이트 실패: $goalError")
                        _goalError.value = goalError

                        // 실제 에러인 경우 서버에서 다시 가져와 UI 동기화
                        refreshGoal()
                    }
                }
                is Result.Loading -> {
                    // 로딩 상태는 여기서는 처리하지 않음
                }
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
     * 에러 상태 클리어
     */
    fun clearGoalError() {
        _goalError.value = null
    }

    /**
     * 현재 목표 상태 반환 (UI에서 로컬 상태 초기화에 사용)
     */
    fun getCurrentGoalState(): GoalState = _uiState.value
}
