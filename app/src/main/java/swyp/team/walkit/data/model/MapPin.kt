package swyp.team.walkit.data.model

/** 지도에 표시할 핀. 단일 마커 또는 클러스터 중 하나다. */
sealed class MapPin {
    /** 클러스터링되지 않은 단일 마커 */
    data class Single(val marker: MapMarker) : MapPin()

    /**
     * 여러 마커를 묶은 클러스터.
     *
     * @property latitude  클러스터 중심 위도 (포함 마커 평균)
     * @property longitude 클러스터 중심 경도 (포함 마커 평균)
     * @property count     포함된 마커 수
     * @property markers   포함된 마커 목록
     */
    data class Cluster(
        val latitude: Double,
        val longitude: Double,
        val count: Int,
        val markers: List<MapMarker>,
    ) : MapPin()
}
