package team.swyp.sdu.domain.model

/**
 * 주간 미션 도메인 모델
 */
data class WeeklyMission(
    val userWeeklyMissionId: Long = 0,
    val missionId: Long = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val type: String = "",
    val status: String = "",
    val rewardPoints: Int = 0,
    val assignedConfigJson: String? = "{}",
    val weekStart: String? = null,
    val weekEnd: String? = null,
    val completedAt: String? = null,
    val failedAt: String? = null,
) {
    /**
     * 미션 타입 enum으로 변환
     */
    fun getMissionType(): MissionType? {
        return MissionType.fromApiValue(type)
    }

    /**
     * 미션 설정 파싱
     * 
     * 서버에서 항상 assignedConfigJson을 제공하므로, null이거나 빈 문자열인 경우 기본값 "{}" 사용
     * 파싱 실패 시 null 반환 (UI에서 description을 fallback으로 사용)
     */
    fun getMissionConfig(): MissionConfig? {
        // assignedConfigJson이 null이거나 빈 문자열인 경우 기본값 사용
        val configJson = assignedConfigJson?.takeIf { it.isNotBlank() } ?: "{}"
        
        return try {
            val missionType = getMissionType() ?: return null
            
            // 빈 JSON 객체인 경우 null 반환 (UI에서 description 사용)
            if (configJson == "{}" || configJson.trim() == "{}") {
                return null
            }
            
            MissionConfigParser.parseMissionConfig(missionType, configJson)
        } catch (e: Exception) {
            // 파싱 실패 시 null 반환 (UI에서 description을 fallback으로 사용)
            null
        }
    }

    companion object {
        val EMPTY = WeeklyMission(
            userWeeklyMissionId = 0L,
            missionId = 0L,
            title = "",
            description = "",
            category = "",
            type = "",
            status = "",
            rewardPoints = 0,
            assignedConfigJson = "{}",
            weekStart = "",
            weekEnd = "",
            completedAt = null,
            failedAt = null,
        )
    }
}

