package team.swyp.sdu.data.dto.mission

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.MissionConfig
import team.swyp.sdu.domain.model.MissionConfigParser
import team.swyp.sdu.domain.model.MissionType

@Keep
@Serializable
data class WeeklyMissionData(
    @SerialName("userWeeklyMissionId")
    val userWeeklyMissionId: Long,

    @SerialName("missionId")
    val missionId: Long,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String,

    @SerialName("category")
    val category: String,

    @SerialName("type")
    val type: String,

    @SerialName("status")
    val status: String,

    @SerialName("rewardPoints")
    val rewardPoints: Int,

    @SerialName("assignedConfigJson")
    val assignedConfigJson: String,

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
     * 미션 타입 enum으로 변환
     */
    fun getMissionType(): MissionType? {
        return MissionType.fromApiValue(type)
    }

    /**
     * 미션 설정 파싱
     */
    fun getMissionConfig(): MissionConfig? {
        return try {
            val missionType = getMissionType()
            missionType?.let { MissionConfigParser.parseMissionConfig(it, assignedConfigJson) }
        } catch (e: Exception) {
            null // 파싱 실패 시 null 반환
        }
    }

    companion object {
        val EMPTY = WeeklyMissionData(
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
