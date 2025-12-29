package team.swyp.sdu.data.remote.user

import team.swyp.sdu.data.remote.user.dto.UserSummaryDto
import team.swyp.sdu.data.remote.user.dto.ResponseCharacterDto
import team.swyp.sdu.data.remote.user.dto.WalkTotalSummaryResponseDto
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.domain.model.UserSummary
import team.swyp.sdu.domain.model.WalkSummary

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
                headImageName = dto.responseCharacterDto.headImageName,
                bodyImageName = dto.responseCharacterDto.bodyImageName,
                feetImageName = dto.responseCharacterDto.feetImageName,
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
            headImageName = dto.headImageName,
            bodyImageName = dto.bodyImageName,
            feetImageName = dto.feetImageName,
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
