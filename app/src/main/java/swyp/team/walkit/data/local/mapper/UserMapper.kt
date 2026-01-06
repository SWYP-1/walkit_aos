package swyp.team.walkit.data.local.mapper

import swyp.team.walkit.data.local.entity.UserEntity
import swyp.team.walkit.domain.model.User

/**
 * 사용자 캐시 매퍼
 *
 * User 도메인 모델과 UserEntity 간의 변환을 담당합니다.
 */
object UserMapper {
    fun toEntity(domain: User): UserEntity =
        UserEntity(
            nickname = domain.nickname,
            userId = domain.userId,
            imageName = domain.imageName,
            birthDate = domain.birthDate,
            email = domain.email,
        )

    fun toDomain(entity: UserEntity): User =
        User(
            userId = entity.userId ?: 0L,
            imageName = entity.imageName,
            nickname = entity.nickname,
            birthDate = entity.birthDate,
            email = entity.email,
        )
}
