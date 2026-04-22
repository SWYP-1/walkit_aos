package swyp.team.walkit.data.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.core.map
import swyp.team.walkit.data.remote.follower.FollowerMapRemoteDataSource
import swyp.team.walkit.data.remote.follower.dto.FollowerLatestWalkRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerMapRecordDto
import swyp.team.walkit.data.remote.follower.dto.FollowerRecentActivityDto
import swyp.team.walkit.domain.model.FollowerLatestWalkRecord
import swyp.team.walkit.domain.model.FollowerMapRecord
import swyp.team.walkit.domain.model.FollowerRecentActivity
import swyp.team.walkit.domain.model.WalkPoint
import swyp.team.walkit.domain.repository.FollowerMapRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FollowerMapRepository] 구현체
 *
 * 원격 데이터 소스에서 DTO를 받아 도메인 모델로 변환한다.
 */
@Singleton
class FollowerMapRepositoryImpl @Inject constructor(
    private val remoteDataSource: FollowerMapRemoteDataSource,
) : FollowerMapRepository {

    override suspend fun getFollowerWalkingRecords(
        lat: Double,
        lon: Double,
        radius: Int,
    ): Result<List<FollowerMapRecord>> =
        remoteDataSource.getFollowerWalkingRecords(lat, lon, radius)
            .map { dtoList -> dtoList.map { it.toDomain() } }

    override suspend fun getFollowerRecentActivities(): Result<List<FollowerRecentActivity>> =
        remoteDataSource.getFollowerRecentActivities()
            .map { dtoList -> dtoList.map { it.toDomain() } }

    override suspend fun getFollowerLatestWalkRecord(userId: Long): Result<FollowerLatestWalkRecord> =
        remoteDataSource.getFollowerLatestWalkRecord(userId)
            .map { it.toDomain() }

    // ── DTO → 도메인 변환 ──────────────────────────────────────────────────────

    private fun FollowerMapRecordDto.toDomain(): FollowerMapRecord = FollowerMapRecord(
        userId = userId,
        walkId = walkId,
        latitude = latitude,
        longitude = longitude,
        grade = responseCharacterDto.grade,
        headImageName = responseCharacterDto.headImageName,
        bodyImageName = responseCharacterDto.bodyImageName,
    )

    private fun FollowerRecentActivityDto.toDomain(): FollowerRecentActivity = FollowerRecentActivity(
        userId = userId,
        nickName = nickName,
        walkedYesterday = walkedYesterday,
        grade = responseCharacterDto.grade,
        headImageName = headImage?.imageName ?: responseCharacterDto.headImageName,
        bodyImageName = bodyImage?.imageName ?: responseCharacterDto.bodyImageName,
        headItemTag = headImage?.itemTag,
    )

    private fun FollowerLatestWalkRecordDto.toDomain(): FollowerLatestWalkRecord =
        FollowerLatestWalkRecord(
            level = level,
            grade = grade,
            nickName = nickName,
            createdDate = responseWalkRecordDto.createdDate,
            imageUrl = responseWalkRecordDto.imageUrl,
            points = responseWalkRecordDto.points.map { point ->
                WalkPoint(
                    latitude = point.latitude,
                    longitude = point.longitude,
                    timestampMillis = point.timestampMillis,
                )
            },
            totalTime = responseWalkRecordDto.totalTime,
            stepCount = responseWalkRecordDto.stepCount,
            likeCount = likeCount,
            liked = liked,
            walkId = responseWalkRecordDto.walkId,
        )
}
