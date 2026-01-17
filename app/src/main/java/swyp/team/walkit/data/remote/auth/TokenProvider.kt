package swyp.team.walkit.data.remote.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import swyp.team.walkit.data.api.auth.AuthApi
import swyp.team.walkit.data.api.auth.AuthTokenResponse
import swyp.team.walkit.data.api.auth.RefreshTokenRequest
import swyp.team.walkit.data.local.datastore.AuthDataStore
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 토큰 제공자 인터페이스
 * 동기적으로 토큰을 가져올 수 있도록 캐시된 토큰 제공
 */
interface TokenProvider {
    /**
     * 현재 저장된 액세스 토큰 반환 (동기)
     * @return 액세스 토큰, 없으면 null
     */
    fun getAccessToken(): String?

    /**
     * 현재 저장된 리프레시 토큰 반환 (동기)
     * @return 리프레시 토큰, 없으면 null
     */
    fun getRefreshToken(): String?

    /**
     * 토큰 업데이트 (비동기)
     * @param accessToken 새로운 액세스 토큰
     * @param refreshToken 새로운 리프레시 토큰 (선택적)
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String? = null)

    /**
     * 토큰 삭제
     */
    suspend fun clearTokens()

    /**
     * 토큰 갱신이 필요한 경우 자동으로 갱신 (동시성 제어 포함)
     * @param authApi AuthApi 인스턴스
     * @param refreshToken 리프레시 토큰
     * @return 갱신 성공 여부
     */
    suspend fun refreshTokensIfNeeded(authApi: AuthApi, refreshToken: String): Boolean

    /**
     * 강제 토큰 갱신 (401 발생 시 무조건 리프레시)
     * @param authApi AuthApi 인스턴스
     * @return 갱신 성공 여부
     */
    suspend fun forceRefreshTokens(authApi: AuthApi): Boolean

    fun isRefreshTokenValid(): Boolean
}

/**
 * TokenProvider 구현체
 * AuthDataStore의 Flow를 구독하여 메모리에 캐시
 */
@Singleton
class TokenProviderImpl @Inject constructor(
    private val authDataStore: AuthDataStore,
    @Named("walkit") private val retrofitProvider: Provider<Retrofit>,
) : TokenProvider {
    // 메모리 캐시
    private val _cachedAccessToken = MutableStateFlow<String?>(null)
    private val _cachedRefreshToken = MutableStateFlow<String?>(null)

    // 리프레시 동기화
    private val refreshMutex = Mutex()
    private var isRefreshing = false
    private var currentRefreshDeferred: CompletableDeferred<Boolean>? = null

    // 리프레시 상태 추적
    private var lastRefreshSuccessTime = 0L
    private var lastRefreshFailureTime = 0L
    private val REFRESH_FAILURE_COOLDOWN_MS = 30000L // 30초 쿨다운

    // Flow 구독을 위한 CoroutineScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // AuthDataStore의 Flow를 구독하여 캐시 업데이트
        scope.launch {
            authDataStore.accessToken.collect { token ->
                _cachedAccessToken.value = token
            }
        }

        scope.launch {
            authDataStore.refreshToken.collect { token ->
                _cachedRefreshToken.value = token
            }
        }
    }

    override fun getAccessToken(): String? {
        return _cachedAccessToken.value
    }

    override fun getRefreshToken(): String? {
        return _cachedRefreshToken.value
    }

    override suspend fun updateTokens(accessToken: String, refreshToken: String?) {
        Timber.d(
            "🔑 토큰 저장: accessToken=${accessToken.take(20)}..., refreshToken=${
                refreshToken?.take(
                    20
                )
            }..."
        )
        authDataStore.saveTokens(accessToken, refreshToken)
        // Flow 구독으로 자동 업데이트됨
    }

    override suspend fun clearTokens() {
        authDataStore.clear()
        // Flow 구독으로 자동 업데이트됨
    }

    override suspend fun refreshTokensIfNeeded(authApi: AuthApi, refreshToken: String): Boolean {
        return doRefreshTokens(authApi, forceRefresh = false)
    }

    /**
     * 강제 토큰 갱신 (401 발생 시 사용)
     */
    override suspend fun forceRefreshTokens(authApi: AuthApi): Boolean {
        return doRefreshTokens(authApi, forceRefresh = true)
    }

    /**
     * 리프레시 토큰이 유효한지 확인 (실패 쿨다운 중인지)
     */
    override fun isRefreshTokenValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastRefreshFailureTime >= REFRESH_FAILURE_COOLDOWN_MS
    }

    /**
     * 공통 토큰 갱신 로직 - AuthApi 방식
     * @param forceRefresh true면 캐시된 토큰 존재 여부와 관계없이 강제 리프레시
     */
    private suspend fun doRefreshTokens(authApi: AuthApi, forceRefresh: Boolean = false): Boolean {
        val currentTime = System.currentTimeMillis()

        // 🔍 최근 리프레시 실패 후 쿨다운 기간인지 확인 (불필요한 재시도 방지)
        if (!forceRefresh && currentTime - lastRefreshFailureTime < REFRESH_FAILURE_COOLDOWN_MS) {
            Timber.d("TokenProvider - 최근 리프레시 실패 후 쿨다운 기간(${REFRESH_FAILURE_COOLDOWN_MS}ms), 리프레시 생략")
            return false
        }

        // 🔍 리프레시 진행 중인지 확인 (CompletableDeferred로 대기)
        if (isRefreshing && currentRefreshDeferred != null && !forceRefresh) {
            Timber.d("TokenProvider - 다른 요청에서 리프레시 진행 중, 결과 대기")

            // ✅ 효율적 대기 (타임아웃 10초)
            return withTimeoutOrNull(10000) {
                currentRefreshDeferred?.await()
            } ?: run {
                Timber.w("TokenProvider - 리프레시 대기 시간 초과")
                false
            }
        }

        // 🔍 이미 유효한 토큰이 있는지 먼저 확인 (중복 refresh 방지)
        // 단, forceRefresh가 true이면 캐시 확인 생략
        if (!forceRefresh) {
            val currentToken = getAccessToken()
            if (!currentToken.isNullOrBlank()) {
                Timber.d("TokenProvider - 이미 유효한 토큰 존재(${currentToken.take(10)}...), refresh 생략")
                return true
            }
        }

        // ✅ 리프레시 작업 시작 (Mutex로 보호)
        return refreshMutex.withLock {
            isRefreshing = true
            currentRefreshDeferred = CompletableDeferred()

            try {
                val refreshToken = getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    Timber.w("리프레시 토큰 없음")
                    clearTokens()
                    currentRefreshDeferred?.complete(false)
                    isRefreshing = false
                    currentRefreshDeferred = null
                    return false
                }

                Timber.d("토큰 갱신 시작")
                val refreshRequest = RefreshTokenRequest(refreshToken)
                val response = authApi.refreshToken(refreshRequest)

                if (response.isSuccessful) {
                    val newTokens = response.body()
                    if (newTokens?.accessToken?.isNotBlank() == true) {
                        updateTokens(newTokens.accessToken, newTokens.refreshToken)
                        lastRefreshSuccessTime = System.currentTimeMillis()
                        lastRefreshFailureTime = 0L // 성공 시 실패 쿨다운 리셋
                        Timber.i("토큰 갱신 성공")
                        currentRefreshDeferred?.complete(true)
                        isRefreshing = false
                        currentRefreshDeferred = null
                        return true
                    }
                }

                Timber.e("토큰 갱신 실패: ${response.code()}")
                lastRefreshFailureTime = System.currentTimeMillis()
                clearTokens()
                currentRefreshDeferred?.complete(false)
                isRefreshing = false
                currentRefreshDeferred = null
                return false

            } catch (e: Exception) {
                Timber.e("토큰 갱신 예외: ${e.message}")
                lastRefreshFailureTime = System.currentTimeMillis()
                clearTokens()
                currentRefreshDeferred?.complete(false)
                isRefreshing = false
                currentRefreshDeferred = null
                return false
            }
        }
    }
}

