package swyp.team.walkit.data.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,   // "Bearer"
    val expiresIn: Long,     // seconds (3600)
    val registered: Boolean? = null  // 토큰 갱신 시에만 사용 (선택적)
)