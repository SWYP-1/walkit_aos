package team.swyp.sdu.data.remote.walking.mapper

import team.swyp.sdu.data.remote.walking.dto.CharacterDto
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Grade as DomainGrade

/**
 * CharacterDto와 Character 도메인 모델 간 변환 매퍼
 */
object CharacterMapper {

    /**
     * CharacterDto를 Character 도메인 모델로 변환
     */
    fun toDomain(dto: CharacterDto): Character {
        return Character(
            headImageName = dto.headImageName,
            bodyImageName = dto.bodyImageName,
            feetImageName = dto.feetImageName,
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = DomainGrade.fromApiGrade(dto.grade), // API Grade → Domain Grade 변환
            nickName = dto.nickName ?: "게스트"
        )
    }

    /**
     * Character 도메인 모델을 CharacterDto로 변환
     */
    fun toDto(domain: Character): CharacterDto {
        return CharacterDto(
            headImageName = domain.headImageName,
            bodyImageName = domain.bodyImageName,
            feetImageName = domain.feetImageName,
            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = DomainGrade.toApiGrade(domain.grade), // Domain Grade → API Grade 변환
            nickName = domain.nickName
        )
    }
}
