package team.swyp.sdu.domain.repository

import team.swyp.sdu.core.Result
import team.swyp.sdu.domain.model.Friend

/**
 * 친구 정보 Repository 인터페이스
 */
interface FriendRepository {
    /**
     * 친구 목록 조회
     */
    suspend fun getFriends(): Result<List<Friend>>

    /**
     * 사용자 차단
     */
    suspend fun blockUser(nickname: String) : Result<Unit>
}


