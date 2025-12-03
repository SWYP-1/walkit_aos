package team.swyp.sdu.di

import android.content.Context
import team.swyp.sdu.domain.service.ActivityRecognitionManager
import team.swyp.sdu.domain.service.LocationTrackingService
import team.swyp.sdu.domain.service.StepCounterManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Service 관련 의존성 제공 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    /**
     * StepCounterManager 제공
     */
    @Provides
    @Singleton
    fun provideStepCounterManager(
        @ApplicationContext context: Context,
    ): StepCounterManager = StepCounterManager(context)

    /**
     * ActivityRecognitionManager 제공
     */
    @Provides
    @Singleton
    fun provideActivityRecognitionManager(
        @ApplicationContext context: Context,
    ): ActivityRecognitionManager = ActivityRecognitionManager(context)

    /**
     * LocationTrackingService는 @AndroidEntryPoint로 주입되므로
     * 여기서는 제공하지 않습니다.
     * 필요시 Context를 제공할 수 있습니다.
     */
}
