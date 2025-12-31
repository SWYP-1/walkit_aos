package team.swyp.sdu.data.remote.user

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response
import team.swyp.sdu.data.api.user.UpdateUserProfileRequest
import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.domain.model.User
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 프로필 관련 원격 데이터 소스
 * - 프로필 조회, 업데이트, 이미지 업로드
 */
@Singleton
class UserProfileRemoteDataSource @Inject constructor(
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

    /**
     * 현재 로그인한 사용자 정보 조회
     */
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
     * 사용자 프로필 업데이트 (온보딩 완료)
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
     * 사용자 프로필 이미지 업데이트
     */
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
     * 프로필 이미지 삭제
     */
    suspend fun deleteImage(imageId: Long): Response<Unit> {
        return try {
            val response = userApi.deleteImage(imageId)
            if (response.isSuccessful) {
                Timber.d("프로필 이미지 삭제 성공: $imageId")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "프로필 이미지 삭제 실패"
                Timber.e("프로필 이미지 삭제 실패: $errorMessage (코드: ${response.code()})")
            }
            response
        } catch (e: Exception) {
            Timber.e(e, "프로필 이미지 삭제 실패: $imageId")
            throw e
        }
    }
}
