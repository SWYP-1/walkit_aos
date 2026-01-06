package swyp.team.walkit.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import swyp.team.walkit.data.remote.mission.MissionRemoteDataSource
import swyp.team.walkit.data.repository.MissionRepositoryImpl
import swyp.team.walkit.domain.repository.MissionRepository

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









