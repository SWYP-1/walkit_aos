package team.swyp.sdu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.friend.FollowRemoteDataSource
import team.swyp.sdu.domain.model.Friend
import team.swyp.sdu.domain.repository.FriendRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 정보 Repository 구현체
 */
@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val followRemoteDataSource: FollowRemoteDataSource,
) : FriendRepository {

    override suspend fun getFriends(): Result<List<Friend>> =
        withContext(Dispatchers.IO) {
            try {
                val friends = followRemoteDataSource.getFriends()
                Result.Success(friends)
            } catch (e: Exception) {
                Timber.e(e, "친구 목록 조회 실패")
                Result.Error(e, e.message)
            }
        }

    override suspend fun blockUser(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val friends = followRemoteDataSource.blockUser(nickname)
                Result.Success(friends)
            } catch (e: Exception) {
                Timber.e(e, "사용자 차단 실패")
                Result.Error(e, e.message)
            }
        }
}



