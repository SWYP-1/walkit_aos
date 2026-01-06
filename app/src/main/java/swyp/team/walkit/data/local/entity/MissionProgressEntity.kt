package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 하루 미션 진행도 Entity
 */
@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey
    val date: String,
    val distanceKm: Double,
    val steps: Int,
    val missionCompleted: Boolean,
    val lastUpdatedAt: Long,
)











