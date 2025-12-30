package team.swyp.sdu.data.repository

import android.net.Uri
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
import team.swyp.sdu.data.remote.user.UserSearchResult as RemoteUserSearchResult
import team.swyp.sdu.data.remote.user.UserSummaryMapper
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.UserProfile
import team.swyp.sdu.domain.model.UserSearchResult
import team.swyp.sdu.domain.model.UserSummary
import team.swyp.sdu.domain.repository.UserRepository
import timber.log.Timber

/**
 * User Repository
 *
 * Single Source of Truth:
 * Remote → Room → Flow → StateFlow
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
        // ✅ Room 만이 userState 를 변경한다
        userDao.observeUser()
            .onEach { entity ->
                userState.value = entity?.let(UserMapper::toDomain)
            }
            .launchIn(scope)
    }

    override suspend fun getUser(): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // ✅ Room에서 직접 값을 가져옴 (Single Source of Truth)
                val entity = userDao.getUser()
                if (entity != null) {
                    val user = UserMapper.toDomain(entity)
                    Timber.d("Room에서 사용자 조회: nickname=${user.nickname}, imageName=${user.imageName}")
                    Result.Success(user)
                } else {
                    Timber.d("Room에 사용자 정보 없음, 서버에서 가져오기")
                    // 캐시에 없으면 서버에서 가져오기
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
                val user = remoteDataSource.fetchUser() // DTO 직접 가져오기
                Timber.d("사용자 정보 API 응답: nickname=${user.nickname}, imageName=${user.imageName}")

                // ✅ 이전 사용자 데이터 삭제 후 새 사용자 데이터 저장
                // PrimaryKey가 nickname이므로 다른 사용자로 로그인 시 여러 레코드가 쌓일 수 있음
                userDao.clear()
                val entity = UserMapper.toEntity(user)
                userDao.upsert(entity)

                // 저장 확인
                val savedEntity = userDao.getUser()
                Timber.d("Room 저장 확인: nickname=${savedEntity?.nickname}, imageName=${savedEntity?.imageName}")

                Result.Success(user)
            } catch (e: Exception) {
                Timber.e(e, "사용자 프로필 갱신 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateUser(user: User): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 서버 API 연동 시 Remote → Room 으로 변경
                userDao.upsert(UserMapper.toEntity(user))
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

    override suspend fun updateUserProfileImage(imageUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 이미지 업로드만 수행, Room 업데이트는 ViewModel에서 refreshUser()로 처리
                remoteDataSource.updateUserProfileImage(imageUri)
                Timber.d("프로필 이미지 서버 업로드 완료: $imageUri")
                Result.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "사용자 프로필 이미지 업데이트 실패: $imageUri")
                Result.Error(e, e.message)
            }
        }

    override suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
    ): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                remoteDataSource.updateUserProfile(
                    nickname = nickname,
                    birthDate = birthDate,
                )
                // 서버에만 업데이트하고 Room은 ViewModel에서 refreshUser()로 처리
                val updatedUser = remoteDataSource.fetchUser()
                Result.Success(updatedUser)
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

    override suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String?,
    ) {
        withContext(Dispatchers.IO) {
            authDataStore.saveTokens(accessToken, refreshToken)
        }
    }

    override suspend fun clearAuth(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                authDataStore.clear()
                userDao.clear() // 🔥 Room clear → Flow emit → StateFlow null
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, e.message)
            }
        }

    override suspend fun searchUserByNickname(nickname: String): Result<UserSearchResult> =
        withContext(Dispatchers.IO) {
            try {
                val remoteResult = remoteDataSource.searchUserByNickname(nickname)
                val domainResult = UserSearchResult(
                    userId = remoteResult.userId,
                    imageName = remoteResult.imageName,
                    nickname = remoteResult.nickname,
                    followStatus = remoteResult.followStatus,
                )
                Timber.d("사용자 검색 성공: ${domainResult.nickname}")
                Result.Success(domainResult)
            } catch (e: Exception) {
                Timber.e(e, "사용자 검색 실패: $nickname")
                Result.Error(e, e.message)
            }
        }

    override suspend fun getUserSummaryByNickname(
        nickname: String,
        lat: Double,
        lon: Double,
    ): Result<UserSummary> =
        withContext(Dispatchers.IO) {
            try {
                val dto = remoteDataSource.getUserSummaryByNickname(nickname, lat, lon)
                val domainResult = UserSummaryMapper.toDomain(dto)
                Timber.d("사용자 요약 정보 조회 성공: ${domainResult.character.nickName}")
                Result.Success(domainResult)
            } catch (e: Exception) {
                Timber.e(e, "사용자 요약 정보 조회 실패: $nickname")
                Result.Error(e, e.message)
            }
        }
}