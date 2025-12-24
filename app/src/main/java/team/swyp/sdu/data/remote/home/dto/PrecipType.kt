package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 강수 형태 Enum
 */
@Serializable
enum class PrecipType {
    @SerialName("NONE")
    NONE,
    
    @SerialName("RAIN")
    RAIN,
    
    @SerialName("RAIN_SNOW")
    RAIN_SNOW,
    
    @SerialName("SNOW")
    SNOW,
    
    @SerialName("SHOWER")
    SHOWER,
    
    @SerialName("UNKNOWN")
    UNKNOWN,
}



