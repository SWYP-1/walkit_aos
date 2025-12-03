package team.swyp.sdu.utils

import team.swyp.sdu.data.model.LocationPoint

/**
 * 테스트용 위치 데이터 유틸리티
 * 
 * 서울 중심(서울시청)을 기준으로 한 작은 경로를 생성합니다.
 */
object LocationTestData {
    /**
     * 서울 중심을 기준으로 한 테스트용 위치 20개 반환
     * 
     * 서울시청(37.5665, 126.9780)을 시작점으로 하여
     * 작은 원형 경로를 생성합니다.
     */
    fun getSeoulTestLocations(): List<LocationPoint> {
        val baseLat = 37.5665 // 서울시청 위도
        val baseLon = 126.9780 // 서울시청 경도
        
        // 작은 원형 경로 생성 (약 500m 반경)
        val radius = 0.0045 // 약 500m에 해당하는 위경도 차이
        val centerTime = System.currentTimeMillis()
        
        return (0 until 20).map { index ->
            val angle = (index * 18.0) * Math.PI / 180.0 // 20개 포인트로 원형 경로
            val lat = baseLat + radius * Math.sin(angle)
            val lon = baseLon + radius * Math.cos(angle)
            
            LocationPoint(
                latitude = lat,
                longitude = lon,
                timestamp = centerTime + (index * 10000L), // 10초 간격
                accuracy = 10f, // 테스트용 정확도
            )
        }
    }
}
