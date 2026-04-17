package swyp.team.walkit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import swyp.team.walkit.data.repository.SpotRepositoryImpl
import swyp.team.walkit.domain.repository.SpotRepository
import javax.inject.Singleton

/**
 * 장소(Spot) 관련 Hilt 모듈
 *
 * [SpotRepository] 인터페이스를 [SpotRepositoryImpl]에 바인딩한다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SpotModule {

    @Binds
    @Singleton
    abstract fun bindSpotRepository(
        impl: SpotRepositoryImpl,
    ): SpotRepository
}