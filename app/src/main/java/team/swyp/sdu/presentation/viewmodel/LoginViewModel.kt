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
import kotlinx.coroutines.launch
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
class LoginViewModel @Inject constructor() : ViewModel() {
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
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoginChecked.value = false
            try {
                // 토큰 존재 여부 확인
                val hasToken = com.kakao.sdk.auth.AuthApiClient.instance.hasToken()
                if (hasToken) {
                    // 토큰 유효성 확인
                    UserApiClient.instance.accessTokenInfo { _, error ->
                        if (error != null) {
                            // 토큰이 유효하지 않음
                            _isLoggedIn.value = false
                        } else {
                            // 토큰 유효성 체크 성공
                            _isLoggedIn.value = true
                        }
                    }
                } else {
                    _isLoggedIn.value = false
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
                    _uiState.value = LoginUiState.Success(token)
                    _isLoggedIn.value = true
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
                _uiState.value = LoginUiState.Success(token)
                _isLoggedIn.value = true
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
                val refreshToken = NidOAuth.getRefreshToken()
                if (accessToken != null) {
                    Timber.i("네이버 로그인 성공: $accessToken")
                    // 네이버 SDK에서 실제 만료 시간 가져오기 (초 단위 타임스탬프)
                    // nullable이므로 카카오와 동일한 기본값 사용
                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                    val expiresAtSeconds = NidOAuth.getExpiresAt() ?: (currentTimeSeconds + 12 * 60 * 60) // 12시간 (카카오와 동일)
                    // 초 단위 타임스탬프를 Date 객체로 변환 (밀리초로 변환)
                    val accessTokenExpiresAt = Date(expiresAtSeconds * 1000)
                    // 카카오 토큰과 호환되도록 더미 토큰 생성 (실제로는 네이버 토큰을 별도로 관리해야 함)
                    // 카카오와 동일하게 Refresh Token 만료 시간: 2개월 (약 5,184,000초)
                    val refreshTokenExpiresAtSeconds = expiresAtSeconds + (60L * 24 * 60 * 60) // 60일 (2개월, 카카오와 동일)
                    val refreshTokenExpiresAt = Date(refreshTokenExpiresAtSeconds * 1000)
                    val dummyToken = OAuthToken(
                        accessToken = accessToken,
                        refreshToken = refreshToken!!,
                        accessTokenExpiresAt = accessTokenExpiresAt,
                        refreshTokenExpiresAt = refreshTokenExpiresAt,
                    )
                    _uiState.value = LoginUiState.Success(dummyToken)
                    _isLoggedIn.value = true
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
                val refreshToken = NidOAuth.getRefreshToken()
                if (accessToken != null) {
                    Timber.i("네이버 로그인 성공: $accessToken")
                    // 네이버 SDK에서 실제 만료 시간 가져오기 (초 단위 타임스탬프)
                    // nullable이므로 카카오와 동일한 기본값 사용
                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                    val expiresAtSeconds = NidOAuth.getExpiresAt() ?: (currentTimeSeconds + 12 * 60 * 60) // 12시간 (카카오와 동일)
                    // 초 단위 타임스탬프를 Date 객체로 변환 (밀리초로 변환)
                    val accessTokenExpiresAt = Date(expiresAtSeconds * 1000)
                    // 카카오와 동일하게 Refresh Token 만료 시간: 2개월 (약 5,184,000초)
                    val refreshTokenExpiresAtSeconds = expiresAtSeconds + (60L * 24 * 60 * 60) // 60일 (2개월, 카카오와 동일)
                    val refreshTokenExpiresAt = Date(refreshTokenExpiresAtSeconds * 1000)
                    val dummyToken = OAuthToken(
                        accessToken = accessToken,
                        refreshToken = refreshToken!!,
                        accessTokenExpiresAt = accessTokenExpiresAt,
                        refreshTokenExpiresAt = refreshTokenExpiresAt,
                    )
                    _uiState.value = LoginUiState.Success(dummyToken)
                    _isLoggedIn.value = true
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
     */
    fun logout() {
        viewModelScope.launch {
            // 카카오 로그아웃
            UserApiClient.instance.logout { error ->
                if (error != null) {
                    Timber.e(error, "카카오 로그아웃 실패")
                } else {
                    Timber.i("카카오 로그아웃 성공")
                }
            }

            // 네이버 로그아웃
            val naverCallback = object : NidOAuthCallback {
                override fun onSuccess() {
                    Timber.i("네이버 로그아웃 성공")
                }

                override fun onFailure(errorCode: String, errorDesc: String) {
                    Timber.e("네이버 로그아웃 실패: $errorCode - $errorDesc")
                }
            }
            NidOAuth.logout(naverCallback)

            _isLoggedIn.value = false
            _uiState.value = LoginUiState.Idle
        }
    }
}

