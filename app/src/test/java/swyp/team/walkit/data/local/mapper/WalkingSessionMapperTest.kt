package swyp.team.walkit.data.local.mapper

import org.junit.Assert
import org.junit.Assert.assertNull

import org.junit.Test
import swyp.team.walkit.data.local.entity.SyncState
import swyp.team.walkit.data.local.entity.WalkingSessionEntity
import swyp.team.walkit.data.local.mapper.WalkingSessionMapper.toRequest
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.remote.walking.dto.WalkPoint
import swyp.team.walkit.data.remote.walking.dto.WalkingSessionRequest

/**
 * WalkingSessionMapper 단위 테스트
 *
 * 테스트 대상:
 * - toEntity(): WalkingSession → WalkingSessionEntity 변환
 * - toDomain(): WalkingSessionEntity → WalkingSession 변환
 * - JSON 직렬화/역직렬화 처리
 * - 에러 처리 (JSON 파싱 실패 시)
 * - toRequest(): WalkingSession → WalkingSessionRequest 변환
 */
class WalkingSessionMapperTest {

    @Test
    fun `toEntity - WalkingSession을 WalkingSessionEntity로 변환 (기본 케이스)`() {
        // Given: WalkingSession 도메인 객체
        val session = WalkingSession(
            id = "session_123",
            userId = 456L,
            serverId = "server_789",
            startTime = 1000000L,
            endTime = 2000000L,
            stepCount = 5000,
            locations = listOf(
                LocationPoint(37.5665, 126.9780, 1000000L),
                LocationPoint(37.5666, 126.9781, 1001000L)
            ),
            filteredLocations = listOf(
                LocationPoint(37.5665, 126.9780, 1000000L)
            ),
            smoothedLocations = null,
            totalDistance = 1.5f,
            preWalkEmotion = "HAPPY",
            postWalkEmotion = "CONTENT",
            note = "좋은 산책이었어요",
            localImagePath = "/path/to/image.jpg",
            serverImageUrl = "https://server.com/image.jpg",
            createdDate = "2024-01-01",
            targetStepCount = 10000,
            targetWalkCount = 1,
            isSynced = true
        )

        // When: Entity로 변환
        val entity = WalkingSessionMapper.toEntity(session, SyncState.SYNCED)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals("session_123", entity.id)
        Assert.assertEquals(456L, entity.userId)
        Assert.assertEquals("server_789", entity.serverId)
        Assert.assertEquals(1000000L, entity.startTime)
        Assert.assertEquals(2000000L, entity.endTime)
        Assert.assertEquals(5000, entity.stepCount)
        Assert.assertEquals(1.5f, entity.totalDistance)
        Assert.assertEquals(SyncState.SYNCED, entity.syncState)
        Assert.assertEquals("HAPPY", entity.preWalkEmotion)
        Assert.assertEquals("CONTENT", entity.postWalkEmotion)
        Assert.assertEquals("좋은 산책이었어요", entity.note)
        Assert.assertEquals("/path/to/image.jpg", entity.localImagePath)
        Assert.assertEquals("https://server.com/image.jpg", entity.serverImageUrl)
        Assert.assertEquals("2024-01-01", entity.createdDate)
        Assert.assertEquals(10000, entity.targetStepCount)
        Assert.assertEquals(1, entity.targetWalkCount)
        Assert.assertTrue("엔티티가 동기화 상태여야 함", entity.isSynced)

        // JSON 필드들이 직렬화되었는지 확인
        Assert.assertTrue("위치 JSON이 비어있지 않아야 함", entity.locationsJson.isNotEmpty())
        Assert.assertTrue("필터링된 위치 JSON이 비어있지 않아야 함", entity.filteredLocationsJson?.isNotEmpty() == true)
        assertNull(entity.smoothedLocationsJson)
    }

    @Test
    fun `toDomain - WalkingSessionEntity를 WalkingSession으로 변환 (기본 케이스)`() {
        // Given: WalkingSessionEntity 객체
        val locationsJson =
            """[{"latitude":37.5665,"longitude":126.978,"timestamp":1000000},{"latitude":37.5666,"longitude":126.9781,"timestamp":1001000}]"""
        val filteredLocationsJson =
            """[{"latitude":37.5665,"longitude":126.978,"timestamp":1000000}]"""

        val entity = WalkingSessionEntity(
            id = "session_456",
            userId = 789L,
            serverId = "server_101",
            startTime = 2000000L,
            endTime = 3000000L,
            stepCount = 8000,
            locationsJson = locationsJson,
            filteredLocationsJson = filteredLocationsJson,
            smoothedLocationsJson = null,
            totalDistance = 2.5f,
            syncState = SyncState.PENDING,
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            note = "피곤했지만 기분 좋았어요",
            localImagePath = "/local/image.png",
            serverImageUrl = "https://cdn.com/image.png",
            createdDate = "2024-01-02",
            targetStepCount = 8000,
            targetWalkCount = 2,
            isSynced = false
        )

        // When: Domain으로 변환
        val session = WalkingSessionMapper.toDomain(entity)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals("session_456", session.id)
        Assert.assertEquals(789L, session.userId)
        Assert.assertEquals("server_101", session.serverId)
        Assert.assertEquals(2000000L, session.startTime)
        Assert.assertEquals(3000000L, session.endTime)
        Assert.assertEquals(8000, session.stepCount)
        Assert.assertEquals(2.5f, session.totalDistance)
        Assert.assertEquals("TIRED", session.preWalkEmotion)
        Assert.assertEquals("HAPPY", session.postWalkEmotion)
        Assert.assertEquals("피곤했지만 기분 좋았어요", session.note)
        Assert.assertEquals("/local/image.png", session.localImagePath)
        Assert.assertEquals("https://cdn.com/image.png", session.serverImageUrl)
        Assert.assertEquals("2024-01-02", session.createdDate)
        Assert.assertEquals(8000, session.targetStepCount)
        Assert.assertEquals(2, session.targetWalkCount)
        Assert.assertEquals(false, session.isSynced)

        // 위치 데이터가 올바르게 역직렬화됨
        Assert.assertEquals(2, session.locations.size)
        Assert.assertEquals(37.5665, session.locations[0].latitude, 0.001)
        Assert.assertEquals(126.978, session.locations[0].longitude, 0.001)
        Assert.assertEquals(1000000L, session.locations[0].timestamp)

        Assert.assertEquals(1, session.filteredLocations?.size)
        Assert.assertEquals(37.5665, session.filteredLocations!![0].latitude, 0.001)

        assertNull(session.smoothedLocations)
    }

    @Test
    fun `toDomain - JSON 파싱 실패 시 기본값 반환`() {
        // Given: 잘못된 JSON이 포함된 Entity
        val entity = WalkingSessionEntity(
            id = "session_error",
            userId = 123L,
            serverId = null,
            startTime = 1000000L,
            endTime = 2000000L,
            stepCount = 1000,
            locationsJson = "invalid json",  // 잘못된 JSON
            filteredLocationsJson = "also invalid",  // 잘못된 JSON
            smoothedLocationsJson = "not json either",  // 잘못된 JSON
            totalDistance = 1.0f,
            syncState = SyncState.PENDING,
            preWalkEmotion = "",
            postWalkEmotion = "",
            note = "에러 테스트",
            localImagePath = null,
            serverImageUrl = null,
            createdDate = "2024-01-01",
            targetStepCount = 5000,
            targetWalkCount = 1,
            isSynced = false
        )

        // When: Domain으로 변환 (JSON 파싱 실패에도 성공해야 함)
        val session = WalkingSessionMapper.toDomain(entity)

        // Then: 기본값들로 채워진 Session이 반환됨
        Assert.assertEquals("session_error", session.id)
        Assert.assertEquals(123L, session.userId)
        Assert.assertEquals(1000000L, session.startTime)
        Assert.assertEquals(2000000L, session.endTime)
        Assert.assertEquals(1000, session.stepCount)
        Assert.assertEquals(1.0f, session.totalDistance)
        Assert.assertEquals("에러 테스트", session.note)

        // JSON 파싱 실패로 기본값들 설정
        Assert.assertTrue("JSON 파싱 실패로 위치 정보가 비어 있어야 함", session.locations.isEmpty())
        assertNull(session.filteredLocations)
        assertNull(session.smoothedLocations)

        // 빈 문자열 감정은 기본값으로 설정
        Assert.assertEquals("CONTENT", session.preWalkEmotion)
        Assert.assertEquals("CONTENT", session.postWalkEmotion)
    }

    @Test
    fun `toRequest - WalkingSession을 WalkingSessionRequest로 변환`() {
        // Given: WalkingSession 객체
        val session = WalkingSession(
            id = "session_req",
            userId = 999L,
            serverId = "server_req",
            startTime = 5000000L,
            endTime = 6000000L,
            stepCount = 3000,
            locations = listOf(
                LocationPoint(37.123, 127.456, 5000000L),
                LocationPoint(37.124, 127.457, 5001000L)
            ),
            filteredLocations = null,
            smoothedLocations = null,
            totalDistance = 0.8f,
            preWalkEmotion = "HAPPY",
            postWalkEmotion = "JOYFUL",
            note = "즐거운 산책",
            localImagePath = null,
            serverImageUrl = null,
            createdDate = "2024-01-03",
            targetStepCount = 3000,
            targetWalkCount = 1,
            isSynced = true
        )

        // When: Request로 변환
        val request = session.toRequest()

        // Then: API 요청에 필요한 필드들만 포함됨
        Assert.assertEquals("HAPPY", request.preWalkEmotion)
        Assert.assertEquals("JOYFUL", request.postWalkEmotion)
        Assert.assertEquals("즐거운 산책", request.note)
        Assert.assertEquals(6000000L, request.endTime)
        Assert.assertEquals(5000000L, request.startTime)
        Assert.assertEquals(0.8f, request.totalDistance)
        Assert.assertEquals(3000, request.stepCount)

        // 위치 데이터가 WalkPoint로 변환됨
        Assert.assertEquals(2, request.points.size)
        Assert.assertEquals(37.123, request.points[0].latitude, 0.001)
        Assert.assertEquals(127.456, request.points[0].longitude, 0.001)
        Assert.assertEquals(5000000L, request.points[0].timestampMillis)
    }

    @Test
    fun `toRequest - endTime이 null인 경우 현재 시간 사용`() {
        // Given: endTime이 null인 WalkingSession
        val session = WalkingSession(
            id = "session_null_end",
            userId = 111L,
            startTime = 3000000L,
            endTime = 3003300L,  // null 값
            stepCount = 1500,
            locations = listOf(LocationPoint(37.0, 127.0, 3000000L)),
            totalDistance = 0.5f,
            preWalkEmotion = "TIRED",
            postWalkEmotion = "CONTENT",
            note = null,
            localImagePath = null,
            serverImageUrl = null,
            createdDate = "2024-01-04",
            targetStepCount = 1500,
            targetWalkCount = 1,
            isSynced = false
        )

        // When: Request로 변환
        val request = session.toRequest()

        // Then: endTime이 현재 시간보다 크거나 같아야 함 (시스템 시간 사용)
        val currentTime = System.currentTimeMillis()
        Assert.assertTrue(
            "endTime이 현재 시간 이후여야 함",
            request.endTime >= currentTime - 1000,
        ) // 1초 오차 허용
        Assert.assertEquals(3000000L, request.startTime)
        Assert.assertEquals(0.5f, request.totalDistance)
        Assert.assertEquals(1500, request.stepCount)
    }

    @Test
    fun `양방향 변환 - WalkingSession ↔ WalkingSessionEntity 일관성`() {
        // Given: 원본 WalkingSession
        val originalSession = WalkingSession(
            id = "test_session",
            userId = 555L,
            serverId = "test_server",
            startTime = 4000000L,
            endTime = 5000000L,
            stepCount = 2500,
            locations = listOf(
                LocationPoint(35.123, 129.456, 4000000L),
                LocationPoint(35.124, 129.457, 4001000L)
            ),
            filteredLocations = listOf(LocationPoint(35.123, 129.456, 4000000L)),
            smoothedLocations = null,
            totalDistance = 1.2f,
            preWalkEmotion = "JOYFUL",
            postWalkEmotion = "HAPPY",
            note = "테스트 산책",
            localImagePath = "/test/image.jpg",
            serverImageUrl = "https://test.com/image.jpg",
            createdDate = "2024-01-05",
            targetStepCount = 2500,
            targetWalkCount = 1,
            isSynced = true
        )

        // When: Session → Entity → Session 변환
        val entity = WalkingSessionMapper.toEntity(originalSession, SyncState.SYNCED)
        val convertedSession = WalkingSessionMapper.toDomain(entity)

        // Then: 원본과 변환 결과가 동일함 (JSON 직렬화/역직렬화로 인한 부동소수점 오차 제외)
        Assert.assertEquals(originalSession.id, convertedSession.id)
        Assert.assertEquals(originalSession.userId, convertedSession.userId)
        Assert.assertEquals(originalSession.serverId, convertedSession.serverId)
        Assert.assertEquals(originalSession.startTime, convertedSession.startTime)
        Assert.assertEquals(originalSession.endTime, convertedSession.endTime)
        Assert.assertEquals(originalSession.stepCount, convertedSession.stepCount)
        Assert.assertEquals(originalSession.totalDistance, convertedSession.totalDistance)
        Assert.assertEquals(originalSession.preWalkEmotion, convertedSession.preWalkEmotion)
        Assert.assertEquals(originalSession.postWalkEmotion, convertedSession.postWalkEmotion)
        Assert.assertEquals(originalSession.note, convertedSession.note)
        Assert.assertEquals(originalSession.localImagePath, convertedSession.localImagePath)
        Assert.assertEquals(originalSession.serverImageUrl, convertedSession.serverImageUrl)
        Assert.assertEquals(originalSession.createdDate, convertedSession.createdDate)
        Assert.assertEquals(originalSession.targetStepCount, convertedSession.targetStepCount)
        Assert.assertEquals(originalSession.targetWalkCount, convertedSession.targetWalkCount)
        Assert.assertEquals(originalSession.isSynced, convertedSession.isSynced)

        // 위치 데이터는 JSON 변환으로 인해 동일한지 확인
        Assert.assertEquals(originalSession.locations.size, convertedSession.locations.size)
        Assert.assertEquals(
            originalSession.filteredLocations?.size,
            convertedSession.filteredLocations?.size
        )
    }

    @Test
    fun `nullable 필드 처리 테스트`() {
        // Given: nullable 필드들이 null인 WalkingSession
        val sessionWithNulls = WalkingSession(
            id = "null_test",
            userId = 222L,
            serverId = null,
            startTime = 1000000L,
            endTime = 2000000L,
            stepCount = 500,
            locations = emptyList(),
            filteredLocations = null,
            smoothedLocations = null,
            totalDistance = 0f,
            preWalkEmotion = "CONTENT",
            postWalkEmotion = "CONTENT",
            note = null,
            localImagePath = null,
            serverImageUrl = null,
            createdDate = "",
            targetStepCount = 500,
            targetWalkCount = 1,
            isSynced = false
        )

        // When: 변환 수행
        val entity = WalkingSessionMapper.toEntity(sessionWithNulls, SyncState.PENDING)
        val convertedSession = WalkingSessionMapper.toDomain(entity)

        // Then: null 값들이 적절히 처리됨
        assertNull(convertedSession.serverId)
        assertNull(convertedSession.note)
        assertNull(convertedSession.localImagePath)
        assertNull(convertedSession.serverImageUrl)
        assertNull(convertedSession.filteredLocations)
        assertNull(convertedSession.smoothedLocations)

        // JSON 필드들도 null 처리됨
        assertNull(entity.smoothedLocationsJson)
    }
}