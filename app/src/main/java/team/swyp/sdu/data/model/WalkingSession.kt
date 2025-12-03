package team.swyp.sdu.data.model

import android.os.Parcelable
import team.swyp.sdu.domain.service.ActivityType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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
 * @param startTime 산책 시작 시간 (밀리초)
 * @param endTime 산책 종료 시간 (밀리초, null이면 진행 중)
 * @param stepCount 걸음 수
 * @param locations 위치 좌표 리스트
 * @param totalDistance 총 이동 거리 (미터)
 * @param activityStats 활동 상태별 통계 리스트
 * @param primaryActivity 주요 활동 상태 (가장 많이 한 활동)
 */
@Parcelize
@Serializable
data class WalkingSession(
    val startTime: Long,
    val endTime: Long? = null,
    val stepCount: Int = 0,
    val locations: List<LocationPoint> = emptyList(),
    val totalDistance: Float = 0f,
    val activityStats: List<ActivityStats> = emptyList(),
    val primaryActivity: ActivityType? = null,
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
     * 특정 활동 상태의 통계 반환
     */
    fun getActivityStat(type: ActivityType): ActivityStats? = activityStats.firstOrNull { it.type == type }

    /**
     * 활동 상태별 시간 비율 반환 (0.0 ~ 1.0)
     */
    fun getActivityDurationRatio(type: ActivityType): Float {
        if (duration == 0L) return 0f
        val activityDuration = activityStats.firstOrNull { it.type == type }?.duration ?: 0L
        return activityDuration.toFloat() / duration.toFloat()
    }

    /**
     * 활동 상태별 거리 비율 반환 (0.0 ~ 1.0)
     */
    fun getActivityDistanceRatio(type: ActivityType): Float {
        if (totalDistance == 0f) return 0f
        val activityDistance = activityStats.firstOrNull { it.type == type }?.distance ?: 0f
        return activityDistance / totalDistance
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
