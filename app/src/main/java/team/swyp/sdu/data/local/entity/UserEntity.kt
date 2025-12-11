package team.swyp.sdu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 캐시 Entity
 */
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val nickname: String,
    val clearedCount: Int,
    val point: Int,
    val goalKmPerWeek: Double,
    val updatedAt: Long = System.currentTimeMillis(),
)
