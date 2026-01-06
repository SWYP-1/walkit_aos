package swyp.team.walkit.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import swyp.team.walkit.data.local.dao.UserDao
import swyp.team.walkit.data.local.datastore.AuthDataStore
import swyp.team.walkit.data.local.datastore.LocationAgreementDataStore
import swyp.team.walkit.data.remote.user.UserManagementRemoteDataSource
import swyp.team.walkit.data.remote.user.UserProfileRemoteDataSource
import swyp.team.walkit.data.remote.user.UserRemoteDataSource
import swyp.team.walkit.data.repository.PointRepositoryImpl
import swyp.team.walkit.data.repository.UserRepositoryImpl
import swyp.team.walkit.domain.repository.PointRepository
import swyp.team.walkit.domain.repository.UserRepository

/**
 * 사용자 관련 의존성 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        userRemoteDataSource: UserRemoteDataSource,
        userManagementRemoteDataSource: UserManagementRemoteDataSource,
        userProfileRemoteDataSource: UserProfileRemoteDataSource,
        authDataStore: AuthDataStore,
    ): UserRepository = UserRepositoryImpl(userDao, userRemoteDataSource, userManagementRemoteDataSource, userProfileRemoteDataSource, authDataStore)

    @Provides
    @Singleton
    fun providePointRepository(
        userRemoteDataSource: UserRemoteDataSource,
    ): PointRepository = PointRepositoryImpl(userRemoteDataSource)
}









