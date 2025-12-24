package team.swyp.sdu.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import team.swyp.sdu.MainActivity
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.domain.contract.WalkingRawEvent
import team.swyp.sdu.domain.service.ActivityType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import android.content.Intent as BroadcastIntent

/**
 * 위치 추적을 위한 Foreground Service
 *
 * 산책 중 사용자의 위치를 주기적으로 추적하여 Flow로 제공합니다.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private var activityReceiver: BroadcastReceiver? = null

    private var isTracking = false
    private val locationPoints = mutableListOf<LocationPoint>()
    private var lastSentIndex = 0 // 마지막으로 전송한 위치 인덱스
    private var currentActivityType: ActivityType? = null // 현재 활동 상태

    // 알림 업데이트용 데이터
    private var currentStepCount: Int = 0
    private var currentDistance: Float = 0f
    private var currentDuration: Long = 0L

    // 배터리 최적화 관련
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var batteryMonitoringJob: kotlinx.coroutines.Job? = null
    private var currentBatteryLevel: Int = 100
    private var isPowerSaveMode: Boolean = false

    // Contract-based architecture를 위한 SharedFlow
    private val _rawEvents = MutableSharedFlow<WalkingRawEvent>(replay = 1)
    val rawEvents: SharedFlow<WalkingRawEvent> = _rawEvents.asSharedFlow()

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1

        // 활동 상태별 업데이트 간격 (밀리초) - 기본값
        // 경로 표시를 위해 더 자주 수집 (Strava, Nike Run Club 기준)
        private const val INTERVAL_WALKING = 2000L // 걷기: 2초 (더 자주 수집)
        private const val INTERVAL_RUNNING = 1000L // 달리기: 1초 (더 자주 수집)
        private const val INTERVAL_STILL = 8000L // 정지: 8초
        private const val INTERVAL_VEHICLE = 5000L // 차량: 5초
        private const val INTERVAL_DEFAULT = 2000L // 기본: 2초

        private const val FASTEST_UPDATE_INTERVAL = 1000L // 최소 간격: 1초 (더 빠른 업데이트)

        // 거리 기반 최소 이동 거리 (미터) - 활동 상태별로 다르게 설정
        private const val MIN_DISTANCE_WALKING = 4f // 걷기: 4m 이동 시 위치 추가
        private const val MIN_DISTANCE_RUNNING = 5f // 달리기: 5m 이동 시 위치 추가
        private const val MIN_DISTANCE_STILL = 10f // 정지: 10m (거의 이동 안 함)
        private const val MIN_DISTANCE_DEFAULT = 4f // 기본: 4m

        // 배터리 최적화 임계값
        private const val BATTERY_HIGH_THRESHOLD = 50 // 50% 이상: 정상 간격
        private const val BATTERY_MEDIUM_THRESHOLD = 20 // 20-50%: 간격 1.5배
        // 20% 미만: 간격 2배

        // 배터리 모니터링 간격
        private const val BATTERY_CHECK_INTERVAL_MS = 60000L // 1분마다 체크

        const val ACTION_START_TRACKING = "team.swyp.sdu.START_TRACKING"
        const val ACTION_STOP_TRACKING = "team.swyp.sdu.STOP_TRACKING"
        const val ACTION_LOCATION_DATA = "team.swyp.sdu.LOCATION_DATA"
        const val ACTION_ACTIVITY_UPDATE = "team.swyp.sdu.ACTIVITY_UPDATE"
        const val ACTION_UPDATE_NOTIFICATION = "team.swyp.sdu.UPDATE_NOTIFICATION"
        const val EXTRA_LOCATIONS = "locations"
        const val EXTRA_ACTIVITY_TYPE = "activity_type"
        const val EXTRA_STEP_COUNT = "step_count"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_DURATION = "duration"
    }

    /**
     * 이벤트 emit 헬퍼 메서드들
     */
    private suspend fun emitEvent(event: WalkingRawEvent) {
        _rawEvents.emit(event)
    }

    private suspend fun emitTrackingStarted() {
        emitEvent(WalkingRawEvent.TrackingStarted)
    }

    private suspend fun emitTrackingStopped() {
        emitEvent(WalkingRawEvent.TrackingStopped)
    }

    private suspend fun emitTrackingPaused() {
        emitEvent(WalkingRawEvent.TrackingPaused)
    }

    private suspend fun emitTrackingResumed() {
        emitEvent(WalkingRawEvent.TrackingResumed)
    }

    private suspend fun emitLocationUpdate(locations: List<LocationPoint>) {
        emitEvent(WalkingRawEvent.LocationUpdate(locations))
    }

    private suspend fun emitStepCountUpdate(rawStepCount: Int) {
        emitEvent(WalkingRawEvent.StepCountUpdate(rawStepCount))
    }

    private suspend fun emitAccelerometerUpdate(acceleration: Float, movementState: MovementState) {
        emitEvent(WalkingRawEvent.AccelerometerUpdate(acceleration, movementState))
    }

    private suspend fun emitActivityStateChange(activityType: ActivityType, confidence: Int) {
        emitEvent(WalkingRawEvent.ActivityStateChange(activityType, confidence))
    }

    private suspend fun emitError(message: String, cause: Throwable? = null) {
        emitEvent(WalkingRawEvent.Error(message, cause))
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createNotificationChannel()
        registerActivityReceiver()

        // 초기 배터리 상태 확인
        currentBatteryLevel = getBatteryLevel()
        isPowerSaveMode = isPowerSaveModeActive()
        Timber.d("서비스 생성: 초기 배터리 $currentBatteryLevel%, 저전력 모드: $isPowerSaveMode")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterActivityReceiver()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                serviceScope.launch {
                    startTracking()
                }
            }

            ACTION_STOP_TRACKING -> {
                serviceScope.launch {
                    stopTracking()
                }
            }

            ACTION_UPDATE_NOTIFICATION -> {
                // 알림 업데이트 데이터 수신
                val stepCount = intent.getIntExtra(EXTRA_STEP_COUNT, currentStepCount)
                val distance = intent.getFloatExtra(EXTRA_DISTANCE, currentDistance)
                val duration = intent.getLongExtra(EXTRA_DURATION, currentDuration)
                updateNotification(stepCount, distance, duration)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 위치 추적 시작
     */
    suspend fun startTracking() {
        if (isTracking) {
            Timber.d("이미 위치 추적 중입니다")
            return
        }

        locationPoints.clear()
        lastSentIndex = 0
        startForeground(NOTIFICATION_ID, createNotification())
        isTracking = true

        // Contract-based architecture: 추적 시작 이벤트 emit
        emitTrackingStarted()

        // 배터리 모니터링 시작
        startBatteryMonitoring()

        locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    // 추적이 중지된 경우 위치 업데이트 무시
                    if (!isTracking) {
                        Timber.d("추적이 중지되어 위치 업데이트를 무시합니다")
                        return
                    }
                    
                    result.lastLocation?.let { location ->
                        // GPS 정확도 필터링: 정확도가 50m 이하인 경우만 사용
                        val accuracy = location.accuracy
                        if (accuracy > 0 && accuracy > 50f) {
                            Timber.w("GPS 정확도가 낮아 위치를 무시합니다: ${accuracy}m")
                            return@let
                        }

                        val point =
                            LocationPoint(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = location.time,
                                accuracy = if (accuracy > 0) accuracy else null,
                            )

                        // 활동 상태에 따른 최소 거리 필터링: 이전 위치와 일정 거리 이상 떨어진 경우만 추가
                        if (locationPoints.isNotEmpty()) {
                            val lastPoint = locationPoints.last()
                            val distance =
                                calculateDistance(
                                    lastPoint.latitude,
                                    lastPoint.longitude,
                                    point.latitude,
                                    point.longitude,
                                )

                            // 활동 상태에 따른 최소 거리 가져오기
                            val minDistance = getMinDistanceForActivity()

                            // 최소 거리 미만 이동은 GPS 노이즈로 간주하고 무시
                            if (distance < minDistance) {
//                            Timber.d("최소 거리 미만으로 위치를 무시합니다: ${distance}m (필요: ${minDistance}m, 활동: $currentActivityType)")
                                return@let
                            }
                        }

                        locationPoints.add(point)
                        Timber.d("위치 업데이트: ${point.latitude}, ${point.longitude}, 정확도: ${accuracy}m")

                        // Contract-based architecture: 위치 업데이트 이벤트 emit
                        serviceScope.launch {
                            val newLocations = listOf(point) // 새로운 위치만 emit
                            emitLocationUpdate(newLocations)
                        }

                        // 새로운 위치만 Broadcast로 전송 (전체 리스트가 아닌 새로 추가된 부분만)
                        sendNewLocationDataBroadcast()
                    }
                }
            }

        try {
            locationRequest?.let { request ->
                fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback!!,
                    android.os.Looper.getMainLooper(),
                )
            }
            Timber.d("위치 추적 시작")
        } catch (e: SecurityException) {
            Timber.e(e, "위치 권한이 없습니다")
            stopSelf()
        }
    }

    /**
     * 위치 추적 중지
     */
    suspend fun stopTracking() {
        if (!isTracking) {
            return
        }

        // 먼저 추적 상태를 false로 설정하여 새로운 위치 업데이트 차단
        isTracking = false

        // 위치 업데이트 제거
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null

        // Contract-based architecture: 추적 중지 이벤트 emit
        emitTrackingStopped()

        // 배터리 모니터링 중지
        stopBatteryMonitoring()

        // 위치 데이터를 Broadcast로 전송
        sendLocationDataBroadcast()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.d("위치 추적 중지")
    }

    /**
     * 위치 추적 일시정지
     */
    suspend fun pauseTracking() {
        if (!isTracking) {
            Timber.d("추적이 시작되지 않았습니다")
            return
        }

        // Contract-based architecture: 추적 일시정지 이벤트 emit
        emitTrackingPaused()

        Timber.d("위치 추적 일시정지")
    }

    /**
     * 위치 추적 재개
     */
    suspend fun resumeTracking() {
        if (!isTracking) {
            Timber.d("추적이 시작되지 않았습니다")
            return
        }

        // Contract-based architecture: 추적 재개 이벤트 emit
        emitTrackingResumed()

        Timber.d("위치 추적 재개")
    }

    /**
     * 걸음 수 업데이트 (외부에서 호출)
     */
    suspend fun onStepCountUpdate(rawStepCount: Int) {
        emitStepCountUpdate(rawStepCount)
    }

    /**
     * 가속도계 업데이트 (외부에서 호출)
     */
    suspend fun onAccelerometerUpdate(acceleration: Float, movementState: MovementState) {
        emitAccelerometerUpdate(acceleration, movementState)
    }

    /**
     * 추적 상태 확인
     */
    fun isCurrentlyTracking(): Boolean = isTracking

    /**
     * 새로운 위치 데이터만 Broadcast로 전송 (성능 최적화)
     * 마지막으로 전송한 위치 이후의 새로운 위치만 전송합니다.
     */
    private fun sendNewLocationDataBroadcast() {
        try {
            // 새로운 위치가 없으면 전송하지 않음
            if (lastSentIndex >= locationPoints.size) {
                return
            }

            // 마지막 전송 이후의 새로운 위치만 추출
            val newLocations = locationPoints.subList(lastSentIndex, locationPoints.size)

            if (newLocations.isEmpty()) {
                return
            }

            val locationsJson = Json.encodeToString(newLocations)
            val intent =
                BroadcastIntent(ACTION_LOCATION_DATA).apply {
                    putExtra(EXTRA_LOCATIONS, locationsJson)
                    setPackage(packageName) // 명시적으로 패키지 설정
                }
            sendBroadcast(intent)

            // 전송한 인덱스 업데이트
            lastSentIndex = locationPoints.size

            Timber.d("새 위치 데이터 Broadcast 전송: ${newLocations.size}개 포인트 (총 ${locationPoints.size}개)")
        } catch (e: Exception) {
            Timber.e(e, "위치 데이터 Broadcast 전송 실패")
        }
    }

    /**
     * 수집된 모든 위치 데이터를 Broadcast로 전송 (산책 종료 시 사용)
     */
    private fun sendLocationDataBroadcast() {
        try {
            val locationsJson = Json.encodeToString(locationPoints)
            val intent =
                BroadcastIntent(ACTION_LOCATION_DATA).apply {
                    putExtra(EXTRA_LOCATIONS, locationsJson)
                    setPackage(packageName) // 명시적으로 패키지 설정
                }
            sendBroadcast(intent)
            Timber.d("전체 위치 데이터 Broadcast 전송: ${locationPoints.size}개 포인트")
        } catch (e: Exception) {
            Timber.e(e, "위치 데이터 Broadcast 전송 실패")
        }
    }

    /**
     * 수집된 위치 포인트 리스트 반환
     */
    fun getLocationPoints(): List<LocationPoint> = locationPoints.toList()

    /**
     * 두 지점 간 거리 계산 (Haversine 공식) - 내부 필터링용
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Float {
        val earthRadius = 6371000.0 // 지구 반지름 (미터)

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    /**
     * Activity 상태 변경 수신을 위한 BroadcastReceiver 등록
     */
    private fun registerActivityReceiver() {
        activityReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    when (intent?.action) {
                        ACTION_ACTIVITY_UPDATE -> {
                            val activityTypeOrdinal = intent.getIntExtra(EXTRA_ACTIVITY_TYPE, -1)
                            if (activityTypeOrdinal >= 0) {
                                val newActivityType = ActivityType.values().getOrNull(activityTypeOrdinal)
                                if (newActivityType != null && newActivityType != currentActivityType) {
                                    currentActivityType = newActivityType
                                    Timber.d("활동 상태 변경 수신: $newActivityType - LocationRequest 즉시 업데이트")

                                    // Contract-based architecture: 활동 상태 변경 이벤트 emit
                                    serviceScope.launch {
                                        emitActivityStateChange(newActivityType, 100) // confidence 값은 임시로 100
                                    }

                                    // 즉시 LocationRequest 업데이트 (지연 해결)
                                    updateLocationRequest()
                                }
                            }
                        }

                        ACTION_UPDATE_NOTIFICATION -> {
                            // 알림 업데이트 데이터 수신
                            val stepCount = intent.getIntExtra(EXTRA_STEP_COUNT, currentStepCount)
                            val distance = intent.getFloatExtra(EXTRA_DISTANCE, currentDistance)
                            val duration = intent.getLongExtra(EXTRA_DURATION, currentDuration)
                            updateNotification(stepCount, distance, duration)
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(ACTION_ACTIVITY_UPDATE)
                addAction(ACTION_UPDATE_NOTIFICATION)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(activityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(activityReceiver, filter)
        }
    }

    private fun unregisterActivityReceiver() {
        activityReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.e(e, "Activity Receiver 해제 실패")
            }
        }
        activityReceiver = null
    }

    /**
     * 활동 상태에 따른 LocationRequest 업데이트 (배터리 상태 고려)
     */
    private fun updateLocationRequest() {
        if (!isTracking || locationCallback == null) return

        val updateInterval = getUpdateIntervalForActivity()
        val priority = getPriorityForBatteryAndActivity() // 배터리 상태 고려

        val newLocationRequest =
            LocationRequest
                .Builder(priority, updateInterval)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setMaxUpdateDelayMillis(updateInterval * 2)
                .setWaitForAccurateLocation(false) // 빠른 응답을 위해 false
                .build()

        // 기존 업데이트 제거 후 새로 등록
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            try {
                fusedLocationClient.requestLocationUpdates(
                    newLocationRequest,
                    callback,
                    android.os.Looper.getMainLooper(),
                )
                locationRequest = newLocationRequest
                Timber.d(
                    "LocationRequest 업데이트 완료: 간격=${updateInterval}ms, 활동=$currentActivityType, 배터리=$currentBatteryLevel%, 저전력모드=$isPowerSaveMode, 우선순위=$priority",
                )
            } catch (e: SecurityException) {
                Timber.e(e, "LocationRequest 업데이트 실패")
            }
        }
    }

    /**
     * 활동 상태에 따른 업데이트 간격 반환 (배터리 상태 고려)
     */
    private fun getUpdateIntervalForActivity(): Long {
        val baseInterval =
            when (currentActivityType) {
                ActivityType.WALKING -> INTERVAL_WALKING
                ActivityType.RUNNING -> INTERVAL_RUNNING
                ActivityType.STILL -> INTERVAL_STILL
                ActivityType.IN_VEHICLE -> INTERVAL_VEHICLE
                ActivityType.ON_BICYCLE -> INTERVAL_VEHICLE
                else -> INTERVAL_DEFAULT
            }

        // 배터리 상태에 따른 간격 조정
        return adjustIntervalForBattery(baseInterval)
    }

    /**
     * 활동 상태에 따른 최소 이동 거리 반환
     * 경로 표시를 위해 적절한 간격으로 위치 포인트를 수집합니다.
     */
    private fun getMinDistanceForActivity(): Float =
        when (currentActivityType) {
            ActivityType.WALKING -> MIN_DISTANCE_WALKING

            // 걷기: 4m마다 위치 추가
            ActivityType.RUNNING -> MIN_DISTANCE_RUNNING

            // 달리기: 5m마다 위치 추가
            ActivityType.STILL -> MIN_DISTANCE_STILL

            // 정지: 10m (거의 이동 안 함)
            else -> MIN_DISTANCE_DEFAULT // 기본: 4m
        }

    /**
     * 배터리 상태에 따른 업데이트 간격 조정
     */
    private fun adjustIntervalForBattery(baseInterval: Long): Long {
        var adjustedInterval = baseInterval

        // 저전력 모드 체크
        if (isPowerSaveMode) {
            adjustedInterval = (adjustedInterval * 2.0).toLong()
            Timber.d("저전력 모드 감지: 간격 2배 증가 (${adjustedInterval}ms)")
        }

        // 배터리 레벨에 따른 조정
        when {
            currentBatteryLevel < 20 -> {
                // 배터리 20% 미만: 간격 2배 증가
                adjustedInterval = (adjustedInterval * 2.0).toLong()
                Timber.d("배터리 부족 ($currentBatteryLevel%): 간격 2배 증가 (${adjustedInterval}ms)")
            }

            currentBatteryLevel < BATTERY_MEDIUM_THRESHOLD -> {
                // 배터리 20-50%: 간격 1.5배 증가
                adjustedInterval = (adjustedInterval * 1.5).toLong()
                Timber.d("배터리 보통 ($currentBatteryLevel%): 간격 1.5배 증가 (${adjustedInterval}ms)")
            }
            // 배터리 50% 이상: 기본 간격 유지
        }

        return adjustedInterval
    }

    /**
     * GPS 우선순위 반환 (배터리 상태 고려)
     * LocationRequest.Builder는 Int를 기대하므로 Priority enum의 값을 Int로 변환합니다.
     * Priority enum 값: PRIORITY_HIGH_ACCURACY = 100, PRIORITY_BALANCED_POWER_ACCURACY = 102
     */
    private fun getPriorityForBatteryAndActivity(): Int {
        // 저전력 모드이거나 배터리가 20% 미만이면 BALANCED_POWER_ACCURACY 사용
        if (isPowerSaveMode || currentBatteryLevel < 20) {
            return 102 // Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        // 배터리가 20-50%이고 정지 상태면 BALANCED_POWER_ACCURACY
        if (currentBatteryLevel < BATTERY_MEDIUM_THRESHOLD &&
            currentActivityType == ActivityType.STILL
        ) {
            return 102 // Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        // 그 외에는 HIGH_ACCURACY
        return 100 // Priority.PRIORITY_HIGH_ACCURACY
    }

    /**
     * 배터리 레벨 가져오기
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * 저전력 모드 확인
     */
    private fun isPowerSaveModeActive(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }

    /**
     * 배터리 모니터링 시작
     */
    private fun startBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob =
            serviceScope.launch {
                while (isTracking) {
                    try {
                        // 배터리 레벨 업데이트
                        val newBatteryLevel = getBatteryLevel()
                        val batteryChanged = newBatteryLevel != currentBatteryLevel

                        // 저전력 모드 상태 업데이트
                        val newPowerSaveMode = isPowerSaveModeActive()
                        val powerSaveModeChanged = newPowerSaveMode != isPowerSaveMode

                        if (batteryChanged || powerSaveModeChanged) {
                            currentBatteryLevel = newBatteryLevel
                            isPowerSaveMode = newPowerSaveMode

                            Timber.d("배터리 상태 업데이트: $currentBatteryLevel%, 저전력 모드: $isPowerSaveMode")

                            // 배터리 상태 변경 시 LocationRequest 업데이트
                            if (isTracking) {
                                updateLocationRequest()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "배터리 모니터링 오류")
                    }

                    delay(BATTERY_CHECK_INTERVAL_MS)
                }
            }
    }

    /**
     * 배터리 모니터링 중지
     */
    private fun stopBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob = null
    }

    /**
     * LocationRequest 생성 (초기값)
     */
    private fun createLocationRequest() {
        locationRequest =
            LocationRequest
                .Builder(
                    100, // Priority.PRIORITY_HIGH_ACCURACY
                    INTERVAL_DEFAULT,
                ).setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setMaxUpdateDelayMillis(INTERVAL_DEFAULT * 2)
                .build()
    }

    /**
     * Notification Channel 생성
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "위치 추적",
                    NotificationManager.IMPORTANCE_LOW, // 진동 없이 조용히 표시
                ).apply {
                    description = "산책 중 위치 추적 알림"
                    setShowBadge(false) // 배지 표시 안 함
                    enableVibration(false) // 진동 비활성화
                    enableLights(false) // LED 비활성화
                    setSound(null, null) // 사운드 비활성화
                    vibrationPattern = longArrayOf(0) // 진동 패턴 없음
                }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Foreground Service용 Notification 생성
     */
    private fun createNotification(): Notification =
        createNotificationWithData(currentStepCount, currentDistance, currentDuration)

    /**
     * 데이터를 포함한 Notification 생성
     */
    private fun createNotificationWithData(
        stepCount: Int,
        distance: Float,
        duration: Long,
    ): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        // 시간 포맷팅 (초 -> 시:분:초)
        val hours = duration / 3600000
        val minutes = (duration % 3600000) / 60000
        val seconds = (duration % 60000) / 1000
        val timeText =
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }

        // 거리 포맷팅 (m -> km)
        val distanceText =
            if (distance >= 1000f) {
                String.format("%.2f km", distance / 1000f)
            } else {
                String.format("%.0f m", distance)
            }

        // 알림 내용 구성
        val contentText =
            if (stepCount > 0 || distance > 0f || duration > 0L) {
                "$stepCount 걸음 • $distanceText • $timeText"
            } else {
                "산책중입니다"
            }

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("산책중입니다")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // PRIORITY_LOW는 알림이 표시되지 않을 수 있음
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVibrate(longArrayOf(0)) // 진동 비활성화
            .setDefaults(0) // 기본 사운드, 진동, LED 비활성화
            .setSilent(true) // 사운드 비활성화
            .build()
    }

    /**
     * 알림 업데이트
     * 내용이 실제로 변경될 때만 업데이트하여 불필요한 알림 업데이트를 방지합니다.
     */
    private fun updateNotification(
        stepCount: Int,
        distance: Float,
        duration: Long,
    ) {
        // 내용 변경 여부와 상관없이 추가 알림 업데이트를 하지 않음 (진동 완전 차단)
        // Foreground 서비스 유지용으로 onStartCommand의 startForeground 알림만 사용
        return
    }
}
