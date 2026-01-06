package swyp.team.walkit.data.remote.goal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import swyp.team.walkit.domain.model.Goal

/**
 * 목표 API 응답 DTO
 */
@Serializable
data class RemoteGoalDto(
    @SerialName("targetStepCount")
    val targetStepCount: Int,
    @SerialName("targetWalkCount")
    val targetWalkCount: Int,
) {
    fun toDomain(): Goal = Goal(
        targetStepCount = targetStepCount,
        targetWalkCount = targetWalkCount,
    )
}










