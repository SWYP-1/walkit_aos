package team.swyp.sdu.data.remote.user.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.data.remote.walking.dto.Grade

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
 * - REJECTED: 팔로우 요청이 거절된 경우
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
    val followStatus: String? = null, // EMPTY, PENDING, ACCEPTED, REJECTED, MYSELF 또는 null
) {
    /**
     * followStatus를 FollowStatus enum으로 변환
     * null인 경우 EMPTY을 반환 (로그인하지 않은 경우)
     */
    fun getFollowStatusEnum(): FollowStatus {
        if (followStatus == null) {
            return FollowStatus.EMPTY // 로그인하지 않은 경우
        }
        return try {
            FollowStatus.valueOf(followStatus)
        } catch (e: IllegalArgumentException) {
            // 알 수 없는 값인 경우 기본값 반환
            FollowStatus.EMPTY
        }
    }
}

/**
 * 사용자 요약 정보 DTO
 *
 * 친구 검색 결과에서 사용자 선택 시 상세 정보를 가져올 때 사용됩니다.
 * Kotlinx Serialization을 사용합니다.
 */
@Serializable
data class UserSummaryDto(
    @SerialName("responseCharacterDto")
    val responseCharacterDto: ResponseCharacterDto,
    @SerialName("walkTotalSummaryResponseDto")
    val walkTotalSummaryResponseDto: WalkTotalSummaryResponseDto,
)

/**
 * 캐릭터 응답 DTO
 * 
 * API 응답 구조:
 * {
 *   "headImage": { "imageName": "...", "itemPosition": "HEAD", "itemTag": "TOP" },
 *   "bodyImage": { "imageName": "...", "itemPosition": "BODY", "itemTag": null },
 *   "feetImage": { "imageName": "...", "itemPosition": "FEET", "itemTag": null }
 * }
 */
@Serializable
data class ResponseCharacterDto(
    @SerialName("headImage")
    val headImage: team.swyp.sdu.data.remote.walking.dto.ItemImageDto? = null,
    @SerialName("bodyImage")
    val bodyImage: team.swyp.sdu.data.remote.walking.dto.ItemImageDto? = null,
    @SerialName("feetImage")
    val feetImage: team.swyp.sdu.data.remote.walking.dto.ItemImageDto? = null,
    @SerialName("characterImageName")
    val characterImageName: String,
    @SerialName("backgroundImageName")
    val backgroundImageName: String,
    @SerialName("level")
    val level: Int,
    @SerialName("grade")
    val grade: Grade,
    @SerialName("nickName")
    val nickName: String,
    @SerialName("currentGoalSequence")
    val currentGoalSequence: Int? = null,
)

/**
 * 산책 총 요약 응답 DTO
 */
@Serializable
data class WalkTotalSummaryResponseDto(
    @SerialName("totalWalkCount")
    val totalWalkCount: Int,
    @SerialName("totalWalkTimeMillis")
    val totalWalkTimeMillis: Long,
)

