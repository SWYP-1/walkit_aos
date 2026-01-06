package swyp.team.walkit.data.remote.cosmetic.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WearItemRequest(
    @SerialName("worn")
    val isWorn: Boolean
)


