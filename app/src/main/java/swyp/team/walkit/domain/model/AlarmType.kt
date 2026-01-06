package swyp.team.walkit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알람 타입
 */
@Serializable
enum class AlarmType {
    @SerialName("FOLLOW")
    FOLLOW, // 팔로우 알람

    @SerialName("GOAL")
    GOAL, // 목표 달성 알람

    @SerialName("INACTIVE_USER")
    INACTIVE_USER, // 미접속 알람

    @SerialName("MISSION_OPEN")
    MISSION_OPEN, // 미접속 알람
}








