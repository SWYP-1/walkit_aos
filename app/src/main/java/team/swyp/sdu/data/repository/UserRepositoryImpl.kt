package team.swyp.sdu.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.local.dao.UserDao
import team.swyp.sdu.data.local.datastore.AuthDataStore
import team.swyp.sdu.data.local.mapper.UserMapper
import team.swyp.sdu.data.remote.user.UserRemoteDataSource
import team.swyp.sdu.domain.model.UserProfile
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber

/**
 * 사용자 정보 Repository 구현체
 *
 * - 메모리(StateFlow) + Room + Remote 병합
 * - 토큰은 DataStore에 저장
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteDataSource: UserRemoteDataSource,
    private val authDataStore: AuthDataStore,
) : UserRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userProfileState = MutableStateFlow<UserProfile?>(null)
    override val userProfileFlow: StateFlow<UserProfile?> = userProfileState.asStateFlow()

    init {
        userDao
            .observeUser()
            .onEach { entity -> userProfileState.value = entity?.let(UserMapper::toDomain) }
            .launchIn(scope)
    }

    override suspend fun refreshUserProfile(): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            try {
                val profile = remoteDataSource.fetchUserProfile()
                userDao.upsert(UserMapper.toEntity(profile))
                userProfileState.value = profile
                Result.Success(profile)
            } catch (e: Exception) {
                Timber.e(e, "사용자 프로필 갱신 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun saveAuthTokens(accessToken: String, refreshToken: String?) {
        withContext(Dispatchers.IO) {
            authDataStore.saveTokens(accessToken, refreshToken)
        }
    }

    override suspend fun clearAuth(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                authDataStore.clear()
                userDao.clear()
                userProfileState.value = null
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, e.message)
            }
        }
}
