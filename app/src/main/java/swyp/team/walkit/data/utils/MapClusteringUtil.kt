package swyp.team.walkit.data.utils

import swyp.team.walkit.data.model.MapMarker
import swyp.team.walkit.data.model.MapMarkerType
import swyp.team.walkit.data.model.MapPin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 지도 마커 클러스터링 유틸리티.
 *
 * Greedy 방식: 마커를 순서대로 순회하며 반경 [CLUSTER_THRESHOLD_METERS] 이내의 미할당
 * 마커를 같은 그룹으로 묶는다. [UNCLUSTER_ZOOM] 이상이면 클러스터링을 비활성화한다.
 */
object MapClusteringUtil {

    /** 클러스터 묶음 기준 거리 (미터) */
    private const val CLUSTER_THRESHOLD_METERS = 500.0

    /** 이 줌 레벨 이상이면 개별 핀으로 풀어서 표시한다 */
    const val UNCLUSTER_ZOOM = 16

    /**
     * 마커 목록을 현재 줌 레벨에 맞게 클러스터링하여 [MapPin] 목록을 반환한다.
     *
     * FRIEND 마커는 줌 레벨과 무관하게 항상 개별 핀으로 표시한다.
     * SPOT 마커만 클러스터링 대상이다.
     *
     * @param markers   클러스터링 대상 마커
     * @param zoomLevel 현재 카메라 줌 레벨
     */
    fun cluster(markers: List<MapMarker>, zoomLevel: Int): List<MapPin> {
        if (markers.isEmpty()) return emptyList()

        // FRIEND 핀은 항상 개별 표시
        val friendPins = markers
            .filter { it.type == MapMarkerType.FRIEND }
            .map { MapPin.Single(it) }

        val spotMarkers = markers.filter { it.type == MapMarkerType.SPOT }

        // 줌 레벨이 충분히 높으면 SPOT도 개별 표시
        val spotPins = if (zoomLevel >= UNCLUSTER_ZOOM || spotMarkers.isEmpty()) {
            spotMarkers.map { MapPin.Single(it) }
        } else {
            clusterSpots(spotMarkers)
        }

        return friendPins + spotPins
    }

    /** SPOT 마커만 대상으로 greedy 클러스터링을 수행한다. */
    private fun clusterSpots(spots: List<MapMarker>): List<MapPin> {
        val assigned = BooleanArray(spots.size)
        val result = mutableListOf<MapPin>()

        for (i in spots.indices) {
            if (assigned[i]) continue
            val group = mutableListOf(spots[i])
            assigned[i] = true
            for (j in i + 1 until spots.size) {
                if (assigned[j]) continue
                val dist = haversineMeters(
                    spots[i].latitude, spots[i].longitude,
                    spots[j].latitude, spots[j].longitude,
                )
                if (dist <= CLUSTER_THRESHOLD_METERS) {
                    group.add(spots[j])
                    assigned[j] = true
                }
            }
            if (group.size == 1) {
                result.add(MapPin.Single(group[0]))
            } else {
                val avgLat = group.sumOf { it.latitude } / group.size
                val avgLon = group.sumOf { it.longitude } / group.size
                result.add(MapPin.Cluster(avgLat, avgLon, group.size, group))
            }
        }
        return result
    }

    /** Haversine 공식으로 두 좌표 사이의 거리를 미터 단위로 계산한다. */
    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
