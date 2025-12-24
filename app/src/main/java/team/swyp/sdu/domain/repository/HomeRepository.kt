package team.swyp.sdu.domain.repository

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.home.HomeRemoteDataSource
import team.swyp.sdu.data.remote.home.mapper.HomeMapper
import team.swyp.sdu.data.remote.home.mapper.HomeMapper.toDomain
import team.swyp.sdu.domain.model.HomeData
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

