package team.swyp.sdu.data.remote.mission.dto

import kotlinx.serialization.Serializable

/**
 * 월간 미션 완료 목록 응답 DTO
 * 완료된 날짜들의 리스트
 */
@Serializable
data class MonthlyCompletedMissionDto(
    val dates: List<String>
)

