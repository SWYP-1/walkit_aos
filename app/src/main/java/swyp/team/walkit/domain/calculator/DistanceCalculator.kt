package swyp.team.walkit.domain.calculator

import swyp.team.walkit.data.model.LocationPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 거리 계산 전담 클래스
 *
 * GPS / Step Counter 하이브리드 거리 계산
 * 평균 보폭 관리
 */
class DistanceCalculator @Inject constructor() {
    private var initialStepCount: Int = 0
    private var averageStepLength: Float? = null // 평균 보폭 (미터)

    /**
     * 초기화
     */
    fun initialize(initialStepCount: Int) {
        this.initialStepCount = initialStepCount
        this.averageStepLength = null
    }

    /**
     * 총 거리 계산 (하이브리드: GPS + Step Counter)
     *
     * @param locations 위치 포인트 리스트
     * @param stepCount 현재 걸음 수
     * @return 계산된 총 거리 (미터)
     */
    fun calculateTotalDistance(
        locations: List<LocationPoint>,
        stepCount: Int,
    ): Float {
        // GPS 기반 거리 계산
        val gpsDistance = calculateGpsDistance(locations)

        // Step Counter 기반 거리 계산
        val stepBasedDistance = calculateStepBasedDistance(stepCount)

        // GPS 데이터가 충분한 경우 (3개 이상 포인트)
        if (locations.size >= 3) {
            // 평균 보폭 계산 및 업데이트
            val stepsTaken = stepCount - initialStepCount
            if (stepsTaken > 0 && gpsDistance > 0f) {
                val calculatedStepLength = gpsDistance / stepsTaken
                // 평균 보폭 업데이트 (이동 평균)
                averageStepLength =
                    if (averageStepLength == null) {
                        calculatedStepLength
                    } else {
                        // 가중 이동 평균 (최근 값에 더 높은 가중치)
                        averageStepLength!! * 0.7f + calculatedStepLength * 0.3f
                    }
            }

            // GPS 정확도 확인
            val lastPoint = locations.lastOrNull()
            val isGpsAccurate = lastPoint?.accuracy?.let { it > 0 && it <= 20f } ?: false

            // GPS가 정확하면 GPS 거리 우선 사용
            if (isGpsAccurate && gpsDistance > 0f) {
                // Step Counter 거리와 비교하여 보정
                val difference = abs(gpsDistance - stepBasedDistance)
                val differenceRatio = if (gpsDistance > 0f) difference / gpsDistance else 0f

                // 차이가 20% 이내면 GPS 거리 사용, 그 외에는 가중 평균
                return if (differenceRatio <= 0.2f) {
                    gpsDistance
                } else {
                    // 가중 평균 (GPS 70%, Step 30%)
                    gpsDistance * 0.7f + stepBasedDistance * 0.3f
                }
            } else {
                // GPS가 부정확하면 Step Counter 거리 우선 사용
                return if (averageStepLength != null && stepBasedDistance > 0f) {
                    // 평균 보폭이 있으면 Step Counter 거리 사용
                    stepBasedDistance
                } else {
                    // 평균 보폭이 없으면 GPS 거리 사용 (부정확해도)
                    gpsDistance
                }
            }
        } else {
            // GPS 데이터가 부족하면 Step Counter 거리 사용
            return stepBasedDistance
        }
    }

    /**
     * GPS 속도 계산 (m/s) - 3점 이동 평균 방식
     *
     * @param locations 위치 포인트 리스트
     * @return 계산된 속도 (m/s)
     */
    fun calculateSpeed(locations: List<LocationPoint>): Float {
        // 3점 미만이면 2점 방식으로 폴백
        if (locations.size < 3) {
            if (locations.size < 2) return 0f

            val lastPoint = locations.last()
            val secondLastPoint = locations[locations.size - 2]

            val distance =
                calculateDistanceBetweenPoints(
                    secondLastPoint.latitude,
                    secondLastPoint.longitude,
                    lastPoint.latitude,
                    lastPoint.longitude,
                )

            val timeDiffSeconds =
                ((lastPoint.timestamp - secondLastPoint.timestamp) / 1000f)
                    .coerceAtLeast(0.1f)

            return distance / timeDiffSeconds
        }

        // 3점 이상이면 이동 평균 방식 사용
        val lastPoint = locations.last()
        val secondLastPoint = locations[locations.size - 2]
        val thirdLastPoint = locations[locations.size - 3]

        // 첫 번째 구간 (thirdLast -> secondLast)
        val distance1 =
            calculateDistanceBetweenPoints(
                thirdLastPoint.latitude,
                thirdLastPoint.longitude,
                secondLastPoint.latitude,
                secondLastPoint.longitude,
            )
        val timeDiff1 =
            ((secondLastPoint.timestamp - thirdLastPoint.timestamp) / 1000f)
                .coerceAtLeast(0.1f)
        val speed1 = if (timeDiff1 > 0f) distance1 / timeDiff1 else 0f

        // 두 번째 구간 (secondLast -> last)
        val distance2 =
            calculateDistanceBetweenPoints(
                secondLastPoint.latitude,
                secondLastPoint.longitude,
                lastPoint.latitude,
                lastPoint.longitude,
            )
        val timeDiff2 =
            ((lastPoint.timestamp - secondLastPoint.timestamp) / 1000f)
                .coerceAtLeast(0.1f)
        val speed2 = if (timeDiff2 > 0f) distance2 / timeDiff2 else 0f

        // 이동 평균: 두 구간의 가중 평균 (최근 데이터에 더 높은 가중치)
        // 가중치: 최근 구간 70%, 이전 구간 30%
        return if (speed1 > 0f && speed2 > 0f) {
            speed2 * 0.7f + speed1 * 0.3f
        } else if (speed2 > 0f) {
            speed2
        } else {
            0f
        }
    }

    /**
     * GPS 기반 거리 계산 (미터)
     */
    fun calculateGpsDistance(points: List<LocationPoint>): Float {
        if (points.size < 2) return 0f

        var totalDistance = 0f
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val distance =
                calculateDistanceBetweenPoints(
                    prev.latitude,
                    prev.longitude,
                    curr.latitude,
                    curr.longitude,
                )

            // GPS 노이즈 필터링: 5m 미만 이동은 무시
            if (distance >= 5f) {
                totalDistance += distance
            }
        }
        return totalDistance
    }

    /**
     * Step Counter 기반 거리 계산
     *
     * @param stepCount 현재 걸음 수
     * @return Step Counter 기반 계산 거리 (미터)
     */
    private fun calculateStepBasedDistance(stepCount: Int): Float {
        val stepsTaken = stepCount - initialStepCount
        if (stepsTaken <= 0) return 0f

        // 평균 보폭이 있으면 사용, 없으면 기본값 사용
        val stepLength = averageStepLength ?: 0.7f // 기본 보폭: 0.7m (성인 평균)

        return stepsTaken * stepLength
    }

    /**
     * 두 지점 간 거리 계산 (Haversine 공식)
     */
    private fun calculateDistanceBetweenPoints(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Float {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    /**
     * 평균 보폭 반환
     */
    fun getAverageStepLength(): Float? = averageStepLength

    /**
     * 상태 초기화
     */
    fun reset() {
        initialStepCount = 0
        averageStepLength = null
    }
}

