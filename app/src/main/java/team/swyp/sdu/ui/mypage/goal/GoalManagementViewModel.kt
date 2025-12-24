package team.swyp.sdu.ui.mypage.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

/**
 * 목표 관리 화면의 상태
 */
data class GoalState(
    val targetSteps: Int = 10000,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 7)
    }.timeInMillis,
    val walkFrequency: Int = 3,
    val missionSuccessCount: Int = 0,
)

/**
 * 목표 관리 ViewModel
 */
@HiltViewModel
class GoalManagementViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        GoalState(
            targetSteps = 10000,
            startDate = System.currentTimeMillis(),
            endDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 7)
            }.timeInMillis,
            walkFrequency = 3,
            missionSuccessCount = 0,
        ),
    )
    val uiState: StateFlow<GoalState> = _uiState.asStateFlow()

    private val currentUserId = MutableStateFlow<Long?>(null)

    private val serverGoalState: StateFlow<Result<Goal>> =
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
        // 목표 정보 로드 (화면 진입 시)
        refreshGoal()

        // 캐시된 데이터로 로컬 상태 초기화 (빠른 로딩)
        viewModelScope.launch {
            serverGoalState.collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val goal = result.data
                        // 캐시된 데이터로 로컬 상태 업데이트
                        _uiState.value = _uiState.value.copy(
                            targetSteps = goal.targetStepCount,
                            walkFrequency = goal.targetWalkCount,
                        )
                    }
                    is Result.Error -> {
                        // 에러 발생 시 기본값 유지
                    }
                    Result.Loading -> {
                        // 로딩 중에는 현재 상태 유지
                    }
                }
            }
        }
    }

    fun refreshGoal() {
        viewModelScope.launch {
            goalRepository.refreshGoal()
                .onError { throwable, message ->
                    Timber.e(throwable, "목표 갱신 실패: $message")
                }
        }
    }

    /**
     * 목표 업데이트
     */
    fun updateGoal(
        targetSteps: Int,
        startDate: Long,
        endDate: Long,
        walkFrequency: Int,
        missionSuccessCount: Int,
    ) {
        viewModelScope.launch {
            // 로컬 상태 업데이트
            _uiState.value = GoalState(
                targetSteps = targetSteps,
                startDate = startDate,
                endDate = endDate,
                walkFrequency = walkFrequency,
                missionSuccessCount = missionSuccessCount,
            )

            // 서버에 저장
            val targetUserId = currentUserId.value
            if (targetUserId != null) {
                val goal = Goal(
                    targetStepCount = targetSteps,
                    targetWalkCount = walkFrequency,
                )
                goalRepository.updateGoal(goal)
                    .onError { throwable, message ->
                        Timber.e(throwable, "목표 업데이트 실패: $message")
                    }
            }
        }
    }

    /**
     * 목표 초기화
     */
    fun resetGoal() {
        viewModelScope.launch {
            val defaultState = GoalState(
                targetSteps = 10000,
                startDate = System.currentTimeMillis(),
                endDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 7)
                }.timeInMillis,
                walkFrequency = 3,
                missionSuccessCount = 0,
            )
            _uiState.value = defaultState

            // 서버에 기본값 저장
            val targetUserId = currentUserId.value
            if (targetUserId != null) {
                val defaultGoal = Goal(
                    targetStepCount = defaultState.targetSteps,
                    targetWalkCount = defaultState.walkFrequency,
                )
                goalRepository.updateGoal(defaultGoal)
                    .onError { throwable, message ->
                        Timber.e(throwable, "목표 초기화 실패: $message")
                    }
            }
        }
    }
}
