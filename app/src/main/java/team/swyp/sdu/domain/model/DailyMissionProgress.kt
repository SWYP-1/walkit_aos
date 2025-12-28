package team.swyp.sdu.domain.model

import java.time.LocalDate

/**
 * 하루 미션/도전과제 진행도
 */
data class DailyMissionProgress(
    val date: LocalDate,
    val distanceKm: Double = 0.0,
    val steps: Int = 0,
    val missionCompleted: Boolean = false,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
)









