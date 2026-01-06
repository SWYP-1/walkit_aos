package swyp.team.walkit.domain.service.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PathSmootherTest {

    private lateinit var pathSmoother: PathSmoother

    @BeforeEach
    fun setUp() {
        pathSmoother = PathSmoother()
    }

    @Test
    fun `좌표가 2개 미만이면 원본을 반환한다`() {
        // Given
        val latitudes = listOf(37.5665)
        val longitudes = listOf(126.9780)

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertEquals(latitudes, result.first)
        assertEquals(longitudes, result.second)
    }

    @Test
    fun `좌표 리스트 크기가 다르면 원본을 반환한다`() {
        // Given
        val latitudes = listOf(37.5665, 37.5666, 37.5667)
        val longitudes = listOf(126.9780, 126.9781) // 크기 불일치

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertEquals(latitudes, result.first)
        assertEquals(longitudes, result.second)
    }

    @Test
    fun `정상적인 경로에 스무딩이 적용된다`() {
        // Given - 서울 시내 이동 경로 (약 1km)
        val latitudes = listOf(
            37.5665,  // 시작점
            37.5670,  // 500m 북쪽
            37.5675,  // 1km 북쪽
            37.5680   // 1.5km 북쪽
        )
        val longitudes = listOf(
            126.9780, // 시작점
            126.9780, // 동일 경도
            126.9780, // 동일 경도
            126.9780  // 동일 경도
        )

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertNotNull(result.first)
        assertNotNull(result.second)
        assertEquals(result.first.size, result.second.size)

        // 스무딩 후에도 시작점과 끝점은 유지되어야 함
        assertEquals(latitudes.first(), result.first.first(), 0.0001)
        assertEquals(latitudes.last(), result.first.last(), 0.0001)
        assertEquals(longitudes.first(), result.second.first(), 0.0001)
        assertEquals(longitudes.last(), result.second.last(), 0.0001)
    }

    @Test
    fun `노이즈가 있는 경로가 부드러워진다`() {
        // Given - 노이즈가 있는 지그재그 경로
        val latitudes = listOf(
            37.5665,  // 시작점
            37.5663,  // 살짝 남쪽으로 (노이즈)
            37.5668,  // 북쪽으로 (노이즈)
            37.5666,  // 다시 정상
            37.5671,  // 북쪽으로 (노이즈)
            37.5669,  // 살짝 남쪽으로 (노이즈)
            37.5674   // 끝점
        )
        val longitudes = listOf(
            126.9780,
            126.9778, // 서쪽 노이즈
            126.9782, // 동쪽 노이즈
            126.9780,
            126.9783, // 동쪽 노이즈
            126.9779, // 서쪽 노이즈
            126.9780
        )

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertNotNull(result.first)
        assertNotNull(result.second)

        // 스무딩 후 포인트 수가 줄어들거나 같아야 함
        assertTrue(result.first.size <= latitudes.size)

        // 시작점과 끝점은 유지
        assertEquals(latitudes.first(), result.first.first(), 0.0001)
        assertEquals(latitudes.last(), result.first.last(), 0.0001)
    }

    @Test
    fun `커스텀 파라미터로 스무딩이 작동한다`() {
        // Given
        val latitudes = listOf(37.5665, 37.5670, 37.5675, 37.5680)
        val longitudes = listOf(126.9780, 126.9780, 126.9780, 126.9780)

        // When - 커스텀 파라미터 사용
        val result = pathSmoother.smoothPath(
            latitudes = latitudes,
            longitudes = longitudes,
            simplifyTolerance = 1.0, // 더 엄격한 단순화
            smoothSegments = 3      // 적은 보간 세그먼트
        )

        // Then
        assertNotNull(result.first)
        assertNotNull(result.second)
        assertTrue(result.first.size >= latitudes.size) // 보간으로 포인트가 늘어날 수 있음
    }

    @Test
    fun `스무딩 통계가 올바르게 계산된다`() {
        // Given
        val originalLatitudes = listOf(37.5665, 37.5670, 37.5675, 37.5680, 37.5685)
        val originalLongitudes = listOf(126.9780, 126.9780, 126.9780, 126.9780, 126.9780)

        val smoothedResult = pathSmoother.smoothPath(originalLatitudes, originalLongitudes)

        // When
        val stats = pathSmoother.getSmoothingStats(
            originalLatitudes,
            originalLongitudes,
            smoothedResult.first,
            smoothedResult.second
        )

        // Then
        assertNotNull(stats)
        assertTrue(stats.containsKey("original_points"))
        assertTrue(stats.containsKey("smoothed_points"))
        assertTrue(stats.containsKey("compression_ratio"))
        assertTrue(stats.containsKey("simplify_tolerance"))
        assertTrue(stats.containsKey("smooth_segments"))

        assertEquals(originalLatitudes.size, stats["original_points"])
        assertEquals(smoothedResult.first.size, stats["smoothed_points"])
        assertEquals(5.0, stats["simplify_tolerance"])
        assertEquals(5, stats["smooth_segments"])
    }

    @Test
    fun `예외 발생 시 원본 데이터를 반환한다`() {
        // Given - 유효하지 않은 데이터 (빈 리스트)
        val latitudes = emptyList<Double>()
        val longitudes = emptyList<Double>()

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertEquals(latitudes, result.first)
        assertEquals(longitudes, result.second)
    }

    @Test
    fun `실제 GPS 데이터를 시뮬레이션한 스무딩 테스트`() {
        // Given - 실제 GPS 경로 같은 패턴 (서울숲 공원 산책로 시뮬레이션)
        val latitudes = listOf(
            37.5445, 37.5447, 37.5449, 37.5451, 37.5453,  // 북쪽으로 직진
            37.5454, 37.5453, 37.5452,  // 약간 지그재그 (노이즈)
            37.5454, 37.5456, 37.5458, 37.5460   // 다시 북쪽으로
        )
        val longitudes = listOf(
            127.0410, 127.0410, 127.0410, 127.0410, 127.0410,
            127.0411, 127.0412, 127.0411,  // 약간 동쪽으로
            127.0410, 127.0410, 127.0410, 127.0410
        )

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        assertNotNull(result.first)
        assertNotNull(result.second)
        assertTrue(result.first.size > 0)
        assertTrue(result.second.size > 0)
        assertEquals(result.first.size, result.second.size)

        // 경로의 전체적인 방향성은 유지되어야 함 (북쪽으로의 이동)
        val originalLatRange = latitudes.max() - latitudes.min()
        val smoothedLatRange = result.first.max() - result.first.min()
        assertTrue(abs(smoothedLatRange - originalLatRange) < 0.01) // 큰 변화 없음
    }

    @Test
    fun `단순한 직선 경로의 스무딩 결과`() {
        // Given - 완벽한 직선 경로
        val latitudes = List(10) { 37.5665 + it * 0.0001 } // 10개의 점, 일정한 간격
        val longitudes = List(10) { 126.9780 } // 동일 경도

        // When
        val result = pathSmoother.smoothPath(latitudes, longitudes)

        // Then
        // 직선 경로는 크게 단순화될 수 있음
        assertNotNull(result.first)
        assertNotNull(result.second)
        // 시작점과 끝점은 유지
        assertEquals(latitudes.first(), result.first.first(), 0.0001)
        assertEquals(latitudes.last(), result.first.last(), 0.0001)
    }
}

