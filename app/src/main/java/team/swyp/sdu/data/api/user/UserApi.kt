package team.swyp.sdu.data.api.user

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import team.swyp.sdu.data.api.user.TermsAgreementRequest
import team.swyp.sdu.data.api.user.UpdateUserProfileRequest
import team.swyp.sdu.data.remote.home.dto.UserPointDto
import team.swyp.sdu.data.remote.user.dto.RemoteUserDto
import team.swyp.sdu.data.remote.user.dto.UserSearchResultDto
import team.swyp.sdu.data.remote.user.dto.UserSummaryDto

/**
 * 사용자 정보 API
 */
interface UserApi {
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GET("/users")
    suspend fun getUser(): RemoteUserDto

    /**
     * 닉네임으로 사용자 검색
     *
     * @param nickname 검색할 닉네임
     * @return 검색 결과 (사용자 정보 및 친구 요청 상태)
     */
    @GET("/users/nickname")
    suspend fun searchByNickname(
        @Query("nickname") nickname: String
    ): UserSearchResultDto

//    /**
//     * 닉네임 중복 체크
//     *
//     * @param nickname 체크할 닉네임
//     * @return Response로 감싼 응답 (성공=중복아님, 실패=중복)
//     */
//    @GET("/users/nickname/{nickname}")
//    suspend fun checkNicknameDuplicate(
//        @Path("nickname") nickname: String
//    ): Response<Void>

    /**
     * 닉네임 등록
     *
     * @param nickname 등록할 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/users/nickname/{nickname}")
    suspend fun registerNickname(
        @Path("nickname") nickname: String
    ): Response<Unit>

    /**
     * 생년월일 업데이트
     *
     * @param birthDate 생년월일 (ISO 8601 형식: "2015-12-04")
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/users/birth-date/{birthDate}")
    suspend fun updateBirthDate(
        @Path("birthDate") birthDate: String
    ): Response<Unit>

    /**
     * 사용자 정보 등록/업데이트 (온보딩 완료)
     *
     * @param body 사용자 정보 DTO (JSON)
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @PUT("/users")
    suspend fun updateUserProfile(
        @Body body: UpdateUserProfileRequest
    ): Response<Unit>

    @PUT("/users/image")
    @Multipart
    suspend fun updateUserProfileImage(
        @Part image : MultipartBody.Part?
    ) : Response<Unit>

    /**
     * 프로필 이미지 삭제
     *
     * @param imageId 삭제할 이미지 ID
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @DELETE("/users/delete")
    suspend fun deleteImage(
    ): Response<Unit>

    /**
     * 약관 동의
     *
     * @param body 약관 동의 정보 DTO (JSON)
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/users/policy")
    suspend fun agreeToTerms(
        @Body body: TermsAgreementRequest
    ): Response<Unit>

    /**
     * 유저 포인트 조회
     *
     * @return 유저 포인트 정보
     */
    @GET("/users/point")
    suspend fun getUserPoint(): UserPointDto

    /**
     * 닉네임으로 사용자 요약 정보 조회
     *
     * 친구 검색 결과에서 특정 사용자를 선택했을 때 상세 정보를 가져옵니다.
     *
     * @param nickname 검색할 닉네임
     * @param lat 위도
     * @param lon 경도
     * @return 사용자 요약 정보 (캐릭터 정보 + 산책 요약 정보)
     */
    @GET("/users/summary/nickname")
    suspend fun getUserSummaryByNickname(
        @Query("nickname") nickname: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): UserSummaryDto

    /**
     * 사용자 탈퇴
     *
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @DELETE("/users")
    suspend fun deleteUser(): Response<Unit>
}

