package team.swyp.sdu.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.UserSearchResult
import team.swyp.sdu.domain.model.UserSummary

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
    ): Result<Unit>

    suspend fun updateUserProfileImage(
        imageUri: Uri,
    ): Result<Unit>

    suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ): Result<Unit>

    suspend fun saveAuthTokens(accessToken: String, refreshToken: String?)

    suspend fun clearAuth(): Result<Unit>

    /**
     * 닉네임으로 사용자 검색
     *
     * @param nickname 검색할 닉네임
     * @return 검색 결과 (사용자 정보 및 친구 요청 상태)
     */
    suspend fun searchUserByNickname(nickname: String): Result<UserSearchResult>

    /**
     * 닉네임으로 사용자 요약 정보 조회
     *
     * 친구 검색 결과에서 특정 사용자를 선택했을 때 상세 정보를 가져옵니다.
     *
     * @param nickname 검색할 닉네임
     * @param lat 위도
     * @param lon 경도
     * @return 사용자 요약 정보 (캐릭터 정보 + 산책 요약 정보)
     */
    suspend fun getUserSummaryByNickname(
        nickname: String,
        lat: Double,
        lon: Double,
    ): Result<UserSummary>

    /**
     * 사용자 탈퇴
     *
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    suspend fun deleteUser(): Result<Response<Unit>>

    /**
     * 프로필 이미지 삭제
     *
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    suspend fun deleteImage(): Result<Response<Unit>>
}
