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
import team.swyp.sdu.data.local.datastore.AuthDataStore
import team.swyp.sdu.data.local.datastore.OnboardingDataStore
import team.swyp.sdu.data.remote.auth.AuthRemoteDataSource
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
    private val onboardingDataStore: OnboardingDataStore,
    private val tokenProvider: TokenProvider,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoginChecked = MutableStateFlow(false)
    val isLoginChecked: StateFlow<Boolean> = _isLoginChecked.asStateFlow()

    init {
        checkLoginStatus()
    }

    /**
     * 로그인 상태 확인
     * 서버 토큰(AuthDataStore)과 약관 동의 여부를 함께 확인
     * 약관 동의가 안 되어 있으면 자동으로 토큰 삭제 처리
     * 단, 온보딩이 완료된 경우에는 약관 동의 여부와 관계없이 토큰 유지
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoginChecked.value = false
            try {
                // 서버 토큰 확인 (AuthDataStore)
                val accessToken = authDataStore.accessToken.first()
                if (!accessToken.isNullOrBlank()) {
                    // 온보딩 완료 여부 확인 (completeKey 확인)
                    val isOnboardingCompleted = onboardingDataStore.isCompleted.first()
                    
                    Timber.d("로그인 상태 확인 - 서버 토큰: 존재함, completeKey: $isOnboardingCompleted")
                    
                    if (isOnboardingCompleted) {
                        // 온보딩이 완료된 경우 약관 동의 여부와 관계없이 로그인 상태 유지
                        // (온보딩 완료 시 약관 동의 상태는 초기화되므로)
                        _isLoggedIn.value = true
                        Timber.i("온보딩 완료 상태 (completeKey=true) - 로그인 상태 유지")
                    } else {
                        // 온보딩이 완료되지 않은 경우 약관 동의 여부 확인
                        val isTermsAgreed = onboardingDataStore.isTermsAgreed.first()
                        
                        Timber.d("로그인 상태 확인 - completeKey: false, 약관 동의: $isTermsAgreed")
                        
                        if (!isTermsAgreed) {
                            // 토큰은 있지만 약관 동의가 안 되어 있으면 토큰 삭제 처리
                            // (약관 동의 없이 강제 종료한 경우)
                            Timber.w("약관 동의 미완료 상태 감지 - 토큰 삭제 처리")
                            // 토큰만 삭제 (소셜 로그아웃은 하지 않음)
                            tokenProvider.clearTokens()
                            authDataStore.clear()
                            _isLoggedIn.value = false
                            Timber.i("토큰 삭제 완료 - 로그인 필요")
                        } else {
                            // 서버 토큰이 있고 약관 동의도 완료된 경우 로그인 상태로 간주
                            _isLoggedIn.value = true
                            Timber.i("서버 토큰 및 약관 동의 확인됨 - 로그인 상태")
                        }
                    }
                } else {
                    // 서버 토큰이 없으면 로그인 안 된 상태
                    _isLoggedIn.value = false
                    Timber.i("서버 토큰 없음 - 로그인 필요")
                }
            } catch (e: Exception) {
                Timber.e(e, "로그인 상태 확인 실패")
                _isLoggedIn.value = false
            } finally {
                _isLoginChecked.value = true
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
     * 
     * 주의: onboardingDataStore의 completeKey는 삭제하지 않습니다.
     * 온보딩 완료 여부는 로그아웃 후에도 유지되어야 하므로,
     * 로그아웃 시에는 인증 토큰만 삭제하고 온보딩 완료 상태는 보존합니다.
     */
    fun logout() {
        viewModelScope.launch {
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
            // 주의: onboardingDataStore는 건드리지 않음 (completeKey 유지)
            try {
                tokenProvider.clearTokens()
                authDataStore.clear()
                Timber.i("로컬 토큰 및 데이터 삭제 완료 (온보딩 완료 상태는 유지됨)")
            } catch (e: Exception) {
                Timber.e(e, "로컬 데이터 삭제 실패")
            }

            // 로그인 상태 초기화
            _isLoggedIn.value = false
            _uiState.value = LoginUiState.Idle
        }
    }

    /**
     * 강제 재로그인 - 기존 토큰을 모두 삭제하고 처음부터 새로 로그인
     * 
     * 사용 시나리오:
     * - 기존 토큰이 만료되었거나 유효하지 않은 경우
     * - 다른 계정으로 로그인하고 싶은 경우
     * - 토큰 관련 문제 해결을 위해 처음부터 다시 시작하고 싶은 경우
     */
    fun forceReLogin(context: Context) {
        viewModelScope.launch {
            // 1. 기존 토큰 모두 삭제
            tokenProvider.clearTokens()
            authDataStore.clear()
            _isLoggedIn.value = false
            _uiState.value = LoginUiState.Idle
            
            Timber.i("기존 토큰 삭제 완료 - 재로그인 준비됨")
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
                        // 서버 토큰 저장
                        authDataStore.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                        )
                        // TokenProvider도 업데이트 (Flow 구독으로 자동 업데이트되지만 명시적으로 호출)
                        tokenProvider.updateTokens(
                            tokenResponse.accessToken,
                            tokenResponse.refreshToken,
                        )

                        // 로그인 성공 후 온보딩 완료 여부 확인 (completeKey 확인)
                        val isOnboardingCompleted = onboardingDataStore.isCompleted.first()
                        val isTermsAgreed = onboardingDataStore.isTermsAgreed.first()
                        
                        Timber.d("로그인 성공 - completeKey: $isOnboardingCompleted, 약관 동의: $isTermsAgreed")
                        
                        if (isOnboardingCompleted) {
                            Timber.i("로그인 성공 - 온보딩 완료됨 (completeKey=true) → 메인 화면으로 이동 예정")
                        } else if (isTermsAgreed) {
                            Timber.i("로그인 성공 - 약관 동의 완료, 온보딩 미완료 (completeKey=false) → 온보딩 화면으로 이동 예정")
                        } else {
                            Timber.i("로그인 성공 - 약관 동의 미완료 → 약관 동의 다이얼로그 표시 예정")
                        }

                        _isLoggedIn.value = true
                        _uiState.value = LoginUiState.Idle
                        Timber.i("서버 로그인 성공")

                        // FCM 토큰 서버 동기화
                        fcmTokenManager.syncTokenToServer()
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
     * 에러 상태 초기화
     * 에러 다이얼로그를 닫을 때 호출됩니다.
     */
    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}

