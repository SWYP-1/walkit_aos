package team.swyp.sdu.data.api.user

import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.Sex

@Serializable
data class UpdateUserProfileRequest(
    val nickname: String,
    val birthDate: String,
)