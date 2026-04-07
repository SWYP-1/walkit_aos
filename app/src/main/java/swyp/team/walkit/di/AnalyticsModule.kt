package swyp.team.walkit.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import swyp.team.walkit.analytics.AnalyticsTracker
import swyp.team.walkit.analytics.FirebaseAnalyticsTracker
import javax.inject.Singleton

/**
 * Firebase Analytics 및 AnalyticsTracker DI 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideAnalyticsTracker(firebaseAnalytics: FirebaseAnalytics): AnalyticsTracker =
        FirebaseAnalyticsTracker(firebaseAnalytics)
}
