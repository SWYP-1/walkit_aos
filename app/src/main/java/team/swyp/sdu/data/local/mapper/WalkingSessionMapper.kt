package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.WalkingSessionEntity
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
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
            isSynced = isSynced,
            preWalkEmotion = session.preWalkEmotion?.name,
            postWalkEmotion = session.postWalkEmotion?.name,
            note = session.note,
            imageUrl = session.imageUrl,
            createdDate = session.createdDate,
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

        // String을 EmotionType으로 변환
        val preWalkEmotion = entity.preWalkEmotion?.let {
            try {
                EmotionType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }
        val postWalkEmotion = entity.postWalkEmotion?.let {
            try {
                EmotionType.valueOf(it)
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
            preWalkEmotion = preWalkEmotion,
            postWalkEmotion = postWalkEmotion,
            note = entity.note,
            imageUrl = entity.imageUrl,
            createdDate = entity.createdDate,
        )
    }
}
