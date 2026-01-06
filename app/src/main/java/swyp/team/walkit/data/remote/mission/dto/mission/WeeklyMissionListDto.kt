package swyp.team.walkit.data.remote.mission.dto.mission

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyMissionListResponse(
    val active: WeeklyMissionDto,
    val others: List<WeeklyMissionDto>
)
