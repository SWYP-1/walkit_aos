package team.swyp.sdu.data.remote.home.mapper

import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.remote.home.dto.HomeData
import team.swyp.sdu.data.remote.home.dto.PointDto
import team.swyp.sdu.data.remote.home.dto.WalkResponseDto
import team.swyp.sdu.data.remote.mission.dto.mission.WeeklyMissionDto
import team.swyp.sdu.data.remote.mission.mapper.WeeklyMissionMapper
import team.swyp.sdu.data.remote.walking.dto.CharacterDto
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.domain.model.HomeData as DomainHomeData
import team.swyp.sdu.domain.model.WalkRecord
import team.swyp.sdu.domain.model.WeeklyMission

/**
 * 홈 데이터 DTO → Domain Model 변환 매퍼
 */
object HomeMapper {
    /**
     * HomeData DTO → Domain Model
     */
    fun HomeData.toDomain(): DomainHomeData {
        return DomainHomeData(
            character = characterDto.toDomain(),
            walkProgressPercentage = walkProgressPercentage,
            todaySteps = todaySteps,
            temperature = temperature,
            weather = weather,
            weeklyMission = weeklyMissionDto?.toDomain(),
            walkRecords = walkResponseDto.map { it.toDomain() },
        )
    }

    /**
     * CharacterDto → Character Domain Model
     */
    private fun CharacterDto.toDomain(): Character {
        return Character(
            headImageName = headImage?.imageName,
            bodyImageName = bodyImage?.imageName,
            feetImageName = feetImage?.imageName,
            characterImageName = characterImageName,
            backgroundImageName = backgroundImageName,
            level = level,
            grade = Grade.fromApiString(grade), // API String → Domain Grade 변환
            nickName = nickName ?: "게스트",
        )
    }


    /**
     * WeeklyMissionDto → WeeklyMission Domain Model
     */
    private fun WeeklyMissionDto.toDomain(): WeeklyMission {
        return WeeklyMissionMapper.toDomain(this)
    }

    /**
     * WalkResponseDto → WalkRecord Domain Model
     */
    private fun WalkResponseDto.toDomain(): WalkRecord {
        return WalkRecord(
            id = id,
            preWalkEmotion = preWalkEmotion,
            postWalkEmotion = postWalkEmotion,
            note = note,
            imageUrl = imageUrl,
            startTime = startTime,
            endTime = endTime,
            totalTime = totalTime,
            stepCount = stepCount,
            totalDistance = totalDistance,
            createdDate = createdDate,
            points = points.map { it.toLocationPoint() },
        )
    }

    /**
     * PointDto → LocationPoint 변환
     */
    private fun PointDto.toLocationPoint(): LocationPoint {
        return LocationPoint(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestampMillis,
        )
    }
}






