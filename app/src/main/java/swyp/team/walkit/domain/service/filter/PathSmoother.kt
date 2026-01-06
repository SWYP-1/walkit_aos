package swyp.team.walkit.domain.service.filter

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 경로 스무딩 (운동 완료 후 적용)
 *
 * 저장된 GPS 좌표들을 Catmull-Rom Spline으로 보간하여 부드러운 경로를 생성합니다.
 * 상용 러닝앱 수준의 자연스러운 곡선을 제공합니다.
 */
@Singleton
class PathSmoother @Inject constructor() {

    /**
     * 경로 스무딩 적용 (Catmull-Rom Spline)
     *
     * 1. LatLng로 변환
     * 2. PolyUtil.simplify로 불필요한 점 제거
     * 3. Catmull-Rom Spline으로 부드러운 곡선 생성
     *
     * @param latitudes 위도 리스트
     * @param longitudes 경도 리스트
     * @param simplifyTolerance 단순화 허용 오차 (미터) - 기본 5.0m
     * @param smoothSegments 보간 세그먼트 수 - 기본 8개 (높을수록 부드러움)
     * @return 스무딩된 위도/경도 리스트 쌍
     */
    fun smoothPath(
        latitudes: List<Double>,
        longitudes: List<Double>,
        simplifyTolerance: Double = 5.0,
        smoothSegments: Int = 8
    ): Pair<List<Double>, List<Double>> {
        if (latitudes.size < 2 || longitudes.size < 2 || latitudes.size != longitudes.size) {
            Timber.w("스무딩 불가: 유효하지 않은 좌표 데이터")
            return latitudes to longitudes
        }

        try {
            Timber.d("경로 스무딩 시작: ${latitudes.size}개 포인트 (Catmull-Rom Spline)")

            // 1. LatLng로 변환
            val points = latitudes.zip(longitudes).map { (lat, lng) ->
                LatLng(lat, lng)
            }

            // 2. 경로 단순화 (Douglas-Peucker 알고리즘)
            val simplifiedPoints = PolyUtil.simplify(points, simplifyTolerance)
            Timber.d("단순화 완료: ${points.size} → ${simplifiedPoints.size} 포인트")

            // 3. Catmull-Rom Spline 보간
            val smoothedPoints = interpolateCatmullRom(simplifiedPoints, smoothSegments)

            Timber.d("스무딩 완료: 최종 ${smoothedPoints.size} 포인트")

            return smoothedPoints.map { it.latitude } to smoothedPoints.map { it.longitude }

        } catch (t: Throwable) {
            Timber.e(t, "경로 스무딩 중 오류 발생")
            return latitudes to longitudes  // 오류 시 원본 반환
        }
    }

    /**
     * Catmull-Rom Spline 보간
     *
     * 4개의 제어점(P0, P1, P2, P3)을 사용하여 P1과 P2 사이의 부드러운 곡선을 생성합니다.
     * - P0: 이전 점 (곡선의 시작 방향 결정)
     * - P1: 곡선 시작점
     * - P2: 곡선 끝점
     * - P3: 다음 점 (곡선의 끝 방향 결정)
     *
     * 특징:
     * - 모든 제어점을 통과 (interpolating spline)
     * - C1 연속성 보장 (부드러운 연결)
     * - 국소적 제어 (한 점 변경 시 주변만 영향)
     */
    private fun interpolateCatmullRom(
        points: List<LatLng>,
        segments: Int
    ): List<LatLng> {
        if (points.size < 2) return points
        if (points.size == 2) {
            // 2개 점만 있으면 직선 보간
            return interpolateLinear(points, segments)
        }

        val smoothedPoints = mutableListOf<LatLng>()

        // 첫 점 추가
        smoothedPoints.add(points.first())

        for (i in 0 until points.lastIndex) {
            // 4개 제어점 설정
            val p0 = if (i > 0) points[i - 1] else points[i]  // 이전 점 (없으면 현재)
            val p1 = points[i]                                  // 시작점
            val p2 = points[i + 1]                              // 끝점
            val p3 = if (i < points.size - 2) points[i + 2] else points[i + 1]  // 다음 점

            // P1과 P2 사이를 Catmull-Rom으로 보간
            for (j in 1..segments) {
                val t = j.toDouble() / segments
                val interpolated = catmullRomPoint(p0, p1, p2, p3, t)
                smoothedPoints.add(interpolated)
            }
        }

        return smoothedPoints
    }

    /**
     * Catmull-Rom Spline 공식으로 단일 점 계산
     *
     * q(t) = 0.5 * (
     *   (2 * P1) +
     *   (-P0 + P2) * t +
     *   (2*P0 - 5*P1 + 4*P2 - P3) * t² +
     *   (-P0 + 3*P1 - 3*P2 + P3) * t³
     * )
     *
     * @param p0 이전 제어점
     * @param p1 시작점
     * @param p2 끝점
     * @param p3 다음 제어점
     * @param t 보간 파라미터 (0.0 ~ 1.0)
     * @return 보간된 점
     */
    private fun catmullRomPoint(
        p0: LatLng,
        p1: LatLng,
        p2: LatLng,
        p3: LatLng,
        t: Double
    ): LatLng {
        val t2 = t * t
        val t3 = t2 * t

        // 위도 계산
        val lat = 0.5 * (
                (2 * p1.latitude) +
                        (-p0.latitude + p2.latitude) * t +
                        (2 * p0.latitude - 5 * p1.latitude + 4 * p2.latitude - p3.latitude) * t2 +
                        (-p0.latitude + 3 * p1.latitude - 3 * p2.latitude + p3.latitude) * t3
                )

        // 경도 계산 (날짜 변경선 고려)
        val lng0 = p0.longitude
        val lng1 = p1.longitude
        val lng2 = normalizeLongitudeDifference(p2.longitude, lng1)
        val lng3 = normalizeLongitudeDifference(p3.longitude, lng1)

        val lng = 0.5 * (
                (2 * lng1) +
                        (-lng0 + lng2) * t +
                        (2 * lng0 - 5 * lng1 + 4 * lng2 - lng3) * t2 +
                        (-lng0 + 3 * lng1 - 3 * lng2 + lng3) * t3
                )

        return LatLng(lat, normalizeLongitude(lng))
    }

    /**
     * 날짜 변경선을 고려한 경도 차이 정규화
     * 기준 경도에 대해 가장 가까운 경로로 경도를 조정합니다.
     */
    private fun normalizeLongitudeDifference(lng: Double, referenceLng: Double): Double {
        var result = lng
        while (result - referenceLng > 180) result -= 360
        while (result - referenceLng < -180) result += 360
        return result
    }

    /**
     * 경도 정규화 (-180 ~ 180 범위로)
     */
    private fun normalizeLongitude(lng: Double): Double {
        var result = lng
        while (result > 180) result -= 360
        while (result < -180) result += 360
        return result
    }

    /**
     * 선형 보간 (폴백용 - 점이 2개만 있을 때)
     */
    private fun interpolateLinear(
        points: List<LatLng>,
        segments: Int
    ): List<LatLng> {
        if (points.size < 2) return points

        val smoothedPoints = mutableListOf<LatLng>()
        val start = points[0]
        val end = points[1]

        smoothedPoints.add(start)

        for (j in 1 until segments) {
            val fraction = j.toDouble() / segments
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction
            smoothedPoints.add(LatLng(lat, lng))
        }

        smoothedPoints.add(end)

        return smoothedPoints
    }

    /**
     * 스무딩된 경로의 통계 정보
     */
    fun getSmoothingStats(
        originalLatitudes: List<Double>,
        originalLongitudes: List<Double>,
        smoothedLatitudes: List<Double>,
        smoothedLongitudes: List<Double>
    ): Map<String, Any> {
        return mapOf(
            "original_points" to originalLatitudes.size,
            "smoothed_points" to smoothedLatitudes.size,
            "compression_ratio" to String.format("%.2f", smoothedLatitudes.size.toFloat() / originalLatitudes.size),
            "simplify_tolerance" to 5.0,
            "smooth_segments" to 8,
            "algorithm" to "Catmull-Rom Spline"
        )
    }
}