package team.swyp.sdu.data.remote.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.data.local.datastore.AuthDataStore
import timber.log.Timber
import javax.inject.Inject
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
}

/**
 * TokenProvider 구현체
 * AuthDataStore의 Flow를 구독하여 메모리에 캐시
 */
@Singleton
class TokenProviderImpl @Inject constructor(
    private val authDataStore: AuthDataStore,
) : TokenProvider {
    // 메모리 캐시
    private val _cachedAccessToken = MutableStateFlow<String?>(null)
    private val _cachedRefreshToken = MutableStateFlow<String?>(null)

    // Flow 구독을 위한 Scope (앱 생명주기와 독립적)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // AuthDataStore의 Flow를 구독하여 캐시 업데이트
        scope.launch {
            authDataStore.accessToken.collect { token ->
                _cachedAccessToken.value = token
                Timber.d("액세스 토큰 캐시 업데이트: ${token?.take(10)}...")
            }
        }

        scope.launch {
            authDataStore.refreshToken.collect { token ->
                _cachedRefreshToken.value = token
                Timber.d("리프레시 토큰 캐시 업데이트: ${token?.take(10)}...")
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
        authDataStore.saveTokens(accessToken, refreshToken)
        // Flow 구독으로 자동 업데이트됨
    }

    override suspend fun clearTokens() {
        authDataStore.clear()
        // Flow 구독으로 자동 업데이트됨
    }
}






