package team.swyp.sdu.data.api.user

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import team.swyp.sdu.data.api.user.UpdateUserProfileRequest
import team.swyp.sdu.data.remote.user.dto.RemoteUserDto
import team.swyp.sdu.data.remote.user.dto.UserSearchResultDto

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

    /**
     * 닉네임 등록
     *
     * @param nickname 등록할 닉네임
     * @return Response로 감싼 응답 (성공/실패 확인 가능)
     */
    @POST("/users/nickname/{nickname}")
    suspend fun registerNickname(
        @Path("nickname") nickname: String
    ): Response<Void>

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

}

