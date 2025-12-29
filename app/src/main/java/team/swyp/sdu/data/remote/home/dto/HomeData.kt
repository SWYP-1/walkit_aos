package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionDto
import team.swyp.sdu.data.remote.walking.dto.CharacterDto

/**
 * 홈 화면 메인 응답 DTO
 */
@Serializable
data class HomeData(
    @SerialName("characterDto")
    val characterDto: CharacterDto,

    @SerialName("walkProgressPercentage")
    val walkProgressPercentage: String,

    @SerialName("todaySteps")
    val todaySteps: Int,

    @SerialName("weatherDto")
    val weatherDto: WeatherDto,

    @SerialName("weeklyMissionDto")
    val weeklyMissionDto: WeeklyMissionDto? = null,

    @SerialName("walkResponseDto")
    val walkResponseDto: List<WalkResponseDto> = emptyList(),
)






