package team.swyp.sdu.data.remote.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 하늘 상태 Enum
 */
@Serializable
enum class Sky {
    @SerialName("SUNNY")
    SUNNY,
    
    @SerialName("CLOUDY_MANY")
    CLOUDY_MANY,
    
    @SerialName("OVERCAST")
    OVERCAST,
    
    @SerialName("UNKNOWN")
    UNKNOWN,
}






