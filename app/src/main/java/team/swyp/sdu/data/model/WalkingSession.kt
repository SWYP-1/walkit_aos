package team.swyp.sdu.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.service.ActivityType
import java.util.UUID

/**
 * 활동 상태별 통계
 */
@Parcelize
@Serializable
data class ActivityStats(
    val type: ActivityType,
    val duration: Long = 0L, // 활동 시간 (밀리초)
    val distance: Float = 0f, // 활동 거리 (미터)
) : Parcelable

/**
 * 산책 세션 데이터 모델
 *
 * @param id 세션 고유 식별자 (UUID)
 * @param startTime 산책 시작 시간 (밀리초)
 * @param endTime 산책 종료 시간 (밀리초, null이면 진행 중)
 * @param stepCount 걸음 수
 * @param locations 위치 좌표 리스트
 * @param totalDistance 총 이동 거리 (미터)
 */
@Parcelize
@Serializable
data class WalkingSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long? = null,
    val stepCount: Int = 0,
    val locations: List<LocationPoint> = emptyList(),
    val totalDistance: Float = 0f,
    // 산책 저장 API용 필드들
    val preWalkEmotion: EmotionType? = null,
    val postWalkEmotion: EmotionType? = null,
    val note: String? = null,
    val imageUrl: String? = null,
    val createdDate: String? = null,
) : Parcelable {
    /**
     * 산책 시간 (초)
     */
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    /**
     * 산책 시간을 포맷된 문자열로 반환 (mm:ss)
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    /**
     * 거리를 포맷된 문자열로 반환 (km 또는 m)
     */
    fun getFormattedDistance(): String =
        if (totalDistance >= 1000) {
            String.format("%.2f km", totalDistance / 1000f)
        } else {
            String.format("%.0f m", totalDistance)
        }


    /**
     * 평균 보폭 계산 (미터)
     */
    fun averageStride(): Float {
        if (stepCount == 0) return 0f
        return totalDistance / stepCount
    }

    /**
     * 평균 속도 계산 (km/h)
     */
    fun averageSpeedKmh(): Float {
        val durationSeconds = duration / 1000f
        if (durationSeconds == 0f) return 0f
        val speedMs = totalDistance / durationSeconds // m/s
        return speedMs * 3.6f // km/h
    }

    /**
     * 의미 있는 GPS 이동이 있었는지 확인
     */
    fun hasMeaningfulGpsMovement(): Boolean {
        return totalDistance >= 10f // 10m 이상 이동
    }
}

/**
 * 위치 좌표 포인트
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param timestamp 시간 (밀리초)
 * @param accuracy GPS 정확도 (미터), null이면 정확도 정보 없음
 */
@Parcelize
@Serializable
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null,
) : Parcelable
