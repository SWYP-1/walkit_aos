package team.swyp.sdu.data.remote.user

import android.net.Uri
import team.swyp.sdu.data.remote.home.dto.UserPointDto
import team.swyp.sdu.domain.model.User
import team.swyp.sdu.domain.model.UserSearchResult
import team.swyp.sdu.data.remote.user.dto.UserSummaryDto
import team.swyp.sdu.domain.model.FollowStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관련 원격 데이터 소스 (Facade 패턴)
 *
 * 관심사별로 분리된 데이터 소스들을 조합하여 사용하는 facade입니다.
 */
@Singleton
class UserRemoteDataSource @Inject constructor(
    private val userProfileRemoteDataSource: UserProfileRemoteDataSource,
    private val userSearchRemoteDataSource: UserSearchRemoteDataSource,
    private val userManagementRemoteDataSource: UserManagementRemoteDataSource,
    private val userPointRemoteDataSource: UserPointRemoteDataSource,
) {

    suspend fun fetchUser(): User {
        return userProfileRemoteDataSource.fetchUser()
    }

    /**
     * 닉네임으로 사용자 검색
     *
     * @param nickname 검색할 닉네임
     * @return 검색 결과 (사용자 정보 및 친구 요청 상태)
     * @throws UserNotFoundException 존재하지 않는 유저를 조회한 경우 (404, 1001)
     */
    suspend fun searchUserByNickname(nickname: String): team.swyp.sdu.data.remote.user.UserSearchResult {
        return userSearchRemoteDataSource.searchUserByNickname(nickname)
    }

    /**
     * 닉네임 중복 체크
     *
     * @param nickname 체크할 닉네임
     * @return Response 객체 (isSuccessful로 중복 여부 판단)
     */
    suspend fun checkNicknameDuplicate(nickname: String): retrofit2.Response<Void> {
        return userManagementRemoteDataSource.checkNicknameDuplicate(nickname)
    }

    /**
     * 닉네임 등록
     *
     * @param nickname 등록할 닉네임
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun registerNickname(nickname: String) {
        userManagementRemoteDataSource.registerNickname(nickname)
    }

    /**
     * 생년월일 업데이트
     *
     * @param birthDate 생년월일 (ISO 8601 형식: "2015-12-04")
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun updateBirthDate(birthDate: String) {
        userManagementRemoteDataSource.updateBirthDate(birthDate)
    }

    suspend fun updateUserProfileImage(imageUri: Uri) {
        userProfileRemoteDataSource.updateUserProfileImage(imageUri)
    }


    /**
     * 사용자 프로필 업데이트 (온보딩 완료)
     *
     * @param nickname 닉네임
     * @param birthDate 생년월일 (ISO 8601 형식)
     * @param imageUri 선택된 이미지 URI (선택사항, 현재 미사용)
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun updateUserProfile(
        nickname: String,
        birthDate: String,
    ) {
        userProfileRemoteDataSource.updateUserProfile(nickname, birthDate)
    }


    /**
     * 약관 동의
     *
     * @param termsAgreed 서비스 이용약관 동의 여부
     * @param privacyAgreed 개인정보 처리방침 동의 여부
     * @param locationAgreed 위치 정보 이용약관 동의 여부
     * @param marketingConsent 마케팅 정보 수신 동의 여부
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ) {
        userManagementRemoteDataSource.agreeToTerms(
            termsAgreed = termsAgreed,
            privacyAgreed = privacyAgreed,
            locationAgreed = locationAgreed,
            marketingConsent = marketingConsent,
        )
    }

    /**
     * 유저 포인트 조회
     */
    suspend fun getUserPoint(): UserPointDto {
        return userPointRemoteDataSource.getUserPoint()
    }

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
    ): UserSummaryDto {
        return userSearchRemoteDataSource.getUserSummaryByNickname(nickname, lat, lon)
    }

}

/**
 * 사용자 검색 결과 도메인 모델
 */
data class UserSearchResult(
    val userId: Long,
    val imageName: String?,
    val nickname: String,
    val followStatus: FollowStatus,
)

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
class UserNotFoundException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
