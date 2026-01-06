package swyp.team.walkit.domain.model

/**
 * 미션 상태 enum
 */
enum class MissionStatus(
    val apiValue: String,
) {
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    companion object {
        fun fromApiValue(apiValue: String): MissionStatus? {
            return values().find { it.apiValue == apiValue }
        }
    }
}

