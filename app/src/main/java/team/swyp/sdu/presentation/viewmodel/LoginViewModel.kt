package team.swyp.sdu.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.data.local.datastore.AuthDataStore
import team.swyp.sdu.data.remote.auth.AuthRemoteDataSource
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.data.remote.auth.TokenProvider
import team.swyp.sdu.domain.service.FcmTokenManager
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

/**
 * 로그인 상태
 */
sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val token: OAuthToken) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * 로그인 ViewModel
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val authDataStore: AuthDataStore,
    private val userRepository: UserRepository,
    private val tokenProvider: TokenProvider,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    private var onNavigateToMain: (() -> Unit)? = null
    private var onNavigateToTermsAgreement: (() -> Unit)? = null
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoginChecked = MutableStateFlow(false)
    val isLoginChecked: StateFlow<Boolean> = _isLoginChecked.asStateFlow()

    init {
        Timber.i("LoginViewModel 초기화 시작")
        checkLoginStatus()
    }

    /**
     * 네비게이션 콜백 설정
     */
    fun setNavigationCallbacks(
        onNavigateToMain: () -> Unit,
        onNavigateToTermsAgreement: () -> Unit
    ) {
        this.onNavigateToMain = onNavigateToMain
        this.onNavigateToTermsAgreement = onNavigateToTermsAgreement
    }

    /**
     * 로그인 상태 확인
     * 서버 토큰이 있으면 getUser()를 호출하여 사용자 정보를 확인
     * 닉네임이 있는 경우에만 로그인 상태로 인정
     */
    fun checkLoginStatus() {
        Timber.i("checkLoginStatus() 시작")
        viewModelScope.launch {
            Timber.d("checkLoginStatus() 코루틴 시작")
            _isLoginChecked.value = false
            try {
                // 서버 토큰 확인
                Timber.d("서버 토큰 확인 시작")
                val accessToken = try {
                    authDataStore.accessToken.first()
                } catch (e: Exception) {
                    Timber.e(e, "토큰 조회 중 예외 발생")
                    null
                }
                Timber.i("서버 토큰 확인 결과: ${if (accessToken.isNullOrBlank()) "토큰 없음" else "토큰 있음 (길이: ${accessToken?.length})"}")

                if (!accessToken.isNullOrBlank()) {
                    Timber.d("서버 토큰 존재 - 사용자 정보 확인 시도")

                    // getUser() 호출하여 사용자 정보 확인
                    userRepository.getUser().onSuccess { user ->
                        // 닉네임이 있는 경우에만 로그인 상태로 인정
                        if (!user.nickname.isNullOrBlank()) {
                            _isLoggedIn.value = true
                            Timber.i("사용자 정보 확인 성공 - 닉네임: ${user.nickname}, 로그인 상태 유지")
                        } else {
                            // 닉네임이 없으면 온보딩 필요
                            _isLoggedIn.value = false
                            Timber.i("닉네임 없음 - 온보딩 필요")
                        }
                    }.onError { throwable, message ->
                        // 사용자 정보 조회 실패 (토큰 만료 등)
                        Timber.w(throwable, "사용자 정보 조회 실패: $message - 토큰 삭제")
                        tokenProvider.clearTokens()
                        authDataStore.clear()
                        _isLoggedIn.value = false
                        Timber.i("토큰 삭제 완료 - 재로그인 필요")
                    }
                } else {
                    // 서버 토큰이 없으면 로그인 안 된 상태
                    Timber.i("서버 토큰 없음 - 로그인 필요")
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                Timber.e(e, "로그인 상태 확인 실패")
                _isLoggedIn.value = false
            } finally {
                Timber.i("checkLoginStatus() 완료 - isLoginChecked = true 설정")
                _isLoginChecked.value = true
                Timber.d("최종 상태 - isLoggedIn: ${_isLoggedIn.value}, isLoginChecked: ${_isLoginChecked.value}")
            }
        }
    }

    /**
     * 카카오톡으로 로그인
     */
    fun loginWithKakaoTalk(context: Context) {
        _uiState.value = LoginUiState.Loading

        // 카카오톡 로그인 가능 여부 확인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Timber.e(error, "카카오톡으로 로그인 실패")

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        _uiState.value = LoginUiState.Error("로그인이 취소되었습니다.")
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    loginWithKakaoAccount(context)
                } else if (token != null) {
                    Timber.i("카카오톡으로 로그인 성공 ${token.accessToken}")
                    // 서버에 토큰 전송
                    sendTokenToServer(token.accessToken, isKakao = true)
                }
            }
        } else {
            // 카카오톡이 설치되어 있지 않으면 카카오계정으로 로그인
            loginWithKakaoAccount(context)
        }
    }

    /**
     * 카카오계정으로 로그인
     */
    fun loginWithKakaoAccount(context: Context) {
        _uiState.value = LoginUiState.Loading

        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null) {
                Timber.e(error, "카카오계정으로 로그인 실패")
                _uiState.value = LoginUiState.Error("로그인에 실패했습니다: ${error.message}")
            } else if (token != null) {
                Timber.i("카카오계정으로 로그인 성공 ${token.accessToken}")
                // 서버에 토큰 전송
                sendTokenToServer(token.accessToken, isKakao = true)
            }
        }
    }

    /**
     * 네이버 로그인 (ActivityResultLauncher 사용)
     */
    fun loginWithNaver(context: Context, launcher: ActivityResultLauncher<Intent>) {
        _uiState.value = LoginUiState.Loading
        NidOAuth.requestLogin(context, launcher)
    }

    /**
     * 네이버 로그인 결과 처리
     */
    fun handleNaverLoginResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                // 네이버 로그인 성공
                val accessToken = NidOAuth.getAccessToken()
                if (accessToken != null) {
                    Timber.i("네이버 로그인 성공: $accessToken")
                    // 서버에 토큰 전송
                    sendTokenToServer(accessToken, isKakao = false)
                } else {
                    _uiState.value = LoginUiState.Error("토큰을 가져오지 못했습니다.")
                }
            }
            Activity.RESULT_CANCELED -> {
                // 로그인 실패 또는 취소
                val errorCode = NidOAuth.getLastErrorCode().code
                val errorDescription = NidOAuth.getLastErrorDescription()
                Timber.e("네이버 로그인 실패: $errorCode - $errorDescription")
                _uiState.value = LoginUiState.Error("로그인에 실패했습니다: $errorDescription")
            }
        }
    }

    /**
     * 네이버 로그인 (Callback 사용)
     */
    fun loginWithNaver(context: Context) {
        _uiState.value = LoginUiState.Loading

        val nidOAuthCallback = object : NidOAuthCallback {
            override fun onSuccess() {
                val accessToken = NidOAuth.getAccessToken()
                if (accessToken != null) {
                    Timber.i("네이버 로그인 성공: $accessToken")
                    // 서버에 토큰 전송
                    sendTokenToServer(accessToken, isKakao = false)
                } else {
                    _uiState.value = LoginUiState.Error("토큰을 가져오지 못했습니다.")
                }
            }

            override fun onFailure(errorCode: String, errorDesc: String) {
                Timber.e("네이버 로그인 실패: $errorCode - $errorDesc")
                _uiState.value = LoginUiState.Error("로그인에 실패했습니다: $errorDesc")
            }
        }

        NidOAuth.requestLogin(context, nidOAuthCallback)
    }

    /**
     * 로그아웃
     *
     * 소셜 로그아웃 실패 시에도 로컬 데이터는 삭제합니다.
     * (이미 토큰이 없는 상태에서 로그아웃을 시도하면 TokenNotFound 에러가 발생할 수 있음)
     */
    fun logout() {
        viewModelScope.launch {
            // 현재 로그인한 제공자 확인
            val currentProvider = authDataStore.getProvider()

            // 카카오 로그아웃 시도 (실패해도 계속 진행)
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    // TokenNotFound 에러는 이미 로그아웃된 상태이므로 무시
                    if (error is ClientError && error.reason == ClientErrorCause.TokenNotFound) {
                        Timber.w("카카오 로그아웃: 이미 로그아웃된 상태 (토큰 없음)")
                    } else {
                        Timber.e(error, "카카오 로그아웃 실패 (로컬 데이터는 삭제됨)")
                    }
                } else {
                    Timber.i("카카오 로그아웃 성공")
                }
            }

            // 네이버 로그아웃 시도 (실패해도 계속 진행)
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

            // 로컬 토큰 및 데이터 삭제 (소셜 로그아웃 실패 여부와 관계없이 항상 실행)
            try {
                tokenProvider.clearTokens()
                authDataStore.clear()
                // 🔥 Room 사용자 데이터도 삭제 (로그인 전환 시 캐시된 이전 사용자 데이터 제거)
                userRepository.clearAuth()
                Timber.i("로컬 토큰 및 데이터 삭제 완료")
            } catch (e: Exception) {
                Timber.e(e, "로컬 데이터 삭제 실패")
            }

            // 로그인 상태 초기화
            _isLoggedIn.value = false
            _uiState.value = LoginUiState.Idle
        }
    }

    /**
     * 앱 데이터 완전 초기화 (디버깅용)
     */
    fun forceCompleteReset() {
        viewModelScope.launch {
            Timber.i("=== 앱 데이터 완전 초기화 시작 ===")

            // 1. 모든 토큰 삭제
            tokenProvider.clearTokens()
            authDataStore.clear()
            Timber.i("토큰 데이터 삭제 완료")

            // 2. 로그인 상태 초기화
            _isLoggedIn.value = false
            _isLoginChecked.value = false
            _uiState.value = LoginUiState.Idle
            Timber.i("로그인 상태 초기화 완료")

            Timber.i("=== 앱 데이터 완전 초기화 완료 ===")
        }
    }

    /**
     * 소셜 로그인 토큰을 서버에 전송하고 서버 토큰 받기
     */
    private fun sendTokenToServer(socialAccessToken: String, isKakao: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState.Loading

                val result = if (isKakao) {
                    authRemoteDataSource.loginWithKakao(socialAccessToken)
                } else {
                    authRemoteDataSource.loginWithNaver(socialAccessToken)
                }

                when (result) {
                    is Result.Success -> {
                        val tokenResponse = result.data
                        // 서버 토큰 및 provider 저장
                        val provider = if (isKakao) "카카오" else "네이버"
                        authDataStore.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                        authDataStore.saveProvider(provider)
                        // TokenProvider도 업데이트 (Flow 구독으로 자동 업데이트되지만 명시적으로 호출)
                        tokenProvider.updateTokens(
                            tokenResponse.accessToken,
                            tokenResponse.refreshToken,
                        )

                        // 로그인 성공 - 즉시 사용자 정보 확인
                        Timber.i("서버 로그인 성공 - 토큰 저장됨")

                        // FCM 토큰 서버 동기화 먼저 실행
                        Timber.d("로그인 성공 후 FCM 토큰 서버 동기화 시작")
                        fcmTokenManager.syncTokenToServer()
                        Timber.d("로그인 성공 후 FCM 토큰 서버 동기화 완료")

                        // 즉시 사용자 정보 확인 (Splash 대신 여기서 처리)
                        Timber.i("로그인 직후 사용자 정보 확인 시작")
                        checkUserStatusAfterLogin()
                    }
                    is Result.Error -> {
                        _uiState.value = LoginUiState.Error(
                            result.message ?: "서버 로그인에 실패했습니다",
                        )
                        Timber.e(result.exception, "서버 로그인 실패")
                    }
                    Result.Loading -> {
                        // 이미 Loading 상태
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("로그인 처리 중 오류 발생: ${e.message}")
                Timber.e(e, "로그인 처리 실패")
            }
        }
    }

    /**
     * 로그인 직후 사용자 상태 확인
     * getUser()를 호출하여 닉네임 존재 여부를 확인하고 적절한 상태로 설정
     */
    private fun checkUserStatusAfterLogin() {
        viewModelScope.launch {
            try {
                Timber.i("로그인 직후 사용자 상태 확인 시도")

                // refreshUser() 호출하여 서버에서 최신 사용자 정보 가져오기 (캐시 무시)
                userRepository.refreshUser().onSuccess { user ->
                    Timber.i("로그인 직후 사용자 정보 조회 성공: ${user.nickname}")

                    // 닉네임이 있는 경우: Main으로 이동
                    if (!user.nickname.isNullOrBlank()) {
                        _isLoggedIn.value = true
                        _uiState.value = LoginUiState.Idle
                        Timber.i("로그인 완료 - 닉네임 있음: ${user.nickname}")
                        onNavigateToMain?.invoke()
                    } else {
                        // 닉네임이 없으면: 약관 동의 → 온보딩
                        _isLoggedIn.value = true  // 로그인 상태 유지 (약관 동의 필요)
                        _uiState.value = LoginUiState.Idle
                        Timber.i("로그인 완료 - 닉네임 없음, 약관 동의 필요")
                        // 약관 동의 다이얼로그는 UI에서 처리됨
                    }
                }.onError { throwable, message ->
                    // 사용자 정보 조회 실패 (토큰 만료 등)
                    Timber.w(throwable, "로그인 직후 사용자 정보 조회 실패: $message - 토큰 삭제")
                    tokenProvider.clearTokens()
                    authDataStore.clear()
                    _isLoggedIn.value = false
                    _uiState.value = LoginUiState.Error("사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요.")
                    Timber.i("토큰 삭제 완료 - 재로그인 필요")
                }
            } catch (e: Exception) {
                Timber.e(e, "로그인 직후 사용자 상태 확인 실패")
                _isLoggedIn.value = false
                _uiState.value = LoginUiState.Error("사용자 상태 확인 중 오류 발생")
            }
        }
    }

    /**
     * 에러 상태 초기화
     * 에러 다이얼로그를 닫을 때 호출됩니다.
     */
    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}

