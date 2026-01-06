package swyp.team.walkit.data.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val accessToken: String // 서버는 accessToken 기대
)