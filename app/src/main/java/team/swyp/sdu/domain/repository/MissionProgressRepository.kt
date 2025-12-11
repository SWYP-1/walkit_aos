package team.swyp.sdu.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.DailyMissionProgress

/**
 * 미션 진행도 Repository
 */
interface MissionProgressRepository {
    fun observeProgress(date: LocalDate): Flow<DailyMissionProgress?>

    suspend fun saveProgress(progress: DailyMissionProgress): Result<Unit>

    suspend fun clearProgress(date: LocalDate): Result<Unit>
}
