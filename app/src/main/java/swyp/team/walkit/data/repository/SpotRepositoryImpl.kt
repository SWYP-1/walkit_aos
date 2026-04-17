package swyp.team.walkit.data.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.core.map
import swyp.team.walkit.data.remote.spot.SpotRemoteDataSource
import swyp.team.walkit.data.remote.spot.dto.NearbySpotDto
import swyp.team.walkit.domain.model.NearbySpot
import swyp.team.walkit.domain.repository.SpotRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SpotRepository] 구현체
 *
 * 원격 데이터 소스에서 DTO를 받아 도메인 모델로 변환한다.
 */
@Singleton
class SpotRepositoryImpl @Inject constructor(
    private val remoteDataSource: SpotRemoteDataSource,
) : SpotRepository {

    override suspend fun getNearbySpots(
        query: String,
        x: Double,
        y: Double,
        radius: Int,
        size: Int,
        sort: String,
    ): Result<List<NearbySpot>> =
        remoteDataSource.getNearbySpots(
            query = query,
            x = x,
            y = y,
            radius = radius,
            size = size,
            sort = sort,
        ).map { dtoList -> dtoList.mapNotNull { it.toDomain() } }

    /** DTO → 도메인 모델 변환. 필수 좌표값이 없으면 null 반환하여 필터링한다. */
    private fun NearbySpotDto.toDomain(): NearbySpot? {
        val lon = x?.toDoubleOrNull() ?: return null
        val lat = y?.toDoubleOrNull() ?: return null
        return NearbySpot(
            placeName = placeName.orEmpty(),
            addressName = addressName.orEmpty(),
            roadAddressName = roadAddressName.orEmpty(),
            distance = distance.orEmpty(),
            placeUrl = placeUrl.orEmpty(),
            blogReviewCount = blogReviewCount ?: 0,
            blogReviewLink = blogReviewLink.orEmpty(),
            thumbnailUrl = thumbnailUrl.orEmpty(),
            longitude = lon,
            latitude = lat,
        )
    }
}