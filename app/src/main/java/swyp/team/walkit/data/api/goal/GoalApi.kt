package swyp.team.walkit.data.api.goal

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import swyp.team.walkit.data.remote.goal.dto.RemoteGoalDto

/**
 * 목표 관련 API
 */
interface GoalApi {
    /**
     * 목표 생성 (POST /goals)
     * 목표를 처음 생성할 때 사용 (온보딩 등)
     *
     * @param goal 목표 데이터
     * @return 생성된 목표 데이터 (응답이 비어있으면 요청한 데이터 반환)
     */
    @POST("/goals")
    suspend fun createGoal(@Body goal: RemoteGoalDto): Response<Unit>

    /**
     * 목표 수정 (PUT /goals)
     * 기존 목표를 수정할 때 사용
     *
     * @param goal 목표 데이터
     * @return 수정된 목표 데이터 (응답이 비어있으면 요청한 데이터 반환)
     */
    @PUT("/goals")
    suspend fun updateGoal(@Body goal: RemoteGoalDto): Response<Unit>

    /**
     * 목표 조회 (GET /goals)
     *
     * @return 현재 설정된 목표 데이터
     */
    @GET("/goals")
    suspend fun getGoal(): RemoteGoalDto
}

