package team.swyp.sdu.di

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
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
     * FirebaseMessaging 제공
     */
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    /**
     * LocationTrackingService는 @AndroidEntryPoint로 주입되므로
     * 여기서는 제공하지 않습니다.
     * 필요시 Context를 제공할 수 있습니다.
     *
     * LocationManager는 @Inject 생성자를 사용하므로
     * Hilt가 자동으로 주입합니다.
     */
}
