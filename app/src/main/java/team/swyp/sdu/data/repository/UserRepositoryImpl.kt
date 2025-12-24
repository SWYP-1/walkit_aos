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
import team.swyp.sdu.domain.model.Sex
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber

/**
 * 사용자 정보 Repository 구현체
 *
 * - 메모리(StateFlow) + Room + Remote 병합
 * - 토큰은 DataStore에 저장
 * - Goal 정보는 GoalRepository에서 별도 관리
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val remoteDataSource: UserRemoteDataSource,
    private val authDataStore: AuthDataStore,
) : UserRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userState = MutableStateFlow<User?>(null)
    override val userFlow: StateFlow<User?> = userState.asStateFlow()

    init {
        userDao
            .observeUser()
            .onEach { entity -> userState.value = entity?.let(UserMapper::toDomain) }
            .launchIn(scope)
    }

    override suspend fun getUser(): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = userState.value
                if (currentUser != null) {
                    Result.Success(currentUser)
                } else {
                    // 로컬에 없으면 서버에서 가져오기
                    refreshUser()
                }
            } catch (e: Exception) {
                Timber.e(e, "사용자 조회 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun refreshUser(): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val user = remoteDataSource.fetchUser()
                userDao.upsert(UserMapper.toEntity(user))
                userState.value = user
                Result.Success(user)
            } catch (e: Exception) {
                Timber.e(e, "사용자 프로필 갱신 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateUser(user: User): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 서버 API 구현 시 실제 업데이트 호출
                userDao.upsert(UserMapper.toEntity(user))
                userState.value = user
                Result.Success(user)
            } catch (e: Exception) {
                Timber.e(e, "사용자 업데이트 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun registerNickname(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.registerNickname(nickname)
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "닉네임 등록 실패: $nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateBirthDate(birthDate: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.updateBirthDate(birthDate)
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "생년월일 업데이트 실패: $birthDate")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
        imageUri: String?,
    ): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.updateUserProfile(nickname, birthDate, imageUri)
                // 업데이트 후 최신 정보를 서버에서 가져오기
                refreshUser()
            } catch (e: Exception) {
                Timber.e(e, "사용자 프로필 업데이트 실패: $nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.agreeToTerms(
                    termsAgreed = termsAgreed,
                    privacyAgreed = privacyAgreed,
                    locationAgreed = locationAgreed,
                    marketingConsent = marketingConsent,
                )
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "약관 동의 실패")
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
                userState.value = null
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, e.message)
            }
        }
}
