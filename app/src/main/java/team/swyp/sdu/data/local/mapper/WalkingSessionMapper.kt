package team.swyp.sdu.data.local.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import team.swyp.sdu.data.local.entity.SyncState
import team.swyp.sdu.data.local.entity.WalkingSessionEntity
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.remote.walking.dto.WalkingSessionRequest
import team.swyp.sdu.data.remote.walking.mapper.toWalkPoints
import team.swyp.sdu.data.utils.EnumConverter

object WalkingSessionMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Domain → Entity */
    fun toEntity(
        session: WalkingSession,
        syncState: SyncState,
    ): WalkingSessionEntity =
        WalkingSessionEntity(
            id = session.id,
            startTime = session.startTime,
            endTime = session.endTime ?: 0L,
            stepCount = session.stepCount,
            locationsJson = json.encodeToString(session.locations),
            totalDistance = session.totalDistance,
            syncState = syncState,
            preWalkEmotion = session.preWalkEmotion.name,
            postWalkEmotion = session.postWalkEmotion.name,
            note = session.note,
            imageUrl = session.imageUrl, // Deprecated 필드 (하위 호환성 유지)
            localImagePath = session.localImagePath,
            serverImageUrl = session.serverImageUrl,
            createdDate = session.createdDate.orEmpty()
        )

    /** Entity → Domain */
    fun toDomain(entity: WalkingSessionEntity): WalkingSession {
        val locations = runCatching {
            json.decodeFromString<List<LocationPoint>>(entity.locationsJson)
        }.getOrDefault(emptyList())

        return WalkingSession(
            startTime = entity.startTime,
            endTime = entity.endTime,
            stepCount = entity.stepCount,
            locations = locations,
            totalDistance = entity.totalDistance,
            preWalkEmotion = EnumConverter.toEmotionType(entity.preWalkEmotion),
            postWalkEmotion = EnumConverter.toEmotionType(entity.postWalkEmotion),
            note = entity.note,
            imageUrl = entity.imageUrl, // Deprecated 필드 (하위 호환성 유지)
            localImagePath = entity.localImagePath,
            serverImageUrl = entity.serverImageUrl,
            createdDate = entity.createdDate
        )
    }

    /** Domain → API Request 
     * 서버 요구사항에 맞게 필수 필드만 포함
     */
    fun WalkingSession.toRequest(): WalkingSessionRequest =
        WalkingSessionRequest(
            preWalkEmotion = preWalkEmotion.name,
            postWalkEmotion = postWalkEmotion.name,
            note = note,
            points = locations.toWalkPoints(),
            endTime = endTime ?: System.currentTimeMillis(), // endTime이 null이면 현재 시간 사용
            startTime = startTime,
            totalDistance = totalDistance,
            stepCount = stepCount,
        )
}
