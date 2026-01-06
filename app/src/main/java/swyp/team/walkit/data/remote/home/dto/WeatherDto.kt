package swyp.team.walkit.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 날씨 정보 DTO
 */
@Serializable
data class WeatherDto(
    @SerialName("nx")
    val nx: Int = 0,
    
    @SerialName("ny")
    val ny: Int = 0,
    
    @SerialName("generatedAt")
    val generatedAt: String? = null,
    
    @SerialName("tempC")
    val tempC: Double = 0.0,
    
    @SerialName("rain1hMm")
    val rain1hMm: Double = 0.0,
    
    @SerialName("precipType")
    val precipType: PrecipType = PrecipType.NONE,
    
    @SerialName("sky")
    val sky: Sky = Sky.SUNNY,
)

