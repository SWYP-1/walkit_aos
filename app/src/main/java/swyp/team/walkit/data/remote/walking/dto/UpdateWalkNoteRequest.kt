package swyp.team.walkit.data.remote.walking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateWalkNoteRequest(
    @SerialName("note")
    val note: String
)

@Serializable
data class UpdateWalkNoteResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("message")
    val message: String? = null
)


