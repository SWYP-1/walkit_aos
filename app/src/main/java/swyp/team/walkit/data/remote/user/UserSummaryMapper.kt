package swyp.team.walkit.data.remote.user

import swyp.team.walkit.data.remote.user.dto.UserSummaryDto
import swyp.team.walkit.data.remote.user.dto.ResponseCharacterDto
import swyp.team.walkit.data.remote.user.dto.WalkTotalSummaryResponseDto
import swyp.team.walkit.domain.model.Character
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
                headImageName = dto.responseCharacterDto.headImage?.imageName,
                headImageTag = dto.responseCharacterDto.headImage?.itemTag, // ✅ headImageTag 매핑 추가
                bodyImageName = dto.responseCharacterDto.bodyImage?.imageName,
                feetImageName = dto.responseCharacterDto.feetImage?.imageName,
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
     * ResponseCharacterDto를 Character Domain Model로 변환
     */
    fun toCharacter(dto: ResponseCharacterDto): Character {
        return Character(
            headImageName = dto.headImage?.imageName,
            headImageTag = dto.headImage?.itemTag, // ✅ headImageTag 매핑 추가
            bodyImageName = dto.bodyImage?.imageName,
            feetImageName = dto.feetImage?.imageName,
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
