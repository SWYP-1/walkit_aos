package swyp.team.walkit.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.friend.FollowRemoteDataSource
import swyp.team.walkit.domain.model.Friend
import swyp.team.walkit.domain.repository.FriendRepository
import swyp.team.walkit.utils.CrashReportingHelper
import timber.log.Timber
import java.io.IOException
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
    private val cacheTtlMillis = 30_000L // 30초 TTL

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
            } catch (e: IOException) {
                // 네트워크 오류: 복구 가능
                CrashReportingHelper.logNetworkError(e, "loadFriends")
                Timber.e(e, "친구 목록 조회 실패: 네트워크 오류")
                val errorResult = Result.Error(e, "인터넷 연결을 확인해주세요")
                _friendsState.value = errorResult
                errorResult
            } catch (e: HttpException) {
                // HTTP 오류: 복구 가능
                CrashReportingHelper.logHttpError(e, "loadFriends")
                Timber.e(e, "친구 목록 조회 실패: HTTP ${e.code()}")
                val errorMessage = when (e.code()) {
                    in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
                    else -> "친구 목록을 불러올 수 없습니다"
                }
                val errorResult = Result.Error(e, errorMessage)
                _friendsState.value = errorResult
                errorResult
            }
            // NullPointerException, IllegalStateException 등 치명적 오류는 catch하지 않음
            // → 크래시로 이어져서 개발자가 즉시 수정 가능
        }


    /**
     * 사용자 차단
     */
    override suspend fun blockUser(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val result = followRemoteDataSource.blockUser(nickname)
                Result.Success(result)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "blockUser")
                Timber.e(e, "사용자 차단 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "blockUser")
                Timber.e(e, "사용자 차단 실패: HTTP ${e.code()}")
                Result.Error(e, "사용자 차단에 실패했습니다")
            }
        }

    /**
     * 팔로우 요청 수락
     */
    override suspend fun acceptFollowRequest(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                followRemoteDataSource.acceptFollowRequest(nickname)
                Timber.d("팔로우 요청 수락 성공: $nickname")
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "acceptFollowRequest")
                Timber.e(e, "팔로우 요청 수락 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "acceptFollowRequest")
                Timber.e(e, "팔로우 요청 수락 실패: HTTP ${e.code()}")
                Result.Error(e, "팔로우 요청 수락에 실패했습니다")
            }
        }

    /**
     * 팔로우 요청 거절
     */
    override suspend fun rejectFollowRequest(nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                followRemoteDataSource.rejectFollowRequest(nickname)
                Timber.d("팔로우 요청 거절 성공: $nickname")
                Result.Success(Unit)
            } catch (e: IOException) {
                CrashReportingHelper.logNetworkError(e, "rejectFollowRequest")
                Timber.e(e, "팔로우 요청 거절 실패: 네트워크 오류")
                Result.Error(e, "인터넷 연결을 확인해주세요")
            } catch (e: HttpException) {
                CrashReportingHelper.logHttpError(e, "rejectFollowRequest")
                Timber.e(e, "팔로우 요청 거절 실패: HTTP ${e.code()}")
                Result.Error(e, "팔로우 요청 거절에 실패했습니다")
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



