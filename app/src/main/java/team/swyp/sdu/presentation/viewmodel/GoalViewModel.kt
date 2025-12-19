package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.GoalRepository
import timber.log.Timber

/**
 * 목표 정보를 관리하는 ViewModel
 */
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val userRepository: team.swyp.sdu.domain.repository.UserRepository,
) : ViewModel() {
    private val currentUserId = MutableStateFlow<Long?>(null)

    val goalState: StateFlow<Result<Goal>> =
        goalRepository.goalFlow
            .map { goal ->
                goal?.let { Result.Success(it) } ?: Result.Loading
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Result.Loading,
            )

    init {
        // 현재 사용자 ID 가져오기
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                user?.let {
                    currentUserId.value = it.userId
                    // 사용자 ID가 있으면 목표 로드
                    refreshGoal(it.userId)
                }
            }
        }
    }

    fun refreshGoal(userId: Long? = null) {
        viewModelScope.launch {
            val targetUserId = userId ?: currentUserId.value
            if (targetUserId != null) {
                goalRepository.refreshGoal()
                    .onError { throwable, message ->
                        Timber.e(throwable, "목표 갱신 실패: $message")
                    }
            }
        }
    }

    fun updateGoal(goal: Goal, userId: Long? = null) {
        viewModelScope.launch {
            val targetUserId = userId ?: currentUserId.value
            if (targetUserId != null) {
                goalRepository.updateGoal(targetUserId, goal)
                    .onError { throwable, message ->
                        Timber.e(throwable, "목표 업데이트 실패: $message")
                    }
            }
        }
    }
}

