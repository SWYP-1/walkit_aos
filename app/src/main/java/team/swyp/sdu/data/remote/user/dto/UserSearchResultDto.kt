package team.swyp.sdu.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.FollowStatus

/**
 * 사용자 검색 결과 DTO
 *
 * 닉네임으로 사용자를 검색했을 때 반환되는 결과입니다.
 * Kotlinx Serialization을 사용합니다.
 */
@Serializable
data class UserSearchResultDto(
    @SerialName("userId")
    val userId: Long,
    @SerialName("imageName")
    val imageName: String? = null,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("followStatus")
    val followStatus: String, // PENDING, FOLLOWING, NONE
) {
    /**
     * followStatus를 FollowStatus enum으로 변환
     */
    fun getFollowStatusEnum(): FollowStatus {
        return try {
            FollowStatus.valueOf(followStatus)
        } catch (e: IllegalArgumentException) {
            FollowStatus.NONE
        }
    }
}

