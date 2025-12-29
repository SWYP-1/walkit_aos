package team.swyp.sdu.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import team.swyp.sdu.data.model.LocationPoint

/**
 * 위도/경도 리스트로 총 이동 거리(m)를 계산 (Haversine).
 * locations가 2개 미만이면 0을 반환합니다.
 */
fun computeRouteDistanceMeters(locations: List<LocationPoint>): Double {
    if (locations.size < 2) return 0.0
    var distance = 0.0
    for (i in 0 until locations.lastIndex) {
        val a = locations[i]
        val b = locations[i + 1]
        distance += haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
    }
    return distance
}

/**
 * Haversine 공식을 이용해 두 지점 간 거리(m)를 계산.
 */
fun haversineMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val r = 6371000.0 // 지구 반지름 (m)
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}










