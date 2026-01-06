package swyp.team.walkit.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import swyp.team.walkit.data.local.dao.GoalDao
import swyp.team.walkit.data.remote.goal.GoalRemoteDataSource
import swyp.team.walkit.data.repository.GoalRepositoryImpl
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.domain.repository.UserRepository

/**
 * 목표 관련 의존성 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object GoalModule {
    @Provides
    @Singleton
    fun provideGoalRepository(
        goalDao: GoalDao,
        goalRemoteDataSource: GoalRemoteDataSource,
        userRepository: UserRepository,
    ): GoalRepository = GoalRepositoryImpl(goalDao, goalRemoteDataSource, userRepository)
}

