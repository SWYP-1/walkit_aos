package swyp.team.walkit.data.local.mapper

import org.junit.Assert
import org.junit.Test
import swyp.team.walkit.data.local.entity.MissionProgressEntity
import swyp.team.walkit.domain.model.DailyMissionProgress
import java.time.LocalDate

/**
 * MissionProgressMapper 단위 테스트
 *
 * 테스트 대상:
 * - toEntity(): DailyMissionProgress → MissionProgressEntity 변환
 * - toDomain(): MissionProgressEntity → DailyMissionProgress 변환
 * - 날짜 포맷팅 처리 (ISO_LOCAL_DATE)
 * - 양방향 변환 일관성
 */
class MissionProgressMapperTest {

    @Test
    fun `toEntity - DailyMissionProgress를 MissionProgressEntity로 변환`() {
        // Given: DailyMissionProgress 도메인 객체
        val date = LocalDate.of(2024, 1, 15)
        val progress = DailyMissionProgress(
            date = date,
            distanceKm = 5.25,
            steps = 8250,
            missionCompleted = true,
            lastUpdatedAt = 1705296000000L  // 2024-01-15 00:00:00 UTC
        )

        // When: Entity로 변환
        val entity = MissionProgressMapper.toEntity(progress)

        // Then: 값이 올바르게 매핑되고 날짜가 ISO 포맷으로 변환됨
        Assert.assertEquals("2024-01-15", entity.date)  // ISO_LOCAL_DATE 포맷
        Assert.assertEquals(5.25, entity.distanceKm, 0.001)
        Assert.assertEquals(8250, entity.steps)
        Assert.assertEquals(true, entity.missionCompleted)
        Assert.assertEquals(1705296000000L, entity.lastUpdatedAt)
    }

    @Test
    fun `toDomain - MissionProgressEntity를 DailyMissionProgress로 변환`() {
        // Given: MissionProgressEntity 객체
        val entity = MissionProgressEntity(
            date = "2024-01-15",
            distanceKm = 3.75,
            steps = 6200,
            missionCompleted = false,
            lastUpdatedAt = 1705382400000L  // 2024-01-16 00:00:00 UTC
        )

        // When: Domain으로 변환
        val progress = MissionProgressMapper.toDomain(entity)

        // Then: 값이 올바르게 매핑되고 날짜가 LocalDate로 파싱됨
        Assert.assertEquals(LocalDate.of(2024, 1, 15), progress.date)
        Assert.assertEquals(3.75, progress.distanceKm, 0.001)
        Assert.assertEquals(6200, progress.steps)
        Assert.assertEquals(false, progress.missionCompleted)
        Assert.assertEquals(1705382400000L, progress.lastUpdatedAt)
    }

    @Test
    fun `양방향 변환 - DailyMissionProgress ↔ MissionProgressEntity 일관성 보장`() {
        // Given: 원본 DailyMissionProgress
        val originalProgress = DailyMissionProgress(
            date = LocalDate.of(2024, 3, 20),
            distanceKm = 7.5,
            steps = 12000,
            missionCompleted = true,
            lastUpdatedAt = 1710892800000L  // 2024-03-20 00:00:00 UTC
        )

        // When: Progress → Entity → Progress 변환
        val entity = MissionProgressMapper.toEntity(originalProgress)
        val convertedProgress = MissionProgressMapper.toDomain(entity)

        // Then: 원본과 변환 결과가 동일함
        Assert.assertEquals(originalProgress, convertedProgress)
    }

    @Test
    fun `다양한 날짜로 변환 테스트`() {
        // Given: 다양한 날짜들의 DailyMissionProgress
        val testCases = listOf(
            LocalDate.of(2020, 1, 1),    // 최소 연도
            LocalDate.of(2024, 6, 15),   // 일반 날짜
            LocalDate.of(2030, 12, 31),  // 최대 연도
            LocalDate.of(2024, 2, 29),   // 윤년 날짜
        )

        // When & Then: 모든 날짜에서 변환 일관성 보장
        testCases.forEach { date ->
            val originalProgress = DailyMissionProgress(
                date = date,
                distanceKm = 1.0,
                steps = 1000,
                missionCompleted = false,
                lastUpdatedAt = 1000000L
            )

            val entity = MissionProgressMapper.toEntity(originalProgress)
            val convertedProgress = MissionProgressMapper.toDomain(entity)

            Assert.assertEquals("날짜 변환 실패: $date", originalProgress, convertedProgress)
            Assert.assertEquals("ISO 날짜 포맷 실패: $date", date.toString(), entity.date)
        }
    }

    @Test
    fun `다양한 수치 값들로 변환 테스트`() {
        // Given: 다양한 수치 값들의 테스트 케이스
        val testCases = listOf(
            Triple(0.0, 0, false),          // 최소값
            Triple(1.5, 2500, true),        // 일반값
            Triple(99.99, 99999, true),     // 큰 값
            Triple(0.001, 1, false),        // 소수점
        )

        // When & Then: 모든 수치 값에서 변환 일관성 보장
        testCases.forEach { (distance, steps, completed) ->
            val date = LocalDate.of(2024, 1, 1)
            val originalProgress = DailyMissionProgress(
                date = date,
                distanceKm = distance,
                steps = steps,
                missionCompleted = completed,
                lastUpdatedAt = 1640995200000L  // 2022-01-01 00:00:00 UTC
            )

            val entity = MissionProgressMapper.toEntity(originalProgress)
            val convertedProgress = MissionProgressMapper.toDomain(entity)

            Assert.assertEquals("수치 변환 실패: distance=$distance, steps=$steps", originalProgress, convertedProgress)
            Assert.assertEquals(distance, entity.distanceKm, 0.001)
            Assert.assertEquals(steps, entity.steps)
            Assert.assertEquals(completed, entity.missionCompleted)
        }
    }

    @Test
    fun `날짜 포맷팅 검증 - ISO_LOCAL_DATE 포맷 사용`() {
        // Given: 다양한 날짜들
        val testDates = listOf(
            LocalDate.of(2024, 1, 1) to "2024-01-01",
            LocalDate.of(2024, 12, 31) to "2024-12-31",
            LocalDate.of(2024, 2, 29) to "2024-02-29",  // 윤년
            LocalDate.of(2024, 10, 5) to "2024-10-05",  // 0 패딩
        )

        // When & Then: ISO_LOCAL_DATE 포맷이 올바르게 적용됨
        testDates.forEach { (date, expectedString) ->
            val progress = DailyMissionProgress(
                date = date,
                distanceKm = 1.0,
                steps = 1000,
                missionCompleted = false,
                lastUpdatedAt = 1000000L
            )

            val entity = MissionProgressMapper.toEntity(progress)
            Assert.assertEquals("ISO 날짜 포맷 실패: $date", expectedString, entity.date)

            // 역변환도 올바르게 동작해야 함
            val convertedProgress = MissionProgressMapper.toDomain(entity)
            Assert.assertEquals("날짜 파싱 실패: $expectedString", date, convertedProgress.date)
        }
    }

    @Test
    fun `lastUpdatedAt 타임스탬프 보존 테스트`() {
        // Given: 다양한 타임스탬프 값들
        val timestamps = listOf(
            0L,                          // 에포크 시작
            1640995200000L,             // 2022-01-01
            System.currentTimeMillis(),  // 현재 시간
            Long.MAX_VALUE               // 최대값
        )

        // When & Then: 타임스탬프가 변환 과정에서 보존됨
        timestamps.forEach { timestamp ->
            val progress = DailyMissionProgress(
                date = LocalDate.of(2024, 1, 1),
                distanceKm = 1.0,
                steps = 1000,
                missionCompleted = false,
                lastUpdatedAt = timestamp
            )

            val entity = MissionProgressMapper.toEntity(progress)
            val convertedProgress = MissionProgressMapper.toDomain(entity)

            Assert.assertEquals("타임스탬프 보존 실패: $timestamp", timestamp, convertedProgress.lastUpdatedAt)
        }
    }

    @Test
    fun `미션 완료 상태 변환 테스트`() {
        // Given: 완료 상태가 다른 두 개의 Progress
        val completedProgress = DailyMissionProgress(
            date = LocalDate.of(2024, 1, 1),
            distanceKm = 5.0,
            steps = 8000,
            missionCompleted = true,
            lastUpdatedAt = 1640995200000L
        )

        val notCompletedProgress = DailyMissionProgress(
            date = LocalDate.of(2024, 1, 1),
            distanceKm = 2.0,
            steps = 3000,
            missionCompleted = false,
            lastUpdatedAt = 1640995200000L
        )

        // When: 변환 수행
        val completedEntity = MissionProgressMapper.toEntity(completedProgress)
        val notCompletedEntity = MissionProgressMapper.toEntity(notCompletedProgress)

        // Then: 완료 상태가 올바르게 보존됨
        Assert.assertEquals(true, completedEntity.missionCompleted)
        Assert.assertEquals(false, notCompletedEntity.missionCompleted)

        // 역변환도 올바름
        val convertedCompleted = MissionProgressMapper.toDomain(completedEntity)
        val convertedNotCompleted = MissionProgressMapper.toDomain(notCompletedEntity)

        Assert.assertEquals(true, convertedCompleted.missionCompleted)
        Assert.assertEquals(false, convertedNotCompleted.missionCompleted)
    }
}