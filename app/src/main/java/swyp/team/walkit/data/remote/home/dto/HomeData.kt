package swyp.team.walkit.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import swyp.team.walkit.data.remote.mission.dto.mission.WeeklyMissionDto
import swyp.team.walkit.data.remote.walking.dto.CharacterDto
import swyp.team.walkit.data.remote.home.dto.WalkResponseDto
import swyp.team.walkit.ui.home.utils.WeatherType

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

    @SerialName("temperature")
    val temperature: Double? = null,

    @SerialName("weather")
    val weather: WeatherType? = null,

    @SerialName("weeklyMissionDto")
    val weeklyMissionDto: WeeklyMissionDto? = null,

    @SerialName("walkResponseDto")
    val walkResponseDto: List<WalkResponseDto> = emptyList(),
)







