package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.UserEntity
import team.swyp.sdu.domain.model.UserProfile

/**
 * 사용자 캐시 매퍼
 */
object UserMapper {
    fun toEntity(domain: UserProfile): UserEntity =
        UserEntity(
            uid = domain.uid,
            nickname = domain.nickname,
            clearedCount = domain.clearedCount,
            point = domain.point,
            goalKmPerWeek = domain.goalKmPerWeek,
        )

    fun toDomain(entity: UserEntity): UserProfile =
        UserProfile(
            uid = entity.uid,
            nickname = entity.nickname,
            clearedCount = entity.clearedCount,
            point = entity.point,
            goalKmPerWeek = entity.goalKmPerWeek,
        )
}
