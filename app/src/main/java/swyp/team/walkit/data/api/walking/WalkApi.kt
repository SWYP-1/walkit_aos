package swyp.team.walkit.data.api.walking

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import swyp.team.walkit.data.remote.walking.dto.UpdateWalkNoteRequest
import swyp.team.walkit.data.remote.walking.dto.WalkSaveResponse

/**
 * 산책 관련 API
 */
interface WalkApi {

    /**
     * 산책 데이터 저장 (이미지 포함)
     *
     * @param data 산책 데이터 JSON
     * @param image 산책 이미지 (선택사항)
     * @return 저장 결과 (이미지 URL 포함)
     */
    @Multipart
    @POST("/walk/save")
    suspend fun saveWalk(
        @retrofit2.http.Part("data") data: RequestBody,
        @retrofit2.http.Part image: MultipartBody.Part?
    ): WalkSaveResponse

    /**
     * 산책 좋아요 누르기
     *
     * @param walkId 산책 ID
     * @return 응답 (성공 시 빈 응답)
     */
    @POST("/walk-likes/{walkId}")
    suspend fun likeWalk(
        @Path("walkId") walkId: Long
    ): Response<Unit>

    /**
     * 산책 좋아요 취소
     *
     * @param walkId 산책 ID
     * @return 응답 (성공 시 빈 응답)
     */
    @DELETE("/walk-likes/{walkId}")
    suspend fun unlikeWalk(
        @Path("walkId") walkId: Long
    ): Response<Unit>

    /**
     * 산책 노트 업데이트
     *
     * @param walkId 산책 ID
     * @param request 노트 업데이트 요청
     * @return 성공 시 빈 응답 (200 OK)
     */
    @PATCH("/walk/update/{walkId}")
    suspend fun updateWalkNote(
        @Path("walkId") walkId: Long,
        @Body request: UpdateWalkNoteRequest
    ): Response<Unit>
}
