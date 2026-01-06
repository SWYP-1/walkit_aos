package swyp.team.walkit.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import swyp.team.walkit.domain.calculator.DistanceCalculator
import swyp.team.walkit.domain.estimator.StepEstimator
import swyp.team.walkit.domain.movement.MovementStateStabilizer
import swyp.team.walkit.domain.service.CharacterImageLoader
import swyp.team.walkit.domain.service.ImageDownloader
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.domain.validator.DefaultStepCountValidator
import swyp.team.walkit.domain.validator.StepCountValidator
import swyp.team.walkit.data.remote.image.OkHttpImageDownloader
import javax.inject.Singleton

/**
 * Domain Layer Module
 *
 * Domain 계층의 UseCase 및 Validator를 제공하는 DI 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds
    @Singleton
    abstract fun bindStepCountValidator(
        defaultStepCountValidator: DefaultStepCountValidator,
    ): StepCountValidator

    @Binds
    @Singleton
    abstract fun bindImageDownloader(
        okHttpImageDownloader: OkHttpImageDownloader,
    ): ImageDownloader
}

/**
 * Domain Layer Provider Module
 *
 * Domain 계층의 클래스들을 제공하는 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainProviderModule {
    @Provides
    @Singleton
    fun provideMovementStateStabilizer(): MovementStateStabilizer =
        MovementStateStabilizer(stableDurationMs = 3000L)

    @Provides
    @Singleton
    fun provideStepEstimator(): StepEstimator =
        StepEstimator()

    @Provides
    @Singleton
    fun provideDistanceCalculator(): DistanceCalculator =
        DistanceCalculator()

    @Provides
    @Singleton
    fun provideLottieImageProcessor(imageDownloader: ImageDownloader): LottieImageProcessor =
        LottieImageProcessor(imageDownloader)

    @Provides
    @Singleton
    fun provideCharacterImageLoader(imageDownloader: ImageDownloader): CharacterImageLoader =
        CharacterImageLoader(imageDownloader)
}

