package team.swyp.sdu.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.FollowStatus

/**
 * 사용자 검색 결과 DTO
 *
 * 닉네임으로 사용자를 검색했을 때 반환되는 결과입니다.
 * Kotlinx Serialization을 사용합니다.
 * 
 * followStatus 값:
 * - null: 로그인하지 않은 경우
 * - EMPTY: 팔로우 신청하지 않은 경우
 * - PENDING: 팔로우 신청까지만 한 경우
 * - ACCEPTED: 팔로우된 경우
 * - MYSELF: 자기 자신을 검색한 경우
 */
@Serializable
data class UserSearchResultDto(
    @SerialName("userId")
    val userId: Long,
    @SerialName("imageName")
    val imageName: String? = null,
    @SerialName("nickName")
    val nickName: String,
    @SerialName("followStatus")
    val followStatus: String? = null, // EMPTY, PENDING, ACCEPTED, MYSELF 또는 null
) {
    /**
     * followStatus를 FollowStatus enum으로 변환
     * null인 경우 NONE을 반환 (로그인하지 않은 경우)
     */
    fun getFollowStatusEnum(): FollowStatus {
        if (followStatus == null) {
            return FollowStatus.NONE // 로그인하지 않은 경우
        }
        return try {
            FollowStatus.valueOf(followStatus)
        } catch (e: IllegalArgumentException) {
            // 알 수 없는 값인 경우 기본값 반환
            FollowStatus.NONE
        }
    }
}

