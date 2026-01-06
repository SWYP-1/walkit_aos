package swyp.team.walkit.domain.service.filter

/**
 * GPS 정확도 필터
 *
 * GPS accuracy 값이 설정된 최대값보다 높으면(부정확하면) 데이터를 필터링합니다.
 */
class AccuracyFilter(
    private val maxAccuracy: Float = 50f  // 50m 초과 시 무시 (도심 러닝 기준)
) {

    /**
     * GPS 좌표의 정확도를 검사하여 필터링
     *
     * @param lat 위도
     * @param lng 경도
     * @param accuracy GPS 정확도 (미터)
     * @return 필터링된 좌표 쌍, 또는 null (필터링됨)
     */
    fun filter(
        lat: Double,
        lng: Double,
        accuracy: Float
    ): Pair<Double, Double>? {
        return if (accuracy <= maxAccuracy) {
            lat to lng
        } else {
            null  // 정확도가 낮아 필터링
        }
    }
}

