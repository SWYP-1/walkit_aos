package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.WalkingSessionEntity
import team.swyp.sdu.data.model.ActivityStats
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.domain.service.ActivityType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WalkingSession과 WalkingSessionEntity 간 변환 유틸리티
 */
object WalkingSessionMapper {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    /**
     * Domain Model -> Entity 변환
     */
    fun toEntity(
        session: WalkingSession,
        isSynced: Boolean = false,
    ): WalkingSessionEntity =
        WalkingSessionEntity(
            id = 0, // 새로 생성되는 경우 0, 업데이트 시에는 실제 ID 사용
            startTime = session.startTime,
            endTime = session.endTime,
            stepCount = session.stepCount,
            locationsJson = json.encodeToString(session.locations),
            totalDistance = session.totalDistance,
            activityStatsJson = json.encodeToString(session.activityStats),
            primaryActivity = session.primaryActivity?.name,
            isSynced = isSynced,
        )

    /**
     * Entity -> Domain Model 변환
     */
    fun toDomain(entity: WalkingSessionEntity): WalkingSession {
        val locations =
            try {
                json.decodeFromString<List<LocationPoint>>(entity.locationsJson)
            } catch (e: Exception) {
                emptyList()
            }

        val activityStats =
            try {
                json.decodeFromString<List<ActivityStats>>(entity.activityStatsJson)
            } catch (e: Exception) {
                emptyList()
            }

        val primaryActivity =
            entity.primaryActivity?.let {
                try {
                    ActivityType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }

        return WalkingSession(
            startTime = entity.startTime,
            endTime = entity.endTime,
            stepCount = entity.stepCount,
            locations = locations,
            totalDistance = entity.totalDistance,
            activityStats = activityStats,
            primaryActivity = primaryActivity,
        )
    }
}
