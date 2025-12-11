package team.swyp.sdu.data.repository

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.local.dao.MissionProgressDao
import team.swyp.sdu.data.local.mapper.MissionProgressMapper
import team.swyp.sdu.domain.model.DailyMissionProgress
import team.swyp.sdu.domain.repository.MissionProgressRepository

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
                Result.Error(e, e.message)
            }
        }

    override suspend fun clearProgress(date: LocalDate): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                missionProgressDao.deleteByDate(date.format(formatter))
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, e.message)
            }
        }
}
