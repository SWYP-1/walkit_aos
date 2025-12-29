package team.swyp.sdu.data.remote.user

import retrofit2.Response
import team.swyp.sdu.data.api.user.TermsAgreementRequest
import team.swyp.sdu.data.api.user.UserApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 관리 관련 원격 데이터 소스
 * - 닉네임 등록, 생년월일 업데이트, 약관 동의
 */
@Singleton
class UserManagementRemoteDataSource @Inject constructor(
    private val userApi: UserApi,
) {

    /**
     * 닉네임 등록
     */
    suspend fun registerNickname(nickname: String) {
        try {
            val response = userApi.registerNickname(nickname)
            if (response.isSuccessful) {
                Timber.d("닉네임 등록 성공: $nickname")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "닉네임 등록 실패"
                Timber.e("닉네임 등록 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("닉네임 등록 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "닉네임 등록 실패: $nickname")
            throw e
        }
    }

    /**
     * 생년월일 업데이트
     */
    suspend fun updateBirthDate(birthDate: String) {
        try {
            val response = userApi.updateBirthDate(birthDate)
            if (response.isSuccessful) {
                Timber.d("생년월일 업데이트 성공: $birthDate")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "생년월일 업데이트 실패"
                Timber.e("생년월일 업데이트 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("생년월일 업데이트 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "생년월일 업데이트 실패: $birthDate")
            throw e
        }
    }

    /**
     * 약관 동의
     */
    suspend fun agreeToTerms(
        termsAgreed: Boolean,
        privacyAgreed: Boolean,
        locationAgreed: Boolean,
        marketingConsent: Boolean,
    ) {
        try {
            val request = TermsAgreementRequest(
                termsAgreed = termsAgreed,
                privacyAgreed = privacyAgreed,
                locationAgreed = locationAgreed,
                marketingConsent = marketingConsent,
            )

            val response = userApi.agreeToTerms(request)

            if (response.isSuccessful) {
                Timber.d("약관 동의 성공")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "약관 동의 실패"
                Timber.e("약관 동의 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("약관 동의 실패: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "약관 동의 실패")
            throw e
        }
    }
}
