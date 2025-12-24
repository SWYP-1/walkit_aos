package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.UserEntity
import team.swyp.sdu.domain.model.User

/**
 * 사용자 캐시 매퍼
 *
 * User 도메인 모델과 UserEntity 간의 변환을 담당합니다.
 */
object UserMapper {
    fun toEntity(domain: User): UserEntity =
        UserEntity(
            imageName = domain.imageName,
            nickname = domain.nickname,
            birthDate = domain.birthDate,
            sex = domain.sex?.name,
        )

    fun toDomain(entity: UserEntity): User =
        User(
            imageName = entity.imageName,
            nickname = entity.nickname,
            birthDate = entity.birthDate,
            sex = entity.sex?.let {
                try {
                    team.swyp.sdu.domain.model.Sex.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            },
        )
}
