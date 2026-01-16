package swyp.team.walkit.data.local.mapper

import org.junit.Assert
import org.junit.Test
import swyp.team.walkit.data.local.entity.GoalEntity
import swyp.team.walkit.domain.model.Goal

/**
 * GoalMapper 단위 테스트
 *
 * 테스트 대상:
 * - toEntity(): Goal → GoalEntity 변환
 * - toDomain(): GoalEntity → Goal 변환
 * - 양방향 변환 일관성
 */
class GoalMapperTest {

    @Test
    fun `toEntity - Goal을 GoalEntity로 변환`() {
        // Given: Goal 도메인 객체
        val goal = Goal(
            targetStepCount = 10000,
            targetWalkCount = 5
        )

        // When: Entity로 변환
        val entity = GoalMapper.toEntity(goal)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals(10000, entity.targetStepCount)
        Assert.assertEquals(5, entity.targetWalkCount)
    }

    @Test
    fun `toDomain - GoalEntity를 Goal로 변환`() {
        // Given: GoalEntity 객체
        val entity = GoalEntity(
            targetStepCount = 8000,
            targetWalkCount = 3
        )

        // When: Domain으로 변환
        val goal = GoalMapper.toDomain(entity)

        // Then: 값이 올바르게 매핑됨
        Assert.assertEquals(8000, goal.targetStepCount)
        Assert.assertEquals(3, goal.targetWalkCount)
    }

    @Test
    fun `양방향 변환 - Goal ↔ GoalEntity 일관성 보장`() {
        // Given: 원본 Goal
        val originalGoal = Goal(
            targetStepCount = 12000,
            targetWalkCount = 7
        )

        // When: Goal → Entity → Goal 변환
        val entity = GoalMapper.toEntity(originalGoal)
        val convertedGoal = GoalMapper.toDomain(entity)

        // Then: 원본과 변환 결과가 동일함
        Assert.assertEquals(originalGoal, convertedGoal)
    }

    @Test
    fun `다양한 값들로 변환 테스트`() {
        // Given: 다양한 Goal 값들
        val testCases = listOf(
            Goal(0, 0),           // 최소값
            Goal(5000, 1),        // 일반적인 값
            Goal(20000, 10),      // 큰 값
            Goal(Int.MAX_VALUE, Int.MAX_VALUE)  // 최대값
        )

        // When & Then: 모든 케이스에서 양방향 변환이 일관됨
        testCases.forEach { originalGoal ->
            val entity = GoalMapper.toEntity(originalGoal)
            val convertedGoal = GoalMapper.toDomain(entity)
            Assert.assertEquals("Goal 변환 실패: $originalGoal", originalGoal, convertedGoal)
        }
    }

    @Test
    fun `기본값 테스트`() {
        // Given: 기본값이 설정된 Goal (실제로는 파라미터 기본값 없음)
        val goal = Goal(10000, 5)

        // When: 변환 수행
        val entity = GoalMapper.toEntity(goal)
        val convertedBack = GoalMapper.toDomain(entity)

        // Then: 값이 유지됨
        Assert.assertEquals(goal, convertedBack)
    }
}