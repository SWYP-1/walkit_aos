package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 캐릭터 정보 DTO
 */
@Serializable
data class CharacterDto(
    @SerialName("headImageName")
    val headImageName: String? = null,
    
    @SerialName("bodyImageName")
    val bodyImageName: String? = null,
    
    @SerialName("feetImageName")
    val feetImageName: String? = null,
    
    @SerialName("characterImageName")
    val characterImageName: String? = null,
    
    @SerialName("backgroundImageName")
    val backgroundImageName: String? = null,
    
    @SerialName("level")
    val level: Int = 1,
    
    @SerialName("grade")
    val grade: Grade = Grade.SEED,
    
    @SerialName("nickName")
    val nickName: String? = null,
)






