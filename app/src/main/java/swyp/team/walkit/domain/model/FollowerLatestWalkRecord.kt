package swyp.team.walkit.domain.model

/**
 * 팔로워 최근 산책 기록 상세 도메인 모델
 */
data class FollowerLatestWalkRecord(
    /** 캐릭터 레벨 */
    val level: Int,
    /** 캐릭터 등급 */
    val grade: String,
    /** 닉네임 */
    val nickName: String,
    /** 산책 기록 생성 일시 (ISO 8601) */
    val createdDate: String?,
    /** 산책 대표 이미지 URL */
    val imageUrl: String?,
    /** 산책 경로 좌표 목록 */
    val points: List<WalkPoint>,
    /** 산책 총 시간 (밀리초) */
    val totalTime: Long,
    /** 걸음 수 */
    val stepCount: Int,
    /** 좋아요 수 */
    val likeCount: Int,
    /** 내가 좋아요를 눌렀는지 여부 */
    val liked: Boolean,
    /** 산책 기록 ID */
    val walkId: Long,
)

/**
 * 산책 경로 좌표 포인트 도메인 모델
 */
data class WalkPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
)
