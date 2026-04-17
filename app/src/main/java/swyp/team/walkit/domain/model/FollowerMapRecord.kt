package swyp.team.walkit.domain.model

import swyp.team.walkit.data.model.MapMarker
import swyp.team.walkit.data.model.MapMarkerType

/**
 * 지도 화면에 표시할 팔로워 산책 기록 도메인 모델
 */
data class FollowerMapRecord(
    /** 팔로워 사용자 ID */
    val userId: Long,
    /** 산책 기록 ID */
    val walkId: Long,
    /** 위도 */
    val latitude: Double,
    /** 경도 */
    val longitude: Double,
    /** 캐릭터 등급 (SEED, SPROUT, TREE) */
    val grade: String,
    /** 캐릭터 머리 이미지명 */
    val headImageName: String?,
    /** 캐릭터 몸통 이미지명 */
    val bodyImageName: String?,
)

/**
 * [FollowerMapRecord] 도메인 모델을 지도 마커로 변환한다.
 */
fun FollowerMapRecord.toMapMarker(): MapMarker = MapMarker(
    id = "friend_$userId",
    latitude = latitude,
    longitude = longitude,
    title = "친구 #$userId",
    description = grade,
    type = MapMarkerType.FRIEND,
)
