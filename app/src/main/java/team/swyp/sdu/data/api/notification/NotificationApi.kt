package team.swyp.sdu.data.api.notification

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import team.swyp.sdu.data.remote.notification.dto.FcmTokenRequestDto
import team.swyp.sdu.data.remote.notification.dto.NotificationItemDto
import team.swyp.sdu.data.remote.notification.dto.NotificationSettingsDto
import team.swyp.sdu.data.remote.notification.dto.UpdateNotificationSettingsRequest

/**
 * 알림 관련 API
 */
interface NotificationApi {
    /**
     * FCM 토큰 등록
     *
     * @param request FCM 토큰 등록 요청
     * @return Response<Unit>
     */
    @POST("/fcm/token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequestDto,
    ): Response<Unit>

    /**
     * 알림 설정 조회
     *
     * @return 알림 설정 정보
     */
    @GET("/notification/setting")
    suspend fun getNotificationSettings(): NotificationSettingsDto

    /**
     * 알림 설정 업데이트
     *
     * @param request 알림 설정 업데이트 요청
     * @return Response<Unit>
     */
    @PATCH("/notification/setting")
    suspend fun updateNotificationSettings(
        @Body request: UpdateNotificationSettingsRequest,
    ): Response<Unit>

    /**
     * 알림 목록 조회
     *
     * @param limit 조회할 알림 개수 (기본값: 20)
     * @return 알림 목록
     */
    @GET("/notification/list")
    suspend fun getNotificationList(
        @Query("limit") limit: Int = 20,
    ): List<NotificationItemDto>
}

