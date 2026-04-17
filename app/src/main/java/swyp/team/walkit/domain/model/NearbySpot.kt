package swyp.team.walkit.domain.model

/**
 * 주변 추천 장소 도메인 모델
 *
 * [swyp.team.walkit.data.remote.spot.dto.NearbySpotDto]에서 매핑된 순수 도메인 객체.
 */
data class NearbySpot(
    /** 장소명 */
    val placeName: String,
    /** 지번 주소 */
    val addressName: String,
    /** 도로명 주소 */
    val roadAddressName: String,
    /** 중심점으로부터 거리 (미터) */
    val distance: String,
    /** 카카오맵 상세 URL */
    val placeUrl: String,
    /** 블로그 리뷰 수 */
    val blogReviewCount: Int,
    /** 블로그 리뷰 링크 */
    val blogReviewLink: String,
    /** 대표 이미지 URL */
    val thumbnailUrl: String,
    /** 경도 (longitude) */
    val longitude: Double,
    /** 위도 (latitude) */
    val latitude: Double,
)