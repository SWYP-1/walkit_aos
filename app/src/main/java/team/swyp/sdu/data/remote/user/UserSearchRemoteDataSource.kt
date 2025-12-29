package team.swyp.sdu.data.remote.user

import retrofit2.HttpException
import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.data.remote.user.dto.UserSummaryDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 검색 관련 원격 데이터 소스
 * - 사용자 검색, 사용자 요약 정보 조회
 */
@Singleton
class UserSearchRemoteDataSource @Inject constructor(
    private val userApi: UserApi,
) {

    /**
     * 닉네임으로 사용자 검색
     */
    suspend fun searchUserByNickname(nickname: String): UserSearchResult {
        return try {
            val dto = userApi.searchByNickname(nickname)
            Timber.d("사용자 검색 성공: ${dto.nickName}, 상태: ${dto.followStatus}")
            UserSearchResult(
                userId = dto.userId,
                imageName = dto.imageName,
                nickname = dto.nickName,
                followStatus = dto.getFollowStatusEnum(),
            )
        } catch (e: HttpException) {
            // 404 또는 1001 에러 코드 처리
            when (e.code()) {
                404 -> {
                    Timber.e("사용자를 찾을 수 없습니다: $nickname (404)")
                    throw UserNotFoundException("존재하지 않는 유저입니다", e)
                }
                else -> {
                    // 에러 응답 본문에서 에러 코드 확인
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("1001") == true) {
                        Timber.e("사용자를 찾을 수 없습니다: $nickname (1001)")
                        throw UserNotFoundException("존재하지 않는 유저입니다", e)
                    }
                    Timber.e(e, "사용자 검색 실패: $nickname (HTTP ${e.code()})")
                    throw e
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "사용자 검색 실패: $nickname")
            throw e
        }
    }

    /**
     * 닉네임으로 사용자 요약 정보 조회
     */
    suspend fun getUserSummaryByNickname(
        nickname: String,
        lat: Double,
        lon: Double,
    ): UserSummaryDto {
        return try {
            val dto = userApi.getUserSummaryByNickname(nickname, lat, lon)
            Timber.d("사용자 요약 정보 조회 성공: ${dto.responseCharacterDto.nickName}")
            dto
        } catch (e: Exception) {
            Timber.e(e, "사용자 요약 정보 조회 실패: $nickname")
            throw e
        }
    }
}
