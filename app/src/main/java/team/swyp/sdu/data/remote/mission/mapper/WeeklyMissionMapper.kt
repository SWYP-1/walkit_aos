package team.swyp.sdu.data.remote.mission.mapper

import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionDto
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionListResponse
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.domain.model.MissionType
import team.swyp.sdu.domain.model.WeeklyMission

/**
 * WeeklyMission DTO → Domain Model 변환 매퍼
 */
object WeeklyMissionMapper {
    /**
     * WeeklyMissionDto → WeeklyMission Domain Model
     */
    fun toDomain(dto: WeeklyMissionDto): WeeklyMission {
        return WeeklyMission(
            userWeeklyMissionId = dto.userWeeklyMissionId,
            missionId = dto.missionId,
            title = dto.title,
            category = dto.getMissionCategory() ?: MissionCategory.CHALLENGE,
            type = dto.getMissionType() ?: MissionType.CHALLENGE_STEPS,
            status = dto.getMissionStatus(),
            rewardPoints = dto.rewardPoints,
            assignedConfigJson = dto.assignedConfigJson,
            weekStart = dto.weekStart,
            weekEnd = dto.weekEnd,
            completedAt = dto.completedAt,
            failedAt = dto.failedAt,
        )
    }

    /**
     * WeeklyMissionListResponse → List<WeeklyMission> Domain Model
     * 
     * active 미션을 첫 번째로, others를 그 뒤에 추가하여 반환합니다.
     */
    fun toDomainList(response: WeeklyMissionListResponse): List<WeeklyMission> {
        val activeMission = toDomain(response.active)
        val otherMissions = response.others.map { toDomain(it) }
        return listOf(activeMission) + otherMissions
    }
}



