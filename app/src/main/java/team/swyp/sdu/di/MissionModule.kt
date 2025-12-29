package team.swyp.sdu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import team.swyp.sdu.data.remote.mission.MissionRemoteDataSource
import team.swyp.sdu.data.repository.MissionRepositoryImpl
import team.swyp.sdu.domain.repository.MissionRepository

/**
 * 미션 관련 의존성 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object MissionModule {
    @Provides
    @Singleton
    fun provideMissionRepository(
        missionRemoteDataSource: MissionRemoteDataSource,
    ): MissionRepository = MissionRepositoryImpl(missionRemoteDataSource)
}








