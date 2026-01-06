package swyp.team.walkit.data.local.mapper

import swyp.team.walkit.data.local.entity.GoalEntity
import swyp.team.walkit.domain.model.Goal

/**
 * 목표 캐시 매퍼
 *
 * Goal 도메인 모델과 GoalEntity 간의 변환을 담당합니다.
 */
object GoalMapper {
    fun toEntity(domain: Goal): GoalEntity =
        GoalEntity(
            targetStepCount = domain.targetStepCount,
            targetWalkCount = domain.targetWalkCount,
        )

    fun toDomain(entity: GoalEntity): Goal =
        Goal(
            targetStepCount = entity.targetStepCount,
            targetWalkCount = entity.targetWalkCount,
        )
}


