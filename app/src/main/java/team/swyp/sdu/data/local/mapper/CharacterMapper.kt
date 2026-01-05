package team.swyp.sdu.data.local.mapper

import team.swyp.sdu.data.local.entity.CharacterEntity
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Grade

/**
 * 캐릭터 정보 매퍼
 *
 * Character 도메인 모델과 CharacterEntity 간의 변환을 담당합니다.
 */
object CharacterMapper {
    fun toEntity(domain: Character, userId: Long): CharacterEntity =
        CharacterEntity(
            userId = userId,
            headImageName = domain.headImageName,
            headImageTag = domain.headImageTag,
            bodyImageName = domain.bodyImageName,
            feetImageName = domain.feetImageName,
            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = domain.grade, // domain Grade 사용
            nickName = domain.nickName, // nickName 추가
        )

    fun toDomain(entity: CharacterEntity): Character =
        Character(
            headImageName = entity.headImageName,
            headImageTag = entity.headImageTag,
            bodyImageName = entity.bodyImageName,
            feetImageName = entity.feetImageName,
            characterImageName = entity.characterImageName,
            backgroundImageName = entity.backgroundImageName,
            level = entity.level,
            grade = entity.grade, // domain Grade 사용
            nickName = entity.nickName ?: "게스트", // nickName 필드 사용, 없으면 기본값
        )
}




