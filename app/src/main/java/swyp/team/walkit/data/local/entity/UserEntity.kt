package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 사용자 캐시 Entity
 *
 * 새로운 서버 API 구조에 맞춘 Entity입니다.
 * Goal 정보는 별도의 GoalEntity로 분리되었습니다.
 * Character 정보는 CharacterEntity로 별도 관리됩니다.
 */
@Entity(tableName = "user_profile")
data class UserEntity(
    val nickname: String,
    @PrimaryKey
    val userId: Long,
    val imageName: String? = null,
    val birthDate: String? = null,
    val email: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
