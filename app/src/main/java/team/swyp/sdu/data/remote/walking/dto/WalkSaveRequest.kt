package team.swyp.sdu.data.remote.walking.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint

/**
 * 산책 저장 API 요청 DTO
 */
@Serializable
data class WalkSaveRequest(
    @SerializedName("preWalkEmotion")
    val preWalkEmotion: String,

    @SerializedName("postWalkEmotion")
    val postWalkEmotion: String,

    @SerializedName("note")
    val note: String? = null,

    @SerializedName("startTime")
    val startTime: Long,

    @SerializedName("endTime")
    val endTime: Long,

    @SerializedName("stepCount")
    val stepCount: Int,

    @SerializedName("totalDistance")
    val totalDistance: Float,

    @SerializedName("points")
    val points: List<WalkPoint>
) {
    companion object {
        /**
         * LocationPoint 리스트를 WalkPoint 리스트로 변환
         * accuracy 필드는 제외하고 변환합니다.
         */
        fun fromLocationPoints(locationPoints: List<LocationPoint>): List<WalkPoint> {
            return locationPoints.map { locationPoint ->
                WalkPoint(
                    latitude = locationPoint.latitude,
                    longitude = locationPoint.longitude,
                    timestampMillis = locationPoint.timestamp
                )
            }
        }
    }
}

