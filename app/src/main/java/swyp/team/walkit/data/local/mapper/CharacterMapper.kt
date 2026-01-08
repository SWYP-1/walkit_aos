package swyp.team.walkit.data.local.mapper

import swyp.team.walkit.data.local.entity.CharacterEntity
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.Grade

/**
 * 캐릭터 정보 매퍼
 *
 * Character 도메인 모델과 CharacterEntity 간의 변환을 담당합니다.
 */
object CharacterMapper {
    fun toEntity(domain: Character, userId: Long): CharacterEntity =
        CharacterEntity(
            userId = userId,
            // flat 구조로만 저장 (DB 스키마 변경 없음)
            headImageName = domain.headImageName,  // backward compatibility getter 사용
            headImageTag = domain.headImageTag,    // backward compatibility getter 사용
            bodyImageName = domain.bodyImageName,  // backward compatibility getter 사용
            feetImageName = domain.feetImageName,  // backward compatibility getter 사용

            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = domain.grade, // domain Grade 사용
            nickName = domain.nickName, // nickName 추가
        )

    fun toDomain(entity: CharacterEntity): Character =
        Character(
            // flat 구조를 object 구조로 변환 (domain model에서만 개선)
            headImage = entity.headImageName?.let { CharacterImage(it, entity.headImageTag) },
            bodyImage = entity.bodyImageName?.let { CharacterImage(it, entity.headImageTag) }, // Note: DB에는 body/feet tag가 없으므로 headTag 재사용
            feetImage = entity.feetImageName?.let { CharacterImage(it, null) },
            characterImageName = entity.characterImageName,
            backgroundImageName = entity.backgroundImageName,
            level = entity.level,
            grade = entity.grade, // domain Grade 사용
            nickName = entity.nickName ?: "게스트", // nickName 필드 사용, 없으면 기본값
        )
}




