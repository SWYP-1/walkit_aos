package team.swyp.sdu.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.UserProfile

/**
 * 사용자 정보 Repository
 */
interface UserRepository {
    val userProfileFlow: StateFlow<UserProfile?>

    suspend fun refreshUserProfile(): Result<UserProfile>

    suspend fun saveAuthTokens(accessToken: String, refreshToken: String?)

    suspend fun clearAuth(): Result<Unit>
}
