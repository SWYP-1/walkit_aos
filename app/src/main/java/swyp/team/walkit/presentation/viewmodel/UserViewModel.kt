package swyp.team.walkit.presentation.viewmodel

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
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.DailyMissionProgress
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.domain.repository.MissionProgressRepository
import swyp.team.walkit.domain.repository.UserRepository
import timber.log.Timber
import swyp.team.walkit.core.onError

/**
 * Activity 범위에서 사용자 정보를 관리하는 ViewModel
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val missionProgressRepository: MissionProgressRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    val userState: StateFlow<Result<User>> =
        userRepository.userFlow
            .map { user ->
                user?.let { Result.Success(it) } ?: Result.Loading
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

    // init에서 자동으로 refreshUser()를 호출하지 않음
    // 필요한 곳에서 명시적으로 호출

    fun refreshUser() {
        viewModelScope.launch {
            userRepository.refreshUser()
        }
    }

    /**
     * 로그인 후 토큰 저장 + 프로필 최신화 예시
     */
    fun onLoginSuccess(accessToken: String, refreshToken: String?) {
        viewModelScope.launch {
            userRepository.saveAuthTokens(accessToken, refreshToken)
            userRepository.refreshUser()
        }
    }

    /**
     * 로그아웃
     * 
     * 주의: onboardingDataStore의 completeKey는 삭제하지 않습니다.
     * 온보딩 완료 여부는 로그아웃 후에도 유지되어야 하므로,
     * 로그아웃 시에는 인증 토큰만 삭제하고 온보딩 완료 상태는 보존합니다.
     */
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
