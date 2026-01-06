package swyp.team.walkit.data.remote.mission.mapper

import swyp.team.walkit.data.remote.mission.dto.mission.WeeklyMissionDto
import swyp.team.walkit.data.remote.mission.dto.mission.WeeklyMissionListResponse
import swyp.team.walkit.domain.model.MissionCategory
import swyp.team.walkit.domain.model.MissionType
import swyp.team.walkit.domain.model.WeeklyMission

/**
 * WeeklyMission DTO → Domain Model 변환 매퍼
 */
object WeeklyMissionMapper {
    /**
     * WeeklyMissionDto → WeeklyMission Domain Model
     */
    fun toDomain(dto: WeeklyMissionDto): WeeklyMission {
        val missionType = dto.getMissionType() ?: MissionType.CHALLENGE_STEPS
        val category = MissionCategory.fromTypeAndCategory(dto.category, dto.type)
            ?: dto.getMissionCategory() // fallback to basic category logic

        return WeeklyMission(
            userWeeklyMissionId = dto.userWeeklyMissionId,
            missionId = dto.missionId,
            title = dto.title,
            category = category ?: MissionCategory.CHALLENGE_STEPS,
            type = missionType,
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



