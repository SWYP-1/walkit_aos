package swyp.team.walkit.domain.repository

import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.home.HomeRemoteDataSource
import swyp.team.walkit.data.remote.home.mapper.HomeMapper
import swyp.team.walkit.data.remote.home.mapper.HomeMapper.toDomain
import swyp.team.walkit.domain.model.HomeData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val homeRemoteDataSource: HomeRemoteDataSource,
) {
    suspend fun getHomeData(lat: Double, lon: Double): Result<HomeData> {
        return when (val result = homeRemoteDataSource.getHomeData(lat, lon)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> Result.Error(result.exception, result.message)
            Result.Loading -> Result.Loading
        }
    }
}

