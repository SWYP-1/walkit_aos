package team.swyp.sdu.data.remote.user

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import team.swyp.sdu.data.api.follower.FollowerApi
import team.swyp.sdu.data.api.user.TermsAgreementRequest
import team.swyp.sdu.data.api.user.UpdateUserProfileRequest
import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.data.remote.home.dto.UserPointDto
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.domain.model.User
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 정보를 서버에서 가져오는 데이터 소스
 */
@Singleton
class UserRemoteDataSource @Inject constructor(
    private val userApi: UserApi,
    @ApplicationContext private val context: Context,
) {

    /**
     * URI를 File로 변환
     */
    private fun uriToFile(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            tempFile
        } catch (e: Exception) {
            Timber.e(e, "URI를 File로 변환 실패: $uri")
            null
        }
    }

    suspend fun fetchUser(): User {
        return try {
            val dto = userApi.getUser()
            Timber.d("사용자 정보 조회 성공: ${dto.nickname}")
            dto.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "사용자 정보 조회 실패")
            throw e
        }
    }

    /**
     * 닉네임으로 사용자 검색
     *
     * @param nickname 검색할 닉네임
     * @return 검색 결과 (사용자 정보 및 친구 요청 상태)
     * @throws UserNotFoundException 존재하지 않는 유저를 조회한 경우 (404, 1001)
     */
    suspend fun searchUserByNickname(nickname: String): UserSearchResult {
        return try {
            val dto = userApi.searchByNickname(nickname)
            Timber.d("사용자 검색 성공: ${dto.nickName}, 상태: ${dto.followStatus}")
            UserSearchResult(
                userId = dto.userId,
                imageName = dto.imageName,
                nickname = dto.nickName,
                followStatus = dto.getFollowStatusEnum(),
            )
        } catch (e: HttpException) {
            // 404 또는 1001 에러 코드 처리
            when (e.code()) {
                404 -> {
                    Timber.e("사용자를 찾을 수 없습니다: $nickname (404)")
                    throw UserNotFoundException("존재하지 않는 유저입니다", e)
                }
                else -> {
                    // 에러 응답 본문에서 에러 코드 확인
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("1001") == true) {
                        Timber.e("사용자를 찾을 수 없습니다: $nickname (1001)")
                        throw UserNotFoundException("존재하지 않는 유저입니다", e)
                    }
                    Timber.e(e, "사용자 검색 실패: $nickname (HTTP ${e.code()})")
                    throw e
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "사용자 검색 실패: $nickname")
            throw e
        }
    }

    /**
     * 닉네임 등록
     *
     * @param nickname 등록할 닉네임
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun registerNickname(nickname: String) {
        try {
            val response = userApi.registerNickname(nickname)
            if (response.isSuccessful) {
                Timber.d("닉네임 등록 성공: $nickname")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "닉네임 등록 실패"
                Timber.e("닉네임 등록 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("닉네임 등록 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "닉네임 등록 실패: $nickname")
            throw e
        }
    }

    /**
     * 생년월일 업데이트
     *
     * @param birthDate 생년월일 (ISO 8601 형식: "2015-12-04")
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun updateBirthDate(birthDate: String) {
        try {
            val response = userApi.updateBirthDate(birthDate)
            if (response.isSuccessful) {
                Timber.d("생년월일 업데이트 성공: $birthDate")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "생년월일 업데이트 실패"
                Timber.e("생년월일 업데이트 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("생년월일 업데이트 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "생년월일 업데이트 실패: $birthDate")
            throw e
        }
    }

    suspend fun updateUserProfileImage(imageUri: Uri) {
        val file = uriToFile(imageUri, context)
            ?: throw Exception("이미지 파일 변환 실패: $imageUri")

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        try {
            val response = userApi.updateUserProfileImage(body)
            if (response.isSuccessful) {
                Timber.d("사용자 프로필 이미지 업데이트 성공: ${file.name}")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "프로필 이미지 업데이트 실패"
                Timber.e("사용자 프로필 이미지 업데이트 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("프로필 이미지 업데이트 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "사용자 프로필 이미지 업데이트 실패: ${file.name}")
            throw e
        }
    }


    /**
     * 사용자 프로필 업데이트 (온보딩 완료)
     *
     * @param nickname 닉네임
     * @param birthDate 생년월일 (ISO 8601 형식)
     * @param imageUri 선택된 이미지 URI (선택사항, 현재 미사용)
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
    ) {
        try {
            val dto = UpdateUserProfileRequest(
                nickname = nickname,
                birthDate = birthDate,
            )

            val response = userApi.updateUserProfile(dto)
            
            if (response.isSuccessful) {
                Timber.d("사용자 프로필 업데이트 성공: $nickname")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "프로필 업데이트 실패"
                Timber.e("사용자 프로필 업데이트 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("프로필 업데이트 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "사용자 프로필 업데이트 실패: $nickname")
            throw e
        }
    }


    /**
     * 약관 동의
     *
     * @param termsAgreed 서비스 이용약관 동의 여부
     * @param privacyAgreed 개인정보 처리방침 동의 여부
     * @param locationAgreed 위치 정보 이용약관 동의 여부
     * @param marketingConsent 마케팅 정보 수신 동의 여부
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ) {
        try {
            val request = TermsAgreementRequest(
                termsAgreed = termsAgreed,
                privacyAgreed = privacyAgreed,
                locationAgreed = locationAgreed,
                marketingConsent = marketingConsent,
            )
            
            val response = userApi.agreeToTerms(request)
            
            if (response.isSuccessful) {
                Timber.d("약관 동의 성공")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "약관 동의 실패"
                Timber.e("약관 동의 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("약관 동의 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "약관 동의 실패")
            throw e
        }
    }

    /**
     * 유저 포인트 조회
     */
    suspend fun getUserPoint(): UserPointDto {
        return try {
            val data = userApi.getUserPoint()
            Timber.d("유저 포인트 조회 성공: ${data.point}")
            data
        } catch (e: Exception) {
            Timber.e(e, "유저 포인트 조회 실패")
            throw e
        }
    }

}

/**
 * 사용자 검색 결과 도메인 모델
 */
data class UserSearchResult(
    val userId: Long,
    val imageName: String?,
    val nickname: String,
    val followStatus: FollowStatus,
)

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
class UserNotFoundException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
