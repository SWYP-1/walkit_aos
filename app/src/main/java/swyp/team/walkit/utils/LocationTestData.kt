package swyp.team.walkit.utils

import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.domain.model.FollowerMapRecord
import swyp.team.walkit.domain.model.FollowerRecentActivity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 테스트용 위치 데이터 유틸리티
 * 
 * 서울과 용인시를 기준으로 한 다양한 테스트 경로를 제공합니다.
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

    /**
     * 용인시를 기준으로 한 테스트용 위치 40개 반환
     * 
     * 용인시청(37.2411, 127.1776) 근처를 시작점으로 하여
     * 자연스러운 산책 경로(원형이 아닌 패스)를 생성합니다.
     */
    fun getYonginTestLocations(): List<LocationPoint> {
        // 용인시청 근처 시작점
        val startLat = 37.2411
        val startLon = 127.1776
        
        val centerTime = System.currentTimeMillis()
        val locations = mutableListOf<LocationPoint>()
        
        // 현재 위치 추적 변수
        var currentLat = startLat
        var currentLon = startLon
        
        // 40개 포인트로 자연스러운 산책 경로 생성
        // 여러 방향으로 이동하는 패스 형태
        val stepSize = 0.0015 // 약 150m 간격
        
        // 경로 패턴: 북쪽 → 동쪽 → 남쪽 → 서쪽 → 북동쪽 → 남서쪽 (곡선 포함)
        val pathPattern = listOf(
            // 1-10: 북쪽으로 직진
            { _: Int -> Pair(stepSize * 0.8, 0.0) },
            // 11-15: 동쪽으로 전환 (곡선)
            { _: Int -> Pair(stepSize * 0.6, stepSize * 0.4) },
            // 16-20: 동쪽으로 직진
            { _: Int -> Pair(stepSize * 0.5, stepSize * 0.7) },
            // 21-25: 남동쪽으로 전환
            { _: Int -> Pair(stepSize * 0.3, stepSize * 0.6) },
            // 26-30: 남쪽으로 직진
            { _: Int -> Pair(-stepSize * 0.4, stepSize * 0.3) },
            // 31-35: 서쪽으로 전환
            { _: Int -> Pair(-stepSize * 0.5, -stepSize * 0.2) },
            // 36-40: 북서쪽으로 복귀
            { _: Int -> Pair(stepSize * 0.4, -stepSize * 0.5) },
        )
        
        for (i in 0 until 40) {
            val patternIndex = when {
                i < 10 -> 0
                i < 15 -> 1
                i < 20 -> 2
                i < 25 -> 3
                i < 30 -> 4
                i < 35 -> 5
                else -> 6
            }
            
            val (deltaLat, deltaLon) = pathPattern[patternIndex](i)
            
            // 약간의 랜덤성 추가 (자연스러운 GPS 오차 시뮬레이션)
            val randomOffset = 0.0001 // 약 10m 오차
            val latOffset = (Math.random() - 0.5) * randomOffset
            val lonOffset = (Math.random() - 0.5) * randomOffset
            
            currentLat += deltaLat + latOffset
            currentLon += deltaLon + lonOffset
            
            locations.add(
                LocationPoint(
                    latitude = currentLat,
                    longitude = currentLon,
                    timestamp = centerTime + (i * 10000L), // 10초 간격
                    accuracy = 10f, // 테스트용 정확도
                )
            )
        }
        
        return locations
    }

    /**
     * [FollowerRecentActivity] 목록을 받아 현재 기기 위치 주변에 가짜 친구 핀을 생성한다.
     *
     * 각 활동마다 [centerLat], [centerLon] 기준 [radiusKm] km 반경 내
     * 임의 위치에 [FollowerMapRecord]를 배치한다.
     * 실제 API 데이터 없이 UI 개발·테스트 용도로 사용한다.
     *
     * @param activities     가짜 핀으로 변환할 팔로워 활동 목록
     * @param centerLat      기준 위도 (현재 기기 위치)
     * @param centerLon      기준 경도 (현재 기기 위치)
     * @param radiusKm       핀을 배치할 최대 반경 (기본 1.0 km)
     */
    fun generateFakeFollowerMapRecords(
        activities: List<FollowerRecentActivity>,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double = 1.0,
    ): List<FollowerMapRecord> {
        // 위도 1도 ≈ 111 km
        val radiusDeg = radiusKm / 111.0
        // 경도는 위도에 따라 보정
        val lonScale = cos(Math.toRadians(centerLat))

        return activities.mapIndexed { index, activity ->
            // 극좌표 → 직교좌표로 균등 원형 분포 생성
            val angle = (index.toDouble() / activities.size) * 2 * PI +
                (Math.random() - 0.5) * (PI / activities.size)  // 약간의 무작위 섞기
            val distance = sqrt(Math.random()) * radiusDeg       // 반경 내 균등 분포

            val lat = centerLat + distance * sin(angle)
            val lon = centerLon + (distance * cos(angle)) / lonScale

            FollowerMapRecord(
                userId = activity.userId,
                walkId = activity.userId * 1000L + index,   // 가짜 walkId
                latitude = lat,
                longitude = lon,
                grade = activity.grade,
                headImageName = activity.headImageName,
                bodyImageName = activity.bodyImageName,
            )
        }
    }
}
