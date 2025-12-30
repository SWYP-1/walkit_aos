package team.swyp.sdu.domain.service.filter

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import timber.log.Timber

/**
 * 속도 기반 GPS 필터
 *
 * 비정상적으로 빠른 이동(GPS 튐)을 실시간으로 제거합니다.
 * 이전 점과의 거리와 시간을 계산하여 속도를 구하고,
 * 최대 속도를 초과하면 이전 점을 유지합니다.
 */
class SpeedFilter(
    private val maxSpeedMs: Double = 30.0  // 30 m/s = 시속 108km (러닝 충분)
) {
    private var lastPoint: Pair<Double, Double>? = null
    private var lastTimestamp: Long = 0L

    /**
     * 속도 기반 필터링 적용
     *
     * @param lat 현재 측정된 위도
     * @param lng 현재 측정된 경도
     * @param timestamp 현재 측정 타임스탬프
     * @return 필터링된 좌표 쌍
     */
    fun filter(
        lat: Double,
        lng: Double,
        timestamp: Long
    ): Pair<Double, Double> {
        val currentPoint = lat to lng

        if (lastPoint == null) {
            // 첫 데이터: 초기화하고 그대로 반환
            lastPoint = currentPoint
            lastTimestamp = timestamp
            return currentPoint
        }

        // 거리 계산 (SphericalUtil 사용 - 지구 곡면 고려)
        val distance = SphericalUtil.computeDistanceBetween(
            LatLng(lastPoint!!.first, lastPoint!!.second),
            LatLng(lat, lng)
        )

        // 시간 차이 계산 (초 단위)
        val timeDiffSeconds = (timestamp - lastTimestamp) / 1000.0

        if (timeDiffSeconds <= 0) {
            // 타임스탬프 오류: 현재 점 사용
            Timber.w("GPS 타임스탬프 오류: $timeDiffSeconds 초")
            return currentPoint
        }

        // 속도 계산 (m/s)
        val speedMs = distance / timeDiffSeconds

        return if (speedMs <= maxSpeedMs) {
            // 정상 속도: 현재 점 사용하고 상태 업데이트
            lastPoint = currentPoint
            lastTimestamp = timestamp
            currentPoint
        } else {
            // 과속 감지: 이전 점 유지
            Timber.w(
                "GPS 속도 초과 감지: ${String.format("%.1f", speedMs)}m/s " +
                "(최대: ${String.format("%.1f", maxSpeedMs)}m/s), " +
                "거리: ${String.format("%.1f", distance)}m, " +
                "시간: ${String.format("%.1f", timeDiffSeconds)}초"
            )
            lastPoint!!
        }
    }

    /**
     * 필터 상태 리셋 (새로운 세션 시작 시 사용)
     */
    fun reset() {
        lastPoint = null
        lastTimestamp = 0L
    }
}
