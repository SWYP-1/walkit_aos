package swyp.team.walkit.data.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.domain.repository.PointRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
) : PointRepository {

    /**
     * 유저 포인트 조회
     *
     * @return 유저의 포인트 값 (Result로 감싸서 반환)
     */
    override suspend fun getUserPoint(): Result<Int> {
        return try {
            val pointDto = userRemoteDataSource.getUserPoint()
            Timber.d("포인트 조회 성공: ${pointDto.point}")
            Result.Success(pointDto.point)
        } catch (t: Throwable) {
            Timber.e(t, "포인트 조회 실패")
            Result.Error(t, t.message ?: "포인트 정보를 불러오지 못했습니다")
        }
    }
}
