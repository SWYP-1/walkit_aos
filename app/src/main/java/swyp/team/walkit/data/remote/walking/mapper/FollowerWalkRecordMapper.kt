package swyp.team.walkit.data.remote.walking.mapper

import swyp.team.walkit.data.remote.walking.dto.FollowerWalkRecordDto
import swyp.team.walkit.data.remote.walking.dto.ItemImageDto
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.FollowerWalkRecord
import swyp.team.walkit.domain.model.Grade

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
    private fun toDomain(dto: swyp.team.walkit.data.remote.walking.dto.CharacterDto): Character {
        return Character(
            headImage = dto.headImage?.toCharacterImage(),
            bodyImage = dto.bodyImage?.toCharacterImage(),
            feetImage = dto.feetImage?.toCharacterImage(),
            characterImageName = dto.characterImageName,
            backgroundImageName = dto.backgroundImageName,
            level = dto.level,
            grade = Grade.fromApiString(dto.grade), // API String → Domain Grade 변환
            nickName = dto.nickName ?: "게스트",
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
}

