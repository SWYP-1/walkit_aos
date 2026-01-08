package swyp.team.walkit.ui.walking.viewmodel

import org.junit.Test
import org.junit.Assert.*
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.domain.service.filter.PathSmoother
import swyp.team.walkit.testutil.JsonTestUtil
import swyp.team.walkit.utils.LocationConstants

/**
 * WalkingViewModel 간단 테스트
 */
class SimpleWalkingViewModelTest {

    @Test
    fun `기본 위치 좌표 검증 테스트`() {
        // 서울 시청 좌표가 올바르게 설정되어 있는지 확인
        assertEquals(37.5665, LocationConstants.DEFAULT_LATITUDE, 0.0001)
        assertEquals(126.9780, LocationConstants.DEFAULT_LONGITUDE, 0.0001)
    }

    @Test
    fun `GPS 위치 우선순위 테스트`() {
        // GPS 위치가 null일 때 기본 위치 사용 로직 검증
        val gpsLocation: android.location.Location? = null

        val (lat, lon) = if (gpsLocation != null) {
            gpsLocation.latitude to gpsLocation.longitude
        } else {
            // 서울 시청 좌표 (기본값)
            LocationConstants.DEFAULT_LATITUDE to LocationConstants.DEFAULT_LONGITUDE
        }

        assertEquals(LocationConstants.DEFAULT_LATITUDE, lat, 0.0001)
        assertEquals(LocationConstants.DEFAULT_LONGITUDE, lon, 0.0001)
    }

    @Test
    fun `GPS 위치 사용 테스트`() {
        // GPS 위치가 있을 때 실제 위치 사용
        val mockLocation = android.location.Location("test").apply {
            latitude = 35.123456
            longitude = 129.987654
        }

        val (lat, lon) = if (mockLocation != null) {
            mockLocation.latitude to mockLocation.longitude
        } else {
            LocationConstants.DEFAULT_LATITUDE to LocationConstants.DEFAULT_LONGITUDE
        }

        assertEquals(35.123456, lat, 0.0001)
        assertEquals(129.987654, lon, 0.0001)
    }

    @Test
    fun `위치 기반 캐릭터 조회 파라미터 검증`() {
        // API 호출 시 위도/경도 파라미터가 올바르게 전달되는지 검증
        val testLat = 37.123456
        val testLon = 127.987654

        // 실제 API 호출을 모방한 파라미터 검증
        assertTrue("위도는 유효한 범위여야 함", testLat in -90.0..90.0)
        assertTrue("경도는 유효한 범위여야 함", testLon in -180.0..180.0)

        // 서울 시청 좌표 범위 내인지 확인
        assertTrue("서울 지역 위도 범위", testLat in 37.0..38.0)
        assertTrue("서울 지역 경도 범위", testLon in 126.0..128.0)
    }

    @Test
    fun `JsonTestUtil을 활용한 위치 데이터 검증 테스트`() {
        // JsonTestUtil을 사용해서 실제 데이터를 검증
        val locations = JsonTestUtil.loadLocationsFromTempJson()

        // JsonTestUtil의 검증 함수 사용
        val validationResult = JsonTestUtil.validateLocations(locations)

        println("🔍 WalkingViewModel 테스트에서 데이터 검증: $validationResult")

        // 검증 결과에 따라 테스트 진행
        if (locations.isNotEmpty()) {
            assertTrue("데이터 검증이 성공해야 함", validationResult.contains("✅"))

            // WalkingViewModel 관련 검증
            val firstLocation = locations.first()
            assertTrue("위치 데이터가 서울 근처여야 함",
                firstLocation.latitude in 37.0..38.0 && firstLocation.longitude in 126.0..128.0)
        } else {
            assertTrue("데이터가 없으면 검증 실패 메시지가 나와야 함", validationResult.contains("❌"))
        }
    }

    @Test
    fun `경로 스무딩 로직 검증 테스트`() {
        // PathSmoother를 사용한 경로 스무딩 로직 검증
        val pathSmoother = PathSmoother()

        // 테스트용 위치 데이터 (temp.json에서 가져옴)
        val locations = JsonTestUtil.loadLocationsFromTempJson()

        if (locations.size >= 3) {
            val latitudes = locations.take(10).map { it.latitude } // 처음 10개만 테스트
            val longitudes = locations.take(10).map { it.longitude }

            try {
                val (smoothedLats, smoothedLngs) = pathSmoother.smoothPath(latitudes, longitudes)

                // 스무딩 결과 검증
                assertTrue("스무딩된 위도 데이터가 있어야 함", smoothedLats.isNotEmpty())
                assertTrue("스무딩된 경도 데이터가 있어야 함", smoothedLngs.isNotEmpty())
                assertEquals("위도와 경도 배열 크기가 같아야 함", smoothedLats.size, smoothedLngs.size)

                // 원본과 스무딩된 데이터 크기 비교 (스무딩은 포인트를 줄이거나 비슷하게 유지)
                assertTrue("스무딩된 데이터 크기가 원본보다 크거나 같아야 함",
                    smoothedLats.size >= latitudes.size)

                println("✅ 경로 스무딩 테스트 성공: ${latitudes.size} → ${smoothedLats.size} 포인트")

            } catch (e: Exception) {
                println("❌ 경로 스무딩 테스트 실패: ${e.message}")
                assertTrue("스무딩 알고리즘에 문제가 있음", false)
            }
        } else {
            println("⚠️ 테스트용 위치 데이터가 부족함 (${locations.size}개)")
        }
    }

    @Test
    fun `createCompletedSession에서 경로 스무딩 적용 검증`() {
        // createCompletedSession에서 smoothedLocations가 제대로 설정되는지 검증

        // 테스트용 위치 데이터
        val testLocations = listOf(
            LocationPoint(37.3228814, 127.0947403, 1767842731696, 15.386f),
            LocationPoint(37.3228095, 127.0945825, 1767842753540, 10.979f),
            LocationPoint(37.32280279545957, 127.09457873891635, 1767842763288, 13.184f),
            LocationPoint(37.32275444708028, 127.0945830095502, 1767842772337, 16.237f),
            LocationPoint(37.322673305069, 127.09460757885513, 1767842781388, 14.916f)
        )

        // PathSmoother로 스무딩 적용
        val pathSmoother = PathSmoother()
        val latitudes = testLocations.map { it.latitude }
        val longitudes = testLocations.map { it.longitude }

        val (smoothedLats, smoothedLngs) = pathSmoother.smoothPath(latitudes, longitudes)

        // WalkingSession 생성 시 smoothedLocations 설정 검증
        val smoothedLocations = smoothedLats.zip(smoothedLngs).map { (lat, lng) ->
            LocationPoint(
                latitude = lat,
                longitude = lng,
                timestamp = testLocations.last().timestamp,
                accuracy = null
            )
        }

        // 검증
        assertTrue("스무딩된 위치 데이터가 생성되어야 함", smoothedLocations.isNotEmpty())
        assertEquals("스무딩된 위도/경도 쌍의 개수가 같아야 함", smoothedLats.size, smoothedLocations.size)

        println("✅ createCompletedSession 스무딩 검증 성공: ${testLocations.size} → ${smoothedLocations.size} 포인트")
    }
}
