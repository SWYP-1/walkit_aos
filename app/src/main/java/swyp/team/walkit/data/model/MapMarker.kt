package swyp.team.walkit.data.model

import swyp.team.walkit.domain.model.NearbySpot

/** 지도 핀 타입 */
enum class MapMarkerType {
    /** 친구 위치 핀 (파란색) */
    FRIEND,
    /** 추천 장소 핀 (초록색) */
    SPOT,
}

/**
 * 지도에 표시할 마커 데이터 클래스
 */
data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String = "",
    val type: MapMarkerType = MapMarkerType.SPOT,
)

/**
 * [NearbySpot] 도메인 모델을 지도 마커로 변환한다.
 *
 * 마커 id는 좌표 조합으로 생성하여 유일성을 보장한다.
 */
fun NearbySpot.toMapMarker(): MapMarker = MapMarker(
    id = "${longitude}_${latitude}",
    latitude = latitude,
    longitude = longitude,
    title = placeName,
    description = roadAddressName.ifEmpty { addressName },
    type = MapMarkerType.SPOT,
)
