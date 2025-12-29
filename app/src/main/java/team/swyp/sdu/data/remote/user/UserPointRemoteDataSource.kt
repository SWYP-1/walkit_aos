package team.swyp.sdu.data.remote.user

import team.swyp.sdu.data.api.user.UserApi
import team.swyp.sdu.data.remote.home.dto.UserPointDto
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자 포인트 관련 원격 데이터 소스
 * - 포인트 조회
 */
@Singleton
class UserPointRemoteDataSource @Inject constructor(
    private val userApi: UserApi,
) {

    /**
     * 유저 포인트 조회
     */
    suspend fun getUserPoint(): UserPointDto {
        return try {
            val data = userApi.getUserPoint()
            Timber.d("유저 포인트 조회 성공: ${data.point}")
            data
        } catch (e: Exception) {
            Timber.e(e, "유저 포인트 조회 실패")
            throw e
        }
    }
}
