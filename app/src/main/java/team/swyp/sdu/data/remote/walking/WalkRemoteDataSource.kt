package team.swyp.sdu.data.remote.walking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import team.swyp.sdu.data.api.follower.FollowerApi
import team.swyp.sdu.data.api.walking.WalkApi
import team.swyp.sdu.data.model.WalkingSession
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import team.swyp.sdu.data.remote.dto.ApiErrorResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.swyp.sdu.core.Result
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto
import team.swyp.sdu.data.remote.walking.dto.UpdateWalkNoteRequest
import team.swyp.sdu.data.remote.walking.dto.WalkingSessionRequest
import team.swyp.sdu.data.remote.walking.dto.WalkSaveResponse
import team.swyp.sdu.data.remote.walking.mapper.toWalkPoints
import androidx.core.net.toUri
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * 산책 데이터 서버 전송 데이터 소스
 */
@Singleton
class WalkRemoteDataSource @Inject constructor(
    private val walkApi: WalkApi,
    private val followerApi: FollowerApi,
    @ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 산책 데이터를 서버에 저장
     * 
     * @return 서버 응답 (이미지 URL 포함)
     */
    suspend fun saveWalk(
        session: WalkingSession,
        imageUri: String? = null
    ): Result<WalkSaveResponse> {
        return try {
            // 산책 데이터를 JSON으로 변환
            val walkDataJson = createWalkDataJson(session)
            val dataBody = walkDataJson.toRequestBody("application/json".toMediaTypeOrNull())

            // 이미지 파일 처리 (압축 및 리사이즈)
            val imagePart = imageUri?.let { uriString ->
                try {
                    val uri = uriString.toUri()
                    val file = compressAndResizeImage(uri, context)
                    if (file != null && file.exists()) {
                        val fileSizeKB = file.length() / 1024
                        Timber.d("이미지 압축 완료: ${file.name}, 크기: ${fileSizeKB}KB")
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("image", file.name, requestFile)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Timber.w(e, "이미지 파일 처리 실패: $uriString")
                    null
                }
            }

            // API 호출
            val response = walkApi.saveWalk(
                data = dataBody,
                image = imagePart
            )

            Result.Success(response)

        } catch (e: CancellationException) {
            // Coroutine이 취소된 경우 (ViewModel destroy 등)
            // 취소 예외는 그대로 전파하여 상위에서 처리하도록 함
            Timber.d("산책 저장 취소됨 (Coroutine 취소)")
            throw e
        } catch (e: Exception) {
            // 실제 네트워크 에러나 기타 예외인 경우
            Timber.e(e, "산책 저장 실패: ${e.message}")
            Result.Error(e, e.message)
        }
    }

    /**
     * WalkingSession을 API용 JSON 데이터로 변환
     * 서버 요구사항에 맞게 필수 필드만 포함
     */
    private fun createWalkDataJson(session: WalkingSession): String {
        // LocationPoint를 WalkPoint로 변환
        val walkPoints = session.locations.toWalkPoints()

        // WalkSaveRequest 생성 (서버 요구사항: preWalkEmotion, postWalkEmotion, note, points, endTime, startTime, totalDistance, stepCount)
        val request = WalkingSessionRequest(
            preWalkEmotion = session.preWalkEmotion.name,
            postWalkEmotion = session.postWalkEmotion.name,
            note = session.note,
            points = walkPoints,
            endTime = session.endTime ?: System.currentTimeMillis(), // endTime이 null이면 현재 시간 사용
            startTime = session.startTime,
            totalDistance = session.totalDistance,
            stepCount = session.stepCount,
        )

        return json.encodeToString(request)
    }

    /**
     * 이미지를 압축하고 리사이즈하여 File로 변환
     * 
     * - 최대 크기: 1920x1920 픽셀
     * - JPEG 품질: 85%
     * - 최대 파일 크기: 2MB
     */
    private fun compressAndResizeImage(uri: Uri, context: Context): File? {
        return try {
            // 1. 이미지 로드 (메모리 효율적으로)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // 2. 이미지 크기만 먼저 확인
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            Timber.d("원본 이미지 크기: ${imageWidth}x${imageHeight}")
            
            // 3. 리사이즈 비율 계산 (최대 1920x1920)
            val maxDimension = 1920
            val scale = if (imageWidth > imageHeight) {
                imageWidth.toFloat() / maxDimension
            } else {
                imageHeight.toFloat() / maxDimension
            }
            
            val targetWidth = if (scale > 1) (imageWidth / scale).toInt() else imageWidth
            val targetHeight = if (scale > 1) (imageHeight / scale).toInt() else imageHeight
            
            Timber.d("리사이즈 크기: ${targetWidth}x${targetHeight}")
            
            // 4. 이미지 디코딩 (리사이즈된 크기로)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            }
            
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()
            
            if (bitmap == null) {
                Timber.e("비트맵 디코딩 실패")
                return null
            }
            
            // 5. 최종 리사이즈 (정확한 크기로)
            val resizedBitmap = if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }
            
            // 원본 비트맵이 리사이즈된 비트맵과 다르면 원본 해제
            if (resizedBitmap != bitmap) {
                bitmap.recycle()
            }
            
            // 6. JPEG로 압축하여 파일로 저장
            val tempFile = File.createTempFile("walk_upload_", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()
            
            var quality = 85
            var fileSize = 0L
            val maxFileSize = 2 * 1024 * 1024 // 2MB
            
            FileOutputStream(tempFile).use { output ->
                do {
                    output.channel.truncate(0) // 파일 초기화
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                    fileSize = tempFile.length()
                    
                    if (fileSize > maxFileSize && quality > 50) {
                        quality -= 10
                        Timber.d("파일 크기 초과 (${fileSize / 1024}KB), 품질 낮춤: $quality")
                    } else {
                        break
                    }
                } while (quality > 50)
            }
            
            resizedBitmap.recycle()
            
            val finalSizeKB = tempFile.length() / 1024
            Timber.d("이미지 압축 완료: ${tempFile.name}, 최종 크기: ${finalSizeKB}KB, 품질: $quality%")
            
            tempFile
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "메모리 부족으로 이미지 처리 실패")
            null
        } catch (e: Exception) {
            Timber.e(e, "이미지 압축/리사이즈 실패: $uri")
            null
        }
    }
    
    /**
     * BitmapFactory.Options의 inSampleSize 계산
     * 메모리 효율적인 이미지 로딩을 위해 사용
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }

    /**
     * 팔로워 산책 기록 조회
     *
     * @param nickname 팔로워 닉네임
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     * @return 팔로워 산책 기록 정보
     */
    suspend fun getFollowerWalkRecord(
        nickname: String,
        lat: Double? = null,
        lon: Double? = null,
    ): Result<FollowerWalkRecordDto> {
        return try {
            val response = followerApi.getFollowerWalkRecord(nickname, lat, lon)

            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    Timber.d("팔로워 산책 기록 조회 성공: $nickname")
                    Result.Success(data)
                } else {
                    Timber.w("팔로워 산책 기록 응답 본문이 null: $nickname")
                    Result.Error(
                        Exception("응답 데이터가 없습니다"),
                        "응답 데이터가 없습니다"
                    )
                }
            } else {
                // ApiErrorResponse 파싱
                val errorBody = response.errorBody()?.string()
                val apiError = errorBody?.let {
                    try {
                        Json.decodeFromString<ApiErrorResponse>(it)
                    } catch (e: Exception) {
                        Timber.e(e, "ApiErrorResponse 파싱 실패: $errorBody")
                        null
                    }
                }

                val errorCode = apiError?.code ?: response.code()
                val errorName = apiError?.name ?: "HTTP_ERROR"
                val errorMessage = apiError?.message ?: "알 수 없는 에러가 발생했습니다"

                Timber.w("팔로워 산책 기록 조회 실패: HTTP ${response.code()}, Code: $errorCode, Name: $errorName")

                // 서버 에러 코드에 따른 구체적인 예외 생성
                val exception = when (errorCode) {
                    2001 -> Exception("NOT_FOLLOWING") // 팔로워가 아닌 유저 조회
                    5001 -> Exception("NO_WALK_RECORDS") // 산책 기록 없음
                    else -> Exception(errorName)
                }

                Result.Error(exception, errorMessage)
            }
        } catch (e: CancellationException) {
            Timber.d("팔로워 산책 기록 조회 취소됨 (Coroutine 취소)")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "팔로워 산책 기록 조회 실패: $nickname")
            Result.Error(e, e.message ?: "네트워크 오류가 발생했습니다")
        }
    }

    /**
     * 산책 좋아요 누르기
     *
     * @param walkId 산책 ID
     * @return API 호출 결과
     */
    suspend fun likeWalk(walkId: Long): Result<Unit> {
        return try {
            val response = walkApi.likeWalk(walkId)
            
            if (response.isSuccessful) {
                Timber.d("산책 좋아요 성공: walkId=$walkId")
                Result.Success(Unit)
            } else {
                val errorCode = parseErrorCode(response)
                Timber.w("산책 좋아요 실패: HTTP ${response.code()}, 에러 코드: $errorCode")
                
                // 에러 코드에 따른 처리
                when {
                    response.code() == 409 && errorCode == 3001 -> {
                        // 이미 좋아요를 누른 경우
                        Result.Error(
                            Exception("이미 좋아요를 누른 산책입니다"),
                            "이미 좋아요를 누른 산책입니다"
                        )
                    }
                    response.code() == 404 && errorCode == 2001 -> {
                        // 팔로우가 되어 있지 않은 경우
                        Result.Error(
                            Exception("팔로우가 되어 있지 않습니다"),
                            "팔로우가 되어 있지 않습니다"
                        )
                    }
                    else -> {
                        Result.Error(
                            Exception("HTTP ${response.code()}: ${response.message()}"),
                            "좋아요 처리 중 오류가 발생했습니다"
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            Timber.d("산책 좋아요 취소됨 (Coroutine 취소)")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "산책 좋아요 실패: walkId=$walkId")
            Result.Error(e, e.message ?: "네트워크 오류가 발생했습니다")
        }
    }

    /**
     * 산책 좋아요 취소
     *
     * @param walkId 산책 ID
     * @return API 호출 결과
     */
    suspend fun unlikeWalk(walkId: Long): Result<Unit> {
        return try {
            val response = walkApi.unlikeWalk(walkId)
            
            if (response.isSuccessful) {
                Timber.d("산책 좋아요 취소 성공: walkId=$walkId")
                Result.Success(Unit)
            } else {
                val errorCode = parseErrorCode(response)
                Timber.w("산책 좋아요 취소 실패: HTTP ${response.code()}, 에러 코드: $errorCode")
                
                // 에러 코드에 따른 처리
                when {
                    response.code() == 404 && errorCode == 3002 -> {
                        // 좋아요를 누르지 않은 경우
                        Result.Error(
                            Exception("좋아요를 누르지 않은 산책입니다"),
                            "좋아요를 누르지 않은 산책입니다"
                        )
                    }
                    response.code() == 404 && errorCode == 2001 -> {
                        // 팔로우가 되어 있지 않은 경우
                        Result.Error(
                            Exception("팔로우가 되어 있지 않습니다"),
                            "팔로우가 되어 있지 않습니다"
                        )
                    }
                    else -> {
                        Result.Error(
                            Exception("HTTP ${response.code()}: ${response.message()}"),
                            "좋아요 취소 중 오류가 발생했습니다"
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            Timber.d("산책 좋아요 취소 취소됨 (Coroutine 취소)")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "산책 좋아요 취소 실패: walkId=$walkId")
            Result.Error(e, e.message ?: "네트워크 오류가 발생했습니다")
        }
    }

    /**
     * 산책 노트 업데이트
     *
     * @param walkId 산책 ID
     * @param note 업데이트할 노트 내용
     * @return 업데이트 결과 (성공 시 Unit)
     */
    suspend fun updateWalkNote(walkId: Long, note: String): Result<Unit> {
        return try {
            val request = UpdateWalkNoteRequest(note = note)
            val response = walkApi.updateWalkNote(walkId, request)

            if (response.isSuccessful) {
                Timber.d("산책 노트 업데이트 성공: walkId=$walkId")
                Result.Success(Unit)
            } else {
                val errorCode = parseErrorCode(response)
                Timber.w("산책 노트 업데이트 실패: HTTP ${response.code()}, 에러 코드: $errorCode")
                Result.Error(
                    Exception("노트 업데이트 실패"),
                    "노트 업데이트에 실패했습니다"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "산책 노트 업데이트 실패: walkId=$walkId")
            Result.Error(e, e.message ?: "네트워크 오류가 발생했습니다")
        }
    }

    /**
     * 에러 응답에서 에러 코드 파싱
     */
    private fun parseErrorCode(response: retrofit2.Response<*>): Int? {
        return try {
            response.errorBody()?.let { errorBody ->
                val errorString = errorBody.string()
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                val errorResponse = json.decodeFromString<ApiErrorResponse>(errorString)
                errorResponse.code
            }
        } catch (e: Exception) {
            Timber.w(e, "에러 코드 파싱 실패")
            null
        }
    }
}
