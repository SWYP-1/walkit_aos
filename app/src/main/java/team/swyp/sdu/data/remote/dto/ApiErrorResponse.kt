package team.swyp.sdu.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 공용 API 에러 응답 DTO
 * 모든 API 엔드포인트의 에러 응답에 사용
 */
@Serializable
data class ApiErrorResponse(
    @SerialName("code")
    val code: Int?,       // 서버 에러 코드 (HTTP 상태 코드와 다름)

    @SerialName("message")
    val message: String?, // 서버 메시지

    @SerialName("name")
    val name: String?,    // 에러 이름

    @SerialName("errors")
    val errors: String?      // 상세 오류 정보 (없으면 null)
)




