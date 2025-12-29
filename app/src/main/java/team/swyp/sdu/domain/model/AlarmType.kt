package team.swyp.sdu.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 알람 타입
 */
@Serializable
enum class AlarmType {
    @SerialName("FOLLOW")
    FOLLOW, // 팔로우 알람

    @SerialName("GOAL_ACHIEVEMENT")
    GOAL_ACHIEVEMENT, // 목표 달성 알람
}







