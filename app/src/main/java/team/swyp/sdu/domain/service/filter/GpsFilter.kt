package team.swyp.sdu.domain.service.filter

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 통합 GPS 필터
 *
 * 정확도 필터 → 칼만 필터 → 속도 필터 순서로 체인 적용
 * 각 단계에서 필터링되면 즉시 null 반환
 */
@Singleton
class GpsFilter @Inject constructor(
    private val accuracyFilter: AccuracyFilter,
    private val kalmanFilter: KalmanFilter,
    private val speedFilter: SpeedFilter
) {

    /**
     * GPS 좌표에 필터 체인을 적용
     *
     * 필터 순서: 정확도 → 칼만 → 속도
     * 어느 단계에서든 필터링되면 null 반환
     *
     * @param lat 측정된 위도
     * @param lng 측정된 경도
     * @param accuracy GPS 정확도 (미터)
     * @param timestamp 측정 타임스탬프
     * @return 필터링된 좌표 쌍, 또는 null (필터링됨)
     */
    fun filter(
        lat: Double,
        lng: Double,
        accuracy: Float,
        timestamp: Long
    ): Pair<Double, Double>? {
        try {
            // 1단계: 정확도 필터
            val afterAccuracy = accuracyFilter.filter(lat, lng, accuracy)
                ?: run {
                    Timber.d("정확도 필터: ${String.format("%.1f", accuracy)}m로 필터링됨")
                    return null
                }

            // 2단계: 칼만 필터
            val afterKalman = kalmanFilter.filter(
                afterAccuracy.first,
                afterAccuracy.second,
                accuracy,
                timestamp
            )

            // 3단계: 속도 필터
            val finalResult = speedFilter.filter(
                afterKalman.first,
                afterKalman.second,
                timestamp
            )

            Timber.v(
                "GPS 필터링 완료: " +
                "원본(${String.format("%.6f", lat)}, ${String.format("%.6f", lng)}) → " +
                "필터링(${String.format("%.6f", finalResult.first)}, ${String.format("%.6f", finalResult.second)})"
            )

            return finalResult

        } catch (e: Exception) {
            Timber.e(e, "GPS 필터링 중 오류 발생")
            return null  // 오류 발생 시 필터링
        }
    }

    /**
     * 필터 상태 리셋 (새로운 세션 시작 시 사용)
     * 칼만 필터와 속도 필터의 내부 상태를 초기화
     */
    fun reset() {
        kalmanFilter.reset()
        speedFilter.reset()
        Timber.d("GPS 필터 상태 리셋 완료")
    }

    /**
     * 필터링 통계 정보 (디버깅용)
     */
    fun getFilterInfo(): Map<String, Any> {
        return mapOf(
            "accuracy_filter_max" to accuracyFilter.javaClass.getDeclaredField("maxAccuracy")
                .apply { isAccessible = true }.get(accuracyFilter),
            "kalman_filter_q" to kalmanFilter.javaClass.getDeclaredField("q")
                .apply { isAccessible = true }.get(kalmanFilter),
            "speed_filter_max_ms" to speedFilter.javaClass.getDeclaredField("maxSpeedMs")
                .apply { isAccessible = true }.get(speedFilter)
        )
    }
}

