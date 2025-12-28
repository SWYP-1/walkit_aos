package team.swyp.sdu.domain.repository

import team.swyp.sdu.core.Result

/**
 * 포인트 정보 관련 Repository 인터페이스
 */
interface PointRepository {

    /**
     * 유저 포인트 조회
     *
     * @return 유저의 포인트 값 (Result로 감싸서 반환)
     */
    suspend fun getUserPoint(): Result<Int>
}
