package swyp.team.walkit.di

import android.content.Context
import androidx.room.Room
import swyp.team.walkit.data.local.dao.AppliedItemDao
import swyp.team.walkit.data.local.dao.CharacterDao
import swyp.team.walkit.data.local.dao.GoalDao
import swyp.team.walkit.data.local.dao.MissionProgressDao
import swyp.team.walkit.data.local.dao.NotificationSettingsDao
import swyp.team.walkit.data.local.dao.PurchasedItemDao
import swyp.team.walkit.data.local.dao.UserDao
import swyp.team.walkit.data.local.dao.WalkingSessionDao
import swyp.team.walkit.data.local.database.AppDatabase
import swyp.team.walkit.data.repository.CharacterRepositoryImpl
import swyp.team.walkit.data.repository.CosmeticItemRepositoryImpl
import swyp.team.walkit.data.repository.FriendRepositoryImpl
import swyp.team.walkit.data.repository.MissionProgressRepositoryImpl
import swyp.team.walkit.data.repository.WalkRepositoryImpl
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.data.remote.auth.CharacterRemoteDataSource
import swyp.team.walkit.data.remote.friend.FollowRemoteDataSource
import swyp.team.walkit.data.remote.walking.WalkRemoteDataSource
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.CosmeticItemRepository
import swyp.team.walkit.domain.repository.FriendRepository
import swyp.team.walkit.domain.repository.MissionProgressRepository
import swyp.team.walkit.domain.repository.WalkRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import swyp.team.walkit.data.remote.cosmetic.CosmeticItemRemoteDataSource
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
            ).fallbackToDestructiveMigration() // TODO : 반드시 배포전에 지워!!! 마이그레이션 없이 스키마 변경 시 데이터 삭제 (개발 단계)
            .build()

    @Provides
    @Singleton
    fun provideWalkingSessionDao(database: AppDatabase): WalkingSessionDao = database.walkingSessionDao()

    @Provides
    @Singleton
    fun provideWalkingSessionRepository(
        walkingSessionDao: WalkingSessionDao,
        walkRemoteDataSource: WalkRemoteDataSource,
        userDao: UserDao,
        @ApplicationContext context: Context,
    ): WalkingSessionRepository =
        WalkingSessionRepository(walkingSessionDao, walkRemoteDataSource, userDao, context)

    @Provides
    @Singleton
    fun providePurchasedItemDao(database: AppDatabase): PurchasedItemDao = database.purchasedItemDao()

    @Provides
    @Singleton
    fun provideAppliedItemDao(database: AppDatabase): AppliedItemDao = database.appliedItemDao()

    @Provides
    @Singleton
    fun provideCosmeticItemRepository(
        cosmeticItemRemoteDataSource: CosmeticItemRemoteDataSource
    ): CosmeticItemRepository =
        CosmeticItemRepositoryImpl(cosmeticItemRemoteDataSource = cosmeticItemRemoteDataSource)

    @Provides
    @Singleton
    fun provideMissionProgressDao(database: AppDatabase): MissionProgressDao = database.missionProgressDao()

    @Provides
    @Singleton
    fun provideMissionProgressRepository(
        missionProgressDao: MissionProgressDao,
    ): MissionProgressRepository = MissionProgressRepositoryImpl(missionProgressDao)

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideCharacterDao(database: AppDatabase): CharacterDao = database.characterDao()

    @Provides
    @Singleton
    fun provideCharacterRepository(
        characterDao: CharacterDao,
        characterRemoteDataSource: CharacterRemoteDataSource,
    ): CharacterRepository = CharacterRepositoryImpl(characterDao, characterRemoteDataSource)

    @Provides
    @Singleton
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    @Singleton
    fun provideNotificationSettingsDao(database: AppDatabase): NotificationSettingsDao = database.notificationSettingsDao()

    @Provides
    @Singleton
    fun provideFriendRepository(
        followRemoteDataSource: FollowRemoteDataSource,
    ): FriendRepository = FriendRepositoryImpl(followRemoteDataSource)

    @Provides
    @Singleton
    fun provideWalkRepository(
        walkRemoteDataSource: WalkRemoteDataSource,
    ): WalkRepository = WalkRepositoryImpl(walkRemoteDataSource)
}
