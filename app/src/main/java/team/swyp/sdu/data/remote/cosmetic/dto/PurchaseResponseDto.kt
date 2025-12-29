package team.swyp.sdu.data.remote.cosmetic.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseResponseDto(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String? = null
)

