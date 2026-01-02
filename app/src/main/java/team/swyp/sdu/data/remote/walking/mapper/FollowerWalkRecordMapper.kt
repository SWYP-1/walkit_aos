package team.swyp.sdu.data.remote.walking.mapper

import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.Grade

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
            walkProgressPercentage = dto.walkProgressPercentage ?: "0",
            createdDate = dto.createdDate ?: "0",
            stepCount = dto.stepCount,
            totalDistance = dto.totalDistance,
            likeCount = dto.likeCount,
            liked = dto.liked,
            walkId = dto.walkId ?: -1,
            totalTime = dto.totalTime ?: 0L
        )
    }

    /**
     * CharacterDto → Character Domain Model
     */
    private fun toDomain(dto: team.swyp.sdu.data.remote.walking.dto.CharacterDto): Character {
        return Character(
            headImageName = dto.headImage?.imageName,
            bodyImageName = dto.bodyImage?.imageName,
            feetImageName = dto.feetImage?.imageName,
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = Grade.fromApiString(dto.grade), // API String → Domain Grade 변환
            nickName = dto.nickName ?: "게스트",
        )
    }
}

