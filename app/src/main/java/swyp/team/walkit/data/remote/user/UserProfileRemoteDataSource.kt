package swyp.team.walkit.data.remote.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response
import swyp.team.walkit.data.api.user.UpdateUserProfileRequest
import swyp.team.walkit.data.api.user.UserApi
import swyp.team.walkit.domain.model.User
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
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
     * URI를 압축된 File로 변환
     */
    private fun uriToCompressedFile(uri: Uri, context: Context): File? {
        return try {
            // 먼저 원본 Bitmap 로드
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            // 샘플링 사이즈 계산 (최대 1024px로 제한)
            val maxDimension = 1024
            var sampleSize = 1
            while ((originalWidth / sampleSize) > maxDimension || (originalHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            // 실제 Bitmap 디코딩 (압축 적용)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // EXIF orientation 읽기 및 이미지 회전 적용
            val orientedBitmap = try {
                val orientation = getExifOrientation(uri, context)
                rotateBitmap(originalBitmap, orientation)
            } catch (e: Exception) {
                Timber.w(e, "EXIF orientation 처리 실패, 원본 이미지 사용")
                originalBitmap
            }

            // 최종 크기 제한 (1024x1024)
            val scaledBitmap = if (orientedBitmap.width > maxDimension || orientedBitmap.height > maxDimension) {
                val scale = minOf(maxDimension.toFloat() / orientedBitmap.width, maxDimension.toFloat() / orientedBitmap.height)
                val newWidth = (orientedBitmap.width * scale).toInt()
                val newHeight = (orientedBitmap.height * scale).toInt()

                Bitmap.createScaledBitmap(orientedBitmap, newWidth, newHeight, true)
            } else {
                orientedBitmap
            }

            // 압축된 파일로 저장
            val tempFile = File.createTempFile("upload_compressed_", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { output ->
                // JPEG 품질 80%로 압축
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }

            // 메모리 해제 (scaledBitmap 사용 후)
            // rotateBitmap()에서 이미 원본 bitmap을 recycle하므로, 여기서는 scaledBitmap만 해제
            // orientedBitmap이 scaledBitmap과 다르면 (스케일링이 적용된 경우) 해제
            if (orientedBitmap != scaledBitmap) {
                orientedBitmap.recycle()
            } else {
                // orientedBitmap == scaledBitmap인 경우, rotateBitmap에서 이미 recycle했으므로
                // originalBitmap만 해제 (회전이 적용되지 않은 경우)
                if (orientedBitmap == originalBitmap) {
                    scaledBitmap.recycle()
                }
            }

            Timber.d("이미지 압축 완료: ${originalWidth}x${originalHeight} -> ${scaledBitmap.width}x${scaledBitmap.height}, 파일 크기: ${tempFile.length()} bytes")
            tempFile
        } catch (t: Throwable) {
            Timber.e(t, "이미지 압축 실패: $uri")
            null
        }
    }

    /**
     * EXIF orientation 값 읽기
     */
    private fun getExifOrientation(uri: Uri, context: Context): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            Timber.w(e, "EXIF orientation 읽기 실패")
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * EXIF orientation에 따라 Bitmap 회전
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        return when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                val matrix = Matrix().apply { postScale(-1f, 1f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                val matrix = Matrix().apply { postRotate(180f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                val matrix = Matrix().apply { postScale(1f, -1f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                val matrix = Matrix().apply {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                val matrix = Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                val matrix = Matrix().apply {
                    postRotate(-90f)
                    postScale(-1f, 1f)
                }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                val matrix = Matrix().apply { postRotate(270f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            }
            else -> bitmap
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
        } catch (t: Throwable) {
            Timber.e(t, "사용자 정보 조회 실패")
            throw t
        }
    }

    /**
     * 사용자 프로필 업데이트 (온보딩 완료)
     */
    suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
    ): Response<Unit> {
        val dto = UpdateUserProfileRequest(
            nickname = nickname,
            birthDate = birthDate,
        )
        return try {
            val response = userApi.updateUserProfile(dto)
            Timber.d("프로필 업데이트 호출 완료: $nickname (HTTP ${response.code()})")
            response
        } catch (t: Throwable) {
            Timber.e(t, "프로필 업데이트 호출 실패: $nickname")
            throw t
        }
    }

    /**
     * 사용자 프로필 이미지 업데이트
     */
    suspend fun updateUserProfileImage(imageUri: Uri) {
        val file = uriToCompressedFile(imageUri, context)
            ?: throw Exception("이미지 압축 실패: $imageUri")

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        try {
            val response = userApi.updateUserProfileImage(body)
            if (response.isSuccessful) {
                Timber.d("사용자 프로필 이미지 업데이트 성공: ${file.name} (${file.length()} bytes)")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "프로필 이미지 업데이트 실패"
                Timber.e("사용자 프로필 이미지 업데이트 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("프로필 이미지 업데이트 실패: ${response.code()}")
            }
        } catch (t: Throwable) {
            Timber.e(t, "사용자 프로필 이미지 업데이트 실패: ${file.name}")
            throw t
        } finally {
            // 임시 파일 정리
            try {
                file.delete()
            } catch (t: Throwable) {
                Timber.w(t, "임시 파일 삭제 실패: ${file.absolutePath}")
            }
        }
    }

    /**
     * 프로필 이미지 삭제
     */
    suspend fun deleteImage(): Response<Unit> {
        return try {
            val response = userApi.deleteImage()
            if (response.isSuccessful) {
                Timber.d("프로필 이미지 삭제 성공:")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "프로필 이미지 삭제 실패"
                Timber.e("프로필 이미지 삭제 실패: $errorMessage (코드: ${response.code()})")
            }
            response
        } catch (t: Throwable) {
            Timber.e(t, "프로필 이미지 삭제 실패:")
            throw t
        }
    }
}
