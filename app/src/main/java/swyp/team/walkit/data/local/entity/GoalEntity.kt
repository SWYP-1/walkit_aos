package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 목표 캐시 Entity
 *
 * 사용자의 산책 목표 정보를 저장합니다.
 * 단일 사용자 앱이므로 단일 Goal 인스턴스만 관리합니다.
 */
@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey
    val id: Long = 1L, // 단일 Goal 인스턴스
    val targetStepCount: Int,
    val targetWalkCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)


