package team.swyp.sdu.domain.repository

import retrofit2.Response
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.remote.walking.dto.FollowerWalkRecordDto
import team.swyp.sdu.data.remote.walking.dto.WalkSaveResponse

/**
 * 산책 관련 Repository 인터페이스
 */
interface WalkRepository {
    /**
     * 산책 데이터를 서버에 저장
     *
     * @param session 저장할 산책 세션
     * @param imageUri 산책 이미지 URI (선택사항)
     * @return 서버 응답 (이미지 URL 포함)
     */
    suspend fun saveWalk(
        session: WalkingSession,
        imageUri: String? = null
    ): Result<Response<WalkSaveResponse>>

    /**
     * 팔로워 산책 기록 조회
     *
     * @param nickname 팔로워 닉네임
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     * @return 팔로워 산책 기록 정보
     */
    suspend fun getFollowerWalkRecord(
        nickname: String,
        lat: Double? = null,
        lon: Double? = null,
    ): Result<FollowerWalkRecordDto>

    /**
     * 산책 좋아요 누르기
     *
     * @param walkId 산책 ID
     * @return API 호출 결과
     */
    suspend fun likeWalk(walkId: Long): Result<Unit>

    /**
     * 산책 좋아요 취소
     *
     * @param walkId 산책 ID
     * @return API 호출 결과
     */
    suspend fun unlikeWalk(walkId: Long): Result<Unit>
}


