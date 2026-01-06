package swyp.team.walkit.data.api.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,   // "Bearer"
    val expiresIn: Long      // seconds (3600)
)