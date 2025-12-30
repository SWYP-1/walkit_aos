package team.swyp.sdu.domain.service.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccuracyFilterTest {

    @Test
    fun `정확도가 임계값 이하면 좌표를 반환한다`() {
        // Given
        val filter = AccuracyFilter(maxAccuracy = 50f)
        val lat = 37.5665
        val lng = 126.9780
        val accuracy = 30f // 30m - 임계값 이하

        // When
        val result = filter.filter(lat, lng, accuracy)

        // Then
        assertNotNull(result)
        assertEquals(lat to lng, result)
    }

    @Test
    fun `정확도가 임계값을 초과하면 null을 반환한다`() {
        // Given
        val filter = AccuracyFilter(maxAccuracy = 50f)
        val lat = 37.5665
        val lng = 126.9780
        val accuracy = 65f // 65m - 임계값 초과

        // When
        val result = filter.filter(lat, lng, accuracy)

        // Then
        assertNull(result)
    }

    @Test
    fun `정확도가 정확히 임계값이면 좌표를 반환한다`() {
        // Given
        val filter = AccuracyFilter(maxAccuracy = 50f)
        val lat = 37.5665
        val lng = 126.9780
        val accuracy = 50f // 정확히 50m

        // When
        val result = filter.filter(lat, lng, accuracy)

        // Then
        assertNotNull(result)
        assertEquals(lat to lng, result)
    }

    @Test
    fun `커스텀 임계값으로 필터가 작동한다`() {
        // Given
        val customMaxAccuracy = 20f
        val filter = AccuracyFilter(maxAccuracy = customMaxAccuracy)
        val lat = 37.5665
        val lng = 126.9780

        // When & Then
        val resultUnderThreshold = filter.filter(lat, lng, 15f) // 15m - 통과
        val resultOverThreshold = filter.filter(lat, lng, 25f)  // 25m - 필터링

        assertNotNull(resultUnderThreshold)
        assertNull(resultOverThreshold)
    }
}
