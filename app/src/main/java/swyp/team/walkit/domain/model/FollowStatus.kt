package swyp.team.walkit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 친구 요청 상태
 */
@Serializable
enum class FollowStatus {
    @SerialName("EMPTY")
    EMPTY, // 팔로우 신청하지 않은 경우

    @SerialName("PENDING")
    PENDING, // 팔로우 신청까지만 한 경우 (요청 대기 중)

    @SerialName("ACCEPTED")
    ACCEPTED, // 팔로우된 경우 (친구 관계)

    @SerialName("REJECTED")
    REJECTED, // 팔로우 요청이 거절된 경우

    @SerialName("MYSELF")
    MYSELF, // 자기 자신을 검색한 경우
}



