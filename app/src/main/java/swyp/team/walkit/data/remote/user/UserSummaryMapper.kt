package swyp.team.walkit.data.remote.user

import swyp.team.walkit.data.remote.user.dto.UserSummaryDto
import swyp.team.walkit.data.remote.user.dto.ResponseCharacterDto
import swyp.team.walkit.data.remote.user.dto.WalkTotalSummaryResponseDto
import swyp.team.walkit.data.remote.walking.dto.ItemImageDto
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.UserSummary
import swyp.team.walkit.domain.model.WalkSummary

/**
 * UserSummary DTO를 Domain Model로 변환하는 매퍼
 */
object UserSummaryMapper {

    /**
     * UserSummaryDto를 UserSummary Domain Model로 변환
     */
    fun toDomain(dto: UserSummaryDto): UserSummary {
        return UserSummary(
            character = Character(
                headImage = dto.responseCharacterDto.headImage?.toCharacterImage(),
                bodyImage = dto.responseCharacterDto.bodyImage?.toCharacterImage(),
                feetImage = dto.responseCharacterDto.feetImage?.toCharacterImage(),
                characterImageName = dto.responseCharacterDto.characterImageName,
                backgroundImageName = dto.responseCharacterDto.backgroundImageName,
                level = dto.responseCharacterDto.level,
                grade = Grade.fromApiGrade(dto.responseCharacterDto.grade), // API Grade → Domain Grade 변환
                nickName = dto.responseCharacterDto.nickName,
            ),
            walkSummary = WalkSummary(
                totalWalkCount = dto.walkTotalSummaryResponseDto.totalWalkCount,
                totalWalkTimeMillis = dto.walkTotalSummaryResponseDto.totalWalkTimeMillis,
            )
        )
    }

    /**
     * ItemImageDto → CharacterImage 변환
     */
    private fun ItemImageDto.toCharacterImage(): CharacterImage {
        return CharacterImage(
            imageName = imageName,
            itemTag = itemTag
        )
    }

    /**
     * ResponseCharacterDto를 Character Domain Model로 변환
     */
    fun toCharacter(dto: ResponseCharacterDto): Character {
        return Character(
            headImage = dto.headImage?.toCharacterImage(),
            bodyImage = dto.bodyImage?.toCharacterImage(),
            feetImage = dto.feetImage?.toCharacterImage(),
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = Grade.fromApiGrade(dto.grade), // API Grade → Domain Grade 변환
            nickName = dto.nickName,
        )
    }

    /**
     * WalkTotalSummaryResponseDto를 WalkSummary Domain Model로 변환
     */
    fun toWalkSummary(dto: WalkTotalSummaryResponseDto): WalkSummary {
        return WalkSummary(
            totalWalkCount = dto.totalWalkCount,
            totalWalkTimeMillis = dto.totalWalkTimeMillis,
        )
    }
}
