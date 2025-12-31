package team.swyp.sdu.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * 캐시 기반 + 이벤트 기반 친구 목록 관리
 */
@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val followRemoteDataSource: FollowRemoteDataSource,
) : FriendRepository {

    /**
     * 캐시 데이터
     */
    private data class FriendCache(
        val friends: List<Friend>,
        val lastFetchedAt: Long
    )

    private var cache: FriendCache? = null
    private val cacheTtlMillis = 60_000L // 1분 TTL

    /**
     * 친구 목록 상태 (UI에 노출되는 StateFlow)
     */
    private val _friendsState = MutableStateFlow<Result<List<Friend>>>(Result.Loading)
    override val friendsState: StateFlow<Result<List<Friend>>> = _friendsState.asStateFlow()

    /**
     * 친구 상태 변경 이벤트 (외부 이벤트 수신용 SharedFlow)
     * replay = 0, extraBufferCapacity = 1로 최신 이벤트만 유지
     */
    private val _friendUpdated = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val friendUpdated: SharedFlow<Unit> = _friendUpdated.asSharedFlow()

    /**
     * 친구 목록 조회 (캐시 우선, force = true 시 무조건 서버 재호출)
     */
    override suspend fun loadFriends(force: Boolean): Result<List<Friend>> =
        withContext(Dispatchers.IO) {
            try {
                // 캐시 유효성 검사
                val currentTime = System.currentTimeMillis()
                val isCacheValid = cache != null &&
                    (currentTime - cache!!.lastFetchedAt) < cacheTtlMillis

                // 캐시가 유효하고 force가 false면 캐시 반환
                if (!force && isCacheValid) {
                    Timber.d("친구 목록 캐시 사용 (TTL 내)")
                    return@withContext Result.Success(cache!!.friends)
                }

                // 서버에서 데이터 조회
                Timber.d("친구 목록 서버 조회${if (force) " (강제)" else " (캐시 만료)"}")
                val friends = followRemoteDataSource.getFriends()

                // 캐시 업데이트
                cache = FriendCache(friends, currentTime)

                // StateFlow 업데이트
                _friendsState.value = Result.Success(friends)

                Result.Success(friends)
            } catch (e: Exception) {
                Timber.e(e, "친구 목록 조회 실패")

                // 에러 시에도 StateFlow 업데이트 (UI에 에러 상태 표시)
                _friendsState.value = Result.Error(e, e.message)

                Result.Error(e, e.message)
            }
        }


    /**
     * 사용자 차단
     */
    override suspend fun blockUser(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val result = followRemoteDataSource.blockUser(nickname)
                Result.Success(result)
            } catch (e: Exception) {
                Timber.e(e, "사용자 차단 실패")
                Result.Error(e, e.message)
            }
        }

    /**
     * 캐시 무효화 (수동 갱신용)
     */
    override fun invalidateCache() {
        Timber.d("친구 목록 캐시 무효화")
        cache = null
        // StateFlow를 Loading으로 리셋 (다음 loadFriends 호출 시 재갱신 유도)
        _friendsState.value = Result.Loading
    }

    /**
     * 친구 상태 변경 이벤트 발행 (외부에서 호출)
     */
    override suspend fun emitFriendUpdated() {
        Timber.d("친구 상태 변경 이벤트 발행")
        _friendUpdated.emit(Unit)
    }
}



