package team.swyp.sdu.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import team.swyp.sdu.domain.service.ActivityType
import timber.log.Timber
import java.io.File
import java.util.UUID

/**
 * 활동 상태별 통계
 */
@Parcelize
@Serializable
data class ActivityStats(
    val type: ActivityType,
    val duration: Long = 0L, // 활동 시간 (밀리초)
    val distance: Float = 0f, // 활동 거리 (미터)
) : Parcelable

/**
 * 산책 세션 데이터 모델
 *
 * @param id 세션 고유 식별자 (UUID)
 * @param startTime 산책 시작 시간 (밀리초)
 * @param endTime 산책 종료 시간 (밀리초, null이면 진행 중)
 * @param stepCount 걸음 수
 * @param locations 위치 좌표 리스트
 * @param totalDistance 총 이동 거리 (미터)
 */
@Parcelize
data class WalkingSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val stepCount: Int = 0,
    val locations: List<LocationPoint> = emptyList(),  // 원본 GPS 데이터
    val filteredLocations: List<LocationPoint>? = null, // 실시간 필터링된 데이터 (nullable)
    val smoothedLocations: List<LocationPoint>? = null, // 최종 스무딩된 데이터 (nullable)
    val totalDistance: Float = 0f,
    val preWalkEmotion: EmotionType,
    val postWalkEmotion: EmotionType,
    val note: String? = null,
    val localImagePath: String? = null, // 로컬 파일 경로
    val serverImageUrl: String? = null, // 서버 URL
    val createdDate: String,
    val targetStepCount: Int = 0, // 산책 당시 설정된 목표 걸음 수
) : Parcelable {
    /**
     * 산책 시간 (초)
     */
    val duration: Long
        get() = endTime - startTime

    /**
     * 산책 시간을 포맷된 문자열로 반환 (mm:ss)
     */
    fun getFormattedDuration(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    /**
     * 거리를 포맷된 문자열로 반환 (km 또는 m)
     */
    fun getFormattedDistance(): String =
        if (totalDistance >= 1000) {
            String.format("%.2f km", totalDistance / 1000f)
        } else {
            String.format("%.0f m", totalDistance)
        }


    /**
     * 평균 보폭 계산 (미터)
     */
    fun averageStride(): Float {
        if (stepCount == 0) return 0f
        return totalDistance / stepCount
    }

    /**
     * 평균 속도 계산 (km/h)
     */
    fun averageSpeedKmh(): Float {
        val durationSeconds = duration / 1000f
        if (durationSeconds == 0f) return 0f
        val speedMs = totalDistance / durationSeconds // m/s
        return speedMs * 3.6f // km/h
    }

    /**
     * 의미 있는 GPS 이동이 있었는지 확인
     */
    fun hasMeaningfulGpsMovement(): Boolean {
        return totalDistance >= 10f // 10m 이상 이동
    }
    
    /**
     * 표시용 위치 데이터 반환 (우선순위: 스무딩 → 필터링 → 원본)
     *
     * UI 표시나 경로 그리기에 사용할 최적의 좌표 데이터 반환
     *
     * @return 표시할 LocationPoint 리스트
     */
    fun getDisplayLocations(): List<LocationPoint> {
        return when {
            smoothedLocations?.isNotEmpty() == true -> smoothedLocations!!
            filteredLocations?.isNotEmpty() == true -> filteredLocations!!
            else -> locations
        }
    }

    /**
     * GPS 필터링 적용 여부 확인
     */
    fun hasFilteredData(): Boolean {
        return filteredLocations?.isNotEmpty() == true
    }

    /**
     * 경로 스무딩 적용 여부 확인
     */
    fun hasSmoothedData(): Boolean {
        return smoothedLocations?.isNotEmpty() == true
    }

    /**
     * 이미지 로딩 전략 (스마트 Fallback)
     *
     * 우선순위:
     * 1. 로컬 파일 존재 확인 → 사용
     * 2. 로컬 파일 없음 + 서버 URL 있음 → 서버 URL 사용 (조용한 Fallback)
     * 3. 둘 다 없음 → null 반환
     *
     * @return 이미지 URI (로컬 파일 경로 또는 서버 URL)
     */
    fun getImageUri(): String? {
        // 1순위: 로컬 파일이 존재하면 사용 (빠름, 오프라인)
        if (localImagePath != null) {
            val file = File(localImagePath)
            if (file.exists() && file.length() > 0) {
                return localImagePath
            } else {
                // 로컬 파일이 없거나 손상된 경우
                // 조용히 서버 URL로 Fallback (사용자 알림 없음)
                Timber.d("로컬 이미지 파일 없음: $localImagePath → 서버 URL로 Fallback")
            }
        }

        // 2순위: 서버 URL 사용 (다중 기기 지원, 캐시 삭제 대응)
        return serverImageUrl
    }
}

/**
 * 위치 좌표 포인트
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param timestamp 시간 (밀리초)
 * @param accuracy GPS 정확도 (미터), null이면 정확도 정보 없음
 */
@Parcelize
@Serializable
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val accuracy: Float? = null,
) : Parcelable
