package team.swyp.sdu.data.remote.home.mapper

import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.remote.home.dto.HomeData
import team.swyp.sdu.data.remote.home.dto.PointDto
import team.swyp.sdu.data.remote.home.dto.WalkResponseDto
import team.swyp.sdu.data.remote.home.dto.WeatherDto
import team.swyp.sdu.data.remote.home.dto.WeeklyMissionDto
import team.swyp.sdu.data.remote.walking.dto.CharacterDto
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.HomeData as DomainHomeData
import team.swyp.sdu.domain.model.WalkRecord
import team.swyp.sdu.domain.model.Weather
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
            weather = weatherDto.toDomain(),
            weeklyMission = weeklyMissionDto?.toDomain(),
            walkRecords = walkResponseDto.map { it.toDomain() },
        )
    }

    /**
     * CharacterDto → Character Domain Model
     */
    private fun CharacterDto.toDomain(): Character {
        return Character(
            headImageName = headImageName,
            bodyImageName = bodyImageName,
            feetImageName = feetImageName,
            characterImageName = characterImageName,
            backgroundImageName = backgroundImageName,
            level = level,
            grade = grade,
            nickName = nickName,
        )
    }

    /**
     * WeatherDto → Weather Domain Model
     */
    private fun WeatherDto.toDomain(): Weather {
        return Weather(
            nx = nx,
            ny = ny,
            generatedAt = generatedAt,
            tempC = tempC,
            rain1hMm = rain1hMm,
            precipType = precipType,
            sky = sky,
        )
    }

    /**
     * WeeklyMissionDto → WeeklyMission Domain Model
     */
    private fun WeeklyMissionDto.toDomain(): WeeklyMission {
        return WeeklyMission(
            userWeeklyMissionId = userWeeklyMissionId,
            missionId = missionId,
            title = title,
            description = description,
            category = category,
            type = type,
            status = status,
            rewardPoints = rewardPoints,
            assignedConfigJson = assignedConfigJson,
            weekStart = weekStart,
            weekEnd = weekEnd,
            completedAt = completedAt,
            failedAt = failedAt,
        )
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



