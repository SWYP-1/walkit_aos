package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 주간 미션 정보 DTO
 */
@Serializable
data class WeeklyMissionDto(
    @SerialName("userWeeklyMissionId")
    val userWeeklyMissionId: Long = 0,
    
    @SerialName("missionId")
    val missionId: Long = 0,
    
    @SerialName("title")
    val title: String = "",
    
    @SerialName("description")
    val description: String = "",
    
    @SerialName("category")
    val category: String = "",
    
    @SerialName("type")
    val type: String = "",
    
    @SerialName("status")
    val status: String = "",
    
    @SerialName("rewardPoints")
    val rewardPoints: Int = 0,
    
    @SerialName("assignedConfigJson")
    val assignedConfigJson: String? = null,
    
    @SerialName("weekStart")
    val weekStart: String? = null,
    
    @SerialName("weekEnd")
    val weekEnd: String? = null,
    
    @SerialName("completedAt")
    val completedAt: String? = null,
    
    @SerialName("failedAt")
    val failedAt: String? = null,
)



