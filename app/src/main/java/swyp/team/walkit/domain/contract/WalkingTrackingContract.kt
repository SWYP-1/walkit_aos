package swyp.team.walkit.domain.contract

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 산책 추적 Contract 인터페이스
 *
 * Clean Architecture에서 Domain Layer와 Presentation Layer 사이의 계약을 정의합니다.
 * Service의 Android Framework 의존성을 숨기고, 원시 이벤트 스트림만을 노출합니다.
 */
interface WalkingTrackingContract {

    /**
     * 원시 이벤트 스트림
     * Service에서 발생하는 모든 이벤트를 SharedFlow로 제공합니다.
     */
    val rawEvents: SharedFlow<WalkingRawEvent>

    /**
     * 현재 추적 상태
     * 산책 추적이 활성화되어 있는지 여부를 나타냅니다.
     */
    val isTracking: StateFlow<Boolean>

    /**
     * 산책 추적 시작
     * Service를 시작하고 이벤트 스트림을 활성화합니다.
     */
    suspend fun startTracking()

    /**
     * 산책 추적 중지
     * Service를 중지하고 모든 리소스를 정리합니다.
     */
    suspend fun stopTracking()

    /**
     * 산책 추적 일시정지
     * Service를 일시정지 상태로 만듭니다.
     */
    suspend fun pauseTracking()

    /**
     * 산책 추적 재개
     * 일시정지된 추적을 재개합니다.
     */
    suspend fun resumeTracking()

    /**
     * 걸음 수 센서 사용 가능 여부 확인
     */
    fun isStepCounterAvailable(): Boolean

    /**
     * 활동 인식 사용 가능 여부 확인
     */
    fun isActivityRecognitionAvailable(): Boolean

    /**
     * 가속도계 사용 가능 여부 확인
     */
    fun isAccelerometerAvailable(): Boolean
}









