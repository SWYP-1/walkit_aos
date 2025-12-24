package team.swyp.sdu.domain.repository

import kotlinx.coroutines.flow.StateFlow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Sex
import team.swyp.sdu.domain.model.User

/**
 * 사용자 정보 Repository 인터페이스
 *
 * Goal 정보는 GoalRepository에서 별도로 관리합니다.
 */
interface UserRepository {
    val userFlow: StateFlow<User?>

    suspend fun getUser(): Result<User>

    suspend fun refreshUser(): Result<User>

    suspend fun updateUser(user: User): Result<User>

    suspend fun registerNickname(nickname: String): Result<Unit>

    suspend fun updateBirthDate(birthDate: String): Result<Unit>

    suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
        imageUri: String? = null,
    ): Result<User>

    suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ): Result<Unit>

    suspend fun saveAuthTokens(accessToken: String, refreshToken: String?)

    suspend fun clearAuth(): Result<Unit>
}
