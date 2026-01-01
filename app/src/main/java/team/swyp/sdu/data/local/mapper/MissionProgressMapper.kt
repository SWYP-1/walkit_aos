package team.swyp.sdu.data.local.mapper

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import team.swyp.sdu.data.local.entity.MissionProgressEntity
import team.swyp.sdu.domain.model.DailyMissionProgress

/**
 * 미션 진행도 매퍼
 */
object MissionProgressMapper {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toEntity(domain: DailyMissionProgress): MissionProgressEntity =
        MissionProgressEntity(
            date = domain.date.format(formatter),
            distanceKm = domain.distanceKm,
            steps = domain.steps,
            missionCompleted = domain.missionCompleted,
            lastUpdatedAt = domain.lastUpdatedAt,
        )

    fun toDomain(entity: MissionProgressEntity): DailyMissionProgress =
        DailyMissionProgress(
            date = LocalDate.parse(entity.date, formatter),
            distanceKm = entity.distanceKm,
            steps = entity.steps,
            missionCompleted = entity.missionCompleted,
            lastUpdatedAt = entity.lastUpdatedAt,
        )
}











