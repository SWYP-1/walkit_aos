package swyp.team.walkit.data.remote.cosmetic.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseRequestDto(
    @SerialName("items")
    val items: List<PurchaseItemDto>,
    @SerialName("totalPrice")
    val totalPrice: Int
)

@Serializable
data class PurchaseItemDto(
    @SerialName("itemId")
    val itemId: Int
)


