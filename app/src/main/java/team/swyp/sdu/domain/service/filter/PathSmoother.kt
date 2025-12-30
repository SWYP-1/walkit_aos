package team.swyp.sdu.domain.service.filter

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 경로 스무딩 (운동 완료 후 적용)
 *
 * 저장된 GPS 좌표들을 단순화하고 보간하여 부드러운 경로를 생성합니다.
 * Google Maps Utils 라이브러리의 PolyUtil과 SphericalUtil을 사용합니다.
 */
@Singleton
class PathSmoother @Inject constructor() {

    /**
     * 경로 스무딩 적용
     *
     * 1. LatLng로 변환
     * 2. PolyUtil.simplify로 불필요한 점 제거
     * 3. SphericalUtil.interpolate로 점들 사이 보간
     *
     * @param latitudes 위도 리스트
     * @param longitudes 경도 리스트
     * @param simplifyTolerance 단순화 허용 오차 (미터) - 기본 5.0m
     * @param smoothSegments 보간 세그먼트 수 - 기본 5개
     * @return 스무딩된 위도/경도 리스트 쌍
     */
    fun smoothPath(
        latitudes: List<Double>,
        longitudes: List<Double>,
        simplifyTolerance: Double = 5.0,
        smoothSegments: Int = 5
    ): Pair<List<Double>, List<Double>> {
        if (latitudes.size < 2 || longitudes.size < 2 || latitudes.size != longitudes.size) {
            Timber.w("스무딩 불가: 유효하지 않은 좌표 데이터")
            return latitudes to longitudes
        }

        try {
            Timber.d("경로 스무딩 시작: ${latitudes.size}개 포인트")

            // 1. LatLng로 변환
            val points = latitudes.zip(longitudes).map { (lat, lng) ->
                LatLng(lat, lng)
            }

            // 2. 경로 단순화 (Douglas-Peucker 알고리즘)
            val simplifiedPoints = PolyUtil.simplify(points, simplifyTolerance)
            Timber.d("단순화 완료: ${points.size} → ${simplifiedPoints.size} 포인트")

            // 3. 보간 적용하여 부드러운 곡선 생성
            val smoothedPoints = interpolatePoints(simplifiedPoints, smoothSegments)

            Timber.d("스무딩 완료: 최종 ${smoothedPoints.size} 포인트")

            return smoothedPoints.map { it.latitude } to smoothedPoints.map { it.longitude }

        } catch (e: Exception) {
            Timber.e(e, "경로 스무딩 중 오류 발생")
            return latitudes to longitudes  // 오류 시 원본 반환
        }
    }

    /**
     * 점들 사이를 보간하여 부드러운 곡선 생성
     */
    private fun interpolatePoints(
        points: List<LatLng>,
        segments: Int
    ): List<LatLng> {
        if (points.size < 2) return points

        val smoothedPoints = mutableListOf<LatLng>()

        for (i in 0 until points.lastIndex) {
            val start = points[i]
            val end = points[i + 1]

            smoothedPoints.add(start)  // 시작점 추가

            // 두 점 사이 보간
            if (segments > 1) {
                for (j in 1 until segments) {
                    val fraction = j.toDouble() / segments
                    val interpolated = SphericalUtil.interpolate(start, end, fraction)
                    smoothedPoints.add(interpolated)
                }
            }
        }

        smoothedPoints.add(points.last())  // 끝점 추가

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
            "smooth_segments" to 5
        )
    }
}
