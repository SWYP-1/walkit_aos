package team.swyp.sdu.data.remote.mission.mapper

import team.swyp.sdu.data.dto.mission.WeeklyMissionData
import team.swyp.sdu.domain.model.WeeklyMission

/**
 * WeeklyMission DTO → Domain Model 변환 매퍼
 */
object WeeklyMissionMapper {
    /**
     * WeeklyMissionData → WeeklyMission Domain Model
     */
    fun toDomain(dto: WeeklyMissionData): WeeklyMission {
        return WeeklyMission(
            userWeeklyMissionId = dto.userWeeklyMissionId,
            missionId = dto.missionId,
            title = dto.title,
            description = dto.description,
            category = dto.category,
            type = dto.type,
            status = dto.status,
            rewardPoints = dto.rewardPoints,
            assignedConfigJson = dto.assignedConfigJson,
            weekStart = dto.weekStart,
            weekEnd = dto.weekEnd,
            completedAt = dto.completedAt,
            failedAt = dto.failedAt,
        )
    }
}



