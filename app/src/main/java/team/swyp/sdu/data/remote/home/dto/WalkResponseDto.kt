package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 산책 기록 정보 DTO
 */
@Serializable
data class WalkResponseDto(
    @SerialName("id")
    val id: Long = 0,
    
    @SerialName("preWalkEmotion")
    val preWalkEmotion: String = "JOYFUL",
    
    @SerialName("postWalkEmotion")
    val postWalkEmotion: String = "JOYFUL",
    
    @SerialName("note")
    val note: String? = null,
    
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    
    @SerialName("startTime")
    val startTime: Long = 0,
    
    @SerialName("endTime")
    val endTime: Long = 0,
    
    @SerialName("totalTime")
    val totalTime: Long = 0,
    
    @SerialName("stepCount")
    val stepCount: Int = 0,
    
    @SerialName("totalDistance")
    val totalDistance: Double = 0.0,
    
    @SerialName("createdDate")
    val createdDate: String? = null,
    
    @SerialName("points")
    val points: List<PointDto> = emptyList(),
)






