package swyp.team.walkit.domain.model

/**
 * 팔로우 최근 산책 활동 도메인 모델 (최근 산책 순 정렬)
 */
data class FollowerRecentActivity(
    /** 팔로우 사용자 ID */
    val userId: Long,
    /** 닉네임 */
    val nickName: String,
    /** 어제 산책 여부 */
    val walkedYesterday: Boolean,
    /** 캐릭터 등급 */
    val grade: String,
    /** 캐릭터 머리 이미지명 */
    val headImageName: String?,
    /** 캐릭터 몸통 이미지명 */
    val bodyImageName: String?,
    /** 머리 아이템 태그 (headtop / headdecor 슬롯 결정에 사용) */
    val headItemTag: String?,
)
