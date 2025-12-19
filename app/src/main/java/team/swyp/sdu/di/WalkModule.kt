package team.swyp.sdu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import team.swyp.sdu.data.repository.WalkingRepositoryImpl
import team.swyp.sdu.domain.contract.WalkingTrackingContract
import javax.inject.Singleton

/**
 * Walking feature의 의존성 주입 모듈
 *
 * Contract-based architecture를 위한 바인딩을 제공합니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WalkModule {

    /**
     * WalkingTrackingContract를 WalkingRepositoryImpl에 바인딩
     */
    @Binds
    @Singleton
    abstract fun bindWalkingTrackingContract(
        walkingRepositoryImpl: WalkingRepositoryImpl
    ): WalkingTrackingContract
}