package team.swyp.sdu.data.remote.notification

import retrofit2.HttpException
import team.swyp.sdu.data.api.notification.NotificationApi
import team.swyp.sdu.data.remote.notification.dto.FcmTokenRequestDto
import team.swyp.sdu.data.remote.notification.dto.NotificationItemDto
import team.swyp.sdu.data.remote.notification.dto.NotificationSettingsDto
import team.swyp.sdu.data.remote.notification.dto.UpdateNotificationSettingsRequest
import team.swyp.sdu.core.Result
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 관련 서버 API 호출을 담당하는 RemoteDataSource
 */
@Singleton
class NotificationRemoteDataSource @Inject constructor(
    private val notificationApi: NotificationApi,
) {
    /**
     * FCM 토큰 등록
     */
    suspend fun registerFcmToken(
        token: String,
        deviceId: String,
    ): Result<Unit> {
        return try {
            val request = FcmTokenRequestDto(
                token = token,
                deviceType = "ANDROID",
                deviceId = deviceId,
            )
            val response = notificationApi.registerFcmToken(request)
            if (response.isSuccessful) {
                Timber.d("FCM 토큰 등록 성공")
                Result.Success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "FCM 토큰 등록 실패"
                Timber.e("FCM 토큰 등록 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception("FCM 토큰 등록 실패: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Timber.e(e, "FCM 토큰 등록 HTTP 오류: ${e.code()}")
            Result.Error(e)
        } catch (t: Throwable) {
            Timber.e(t, "FCM 토큰 등록 실패")
            Result.Error(t)
        }
    }

    /**
     * 알림 설정 조회
     */
    suspend fun getNotificationSettings(): Result<NotificationSettingsDto> {
        return try {
            val settings = notificationApi.getNotificationSettings()
            Timber.d("알림 설정 조회 성공")
            Result.Success(settings)
        } catch (e: HttpException) {
            Timber.e(e, "알림 설정 조회 HTTP 오류: ${e.code()}")
            Result.Error(e)
        } catch (t: Throwable) {
            Timber.e(t, "알림 설정 조회 실패")
            Result.Error(t)
        }
    }

    /**
     * 알림 설정 업데이트
     */
    suspend fun updateNotificationSettings(
        settings: UpdateNotificationSettingsRequest,
    ): Result<Unit> {
        return try {
            val response = notificationApi.updateNotificationSettings(settings)
            if (response.isSuccessful) {
                Timber.d("알림 설정 업데이트 성공")
                Result.Success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "알림 설정 업데이트 실패"
                Timber.e("알림 설정 업데이트 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception("알림 설정 업데이트 실패: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Timber.e(e, "알림 설정 업데이트 HTTP 오류: ${e.code()}")
            Result.Error(e)
        } catch (t: Throwable) {
            Timber.e(t, "알림 설정 업데이트 실패")
            Result.Error(t)
        }
    }

    /**
     * 알림 목록 조회
     *
     * @param limit 조회할 알림 개수 (기본값: 20)
     * @return 알림 목록
     */
    suspend fun getNotificationList(limit: Int = 20): Result<List<NotificationItemDto>> {
        return try {
            val list = notificationApi.getNotificationList(limit)
            Timber.d("알림 목록 조회 성공: ${list.size}개")
            Result.Success(list)
        } catch (e: HttpException) {
            Timber.e(e, "알림 목록 조회 HTTP 오류: ${e.code()}")
            Result.Error(e)
        } catch (t: Throwable) {
            Timber.e(t, "알림 목록 조회 실패")
            Result.Error(t)
        }
    }

    /**
     * 알림 삭제
     *
     * @param notificationId 삭제할 알림 ID
     * @return Response<Unit>
     */
    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val response = notificationApi.deleteNotification(notificationId)
            if (response.isSuccessful) {
                Timber.d("알림 삭제 성공: $notificationId")
                Result.Success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "알림 삭제 실패"
                Timber.e("알림 삭제 실패: $errorMessage (코드: ${response.code()})")
                Result.Error(Exception("알림 삭제 실패: ${response.code()}"))
            }
        } catch (e: HttpException) {
            Timber.e(e, "알림 삭제 HTTP 오류: ${e.code()}")
            Result.Error(e)
        } catch (t: Throwable) {
            Timber.e(t, "알림 삭제 실패: $notificationId")
            Result.Error(t)
        }
    }
}

