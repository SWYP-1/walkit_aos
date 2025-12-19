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
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Goal
import timber.log.Timber
import team.swyp.sdu.data.model.Emotion
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.GoalRepository
import java.time.LocalDate
import java.time.ZoneId

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nickname: String = "",
        val levelLabel: String = "",
        val todaySteps: Int = 0,
        val sessionsThisWeek: List<WalkingSession>,
        val dominantEmotion : EmotionType? = null,
        val recentEmotions: List<EmotionType?> = emptyList(), // 최근 7개의 postWalkEmotion
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 사용자 정보와 목표 정보 동시에 로드
            val userResult = userRepository.refreshUser()
            val goalResult = goalRepository.getGoal()

            val nickname = when (userResult) {
                is Result.Success -> userResult.data.nickname ?: "사용자"
                else -> "사용자"
            }

            val goal = when (goalResult) {
                is Result.Success -> goalResult.data
                else -> null
            }

            // 레벨과 오늘 걸음 수 계산
            val levelLabel = calculateLevelLabel(goal)
            val todaySteps = calculateTodaySteps()

            // 세션 정보 로드
            loadSessions(nickname, levelLabel, todaySteps)
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            userRepository.refreshUser()
                .onSuccess { user ->
                    // 현재 UI 상태에 사용자 정보 업데이트
                    val currentState = _uiState.value
                    if (currentState is HomeUiState.Success) {
                        _uiState.value = currentState.copy(
                            nickname = user.nickname ?: "사용자",
                            levelLabel = "새싹 Lv.1", // TODO: 레벨 계산 로직 구현
                            todaySteps = 0, // TODO: 오늘 걸음 수 계산 로직 구현
                        )
                    }
                }
                .onError { throwable, message ->
                    // 사용자 정보 로드 실패 - 기본값 유지
                    Timber.e(throwable, "사용자 정보 로드 실패: $message")
                }
        }
    }

    /**
     * 목표 정보를 기반으로 레벨 라벨 계산
     */
    private fun calculateLevelLabel(goal: Goal?): String {
        return when {
            goal == null -> "새싹 Lv.1"
            goal.targetStepCount >= 10000 -> "나무 Lv.3"
            goal.targetStepCount >= 5000 -> "나무 Lv.2"
            goal.targetStepCount > 0 -> "나무 Lv.1"
            else -> "새싹 Lv.1"
        }
    }

    /**
     * 오늘의 실제 걸음 수 계산
     * TODO: 실제 걸음 수 데이터에서 계산하도록 구현
     */
    private fun calculateTodaySteps(): Int {
        // 임시 구현: 실제로는 걸음 수 센서나 건강 데이터에서 가져와야 함
        return 0
    }

    private fun loadSessions(nickname: String, levelLabel: String, todaySteps: Int) {
        viewModelScope.launch {
            walkingSessionRepository
                .getAllSessions()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }
                .collect { sessions ->
                    val thisWeekSessions = sessions.filterThisWeek()
                    val recentEmotions = sessions
                        .sortedByDescending { it.startTime }
                        .take(7)
                        .map { it.postWalkEmotion }
                    val dominantEmotion = findDominantEmotion(thisWeekSessions)
                    _uiState.value = HomeUiState.Success(
                        nickname = nickname,
                        levelLabel = levelLabel,
                        todaySteps = todaySteps,
                        sessionsThisWeek = thisWeekSessions,
                        recentEmotions = recentEmotions,
                        dominantEmotion = dominantEmotion,
                    )
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

    /**
     * 이번주 산책에서 가장 많이 경험된 감정 찾기
     */
    private fun findDominantEmotion(sessions: List<WalkingSession>): EmotionType? {
        val emotionCounts = sessions
            .mapNotNull { it.postWalkEmotion }
            .groupingBy { it }
            .eachCount()
        
        return emotionCounts
            .maxByOrNull { it.value }
            ?.key
    }

}

