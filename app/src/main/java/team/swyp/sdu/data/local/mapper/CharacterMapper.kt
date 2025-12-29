package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.CharacterEntity
import team.swyp.sdu.domain.model.Character

/**
 * 캐릭터 정보 매퍼
 *
 * Character 도메인 모델과 CharacterEntity 간의 변환을 담당합니다.
 */
object CharacterMapper {
    fun toEntity(domain: Character, nickname: String): CharacterEntity =
        CharacterEntity(
            nickname = nickname,
            headImageName = domain.headImageName,
            bodyImageName = domain.bodyImageName,
            feetImageName = domain.feetImageName,
            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = domain.grade,
        )

    fun toDomain(entity: CharacterEntity): Character =
        Character(
            headImageName = entity.headImageName,
            bodyImageName = entity.bodyImageName,
            feetImageName = entity.feetImageName,
            characterImageName = entity.characterImageName,
            backgroundImageName = entity.backgroundImageName,
            level = entity.level,
            grade = entity.grade,
            nickName = entity.nickname,
        )
}




