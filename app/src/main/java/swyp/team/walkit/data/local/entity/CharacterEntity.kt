package swyp.team.walkit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import swyp.team.walkit.data.local.database.Converters
import swyp.team.walkit.domain.model.Grade

/**
 * 캐릭터 정보 Entity
 *
 * Home API에서 받은 Character 정보를 저장합니다.
 * userId를 primary key로 사용하여 User와 1:1 관계를 가집니다.
 *
 * 전략: DB 스키마 변경 없이 domain model만 개선
 * - flat 구조 유지로 migration 불필요
 * - domain model에서만 object 구조로 변환
 */
@Entity(tableName = "character_profile")
@TypeConverters(Converters::class)
data class CharacterEntity(
    @PrimaryKey
    val userId: Long,

    // flat 구조 유지 (migration 불필요)
    val headImageName: String? = null,
    val headImageTag: String? = null,  // HEAD 영역의 tag 정보
    val bodyImageName: String? = null,
    val feetImageName: String? = null,

    // 공통 필드
    val characterImageName: String? = null,
    val backgroundImageName: String? = null,
    val level: Int = 1,
    val grade: Grade = Grade.SEED,
    val nickName: String? = null, // 닉네임 필드 추가
    val updatedAt: Long = System.currentTimeMillis(),
)




