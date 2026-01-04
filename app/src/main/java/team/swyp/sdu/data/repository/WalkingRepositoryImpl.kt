package team.swyp.sdu.data.repository

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.domain.calculator.DistanceCalculator
import team.swyp.sdu.domain.contract.WalkingRawEvent
import team.swyp.sdu.domain.contract.WalkingTrackingContract
import team.swyp.sdu.domain.estimator.StepEstimator
import team.swyp.sdu.domain.model.StepValidationInput
import team.swyp.sdu.domain.model.StepValidationResult
import team.swyp.sdu.domain.movement.MovementStateStabilizer
import team.swyp.sdu.domain.service.AccelerometerManager
import team.swyp.sdu.domain.service.ActivityRecognitionManager
import team.swyp.sdu.domain.service.ActivityState
import team.swyp.sdu.domain.service.LocationTrackingService
import team.swyp.sdu.domain.service.StepCounterManager
import team.swyp.sdu.domain.validator.StepCountValidator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 산책 추적 Repository 구현체
 *
 * WalkingTrackingContract를 구현하여 Service binding을 숨기고
 * Domain Layer에 Contract 인터페이스만 노출합니다.
 */
@Singleton
class WalkingRepositoryImpl @Inject constructor(
    private val application: Application,
    private val stepCounterManager: StepCounterManager,
    private val activityRecognitionManager: ActivityRecognitionManager,
    private val accelerometerManager: AccelerometerManager,
    private val stepCountValidator: StepCountValidator,
    private val movementStateStabilizer: MovementStateStabilizer,
    private val stepEstimator: StepEstimator,
    private val distanceCalculator: DistanceCalculator,
) : WalkingTrackingContract {

    // Service 인스턴스 (lazy 초기화)
    private var locationTrackingService: LocationTrackingService? = null

    // Repository용 CoroutineScope
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Contract 상태 관리
    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // Repository 자체에서 raw events 관리 (Service의 이벤트를 수집하여 emit)
    private val _rawEvents = MutableSharedFlow<WalkingRawEvent>(replay = 1)
    override val rawEvents: SharedFlow<WalkingRawEvent> = _rawEvents.asSharedFlow()
    
    // 위치 업데이트를 수신하기 위한 BroadcastReceiver
    private var locationReceiver: BroadcastReceiver? = null

    // Business logic state
    private val locationPoints = mutableListOf<LocationPoint>()
    private var lastActivityState: ActivityState? = null
    private var lastActivityChangeTime: Long = 0L
    private var currentMovementState: team.swyp.sdu.domain.service.MovementState? = null
    private var lastAcceleration: Float = 0f
    private var lastStepCount: Int = 0
    private var lastRawStepCount: Int = 0
    private var stepOffset: Int = 0
    private var pausedStepBase: Int = 0

    override suspend fun startTracking() {
        try {
            if (!stepCounterManager.isStepCounterAvailable()) {
                throw IllegalStateException("걸음 수 센서를 사용할 수 없습니다")
            }

            // Service 시작
            startLocationTrackingService()

            // 센서 관리자들 시작 및 이벤트 리스닝
            setupSensorListeners()

            _isTracking.value = true
            
            // Raw event emit
            _rawEvents.emit(WalkingRawEvent.TrackingStarted)

            Timber.d("산책 추적 시작")
        } catch (t: Throwable) {
            Timber.e(t, "산책 추적 시작 실패")
            throw t
        }
    }

    /**
     * 센서 이벤트 리스너 설정
     */
    private fun setupSensorListeners() {
        // 상태 초기화
        locationPoints.clear()
        lastActivityState = null
        lastActivityChangeTime = 0L
        currentMovementState = null
        lastAcceleration = 0f
        lastStepCount = 0
        lastRawStepCount = 0
        stepOffset = 0
        pausedStepBase = 0

        // Domain 클래스 초기화
        movementStateStabilizer.reset()
        stepEstimator.reset()
        distanceCalculator.reset()

        // 센서 관리자들 시작
        stepCounterManager.startTracking()
        activityRecognitionManager.startTracking()
        accelerometerManager.startTracking()

        // Repository scope에서 센서 이벤트 collect
        serviceScope.launch {
            // 걸음 수 센서 리스너
            stepCounterManager.getStepCountUpdates().collect { rawStepCount ->
                handleStepCountUpdate(rawStepCount)
            }
        }

        serviceScope.launch {
            // 활동 인식 리스너
            activityRecognitionManager.getActivityUpdates().collect { activityState ->
                handleActivityStateChange(activityState)
            }
        }

        serviceScope.launch {
            // 가속도계 리스너
            accelerometerManager.getMovementUpdates().collect { detection ->
                handleAccelerometerUpdate(detection.acceleration, detection.state)
            }
        }

        // 위치 업데이트를 BroadcastReceiver로 수신
        registerLocationReceiver()
    }

    override suspend fun stopTracking() {
        try {
            // Service 중지 (Intent를 통해 중지 신호 전송)
            stopLocationTrackingService()

            // 센서 관리자들 중지
            stepCounterManager.stopTracking()
            activityRecognitionManager.stopTracking()
            accelerometerManager.stopTracking()

            // BroadcastReceiver 해제
            unregisterLocationReceiver()

            _isTracking.value = false

            // Raw event emit
            _rawEvents.emit(WalkingRawEvent.TrackingStopped)

            Timber.d("산책 추적 중지")
        } catch (t: Throwable) {
            Timber.e(t, "산책 추적 중지 실패")
            throw t
        }
    }

    /**
     * 활동 상태 변경을 Service에 알림
     * Service에서 자동으로 이벤트를 emit합니다.
     */
    private fun notifyActivityStateChange(activityType: team.swyp.sdu.domain.service.ActivityType, confidence: Int) {
        // Service에 활동 상태 변경을 알림 (Broadcast Intent 사용)
        val intent = Intent(application, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_ACTIVITY_UPDATE
            putExtra(LocationTrackingService.EXTRA_ACTIVITY_TYPE, activityType.ordinal)
        }
        application.sendBroadcast(intent)
    }

    /**
     * 걸음 수 업데이트 처리 (비즈니스 로직 포함)
     */
    private suspend fun handleStepCountUpdate(rawStepCount: Int) {
        lastRawStepCount = rawStepCount
        val effectiveStepCount = (rawStepCount - stepOffset).coerceAtLeast(0)

        // 초기 걸음 수 설정
        val previousStepCount = lastStepCount
        if (distanceCalculator.getAverageStepLength() == null && effectiveStepCount > 0) {
            distanceCalculator.initialize(effectiveStepCount)
        }

        // StepEstimator에 실제 걸음 수 업데이트 알림
        stepEstimator.onRealStepUpdated(effectiveStepCount, System.currentTimeMillis())

        // 걸음 수 검증
        val stepDelta = effectiveStepCount - previousStepCount
        lastStepCount = effectiveStepCount
        val gpsDistance = distanceCalculator.calculateGpsDistance(locationPoints)
        val gpsSpeed = distanceCalculator.calculateSpeed(locationPoints)
        val validationInput = StepValidationInput(
            stepDelta = stepDelta,
            activityType = lastActivityState?.type,
            movementState = currentMovementState,
            gpsDistance = gpsDistance,
            gpsSpeed = gpsSpeed,
            acceleration = lastAcceleration,
            locations = locationPoints,
        )
        val validationResult = stepCountValidator.validate(validationInput)

        // 검증 통과 시에만 걸음 수 업데이트
        val validatedStepCount = when (validationResult) {
            is StepValidationResult.Accepted -> {
                Timber.tag("Walking Validator").w("정상 작동")
                effectiveStepCount
            }
            is StepValidationResult.Rejected.StationaryWalking -> {
                Timber.tag("Walking Validator").w("제자리 걷기 감지: 걸음수 증가 차단")
                lastStepCount
            }
            else -> {
                Timber.tag("Walking Validator").w("걸음 수 검증 실패: $validationResult")
                lastStepCount
            }
        }

        // Raw event emit - Repository의 _rawEvents로 직접 emit
        _rawEvents.emit(WalkingRawEvent.StepCountUpdate(validatedStepCount, validationResult))
        
        // Service에도 알림 (Service가 시작된 경우)
        locationTrackingService?.onStepCountUpdate(validatedStepCount)
    }

    /**
     * 활동 상태 변경 처리
     */
    private fun handleActivityStateChange(activityState: ActivityState) {
        val currentTime = System.currentTimeMillis()

        // 현재 활동 상태 업데이트
        lastActivityState = activityState
        lastActivityChangeTime = currentTime

        // Service에 활동 상태 변경 알림
        notifyActivityStateChange(activityState.type, activityState.confidence)
        
        // Raw event emit
        serviceScope.launch {
            _rawEvents.emit(WalkingRawEvent.ActivityStateChange(activityState.type, activityState.confidence))
        }

        Timber.d("활동 상태 변경: ${activityState.type.name}, 신뢰도: ${activityState.confidence}%")
    }

    /**
     * 가속도계 업데이트 처리
     */
    private fun handleAccelerometerUpdate(acceleration: Float, movementState: team.swyp.sdu.domain.service.MovementState) {
        currentMovementState = movementState
        lastAcceleration = acceleration

        // MovementStateStabilizer를 사용하여 상태 스무딩
        val stableState = movementStateStabilizer.update(movementState, System.currentTimeMillis())

        // Raw event emit
        serviceScope.launch {
            _rawEvents.emit(WalkingRawEvent.AccelerometerUpdate(acceleration, stableState))
        }

        // Service에 가속도계 업데이트 알림
        serviceScope.launch {
            locationTrackingService?.onAccelerometerUpdate(acceleration, stableState)
        }
    }

    /**
     * 위치 업데이트 처리
     */
    private fun handleLocationUpdate(locations: List<LocationPoint>) {
        // 새로운 위치 포인트만 추가 (중복 제거)
        locations.forEach { newPoint ->
            val exists = locationPoints.any { existing ->
                existing.timestamp == newPoint.timestamp ||
                (abs(existing.latitude - newPoint.latitude) < 0.000001 &&
                abs(existing.longitude - newPoint.longitude) < 0.000001)
            }

            if (!exists) {
                locationPoints.add(newPoint)
            }
        }

        Timber.d("위치 업데이트: ${locations.size}개 포인트 추가, 총 ${locationPoints.size}개 포인트")
        
        // Raw event emit
        serviceScope.launch {
            _rawEvents.emit(WalkingRawEvent.LocationUpdate(locations))
        }
    }
    
    /**
     * 위치 업데이트 BroadcastReceiver 등록
     */
    private fun registerLocationReceiver() {
        if (locationReceiver != null) {
            return
        }
        
        locationReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LocationTrackingService.ACTION_LOCATION_DATA) {
                    try {
                        val locationsJson = intent.getStringExtra(LocationTrackingService.EXTRA_LOCATIONS)
                        if (locationsJson != null) {
                            val locations = Json.decodeFromString<List<LocationPoint>>(locationsJson)
                            handleLocationUpdate(locations)
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "위치 데이터 파싱 실패")
                    }
                }
            }
        }
        
        val filter = IntentFilter(LocationTrackingService.ACTION_LOCATION_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            application.registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(locationReceiver, filter)
        }
        
        Timber.d("위치 업데이트 BroadcastReceiver 등록")
    }
    
    /**
     * 위치 업데이트 BroadcastReceiver 해제
     */
    private fun unregisterLocationReceiver() {
        locationReceiver?.let {
            try {
                application.unregisterReceiver(it)
                Timber.d("위치 업데이트 BroadcastReceiver 해제")
            } catch (t: Throwable) {
                Timber.e(t, "BroadcastReceiver 해제 실패")
            }
        }
        locationReceiver = null
    }


    override suspend fun pauseTracking() {
        try {
            pausedStepBase = lastRawStepCount
            locationTrackingService?.pauseTracking()
            
            // Raw event emit
            _rawEvents.emit(WalkingRawEvent.TrackingPaused)
            
            Timber.d("산책 추적 일시정지")
        } catch (t: Throwable) {
            Timber.e(t, "산책 추적 일시정지 실패")
            throw t
        }
    }

    override suspend fun resumeTracking() {
        try {
            val pausedDelta = (lastRawStepCount - pausedStepBase).coerceAtLeast(0)
            stepOffset += pausedDelta
            pausedStepBase = lastRawStepCount
            locationTrackingService?.resumeTracking()
            
            // Raw event emit
            _rawEvents.emit(WalkingRawEvent.TrackingResumed)
            
            Timber.d("산책 추적 재개")
        } catch (t: Throwable) {
            Timber.e(t, "산책 추적 재개 실패")
            throw t
        }
    }

    override fun isStepCounterAvailable(): Boolean {
        return stepCounterManager.isStepCounterAvailable()
    }

    override fun isActivityRecognitionAvailable(): Boolean {
        return activityRecognitionManager.isActivityRecognitionAvailable()
    }

    override fun isAccelerometerAvailable(): Boolean {
        return accelerometerManager.isAccelerometerAvailable()
    }


    /**
     * Location Tracking Service 시작
     * Service binding을 내부에서 처리하여 숨깁니다.
     */
    private fun startLocationTrackingService() {
        val intent = Intent(application, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }

        // Service 참조를 저장 (실제로는 Service Connection을 통해 받아와야 하지만,
        // 현재 구조에서는 이렇게 처리)
        // TODO: Service Connection을 통해 안전하게 Service 참조를 얻어와야 함
        Timber.d("Location Tracking Service 시작됨")
    }

    /**
     * Location Tracking Service 중지
     */
    private fun stopLocationTrackingService() {
        val intent = Intent(application, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }
        application.startService(intent)
        locationTrackingService = null
        Timber.d("Location Tracking Service 중지됨")
    }
}
