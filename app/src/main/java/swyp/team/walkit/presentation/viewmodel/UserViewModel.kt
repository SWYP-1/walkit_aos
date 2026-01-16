package swyp.team.walkit.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
     * 카카오톡으로 로그인 (권장)
     */
    fun loginWithKakaoTalk(
        onSuccess: (OAuthToken) -> Unit = { token ->
            onLoginSuccess(token.accessToken, token.refreshToken)
        },
        onError: (Throwable) -> Unit = { error ->
            Timber.e(error, "카카오톡 로그인 실패")
        }
    ) {
        Timber.d("카카오톡 로그인 시도")

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Timber.e(error, "카카오톡 로그인 실패")
                onError(error)
            } else if (token != null) {
                Timber.d("카카오톡 로그인 성공: ${token.accessToken.take(10)}...")
                onSuccess(token)
            }
        }

        // 카카오톡 설치 여부 확인 후 로그인 시도
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
        } else {
            Timber.d("카카오톡 미설치 - 카카오계정 로그인으로 대체")
            loginWithKakaoAccount(onSuccess, onError)
        }
    }

    /**
     * 카카오계정으로 로그인
     */
    fun loginWithKakaoAccount(
        onSuccess: (OAuthToken) -> Unit = { token ->
            onLoginSuccess(token.accessToken, token.refreshToken)
        },
        onError: (Throwable) -> Unit = { error ->
            Timber.e(error, "카카오계정 로그인 실패")
        }
    ) {
        Timber.d("카카오계정 로그인 시도")

        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Timber.e(error, "카카오계정 로그인 실패")
                onError(error)
            } else if (token != null) {
                Timber.d("카카오계정 로그인 성공: ${token.accessToken.take(10)}...")
                onSuccess(token)
            }
        }

        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
    }

    /**
     * 카카오 로그인 (카카오톡 우선, 실패 시 카카오계정으로 자동 전환)
     */
    fun loginWithKakao(
        onSuccess: (OAuthToken) -> Unit = { token ->
            onLoginSuccess(token.accessToken, token.refreshToken)
        },
        onError: (Throwable) -> Unit = { error ->
            Timber.e(error, "카카오 로그인 실패")
        }
    ) {
        loginWithKakaoTalk(onSuccess, onError)
    }

    /**
     * 로그인 후 토큰 저장 + 프로필 최신화
     */
    fun onLoginSuccess(accessToken: String, refreshToken: String?) {
        viewModelScope.launch {
            userRepository.saveAuthTokens(accessToken, refreshToken)
            userRepository.refreshUser()
        }
    }

    /**
     * 카카오 로그아웃 + 앱 로그아웃
     *
     * 주의: onboardingDataStore의 completeKey는 삭제하지 않습니다.
     * 온보딩 완료 여부는 로그아웃 후에도 유지되어야 하므로,
     * 로그아웃 시에는 인증 토큰만 삭제하고 온보딩 완료 상태는 보존합니다.
     */
    fun logout(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = { error ->
            Timber.e(error, "카카오 로그아웃 실패")
        }
    ) {
        Timber.d("카카오 로그아웃 시도")

        UserApiClient.instance.logout { error ->
            if (error != null) {
                Timber.e(error, "카카오 로그아웃 실패")
                onError(error)
            } else {
                Timber.d("카카오 로그아웃 성공")
                // 앱의 인증 토큰도 삭제
                viewModelScope.launch {
                    userRepository.clearAuth()
                    onSuccess()
                }
            }
        }
        try {
            // NidOAuth가 초기화되었는지 확인
            val isInitialized = try {
                NidOAuth.getApplicationContext() != null
            } catch (t: Throwable) {
                false
            }

            if (!isInitialized) {
                Timber.w("네이버 OAuth가 초기화되지 않았으므로 로그아웃 건너뜀")
            } else {
                val naverCallback = object : NidOAuthCallback {
                    override fun onSuccess() {
                        Timber.i("네이버 로그아웃 성공")
                    }

                    override fun onFailure(errorCode: String, errorDesc: String) {
                        // 네이버도 이미 로그아웃된 상태일 수 있으므로 경고만 로깅
                        Timber.w("네이버 로그아웃 실패 (로컬 데이터는 삭제됨): $errorCode - $errorDesc")
                    }
                }
                NidOAuth.logout(naverCallback)
            }
        } catch (t: Throwable) {
            // Naver OAuth가 초기화되지 않았거나 이미 로그아웃된 경우
            Timber.w("네이버 로그아웃 건너뜀 (초기화되지 않음): ${t.message}")
        }

    }

    /**
     * 카카오 연결 해제 + 앱 로그아웃
     */
    fun unlinkKakao(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = { error ->
            Timber.e(error, "카카오 연결 해제 실패")
        }
    ) {
        Timber.d("카카오 연결 해제 시도")

        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Timber.e(error, "카카오 연결 해제 실패")
                onError(error)
            } else {
                Timber.d("카카오 연결 해제 성공")
                // 앱의 인증 토큰도 삭제
                viewModelScope.launch {
                    userRepository.clearAuth()
                    onSuccess()
                }
            }
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
