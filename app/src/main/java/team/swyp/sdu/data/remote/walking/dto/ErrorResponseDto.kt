package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API 에러 응답 DTO
 */
@Serializable
data class ErrorResponseDto(
    @SerialName("code")
    val code: Int,
    
    @SerialName("message")
    val message: String? = null,
)

