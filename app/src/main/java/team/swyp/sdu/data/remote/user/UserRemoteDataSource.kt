package team.swyp.sdu.data.remote.user

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import team.swyp.sdu.data.api.user.UpdateUserProfileRequest
import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.domain.model.Sex
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
     */
    suspend fun searchUserByNickname(nickname: String): UserSearchResult {
        return try {
            val dto = userApi.searchByNickname(nickname)
            Timber.d("사용자 검색 성공: ${dto.nickname}, 상태: ${dto.followStatus}")
            UserSearchResult(
                userId = dto.userId,
                imageName = dto.imageName,
                nickname = dto.nickname,
                followStatus = dto.getFollowStatusEnum(),
            )
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
        imageUri: String? = null,
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
