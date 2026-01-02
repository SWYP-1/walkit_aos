package team.swyp.sdu.data.remote.walking.mapper

import team.swyp.sdu.data.remote.walking.dto.CharacterDto
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.data.remote.walking.dto.ItemImageDto
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
            headImageName = dto.headImage?.imageName,
            bodyImageName = dto.bodyImage?.imageName,
            feetImageName = dto.feetImage?.imageName,
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = DomainGrade.fromApiString(dto.grade), // API String → Domain Grade 변환
            nickName = dto.nickName ?: "게스트"
        )
    }

    /**
     * Character 도메인 모델을 CharacterDto로 변환
     */
    fun toDto(domain: Character): CharacterDto {
        return CharacterDto(
            headImage = domain.headImageName?.let { ItemImageDto(imageName = it, itemPosition = "HEAD", itemTag = null) },
            bodyImage = domain.bodyImageName?.let { ItemImageDto(imageName = it, itemPosition = "BODY", itemTag = null) },
            feetImage = domain.feetImageName?.let { ItemImageDto(imageName = it, itemPosition = "FEET", itemTag = null) },
            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = domain.grade.name, // Domain Grade → API String 변환
            nickName = domain.nickName,
            currentGoalSequence = null
        )
    }
}
