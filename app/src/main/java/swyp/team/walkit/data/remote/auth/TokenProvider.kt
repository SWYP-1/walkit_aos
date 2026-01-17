package swyp.team.walkit.data.remote.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
     * 401 발생 시 토큰 갱신 (앱 전체에서 단일 refresh 보장)
     * @param authApi AuthApi 인스턴스
     * @return 갱신 성공 여부
     */
    suspend fun refreshTokensOn401(authApi: AuthApi): Boolean

    fun isRefreshTokenValid(): Boolean

    /**
     * 마지막 토큰 갱신 성공 시간을 반환
     * @return 마지막 성공 시간 (밀리초)
     */
    fun getLastRefreshSuccessTime(): Long

    /**
     * 현재 토큰 갱신이 진행 중인지 확인
     * @return 진행 중이면 true
     */
    fun isRefreshing(): Boolean

    /**
     * 진행 중인 리프레시 완료를 대기 (동기 버전)
     * @param timeoutMs 타임아웃 시간 (기본 10초)
     * @return 성공 여부, 타임아웃 시 false
     */
    fun awaitRefreshCompletionSync(timeoutMs: Long = 10000): Boolean
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


    // 리프레시 상태 추적
    private var lastRefreshSuccessTime = 0L
    private var lastRefreshFailureTime = 0L
    private val REFRESH_FAILURE_COOLDOWN_MS = 10000L // 10초 쿨다운

    // 단일 refresh 보장을 위한 상태 관리
    private val singleRefreshMutex = Mutex() // 앱 전체 단일 refresh 보장
    private var currentRefreshJob: CompletableDeferred<Boolean>? = null
    private var refreshTokenConsumed = false // refresh token 사용 여부 추적

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
        return _cachedAccessToken.value ?: runBlocking(Dispatchers.IO) {
            try {
                authDataStore.accessToken.first()
            } catch (e: Exception) {
                Timber.w("TokenProvider - DataStore에서 액세스 토큰 로드 실패: ${e.message}")
                null
            }
        }
    }

    override fun getRefreshToken(): String? {
        return _cachedRefreshToken.value ?: runBlocking(Dispatchers.IO) {
            try {
                authDataStore.refreshToken.first()
            } catch (e: Exception) {
                Timber.w("TokenProvider - DataStore에서 리프레시 토큰 로드 실패: ${e.message}")
                null
            }
        }
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

        // 토큰 클리어 시 refresh token 소비 상태도 리셋
        refreshTokenConsumed = false
        Timber.d("TokenProvider - 토큰 클리어, refresh token 소비 상태 리셋")
    }

    /**
     * 401 발생 시 토큰 갱신 (앱 전체에서 단일 refresh 보장)
     * 서버가 refresh token을 한 번만 허용하므로 중복 요청 방지
     */
    override suspend fun refreshTokensOn401(authApi: AuthApi): Boolean {
        // 🔒 앱 전체에서 단 하나의 refresh만 수행되도록 보장
        return singleRefreshMutex.withLock {
            val currentTime = System.currentTimeMillis()

            // 1️⃣ 이미 진행 중인 refresh가 있는 경우 결과 대기
            currentRefreshJob?.let { job ->
                Timber.d("TokenProvider - 이미 진행 중인 refresh가 있음, 결과 대기")
                return withTimeoutOrNull(15000) { // 15초 타임아웃
                    job.await()
                } ?: run {
                    Timber.w("TokenProvider - refresh 대기 시간 초과")
                    false
                }
            }

            // 2️⃣ refresh token이 이미 소비되었는지 확인
            if (refreshTokenConsumed) {
                Timber.w("TokenProvider - refresh token이 이미 소비됨, 추가 refresh 불가")
                return false
            }

            // 3️⃣ 최근 성공 후 보호 기간인지 확인 (5초 내 재요청 방지)
            if (currentTime - lastRefreshSuccessTime < 5000) {
                Timber.d("TokenProvider - 최근 성공 후 보호 기간 중, 재요청 방지")
                return true // 이미 유효한 토큰이 있다고 간주
            }

            // 4️⃣ 쿨다운 기간 확인
            if (currentTime - lastRefreshFailureTime < REFRESH_FAILURE_COOLDOWN_MS) {
                Timber.d("TokenProvider - 쿨다운 기간 중")
                return false
            }

            // 5️⃣ 새로운 refresh 작업 시작
            Timber.d("TokenProvider - 새로운 refresh 작업 시작")
            val refreshJob = CompletableDeferred<Boolean>()
            currentRefreshJob = refreshJob

            try {
                val refreshToken = getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    Timber.w("TokenProvider - refresh token 없음")
                    refreshJob.complete(false)
                    return false
                }

                // refresh token 사용 표시 (한 번만 사용 가능하므로)
                refreshTokenConsumed = true

                val refreshRequest = RefreshTokenRequest(refreshToken)
                val response = authApi.refreshToken(refreshRequest)

                if (response.isSuccessful) {
                    val newTokens = response.body()
                    if (newTokens?.accessToken?.isNotBlank() == true) {
                        updateTokens(newTokens.accessToken, newTokens.refreshToken)
                        lastRefreshSuccessTime = System.currentTimeMillis()
                        lastRefreshFailureTime = 0L // 성공 시 쿨다운 리셋

                        // 새로운 refresh token을 받았으므로 소비 상태 리셋
                        if (newTokens.refreshToken?.isNotBlank() == true) {
                            refreshTokenConsumed = false
                            Timber.d("TokenProvider - 새로운 refresh token 수신, 소비 상태 리셋")
                        }

                        Timber.i("TokenProvider - refresh 성공")
                        refreshJob.complete(true)
                        return true
                    }
                }

                Timber.e("TokenProvider - refresh 실패: ${response.code()}")
                lastRefreshFailureTime = System.currentTimeMillis()
                clearTokens()
                refreshJob.complete(false)
                return false

            } catch (e: Exception) {
                Timber.e("TokenProvider - refresh 예외: ${e.message}")
                lastRefreshFailureTime = System.currentTimeMillis()
                clearTokens()
                refreshJob.complete(false)
                return false
            } finally {
                currentRefreshJob = null
            }
        }
    }

    /**
     * 리프레시 토큰이 유효한지 확인 (실패 쿨다운 중인지)
     * 단순한 시간 기반 검증으로 변경 - 더 예측 가능하고 안정적
     */
    override fun isRefreshTokenValid(): Boolean {
        val currentTime = System.currentTimeMillis()

        // 최근 실패 후 쿨다운 기간 중인지 확인
        if (currentTime - lastRefreshFailureTime < REFRESH_FAILURE_COOLDOWN_MS) {
            Timber.d("TokenProvider - 최근 리프레시 실패 후 쿨다운 기간 중 (${(REFRESH_FAILURE_COOLDOWN_MS - (currentTime - lastRefreshFailureTime))/1000}초 남음)")
            return false
        }

        return true
    }

    override fun getLastRefreshSuccessTime(): Long {
        return lastRefreshSuccessTime
    }

    override fun isRefreshing(): Boolean {
        return currentRefreshJob != null
    }

    override fun awaitRefreshCompletionSync(timeoutMs: Long): Boolean {
        val job = currentRefreshJob ?: return false

        return runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                job.await()
            } ?: false
        }
    }

}

