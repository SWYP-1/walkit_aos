package swyp.team.walkit.data.remote.spot.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 주변 장소 조회 API 응답 DTO
 *
 * GET /spots/nearby 응답의 각 장소 항목을 표현한다.
 * 모든 필드는 Nullable + 기본값으로 설정하여 불완전한 응답에 대비한다.
 */
@Serializable
data class NearbySpotDto(
    @SerialName("placeName")
    val placeName: String? = null,

    @SerialName("addressName")
    val addressName: String? = null,

    @SerialName("roadAddressName")
    val roadAddressName: String? = null,

    @SerialName("distance")
    val distance: String? = null,

    @SerialName("placeUrl")
    val placeUrl: String? = null,

    @SerialName("blogReviewCount")
    val blogReviewCount: Int? = null,

    @SerialName("blogReviewLink")
    val blogReviewLink: String? = null,

    @SerialName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    /** 경도 (longitude) - 서버에서 String으로 내려옴 */
    @SerialName("x")
    val x: String? = null,

    /** 위도 (latitude) - 서버에서 String으로 내려옴 */
    @SerialName("y")
    val y: String? = null,
)