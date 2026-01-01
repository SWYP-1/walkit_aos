package team.swyp.sdu.domain.service.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.abs

class KalmanFilterTest {

    private lateinit var filter: KalmanFilter

    @BeforeEach
    fun setUp() {
        filter = KalmanFilter(q = 3f)
    }

    @Test
    fun `첫 번째 데이터는 초기화되고 그대로 반환된다`() {
        // Given
        val lat = 37.5665
        val lng = 126.9780
        val accuracy = 10f
        val timestamp = System.currentTimeMillis()

        // When
        val result = filter.filter(lat, lng, accuracy, timestamp)

        // Then
        assertNotNull(result)
        assertEquals(lat, result.first, 0.0001)
        assertEquals(lng, result.second, 0.0001)
    }

    @Test
    fun `두 번째 데이터부터 칼만 필터가 적용된다`() {
        // Given
        val timestamp1 = System.currentTimeMillis()
        val timestamp2 = timestamp1 + 1000 // 1초 후

        // 첫 번째 데이터
        filter.filter(37.5665, 126.9780, 10f, timestamp1)

        // 두 번째 데이터 (약간의 노이즈 추가)
        val noisyLat = 37.5667
        val noisyLng = 126.9782

        // When
        val result = filter.filter(noisyLat, noisyLng, 10f, timestamp2)

        // Then
        assertNotNull(result)
        // 칼만 필터가 적용되어 노이즈가 감소되어야 함
        // (정확한 값 검증보다는 필터링이 적용되었는지 확인)
        assert(result.first != noisyLat || result.second != noisyLng)
    }

    @Test
    fun `동일한 좌표가 연속으로 입력되면 안정화된다`() {
        // Given
        val lat = 37.5665
        val lng = 126.9780
        val accuracy = 5f
        var timestamp = System.currentTimeMillis()

        // When - 여러 번 동일 좌표 입력
        val results = mutableListOf<Pair<Double, Double>>()
        for (i in 0..4) {
            timestamp += 1000
            val result = filter.filter(lat, lng, accuracy, timestamp)
            results.add(result)
        }

        // Then - 점차 안정화되어야 함
        assert(results.size == 5)
        // 마지막 결과들은 서로 가까워야 함 (수렴)
        val lastThree = results.takeLast(3)
        val variance = lastThree.map { abs(it.first - lat) + abs(it.second - lng) }.average()
        assert(variance < 0.001) // 충분히 수렴했는지 확인
    }

    @Test
    fun `노이즈가 있는 데이터에 대해 필터링이 효과적이다`() {
        // Given
        val baseLat = 37.5665
        val baseLng = 126.9780
        val timestamp = System.currentTimeMillis()

        // 첫 번째 데이터로 기준 설정
        filter.filter(baseLat, baseLng, 5f, timestamp)

        // 노이즈가 있는 데이터들
        val noisyData = listOf(
            baseLat + 0.001 to baseLng + 0.001, // 100m 정도 노이즈
            baseLat - 0.0005 to baseLng + 0.0008,
            baseLat + 0.002 to baseLng - 0.001,
        )

        // When
        val filteredResults = noisyData.mapIndexed { index, (lat, lng) ->
            filter.filter(lat, lng, 15f, timestamp + index * 1000L)
        }

        // Then
        // 필터링된 결과들이 원본보다 기준값에 가까워야 함
        val originalErrors = noisyData.map { (lat, lng) ->
            abs(lat - baseLat) + abs(lng - baseLng)
        }

        val filteredErrors = filteredResults.map { (lat, lng) ->
            abs(lat - baseLat) + abs(lng - baseLng)
        }

        val avgOriginalError = originalErrors.average()
        val avgFilteredError = filteredErrors.average()

        // 필터링 후 오차가 줄어야 함
        assert(avgFilteredError < avgOriginalError)
    }

    @Test
    fun `리셋 후 필터가 초기화된다`() {
        // Given
        val timestamp = System.currentTimeMillis()

        // 필터 사용
        filter.filter(37.5665, 126.9780, 10f, timestamp)
        filter.filter(37.5666, 126.9781, 10f, timestamp + 1000)

        // When
        filter.reset()

        // Then - 새로운 데이터 입력 시 첫 데이터처럼 동작
        val newResult = filter.filter(37.5645, 126.9790, 10f, timestamp + 2000)
        assertEquals(37.5645, newResult.first, 0.0001)
        assertEquals(126.9790, newResult.second, 0.0001)
    }

    @Test
    fun `커스텀 Q 값으로 필터가 작동한다`() {
        // Given
        val customFilter = KalmanFilter(q = 1f) // 더 낮은 프로세스 노이즈
        val timestamp = System.currentTimeMillis()

        // When
        customFilter.filter(37.5665, 126.9780, 10f, timestamp)
        val result = customFilter.filter(37.5666, 126.9781, 10f, timestamp + 1000)

        // Then
        assertNotNull(result)
        // Q 값이 낮으면 필터링이 더 민감하게 작동
    }
}

