package team.swyp.sdu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import team.swyp.sdu.data.local.database.Converters
import team.swyp.sdu.domain.model.Grade

/**
 * 캐릭터 정보 Entity
 *
 * Home API에서 받은 Character 정보를 저장합니다.
 * nickname을 primary key로 사용하여 User와 1:1 관계를 가집니다.
 */
@Entity(tableName = "character_profile")
@TypeConverters(Converters::class)
data class CharacterEntity(
    @PrimaryKey
    val nickname: String,
    val headImageName: String? = null,
    val bodyImageName: String? = null,
    val feetImageName: String? = null,
    val characterImageName: String? = null,
    val backgroundImageName: String? = null,
    val level: Int = 1,
    val grade: Grade = Grade.SEED,
    val updatedAt: Long = System.currentTimeMillis(),
)




