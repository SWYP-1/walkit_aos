package team.swyp.sdu.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import team.swyp.sdu.domain.calculator.DistanceCalculator
import team.swyp.sdu.domain.estimator.StepEstimator
import team.swyp.sdu.domain.movement.MovementStateStabilizer
import team.swyp.sdu.domain.service.CharacterImageLoader
import team.swyp.sdu.domain.service.ImageDownloader
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.domain.validator.DefaultStepCountValidator
import team.swyp.sdu.domain.validator.StepCountValidator
import team.swyp.sdu.data.remote.image.OkHttpImageDownloader
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

