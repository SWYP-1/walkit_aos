package swyp.team.walkit.data.local.mapper

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import swyp.team.walkit.data.local.entity.SyncState
import swyp.team.walkit.data.local.entity.WalkingSessionEntity
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.remote.walking.dto.WalkingSessionRequest
import swyp.team.walkit.data.remote.walking.mapper.toWalkPoints

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
            userId = session.userId,
            serverId = session.serverId,
            startTime = session.startTime,
            endTime = session.endTime ?: 0L,
            stepCount = session.stepCount,
            locationsJson = json.encodeToString(session.locations),
            filteredLocationsJson = session.filteredLocations?.let { json.encodeToString(it) },
            smoothedLocationsJson = session.smoothedLocations?.let { json.encodeToString(it) },
            totalDistance = session.totalDistance,
            syncState = syncState,
            preWalkEmotion = session.preWalkEmotion,
            postWalkEmotion = session.postWalkEmotion,
            note = session.note,
            localImagePath = session.localImagePath,
            serverImageUrl = session.serverImageUrl,
            createdDate = session.createdDate.orEmpty(),
            targetStepCount = session.targetStepCount,
            targetWalkCount = session.targetWalkCount
        )

    /** Entity → Domain */
    fun toDomain(entity: WalkingSessionEntity): WalkingSession {
        // 전체 함수를 try-catch로 감싸서 모든 Throwable 처리
        // ExceptionInInitializerError 등 Error 타입도 처리
        // EnumConverter.toEmotionType도 내부에서 Throwable을 처리하지만,
        // 추가 방어를 위해 전체를 감싸서 매핑 실패 시 기본값 반환
        return try {
            val locations = try {
                json.decodeFromString<List<LocationPoint>>(entity.locationsJson)
            } catch (e: Throwable) {
                // ExceptionInInitializerError 등 Error 타입도 처리
                emptyList()
            }

            val filteredLocations = entity.filteredLocationsJson?.let { jsonString ->
                try {
                    json.decodeFromString<List<LocationPoint>>(jsonString)
                } catch (e: Throwable) {
                    // ExceptionInInitializerError 등 Error 타입도 처리
                    null
                }
            }

            val smoothedLocations = entity.smoothedLocationsJson?.let { jsonString ->
                try {
                    json.decodeFromString<List<LocationPoint>>(jsonString)
                } catch (e: Throwable) {
                    // ExceptionInInitializerError 등 Error 타입도 처리
                    null
                }
            }

            WalkingSession(
                id = entity.id,  // ✅ DB의 실제 ID를 사용해야 함!
                userId = entity.userId,
                serverId = entity.serverId,
                startTime = entity.startTime,
                endTime = entity.endTime,
                stepCount = entity.stepCount,
                locations = locations,
                filteredLocations = filteredLocations,
                smoothedLocations = smoothedLocations,
                totalDistance = entity.totalDistance,
                preWalkEmotion = entity.preWalkEmotion,
                postWalkEmotion = entity.postWalkEmotion,
                note = entity.note,
                localImagePath = entity.localImagePath,
                serverImageUrl = entity.serverImageUrl,
                createdDate = entity.createdDate,
                targetStepCount = entity.targetStepCount,
                targetWalkCount = entity.targetWalkCount,
                isSynced = entity.isSynced
            )
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리
            // 매핑 실패 시 기본값으로 WalkingSession 생성
            // 최소한의 정보라도 보존하기 위해
            WalkingSession(
                id = entity.id,
                userId = entity.userId,
                serverId = entity.serverId,
                startTime = entity.startTime,
                endTime = entity.endTime,
                stepCount = entity.stepCount,
                locations = emptyList(),
                filteredLocations = null,
                smoothedLocations = null,
                totalDistance = entity.totalDistance,
                preWalkEmotion = entity.preWalkEmotion.ifEmpty { "CONTENT" }, // 기본값
                postWalkEmotion = entity.postWalkEmotion.ifEmpty { "CONTENT" }, // 기본값
                note = entity.note,
                localImagePath = entity.localImagePath,
                serverImageUrl = entity.serverImageUrl,
                createdDate = entity.createdDate,
                targetStepCount = entity.targetStepCount,
                targetWalkCount = entity.targetWalkCount,
                isSynced = entity.isSynced
            )
        }
    }

    /** Domain → API Request 
     * 서버 요구사항에 맞게 필수 필드만 포함
     */
    fun WalkingSession.toRequest(): WalkingSessionRequest =
        WalkingSessionRequest(
            preWalkEmotion = preWalkEmotion,
            postWalkEmotion = postWalkEmotion,
            note = note,
            points = locations.toWalkPoints(),
            endTime = endTime ?: System.currentTimeMillis(), // endTime이 null이면 현재 시간 사용
            startTime = startTime,
            totalDistance = totalDistance,
            stepCount = stepCount,
        )
}
