package team.swyp.sdu.domain.model

import kotlinx.serialization.json.Json

/**
 * 미션 설정 파서
 */
object MissionConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 미션 타입과 JSON 설정을 파싱하여 MissionConfig 객체로 변환
     */
    fun parseMissionConfig(
        type: MissionType,
        jsonString: String,
    ): MissionConfig {
        return when (type) {
            MissionType.CHALLENGE_STEPS -> {
                try {
                    json.decodeFromString<MissionConfig.ChallengeStepsConfig>(jsonString)
                } catch (e: Exception) {
                    error("Failed to parse ChallengeStepsConfig: ${e.message}")
                }
            }

            MissionType.PHOTO_COLOR -> {
                try {
                    json.decodeFromString<MissionConfig.PhotoColorConfig>(jsonString)
                } catch (e: Exception) {
                    error("Failed to parse PhotoColorConfig: ${e.message}")
                }
            }

            MissionType.CHALLENGE_ATTENDANCE -> {
                try {
                    json.decodeFromString<MissionConfig.ChallengeAttendanceConfig>(jsonString)
                } catch (e: Exception) {
                    error("Failed to parse Challenge attendance: ${e.message}")
                }
            }
        }
    }
}
