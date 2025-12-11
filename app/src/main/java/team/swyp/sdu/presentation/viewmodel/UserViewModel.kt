package team.swyp.sdu.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.DailyMissionProgress
import team.swyp.sdu.domain.model.UserProfile
import team.swyp.sdu.domain.repository.MissionProgressRepository
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber
import team.swyp.sdu.core.onError

/**
 * Activity 범위에서 사용자 정보를 관리하는 ViewModel
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val missionProgressRepository: MissionProgressRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    val userState: StateFlow<Result<UserProfile>> =
        userRepository.userProfileFlow
            .map { profile ->
                profile?.let { Result.Success(it) } ?: Result.Loading
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Result.Loading,
            )

    val todayProgress: StateFlow<DailyMissionProgress?> =
        today
            .flatMapLatest { date -> missionProgressRepository.observeProgress(date) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    init {
        refreshUserProfile()
    }

    fun refreshUserProfile() {
        viewModelScope.launch {
            userRepository.refreshUserProfile()
        }
    }

    /**
     * 로그인 후 토큰 저장 + 프로필 최신화 예시
     */
    fun onLoginSuccess(accessToken: String, refreshToken: String?) {
        viewModelScope.launch {
            userRepository.saveAuthTokens(accessToken, refreshToken)
            userRepository.refreshUserProfile()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.clearAuth()
        }
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun upsertTodayProgress(progress: DailyMissionProgress) {
        viewModelScope.launch {
            missionProgressRepository
                .saveProgress(progress)
                .onError { throwable, message ->
                    Timber.e(throwable, "미션 진행도 저장 실패: $message")
                }
        }
    }

    fun clearTodayProgress() {
        viewModelScope.launch {
            missionProgressRepository
                .clearProgress(today.value)
                .onError { throwable, message ->
                    Timber.e(throwable, "미션 진행도 삭제 실패: $message")
                }
        }
    }
}
