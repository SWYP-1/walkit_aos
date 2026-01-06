package swyp.team.walkit.domain.model

/**
 * 알림 타입 enum
 */
enum class NotificationType(
    val value: String,
    val displayName: String
) {
    GOAL("GOAL", "목표 알림"),
    MISSION_OPEN("MISSION_OPEN", "새 미션 알림"),
    FOLLOW("FOLLOW", "팔로우 요청"),
    INACTIVE_USER("INACTIVE_USER", "미접속");

    companion object {
        fun fromValue(value: String): NotificationType? {
            return values().find { it.value == value }
        }
    }
}
