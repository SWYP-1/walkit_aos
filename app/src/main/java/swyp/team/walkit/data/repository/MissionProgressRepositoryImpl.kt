package swyp.team.walkit.data.repository

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.local.dao.MissionProgressDao
import swyp.team.walkit.data.local.mapper.MissionProgressMapper
import swyp.team.walkit.domain.model.DailyMissionProgress
import swyp.team.walkit.domain.repository.MissionProgressRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber

/**
 * 미션 진행도 Repository 구현체
 */
@Singleton
class MissionProgressRepositoryImpl @Inject constructor(
    private val missionProgressDao: MissionProgressDao,
) : MissionProgressRepository {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun observeProgress(date: LocalDate) =
        missionProgressDao
            .observeProgress(date.format(formatter))
            .map { entity -> entity?.let(MissionProgressMapper::toDomain) }

    override suspend fun saveProgress(progress: DailyMissionProgress): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                missionProgressDao.upsert(MissionProgressMapper.toEntity(progress))
                Result.Success(Unit)
            } catch (e: Exception) {
                // DB 저장 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "미션 진행도 저장 실패")
                Result.Error(e, "미션 진행도를 저장할 수 없습니다")
            }
            // Error 타입은 catch하지 않음
        }

    override suspend fun clearProgress(date: LocalDate): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                missionProgressDao.deleteByDate(date.format(formatter))
                Result.Success(Unit)
            } catch (e: Exception) {
                // DB 삭제 실패는 치명적이지 않을 수 있지만, 로깅 후 에러 반환
                CrashReportingHelper.logException(e)
                Timber.e(e, "미션 진행도 삭제 실패")
                Result.Error(e, "미션 진행도를 삭제할 수 없습니다")
            }
            // Error 타입은 catch하지 않음
        }
}











