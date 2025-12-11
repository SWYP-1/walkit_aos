package team.swyp.sdu.di

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
import javax.inject.Singleton
import team.swyp.sdu.data.local.datastore.AuthDataStore

/**
 * DataStore 관련 DI 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") },
        )

    @Provides
    @Singleton
    fun provideAuthDataStore(
        dataStore: DataStore<Preferences>,
    ): AuthDataStore = AuthDataStore(dataStore)
}
