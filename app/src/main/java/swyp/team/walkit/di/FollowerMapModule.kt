package swyp.team.walkit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import swyp.team.walkit.data.repository.FollowerMapRepositoryImpl
import swyp.team.walkit.domain.repository.FollowerMapRepository
import javax.inject.Singleton

/**
 * 지도용 팔로워 산책 기록 Hilt 모듈
 *
 * [FollowerMapRepository] 인터페이스를 [FollowerMapRepositoryImpl]에 바인딩한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FollowerMapModule {

    @Binds
    @Singleton
    abstract fun bindFollowerMapRepository(
        impl: FollowerMapRepositoryImpl,
    ): FollowerMapRepository
}
