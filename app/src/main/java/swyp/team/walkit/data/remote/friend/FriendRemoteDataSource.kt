package swyp.team.walkit.data.remote.friend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import swyp.team.walkit.data.api.follower.FollowerApi
import swyp.team.walkit.domain.model.Friend
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 목록 원격 데이터 소스
 */
@Singleton
class FriendRemoteDataSource @Inject constructor(
    private val followerApi: FollowerApi
) {
    /**
     * 친구 목록 조회
     */
    suspend fun getFriends(): List<Friend> = withContext(Dispatchers.IO) {
        try {
            val response = followerApi.getFriends()
            response.map { dto ->
                Friend(
                    id = dto.userId.toString(),
                    nickname = dto.nickname ?: "게스트",
                    avatarUrl = dto.userImageUrl
                )
            }
        } catch (t: Throwable) {
            Timber.e(t, "친구 목록 조회 실패")
            throw t
        }
    }
}
