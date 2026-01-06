package swyp.team.walkit.data.api.user

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserProfileRequest(
    val nickname: String,
    val birthDate: String,
)