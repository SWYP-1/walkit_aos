package team.swyp.sdu.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Friend

/**
 * 친구 정보 Repository 인터페이스
 * 캐시 기반 + 이벤트 기반 친구 목록 관리
 */
interface FriendRepository {

    /**
     * 친구 목록 조회 (캐시 우선, force = true 시 무조건 서버 재호출)
     */
    suspend fun loadFriends(force: Boolean = false): Result<List<Friend>>

    /**
     * 친구 목록 상태 (캐시된 데이터 노출용 StateFlow)
     */
    val friendsState: kotlinx.coroutines.flow.StateFlow<Result<List<Friend>>>

    /**
     * 친구 상태 변경 이벤트 (외부 이벤트 수신용 SharedFlow)
     * replay = 0, extraBufferCapacity = 1로 설정하여 최신 이벤트만 유지
     */
    val friendUpdated: SharedFlow<Unit>

    /**
     * 사용자 차단
     */
    suspend fun blockUser(nickname: String): Result<Unit>

    /**
     * 캐시 무효화 (수동 갱신용)
     */
    fun invalidateCache()

    /**
     * 친구 상태 변경 이벤트 발행 (외부에서 호출)
     */
    suspend fun emitFriendUpdated()
}



