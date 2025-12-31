package team.swyp.sdu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import team.swyp.sdu.data.local.dao.UserDao
import team.swyp.sdu.data.local.datastore.AuthDataStore
import team.swyp.sdu.data.local.datastore.LocationAgreementDataStore
import team.swyp.sdu.data.remote.user.UserManagementRemoteDataSource
import team.swyp.sdu.data.remote.user.UserProfileRemoteDataSource
import team.swyp.sdu.data.remote.user.UserRemoteDataSource
import team.swyp.sdu.data.repository.PointRepositoryImpl
import team.swyp.sdu.data.repository.UserRepositoryImpl
import team.swyp.sdu.domain.repository.PointRepository
import team.swyp.sdu.domain.repository.UserRepository

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









