package swyp.team.walkit.data.remote.walking.mapper

import swyp.team.walkit.data.remote.walking.dto.CharacterDto
import swyp.team.walkit.data.remote.walking.dto.Grade
import swyp.team.walkit.data.remote.walking.dto.ItemImageDto
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.Grade as DomainGrade

/**
 * CharacterDto와 Character 도메인 모델 간 변환 매퍼
 */
object CharacterMapper {

    /**
     * CharacterDto를 Character 도메인 모델로 변환
     */
    fun toDomain(dto: CharacterDto): Character {
        return Character(
            headImage = dto.headImage?.let { CharacterImage(it.imageName, it.itemTag) },
            bodyImage = dto.bodyImage?.let { CharacterImage(it.imageName, it.itemTag) },
            feetImage = dto.feetImage?.let { CharacterImage(it.imageName, it.itemTag) },
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
            headImage = domain.headImage?.let { ItemImageDto(imageName = it.imageName, itemPosition = "HEAD", itemTag = it.itemTag) },
            bodyImage = domain.bodyImage?.let { ItemImageDto(imageName = it.imageName, itemPosition = "BODY", itemTag = it.itemTag) },
            feetImage = domain.feetImage?.let { ItemImageDto(imageName = it.imageName, itemPosition = "FEET", itemTag = it.itemTag) },
            characterImageName = domain.characterImageName,
            backgroundImageName = domain.backgroundImageName,
            level = domain.level,
            grade = domain.grade.name, // Domain Grade → API String 변환
            nickName = domain.nickName,
            currentGoalSequence = null
        )
    }
}
