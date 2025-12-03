package team.swyp.sdu.di

import android.content.Context
import androidx.room.Room
import team.swyp.sdu.data.local.dao.WalkingSessionDao
import team.swyp.sdu.data.local.database.AppDatabase
import team.swyp.sdu.data.repository.WalkingSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database Module
 *
 * Room Database와 Repository를 제공하는 DI 모듈입니다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
            ).fallbackToDestructiveMigration() // 마이그레이션 없이 스키마 변경 시 데이터 삭제 (개발 단계)
            .build()

    @Provides
    @Singleton
    fun provideWalkingSessionDao(database: AppDatabase): WalkingSessionDao = database.walkingSessionDao()

    @Provides
    @Singleton
    fun provideWalkingSessionRepository(walkingSessionDao: WalkingSessionDao): WalkingSessionRepository =
        WalkingSessionRepository(walkingSessionDao)
}
