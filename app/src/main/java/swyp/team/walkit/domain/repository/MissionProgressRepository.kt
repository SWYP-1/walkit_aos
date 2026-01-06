package swyp.team.walkit.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import swyp.team.walkit.core.Result
import swyp.team.walkit.domain.model.DailyMissionProgress

/**
 * 미션 진행도 Repository
 */
interface MissionProgressRepository {
    fun observeProgress(date: LocalDate): Flow<DailyMissionProgress?>

    suspend fun saveProgress(progress: DailyMissionProgress): Result<Unit>

    suspend fun clearProgress(date: LocalDate): Result<Unit>
}











