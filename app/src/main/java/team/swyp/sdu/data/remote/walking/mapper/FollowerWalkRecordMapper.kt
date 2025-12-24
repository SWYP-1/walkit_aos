package team.swyp.sdu.data.remote.walking.mapper

import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.FollowerWalkRecord

/**
 * FollowerWalkRecord DTO → Domain Model 변환 매퍼
 */
object FollowerWalkRecordMapper {
    /**
     * FollowerWalkRecordDto → FollowerWalkRecord Domain Model
     */
    fun toDomain(dto: FollowerWalkRecordDto): FollowerWalkRecord {
        return FollowerWalkRecord(
            character = toDomain(dto.characterDto),
            walkProgressPercentage = dto.walkProgressPercentage,
            createdDate = dto.createdDate,
            stepCount = dto.stepCount,
            totalDistance = dto.totalDistance,
        )
    }

    /**
     * CharacterDto → Character Domain Model
     */
    private fun toDomain(dto: team.swyp.sdu.data.remote.walking.dto.CharacterDto): Character {
        return Character(
            headImageName = dto.headImageName,
            bodyImageName = dto.bodyImageName,
            feetImageName = dto.feetImageName,
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = dto.grade,
            nickName = dto.nickName,
        )
    }
}

