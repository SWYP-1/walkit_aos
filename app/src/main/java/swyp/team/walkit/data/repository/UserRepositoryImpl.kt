    package swyp.team.walkit.data.repository

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
import retrofit2.HttpException
import retrofit2.Response
import swyp.team.walkit.data.remote.dto.ApiErrorResponse
import kotlinx.serialization.json.Json
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.local.dao.UserDao
import swyp.team.walkit.data.local.datastore.AuthDataStore
import swyp.team.walkit.data.local.mapper.UserMapper
import swyp.team.walkit.data.remote.user.UserManagementRemoteDataSource
import swyp.team.walkit.data.remote.user.UserProfileRemoteDataSource
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.data.remote.user.UserSearchResult as RemoteUserSearchResult
import swyp.team.walkit.data.remote.user.UserSummaryMapper
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.domain.model.UserProfile
import swyp.team.walkit.domain.model.UserSearchResult
import swyp.team.walkit.domain.model.UserSummary
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import java.io.IOException

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
    private val userManagementRemoteDataSource: UserManagementRemoteDataSource,
    private val userProfileRemoteDataSource: UserProfileRemoteDataSource,
    private val authDataStore: AuthDataStore,
) : UserRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userState = MutableStateFlow<User?>(null)
    override val userFlow: StateFlow<User?> = userState.asStateFlow()

    init {
        // ✅ Room 만이 userState 를 변경한다
        Timber.d("UserRepositoryImpl: observeCurrentUser 시작")
        userDao.observeCurrentUser()
            .onEach { entity ->
                Timber.d("UserRepositoryImpl: observeCurrentUser emit - entity=$entity")
                val user = entity?.let(UserMapper::toDomain)
                Timber.d("UserRepositoryImpl: userState 업데이트 - user=$user")
                userState.value = user
            }
            .launchIn(scope)
    }

    override suspend fun getUser(): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // ✅ Room에서 직접 값을 가져옴 (Single Source of Truth)
                val entity = userDao.getCurrentUser()
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
                // DB 조회 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "사용자 조회 실패")
                Result.Error(e, "사용자 정보를 불러올 수 없습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun refreshUser(): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val user = remoteDataSource.fetchUser() // DTO 직접 가져오기
                Timber.d("사용자 정보 API 응답: userId=${user.userId}, nickname=${user.nickname}, imageName=${user.imageName}")

                // ✅ 이전 사용자 데이터 삭제 후 새 사용자 데이터 저장
                // PrimaryKey가 nickname이므로 다른 사용자로 로그인 시 여러 레코드가 쌓일 수 있음
                userDao.clear()
                val entity = UserMapper.toEntity(user)
                userDao.upsert(entity)
                Timber.d("사용자 정보 DB 저장: userId=${entity.userId}, nickname=${entity.nickname}")

                // 저장 확인
                val savedEntity = userDao.getCurrentUser()
                Timber.d("Room 저장 확인: userId=${savedEntity?.userId}, nickname=${savedEntity?.nickname}, imageName=${savedEntity?.imageName}")

                Result.Success(user)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "refreshUser")
                Timber.e(e, "사용자 프로필 갱신 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "refreshUser")
                Timber.e(e, "사용자 프로필 갱신 실패: HTTP ${e.code()}")
                Result.Error(e, "사용자 정보를 갱신할 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun updateUser(user: User): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: 서버 API 연동 시 Remote → Room 으로 변경
                userDao.upsert(UserMapper.toEntity(user))
                Result.Success(user)
            } catch (e: Exception) {
                // DB 업데이트 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "사용자 업데이트 실패")
                Result.Error(e, "사용자 정보를 업데이트할 수 없습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun registerNickname(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = remoteDataSource.registerNickname(nickname)

                if (response.isSuccessful) {
                    // HTTP 2xx 성공 - 로컬 DB 업데이트
                    val currentUser = userDao.getCurrentUser()
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(nickname = nickname)
                        userDao.upsert(updatedUser)
                        Timber.d("닉네임 로컬 DB 업데이트 성공: $nickname")
                    } else {
                        Timber.w("로컬 DB에 사용자 정보가 없어 닉네임 업데이트 생략")
                    }
                    Result.Success(Unit)
                } else {
                    // HTTP 실패 (4xx, 5xx)
                    val errorBody = response.errorBody()?.string()
                    val apiError = errorBody?.let {
                        try {
                            Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorResponse>(it)
                        } catch (e: Exception) {
                            Timber.w(e, "ApiErrorResponse 파싱 실패, 기본 에러 사용")
                            null
                        }
                    }

                    return@withContext when (response.code()) {
                        409 -> Result.Error(
                            Exception("NICKNAME_DUPLICATE_ERROR"),
                            apiError?.message ?: "중복된 닉네임입니다"
                        )
                        400 -> Result.Error(
                            Exception("NICKNAME_VALIDATION_ERROR"),
                            apiError?.message ?: "닉네임 규칙 위반"
                        )
                        else -> Result.Error(
                            Exception("HTTP_ERROR"),
                            apiError?.message ?: "닉네임 등록 실패"
                        )
                    }
                }
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "registerNickname")
                Timber.e(e, "닉네임 등록 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "registerNickname")
                Timber.e(e, "닉네임 등록 실패: HTTP ${e.code()}")
                Result.Error(e, "닉네임 등록에 실패했습니다")
            } catch (e: kotlinx.serialization.SerializationException) {
                // JSON 파싱 오류: 복구 가능
                CrashReportingHelper.logException(e)
                Timber.e(e, "닉네임 등록 실패: JSON 파싱 오류")
                Result.Error(e, "서버 응답을 처리할 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }


    override suspend fun updateBirthDate(birthDate: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 생년월일 업데이트
                remoteDataSource.updateBirthDate(birthDate)
                Timber.d("생년월일 서버 업데이트 성공: $birthDate")

                // 로컬 DB 업데이트 - 현재 사용자 정보 가져와서 birthDate만 수정
                Timber.d("UserRepositoryImpl: 생년월일 로컬 DB 업데이트 시작 - birthDate=$birthDate")
                val currentUser = userDao.getCurrentUser()
                Timber.d("UserRepositoryImpl: 현재 DB 사용자 정보 - currentUser=$currentUser")
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(birthDate = birthDate)
                    Timber.d("UserRepositoryImpl: 업데이트할 사용자 정보 - updatedUser=$updatedUser")
                    userDao.upsert(updatedUser)
                    Timber.d("생년월일 로컬 DB 업데이트 성공: $birthDate")

                    // 업데이트 후 확인
                    val afterUpdate = userDao.getCurrentUser()
                    Timber.d("UserRepositoryImpl: 업데이트 후 DB 상태 - afterUpdate=$afterUpdate")
                } else {
                    Timber.w("로컬 DB에 사용자 정보가 없어 생년월일 업데이트 생략")
                }

                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "updateBirthDate")
                Timber.e(e, "생년월일 업데이트 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "updateBirthDate")
                Timber.e(e, "생년월일 업데이트 실패: HTTP ${e.code()}")
                Result.Error(e, "생년월일 업데이트에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun updateUserProfileImage(imageUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 서버에 이미지 업로드만 수행, Room 업데이트는 ViewModel에서 refreshUser()로 처리
                remoteDataSource.updateUserProfileImage(imageUri)
                Timber.d("프로필 이미지 서버 업로드 완료: $imageUri")
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "updateUserProfileImage")
                Timber.e(e, "사용자 프로필 이미지 업데이트 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "updateUserProfileImage")
                Timber.e(e, "사용자 프로필 이미지 업데이트 실패: HTTP ${e.code()}")
                Result.Error(e, "프로필 이미지 업데이트에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = remoteDataSource.updateUserProfile(
                    nickname = nickname,
                    birthDate = birthDate,
                )
                if (response.isSuccessful) {
                    // HTTP 2xx 성공 - 로컬 DB 업데이트
                    try {
                        val currentUser = userDao.getCurrentUser()
                        if (currentUser != null) {
                            val updatedUser = currentUser.copy(
                                nickname = nickname,
                                birthDate = birthDate
                            )
                            userDao.upsert(updatedUser)
                            Timber.d("프로필 업데이트 성공: 로컬 DB 업데이트됨")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "로컬 DB 업데이트 실패")
                        // 서버 업데이트는 성공했으므로 Result.Success 반환
                    }
                    Result.Success(Unit)
                } else {
                    // HTTP 실패 (4xx, 5xx)
                    val errorBody = response.errorBody()?.string()
                    val apiError = errorBody?.let {
                        try {
                            Json { ignoreUnknownKeys = true }.decodeFromString<ApiErrorResponse>(it)
                        } catch (e: Exception) {
                            Timber.w(e, "ApiErrorResponse 파싱 실패, 기본 에러 사용")
                            null
                        }
                    }

                    return@withContext when (response.code()) {
                        409 -> Result.Error(
                            Exception("NICKNAME_DUPLICATE_ERROR"),
                            apiError?.message ?: "중복된 닉네임입니다"
                        )
                        400 -> Result.Error(
                            Exception("NICKNAME_VALIDATION_ERROR"),
                            apiError?.message ?: "닉네임 규칙 위반"
                        )
                        else -> Result.Error(
                            Exception("HTTP_ERROR"),
                            apiError?.message ?: "닉네임 등록 실패"
                        )
                    }
                }
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "updateUserProfile")
                Timber.e(e, "사용자 프로필 업데이트 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "updateUserProfile")
                Timber.e(e, "사용자 프로필 업데이트 실패: HTTP ${e.code()}")
                Result.Error(e, "프로필 업데이트에 실패했습니다")
            } catch (e: kotlinx.serialization.SerializationException) {
                // JSON 파싱 오류: 복구 가능
                CrashReportingHelper.logException(e)
                Timber.e(e, "사용자 프로필 업데이트 실패: JSON 파싱 오류")
                Result.Error(e, "서버 응답을 처리할 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "agreeToTerms")
                Timber.e(e, "약관 동의 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "agreeToTerms")
                Timber.e(e, "약관 동의 실패: HTTP ${e.code()}")
                Result.Error(e, "약관 동의에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
                // DB 삭제 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "인증 정보 삭제 실패")
                Result.Error(e, "인증 정보를 삭제할 수 없습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun searchUserByNickname(nickname: String): Result<List<UserSearchResult>> =
        withContext(Dispatchers.IO) {
            try {
                val remoteResult = remoteDataSource.searchUserByNickname(nickname)
                val domainResults = remoteResult.map { result ->
                    UserSearchResult(
                        userId = result.userId,
                        imageName = result.imageName,
                        nickname = result.nickname,
                        followStatus = result.followStatus,
                    )
                }
                Timber.d("사용자 검색 성공: ${domainResults.size}개 결과")
                Result.Success(domainResults)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "searchUserByNickname")
                Timber.e(e, "사용자 검색 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "searchUserByNickname")
                Timber.e(e, "사용자 검색 실패: HTTP ${e.code()}")
                Result.Error(e, "사용자 검색에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
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
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "getUserSummaryByNickname")
                Timber.e(e, "사용자 요약 정보 조회 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "getUserSummaryByNickname")
                Timber.e(e, "사용자 요약 정보 조회 실패: HTTP ${e.code()}")
                Result.Error(e, "사용자 정보를 불러올 수 없습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun deleteUser(): Result<Response<Unit>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userManagementRemoteDataSource.deleteUser()
                Timber.d("사용자 탈퇴 요청 완료")
                Result.Success(response)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "deleteUser")
                Timber.e(e, "사용자 탈퇴 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "deleteUser")
                Timber.e(e, "사용자 탈퇴 실패: HTTP ${e.code()}")
                Result.Error(e, "사용자 탈퇴에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }

    override suspend fun deleteImage(): Result<Response<Unit>> =
        withContext(Dispatchers.IO) {
            try {
                val response = userProfileRemoteDataSource.deleteImage()
                Timber.d("프로필 이미지 삭제 요청 완료:")
                Result.Success(response)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "deleteImage")
                Timber.e(e, "프로필 이미지 삭제 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "deleteImage")
                Timber.e(e, "프로필 이미지 삭제 실패: HTTP ${e.code()}")
                Result.Error(e, "프로필 이미지 삭제에 실패했습니다")
            }
            // NullPointerException 등 치명적 오류는 catch하지 않음
        }
}