package swyp.team.walkit.data.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.domain.repository.PointRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import retrofit2.HttpException
import java.io.IOException
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
        } catch (e: IOException) {
            CrashReportingHelper.logNetworkError(e, "getUserPoint")
            Timber.e(e, "포인트 조회 실패: 네트워크 오류")
            Result.Error(e, "인터넷 연결을 확인해주세요")
        } catch (e: HttpException) {
            CrashReportingHelper.logHttpError(e, "getUserPoint")
            Timber.e(e, "포인트 조회 실패: HTTP ${e.code()}")
            Result.Error(e, "포인트 정보를 불러올 수 없습니다")
        }
        // NullPointerException 등 치명적 오류는 catch하지 않음
    }
}
