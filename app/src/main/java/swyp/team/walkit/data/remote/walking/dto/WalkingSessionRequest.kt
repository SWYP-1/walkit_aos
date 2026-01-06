package swyp.team.walkit.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalkPoint(
    val latitude : Double,
    val longitude : Double,
    val timestampMillis : Long
)

@Serializable
data class WalkingSessionRequest(
    val preWalkEmotion: String,
    val postWalkEmotion: String,
    val note: String?,
    @SerialName("points")
    val points: List<WalkPoint>,
    val endTime: Long,
    val startTime: Long,
    val totalDistance: Float,
    val stepCount: Int,
)
