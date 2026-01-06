package swyp.team.walkit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import swyp.team.walkit.data.local.datastore.AuthDataStore
import swyp.team.walkit.data.local.datastore.LocationAgreementDataStore
import swyp.team.walkit.data.local.datastore.NotificationDataStore
import swyp.team.walkit.data.local.datastore.OnboardingDataStore

/**
 * DataStore 관련 DI 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") },
        )

    @Provides
    @Singleton
    @Named("onboarding")
    fun provideOnboardingPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("onboarding_prefs") },
        )

    @Provides
    @Singleton
    @Named("notification")
    fun provideNotificationPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("notification_prefs") },
        )

    @Provides
    @Singleton
    @Named("fcm")
    fun provideFcmPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("fcm_prefs") },
        )

    @Provides
    @Singleton
    @Named("location_agreement")
    fun provideLocationAgreementPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("location_agreement_prefs") },
        )
}


