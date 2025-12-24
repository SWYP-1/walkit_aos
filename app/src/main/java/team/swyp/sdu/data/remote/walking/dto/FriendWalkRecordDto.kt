package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 친구 산책 기록 DTO (좋아요 정보 포함)
 * 
 * FriendRecordScreen에서 사용하는 DTO로,
 * walkId와 좋아요 정보를 포함합니다.
 */
@Serializable
data class FriendWalkRecordDto(
    @SerialName("walkId")
    val walkId: Long,
    
    @SerialName("characterDto")
    val characterDto: CharacterDto,
    
    @SerialName("walkProgressPercentage")
    val walkProgressPercentage: String? = null,
    
    @SerialName("createdDate")
    val createdDate: String? = null,
    
    @SerialName("stepCount")
    val stepCount: Int = 0,
    
    @SerialName("totalDistance")
    val totalDistance: Int = 0,
    
    @SerialName("likeCount")
    val likeCount: Int = 0,
    
    @SerialName("isLiked")
    val isLiked: Boolean = false,
)

