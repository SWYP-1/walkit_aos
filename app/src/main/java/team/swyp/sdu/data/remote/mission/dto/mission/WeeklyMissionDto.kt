package team.swyp.sdu.data.remote.mission.dto.mission

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.domain.model.MissionConfig
import team.swyp.sdu.domain.model.MissionConfigParser
import team.swyp.sdu.domain.model.MissionStatus
import team.swyp.sdu.domain.model.MissionType

@Keep
@Serializable
data class WeeklyMissionDto(
    @SerialName("userWeeklyMissionId")
    val userWeeklyMissionId: Long? = null,

    @SerialName("missionId")
    val missionId: Long,

    @SerialName("title")
    val title: String,

    @SerialName("category")
    val category: String,

    @SerialName("type")
    val type: String,

    @SerialName("status")
    val status: String? = null,

    @SerialName("rewardPoints")
    val rewardPoints: Int,

    @SerialName("assignedConfigJson")
    val assignedConfigJson: String? = null,

    @SerialName("weekStart")
    val weekStart: String,

    @SerialName("weekEnd")
    val weekEnd: String,

    @SerialName("completedAt")
    val completedAt: String? = null,

    @SerialName("failedAt")
    val failedAt: String? = null,
) {
    /**
     * 미션 카테고리 enum으로 변환
     */
    fun getMissionCategory(): MissionCategory? {
        return MissionCategory.fromApiValue(category)
    }

    /**
     * 미션 타입 enum으로 변환
     */
    fun getMissionType(): MissionType? {
        return MissionType.fromApiValue(type)
    }

    /**
     * 미션 상태 enum으로 변환
     */
    fun getMissionStatus(): MissionStatus? {
        return status?.let { MissionStatus.fromApiValue(it) }
    }

    /**
     * 미션 설정 파싱
     */
    fun getMissionConfig(): MissionConfig? {
        return try {
            val missionType = getMissionType()
            val configJson = assignedConfigJson ?: return null // null이면 바로 null
            missionType?.let { MissionConfigParser.parseMissionConfig(it, configJson) }
        } catch (e: Exception) {
            null // 파싱 실패 시 null 반환
        }
    }

    companion object Companion {
        val EMPTY = WeeklyMissionDto(
            userWeeklyMissionId = 0L,
            missionId = 0L,
            title = "",
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
