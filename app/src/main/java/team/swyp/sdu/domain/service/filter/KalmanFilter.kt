package team.swyp.sdu.domain.service.filter

import kotlin.math.pow

/**
 * 칼만 필터
 *
 * GPS 노이즈를 수학적으로 제거하여 좌표를 부드럽게 필터링합니다.
 * 표준 칼만 필터 알고리즘을 구현합니다.
 */
class KalmanFilter(
    private val q: Float = 3f  // 프로세스 노이즈 (3f 고정 - GPS 노이즈 수준)
) {
    private var variance: Double = Double.MAX_VALUE  // 초기 분산 (무한대)
    private var timestamp: Long = 0L                 // 마지막 타임스탬프
    private var lat: Double = 0.0                    // 필터링된 위도
    private var lng: Double = 0.0                    // 필터링된 경도

    /**
     * 칼만 필터를 적용하여 GPS 좌표를 필터링
     *
     * @param lat 측정된 위도
     * @param lng 측정된 경도
     * @param accuracy GPS 정확도 (미터)
     * @param timestamp 측정 타임스탬프
     * @return 필터링된 좌표 쌍
     */
    fun filter(
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    ): Pair<Double, Double> {
        if (this.timestamp == 0L) {
            // 첫 데이터: 초기화만 하고 그대로 반환
            initialize(lat, lng, accuracy, timestamp)
            return lat to lng
        }

        // 시간 업데이트 (예측 단계)
        val timeDiff = (timestamp - this.timestamp) / 1000.0  // 초 단위 변환
        variance += timeDiff * q.toDouble() * q.toDouble()

        // 칼만 게인 계산 (업데이트 단계)
        val accuracyVariance = accuracy.toDouble().pow(2.0)  // 측정 노이즈
        val k = variance / (variance + accuracyVariance)     // 칼만 게인

        // 상태 업데이트
        val newLat = this.lat + k * (lat - this.lat)
        val newLng = this.lng + k * (lng - this.lng)
        variance *= (1 - k)  // 분산 업데이트

        // 상태 저장
        this.lat = newLat
        this.lng = newLng
        this.timestamp = timestamp

        return newLat to newLng
    }

    /**
     * 필터 초기화
     */
    private fun initialize(lat: Double, lng: Double, accuracy: Float, timestamp: Long) {
        this.lat = lat
        this.lng = lng
        this.timestamp = timestamp
        this.variance = accuracy.toDouble().pow(2.0)  // 초기 분산 = 측정 정확도²
    }

    /**
     * 필터 상태 리셋 (새로운 세션 시작 시 사용)
     */
    fun reset() {
        variance = Double.MAX_VALUE
        timestamp = 0L
        lat = 0.0
        lng = 0.0
    }
}
