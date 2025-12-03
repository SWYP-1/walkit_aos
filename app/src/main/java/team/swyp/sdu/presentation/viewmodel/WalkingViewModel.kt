package team.swyp.sdu.presentation.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.model.ActivityStats
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.service.AccelerometerManager
import team.swyp.sdu.domain.service.ActivityRecognitionManager
import team.swyp.sdu.domain.service.ActivityState
import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.domain.service.LocationTrackingService
import team.swyp.sdu.domain.service.MovementState
import team.swyp.sdu.domain.service.StepCounterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * 산책 상태를 관리하는 ViewModel
 */
@HiltViewModel
class WalkingViewModel
    @Inject
    constructor(
        application: Application,
        private val stepCounterManager: StepCounterManager,
        private val activityRecognitionManager: ActivityRecognitionManager,
        private val accelerometerManager: AccelerometerManager,
        private val walkingSessionRepository: WalkingSessionRepository,
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow<WalkingUiState>(WalkingUiState.Initial)
        val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()

        private var currentSession: WalkingSession? = null
        private val locationPoints = mutableListOf<LocationPoint>()
        private var stepCountJob: Job? = null
        private var locationJob: Job? = null
        private var durationUpdateJob: Job? = null
        private var activityJob: Job? = null
        private var accelerometerJob: Job? = null
        private var locationReceiver: BroadcastReceiver? = null

        // 활동 상태 추적 관련
        private val activityStatsList = mutableListOf<ActivityStats>()
        private var lastActivityState: ActivityState? = null
        private var lastActivityChangeTime: Long = 0L
        private var lastLocationForActivity: LocationPoint? = null

        // 가속도계 기반 즉각 피드백
        private var currentMovementState: MovementState? = null
        private var pendingMovementState: MovementState? = null // 대기 중인 상태
        private var movementStateChangeTime: Long = 0L // 상태 변화 시간
        private var stableMovementState: MovementState? = null // 안정화된 상태 (UI에 표시)

        // 상태 변화 스무딩을 위한 상수 (밀리초)
        private companion object {
            const val MOVEMENT_STATE_STABLE_DURATION = 3000L // 3초 동안 같은 상태 유지해야 변경 (1.5초 -> 3초로 증가)
        }

        // 하이브리드 거리 계산을 위한 데이터
        private var initialStepCount: Int = 0
        private var lastStepCount: Int = 0
        private var lastGpsDistance: Float = 0f
        private var averageStepLength: Float? = null // 평균 보폭 (미터)

        // 가속도계 기반 실시간 걸음 수 보간
        private var lastRealStepCount: Int = 0 // 마지막 실제 걸음 수
        private var lastRealStepCountTime: Long = 0L // 마지막 실제 걸음 수 업데이트 시간
        private var interpolatedStepCount: Int = 0 // 보간된 걸음 수
        private var estimatedStepsPerSecond: Float = 0f // 예상 초당 걸음 수
        private var lastAcceleration: Float = 0f // 마지막 가속도
        private var movementStartTime: Long = 0L // 움직임 시작 시간

        /**
         * 산책 시작
         */
        fun startWalking() {
            if (!stepCounterManager.isStepCounterAvailable()) {
                _uiState.value = WalkingUiState.Error("걸음 수 센서를 사용할 수 없습니다")
                return
            }

            val startTime = System.currentTimeMillis()
            currentSession = WalkingSession(startTime = startTime)
            locationPoints.clear()

            stepCounterManager.startTracking()
            startLocationTracking()
            startActivityTracking()
            startAccelerometerTracking()

            // 하이브리드 거리 계산 초기화
            initialStepCount = 0
            lastStepCount = 0
            lastGpsDistance = 0f
            averageStepLength = null

            // 가속도계 기반 실시간 걸음 수 보간 초기화
            lastRealStepCount = 0
            lastRealStepCountTime = System.currentTimeMillis()
            interpolatedStepCount = 0
            estimatedStepsPerSecond = 0f
            lastAcceleration = 0f
            movementStartTime = 0L

            // 상태 스무딩 초기화
            currentMovementState = null
            pendingMovementState = null
            movementStateChangeTime = 0L
            stableMovementState = null

            _uiState.value =
                WalkingUiState.Walking(
                    stepCount = 0,
                    duration = 0L,
                    distance = 0f,
                    currentActivity = null,
                    currentMovementState = null,
                    currentSpeed = 0f,
                    debugInfo = null,
                )

            // 걸음 수 업데이트 수신 - 하이브리드 거리 계산 사용
            stepCountJob =
                stepCounterManager
                    .getStepCountUpdates()
                    .onEach { realStepCount ->
                        val state = _uiState.value
                        if (state is WalkingUiState.Walking) {
                            // 초기 걸음 수 설정
                            if (initialStepCount == 0 && realStepCount > 0) {
                                initialStepCount = realStepCount
                            }
                            lastStepCount = realStepCount

                            // 가속도계 기반 보간 업데이트 (실제 걸음 수가 업데이트됨)
                            updateInterpolatedStepCount(realStepCount)

                            // 보간된 걸음 수를 UI에 표시 (부드러운 전환)
                            val displayStepCount = interpolatedStepCount

                            // 하이브리드 거리 계산 (GPS + Step Counter) - 실제 걸음 수 사용
                            val totalDistance = calculateHybridDistance(locationPoints, realStepCount)

                            // GPS 속도 계산
                            val gpsSpeed = calculateGpsSpeed(locationPoints)

                            // 디버그 정보 생성
                            val gpsDistance = calculateTotalDistance(locationPoints)
                            val stepBasedDistance = calculateStepBasedDistance(realStepCount)
                            val debugInfo =
                                WalkingUiState.DebugInfo(
                                    acceleration = lastAcceleration,
                                    stepsPerSecond = estimatedStepsPerSecond,
                                    averageStepLength = averageStepLength,
                                    realStepCount = realStepCount,
                                    interpolatedStepCount = displayStepCount,
                                    gpsDistance = gpsDistance,
                                    stepBasedDistance = stepBasedDistance,
                                    locationPointCount = locationPoints.size,
                                    lastLocation = locationPoints.lastOrNull(),
                                )

                            _uiState.value =
                                state.copy(
                                    stepCount = displayStepCount,
                                    distance = totalDistance,
                                    currentSpeed = gpsSpeed,
                                    debugInfo = debugInfo,
                                )
                            updateCurrentSession(stepCount = realStepCount)

                            // 포그라운드 알림 업데이트
                            updateForegroundNotification(displayStepCount, totalDistance, state.duration)

                            Timber.d("걸음 수 업데이트: 실제=$realStepCount, 보간=$displayStepCount")
                        }
                    }.catch { e ->
                        Timber.e(e, "걸음 수 업데이트 오류")
                    }.launchIn(viewModelScope)

            // 시간 업데이트 (1초마다) - 하이브리드 거리 계산 사용 + 보간된 걸음 수 업데이트
            durationUpdateJob =
                viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        val state = _uiState.value
                        if (state is WalkingUiState.Walking) {
                            val currentDuration = System.currentTimeMillis() - startTime

                            // 가속도계 기반 보간 업데이트 (실제 걸음 수가 업데이트되지 않았을 수도 있음)
                            updateInterpolatedStepCount(state.stepCount)

                            // 보간된 걸음 수를 UI에 표시
                            val displayStepCount = interpolatedStepCount

                            // 하이브리드 거리 계산 (GPS + Step Counter) - 실제 걸음 수 사용 (거리 계산은 정확해야 함)
                            val totalDistance = calculateHybridDistance(locationPoints, lastStepCount)

                            // GPS 속도 계산
                            val gpsSpeed = calculateGpsSpeed(locationPoints)

                            // 디버그 정보 생성
                            val gpsDistance = calculateTotalDistance(locationPoints)
                            val stepBasedDistance = calculateStepBasedDistance(lastStepCount)
                            val debugInfo =
                                WalkingUiState.DebugInfo(
                                    acceleration = lastAcceleration,
                                    stepsPerSecond = estimatedStepsPerSecond,
                                    averageStepLength = averageStepLength,
                                    realStepCount = lastStepCount,
                                    interpolatedStepCount = displayStepCount,
                                    gpsDistance = gpsDistance,
                                    stepBasedDistance = stepBasedDistance,
                                    locationPointCount = locationPoints.size,
                                    lastLocation = locationPoints.lastOrNull(),
                                )

                            _uiState.value =
                                state.copy(
                                    stepCount = displayStepCount,
                                    duration = currentDuration,
                                    distance = totalDistance,
                                    currentSpeed = gpsSpeed,
                                    debugInfo = debugInfo,
                                )

                            // 포그라운드 알림 업데이트 (1초마다)
                            updateForegroundNotification(displayStepCount, totalDistance, currentDuration)
                        } else {
                            break
                        }
                    }
                }

            Timber.d("산책 시작")
        }

        /**
         * 산책 종료
         */
        fun stopWalking() {
            val session = currentSession ?: return

            stepCounterManager.stopTracking()
            stopLocationTracking()
            accelerometerManager.stopTracking()

            // Job 취소
            stepCountJob?.cancel()
            locationJob?.cancel()
            durationUpdateJob?.cancel()
            activityJob?.cancel()
            accelerometerJob?.cancel()

            val endTime = System.currentTimeMillis()

            // 마지막 활동 상태 시간 기록
            updateActivityStatsForCurrentState(endTime)

            // BroadcastReceiver에서 수신한 위치 데이터 사용
            val locationPointsFromService = locationPoints.toList()

            // 활동 상태별 통계 계산
            val finalActivityStats = calculateFinalActivityStats(locationPointsFromService)
            val primaryActivity = findPrimaryActivity(finalActivityStats)

            val completedSession =
                session.copy(
                    endTime = endTime,
                    locations = locationPointsFromService,
                    totalDistance = calculateHybridDistance(locationPointsFromService, session.stepCount),
                    activityStats = finalActivityStats,
                    primaryActivity = primaryActivity,
                )

            // 활동 상태 추적 중지
            activityRecognitionManager.stopTracking()

            // 세션 저장 (로컬 저장 + 서버 동기화 시도)
            viewModelScope.launch {
                try {
                    walkingSessionRepository.saveSession(completedSession)
                    Timber.d("산책 세션 저장 완료: ${completedSession.stepCount}걸음, ${completedSession.getFormattedDistance()}")
                } catch (e: Exception) {
                    Timber.e(e, "산책 세션 저장 실패")
                    // 저장 실패해도 UI는 정상적으로 완료 상태로 전환
                }
            }

            currentSession = null
            locationPoints.clear()

            _uiState.value = WalkingUiState.Completed(completedSession)
            Timber.d("산책 종료: ${completedSession.stepCount}걸음, ${completedSession.getFormattedDistance()}")
        }

        /**
         * 위치 추적 시작
         */
        private fun startLocationTracking() {
            val intent =
                Intent(
                    getApplication<Application>(),
                    LocationTrackingService::class.java,
                ).apply {
                    action = LocationTrackingService.ACTION_START_TRACKING
                }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }

            // 위치 데이터를 BroadcastReceiver로 수신
            registerLocationReceiver()

            Timber.d("위치 추적 서비스 시작")
        }

        /**
         * 위치 추적 중지
         */
        private fun stopLocationTracking() {
            val intent =
                android.content
                    .Intent(
                        getApplication<Application>(),
                        LocationTrackingService::class.java,
                    ).apply {
                        action = LocationTrackingService.ACTION_STOP_TRACKING
                    }
            getApplication<Application>().startForegroundService(intent)
        }

        /**
         * 포그라운드 알림 업데이트
         */
        private fun updateForegroundNotification(
            stepCount: Int,
            distance: Float,
            duration: Long,
        ) {
            val intent =
                android.content
                    .Intent(
                        getApplication<Application>(),
                        LocationTrackingService::class.java,
                    ).apply {
                        action = LocationTrackingService.ACTION_UPDATE_NOTIFICATION
                        putExtra(LocationTrackingService.EXTRA_STEP_COUNT, stepCount)
                        putExtra(LocationTrackingService.EXTRA_DISTANCE, distance)
                        putExtra(LocationTrackingService.EXTRA_DURATION, duration)
                    }
            getApplication<Application>().startForegroundService(intent)
        }

        /**
         * 가속도계 기반 즉각 피드백 시작
         */
        private fun startAccelerometerTracking() {
            if (!accelerometerManager.isAccelerometerAvailable()) {
                Timber.w("가속도계를 사용할 수 없습니다")
                return
            }

            accelerometerManager.startTracking()

            // 가속도계 움직임 감지 업데이트 수신 (즉각 피드백 + 실시간 걸음 수 보간)
            accelerometerJob =
                accelerometerManager
                    .getMovementUpdates()
                    .onEach { detection ->
                        val state = _uiState.value
                        if (state is WalkingUiState.Walking) {
                            val currentTime = System.currentTimeMillis()
                            currentMovementState = detection.state
                            lastAcceleration = detection.acceleration

                            // 상태 변화 스무딩: 일정 시간 동안 같은 상태를 유지해야만 변경
                            val newState = detection.state
                            val currentStableState = stableMovementState ?: newState

                            if (newState != currentStableState) {
                                // 상태가 변경되었음
                                if (pendingMovementState != newState) {
                                    // 새로운 상태로 대기 시작
                                    pendingMovementState = newState
                                    movementStateChangeTime = currentTime
                                    Timber.d("움직임 상태 변경 대기: $currentStableState -> $newState")
                                } else {
                                    // 같은 상태로 계속 대기 중
                                    val elapsedTime = currentTime - movementStateChangeTime
                                    if (elapsedTime >= MOVEMENT_STATE_STABLE_DURATION) {
                                        // 충분한 시간 동안 같은 상태 유지됨 -> 상태 변경 확정
                                        stableMovementState = newState
                                        pendingMovementState = null
                                        Timber.d("움직임 상태 변경 확정: $currentStableState -> $newState (${elapsedTime}ms 대기)")
                                    }
                                }
                            } else {
                                // 상태가 변경되지 않았음 -> 대기 중인 상태 초기화
                                if (pendingMovementState != null) {
                                    pendingMovementState = null
                                    movementStateChangeTime = 0L
                                }
                            }

                            // UI에는 안정화된 상태 사용 (대기 중이면 이전 상태 유지)
                            val stateToUse = stableMovementState ?: newState

                            // 움직임 상태에 따라 예상 초당 걸음 수 계산 (안정화된 상태 사용)
                            val newEstimatedStepsPerSecond =
                                when (stateToUse) {
                                    MovementState.WALKING -> {
                                        // 걷기: 가속도에 따라 1.5 ~ 2.5 걸음/초 추정
                                        // 가속도 1.5~3.0 m/s² 범위를 1.5~2.5 걸음/초로 매핑
                                        val normalizedAccel = ((detection.acceleration - 1.5f) / 1.5f).coerceIn(0f, 1f)
                                        1.5f + normalizedAccel * 1.0f // 1.5 ~ 2.5 걸음/초
                                    }

                                    MovementState.RUNNING -> {
                                        // 달리기: 가속도에 따라 2.5 ~ 4.0 걸음/초 추정
                                        // 가속도 3.0~5.0+ m/s² 범위를 2.5~4.0 걸음/초로 매핑
                                        val normalizedAccel = ((detection.acceleration - 3.0f) / 2.0f).coerceIn(0f, 1f)
                                        2.5f + normalizedAccel * 1.5f // 2.5 ~ 4.0 걸음/초
                                    }

                                    MovementState.STILL -> {
                                        // 정지: 0 걸음/초
                                        0f
                                    }

                                    MovementState.UNKNOWN -> {
                                        // 알 수 없음: 이전 값 유지 또는 0
                                        estimatedStepsPerSecond * 0.9f // 점진적으로 감소
                                    }
                                }

                            // 움직임 시작 시간 추적 (안정화된 상태 사용)
                            if (stateToUse == MovementState.WALKING || stateToUse == MovementState.RUNNING) {
                                if (movementStartTime == 0L) {
                                    movementStartTime = System.currentTimeMillis()
                                }
                                estimatedStepsPerSecond = newEstimatedStepsPerSecond
                            } else {
                                // 정지 상태면 움직임 시작 시간 초기화
                                movementStartTime = 0L
                                estimatedStepsPerSecond = 0f
                            }

                            // 실시간 걸음 수 보간 계산
                            updateInterpolatedStepCount(state.stepCount)

                            // 보간된 걸음 수를 UI에 즉시 반영 (부드러운 전환)
                            val displayStepCount = interpolatedStepCount
                            val totalDistance = calculateHybridDistance(locationPoints, lastStepCount)

                            // GPS 속도 계산 (마지막 두 위치 포인트 간 거리와 시간 차이로 계산)
                            val estimatedSpeed = calculateGpsSpeed(locationPoints) // m/s

                            // 디버그 정보 생성
                            val gpsDistance = calculateTotalDistance(locationPoints)
                            val stepBasedDistance = calculateStepBasedDistance(lastStepCount)
                            val debugInfo =
                                WalkingUiState.DebugInfo(
                                    acceleration = detection.acceleration,
                                    stepsPerSecond = estimatedStepsPerSecond,
                                    averageStepLength = averageStepLength,
                                    realStepCount = lastStepCount,
                                    interpolatedStepCount = displayStepCount,
                                    gpsDistance = gpsDistance,
                                    stepBasedDistance = stepBasedDistance,
                                    locationPointCount = locationPoints.size,
                                    lastLocation = locationPoints.lastOrNull(),
                                )

                            _uiState.value =
                                state.copy(
                                    stepCount = displayStepCount,
                                    distance = totalDistance,
                                    currentMovementState = stateToUse, // 안정화된 상태 사용
                                    currentSpeed = estimatedSpeed,
                                    debugInfo = debugInfo,
                                )

//                    Timber.d("가속도계 움직임 감지: ${detection.state}, 가속도: ${detection.acceleration}m/s², 예상 걸음/초: $estimatedStepsPerSecond, 보간 걸음 수: $displayStepCount")
                        }
                    }.catch { e ->
                        Timber.e(e, "가속도계 업데이트 오류")
                    }.launchIn(viewModelScope)
        }

        /**
         * 활동 상태 추적 시작
         */
        private fun startActivityTracking() {
            if (!activityRecognitionManager.isActivityRecognitionAvailable()) {
                Timber.w("Activity Recognition을 사용할 수 없습니다")
                return
            }

            activityStatsList.clear()
            lastActivityState = null
            lastActivityChangeTime = System.currentTimeMillis()
            lastLocationForActivity = null

            activityRecognitionManager.startTracking()

            // 활동 상태 업데이트 수신
            activityJob =
                activityRecognitionManager
                    .getActivityUpdates()
                    .onEach { activityState ->
                        handleActivityStateChange(activityState)
                    }.catch { e ->
                        Timber.e(e, "활동 상태 업데이트 오류")
                    }.launchIn(viewModelScope)
        }

        /**
         * 활동 상태 변경 처리
         */
        private fun handleActivityStateChange(newState: ActivityState) {
            val currentTime = System.currentTimeMillis()

            // 이전 활동 상태의 시간 기록
            if (lastActivityState != null && lastActivityChangeTime > 0) {
                val duration = currentTime - lastActivityChangeTime
                updateActivityStats(lastActivityState!!.type, duration, 0f)
            }

            // 현재 활동 상태 업데이트
            lastActivityState = newState
            lastActivityChangeTime = currentTime

            // LocationTrackingService에 활동 상태 변경 즉시 전송 (지연 해결)
            val intent =
                Intent(
                    LocationTrackingService.ACTION_ACTIVITY_UPDATE,
                ).apply {
                    setPackage(getApplication<Application>().packageName)
                    putExtra(LocationTrackingService.EXTRA_ACTIVITY_TYPE, newState.type.ordinal)
                }
            getApplication<Application>().sendBroadcast(intent)
            Timber.d("활동 상태 변경 Broadcast 전송: ${newState.type.name}")

            // UI 업데이트
            val state = _uiState.value
            if (state is WalkingUiState.Walking) {
                val totalDistance = calculateHybridDistance(locationPoints, state.stepCount)

                // 디버그 정보 생성 (기존 정보 유지)
                val gpsDistance = calculateTotalDistance(locationPoints)
                val stepBasedDistance = calculateStepBasedDistance(state.stepCount)
                val debugInfo =
                    state.debugInfo?.copy(
                        gpsDistance = gpsDistance,
                        stepBasedDistance = stepBasedDistance,
                        locationPointCount = locationPoints.size,
                        lastLocation = locationPoints.lastOrNull(),
                    ) ?: WalkingUiState.DebugInfo(
                        acceleration = lastAcceleration,
                        stepsPerSecond = estimatedStepsPerSecond,
                        averageStepLength = averageStepLength,
                        realStepCount = state.stepCount,
                        interpolatedStepCount = state.stepCount,
                        gpsDistance = gpsDistance,
                        stepBasedDistance = stepBasedDistance,
                        locationPointCount = locationPoints.size,
                        lastLocation = locationPoints.lastOrNull(),
                    )

                _uiState.value =
                    state.copy(
                        currentActivity = newState.type,
                        distance = totalDistance,
                        debugInfo = debugInfo,
                    )
            }

            Timber.d("활동 상태 변경: ${newState.type.name}, 신뢰도: ${newState.confidence}%")
        }

        /**
         * 활동 상태별 통계 업데이트
         */
        private fun updateActivityStats(
            type: ActivityType,
            duration: Long,
            distance: Float,
        ) {
            val existingIndex = activityStatsList.indexOfFirst { it.type == type }
            if (existingIndex >= 0) {
                val currentStats = activityStatsList[existingIndex]
                activityStatsList[existingIndex] =
                    ActivityStats(
                        type = type,
                        duration = currentStats.duration + duration,
                        distance = currentStats.distance + distance,
                    )
            } else {
                activityStatsList.add(ActivityStats(type, duration, distance))
            }
        }

        /**
         * 현재 활동 상태의 시간 기록 (산책 종료 시 호출)
         */
        private fun updateActivityStatsForCurrentState(endTime: Long) {
            lastActivityState?.let { state ->
                val duration = endTime - lastActivityChangeTime
                updateActivityStats(state.type, duration, 0f)
            }
        }

        /**
         * 위치 데이터와 활동 상태를 매칭하여 활동 상태별 거리 계산
         */
        private fun calculateFinalActivityStats(locations: List<LocationPoint>): List<ActivityStats> {
            val finalStats = activityStatsList.toMutableList()

            // 위치 데이터를 시간순으로 정렬
            val sortedLocations = locations.sortedBy { it.timestamp }

            if (sortedLocations.isEmpty() || lastActivityState == null) {
                return finalStats
            }

            // 각 위치 포인트에 대해 활동 상태 매칭
            // 간단한 구현: 마지막 활동 상태를 사용
            // 실제로는 시간 기반으로 활동 상태를 매칭해야 함
            val lastActivity = lastActivityState!!.type
            val totalDistance = calculateTotalDistance(sortedLocations)

            // 전체 거리를 현재 활동 상태에 할당 (간단한 구현)
            // 실제로는 시간 기반으로 더 정확하게 계산해야 함
            val existingIndex = finalStats.indexOfFirst { it.type == lastActivity }
            if (existingIndex >= 0) {
                val currentStats = finalStats[existingIndex]
                finalStats[existingIndex] = currentStats.copy(distance = currentStats.distance + totalDistance)
            } else {
                finalStats.add(ActivityStats(lastActivity, 0L, totalDistance))
            }

            return finalStats
        }

        /**
         * 주요 활동 상태 찾기 (가장 오래 한 활동)
         */
        private fun findPrimaryActivity(stats: List<ActivityStats>): ActivityType? {
            if (stats.isEmpty()) return null

            return stats.maxByOrNull { it.duration }?.type
        }

        /**
         * 현재 세션 업데이트
         */
        private fun updateCurrentSession(stepCount: Int) {
            currentSession =
                currentSession?.copy(
                    stepCount = stepCount,
                    locations = locationPoints.toList(),
                    totalDistance = calculateHybridDistance(locationPoints, stepCount),
                )
        }

        /**
         * 하이브리드 거리 계산 (GPS + Step Counter)
         *
         * GPS가 정확할 때는 GPS 거리를 사용하고,
         * GPS가 부정확하거나 실내일 때는 Step Counter 기반 거리를 사용합니다.
         * 두 값을 결합하여 더 정확한 거리를 계산합니다.
         *
         * @param points GPS 위치 포인트 리스트
         * @param stepCount 현재 걸음 수
         * @return 계산된 총 거리 (미터)
         */
        private fun calculateHybridDistance(
            points: List<LocationPoint>,
            stepCount: Int,
        ): Float {
            // GPS 기반 거리 계산
            val gpsDistance = calculateTotalDistance(points)

            // Step Counter 기반 거리 계산
            val stepBasedDistance = calculateStepBasedDistance(stepCount)

            // GPS 데이터가 충분한 경우 (3개 이상 포인트)
            if (points.size >= 3) {
                // 평균 보폭 계산 및 업데이트
                val stepsTaken = stepCount - initialStepCount
                if (stepsTaken > 0 && gpsDistance > 0f) {
                    val calculatedStepLength = gpsDistance / stepsTaken
                    // 평균 보폭 업데이트 (이동 평균)
                    averageStepLength =
                        if (averageStepLength == null) {
                            calculatedStepLength
                        } else {
                            // 가중 이동 평균 (최근 값에 더 높은 가중치)
                            averageStepLength!! * 0.7f + calculatedStepLength * 0.3f
                        }
                    Timber.d("평균 보폭 업데이트: ${averageStepLength}m (GPS: ${gpsDistance}m, 걸음: $stepsTaken)")
                }

                // GPS 정확도 확인
                val lastPoint = points.lastOrNull()
                val isGpsAccurate = lastPoint?.accuracy?.let { it > 0 && it <= 20f } ?: false

                // GPS가 정확하면 GPS 거리 우선 사용
                if (isGpsAccurate && gpsDistance > 0f) {
                    // Step Counter 거리와 비교하여 보정
                    val difference = kotlin.math.abs(gpsDistance - stepBasedDistance)
                    val differenceRatio = if (gpsDistance > 0f) difference / gpsDistance else 0f

                    // 차이가 20% 이내면 GPS 거리 사용, 그 외에는 가중 평균
                    return if (differenceRatio <= 0.2f) {
                        gpsDistance
                    } else {
                        // 가중 평균 (GPS 70%, Step 30%)
                        gpsDistance * 0.7f + stepBasedDistance * 0.3f
                    }
                } else {
                    // GPS가 부정확하면 Step Counter 거리 우선 사용
                    return if (averageStepLength != null && stepBasedDistance > 0f) {
                        // 평균 보폭이 있으면 Step Counter 거리 사용
                        stepBasedDistance
                    } else {
                        // 평균 보폭이 없으면 GPS 거리 사용 (부정확해도)
                        gpsDistance
                    }
                }
            } else {
                // GPS 데이터가 부족하면 Step Counter 거리 사용
                return stepBasedDistance
            }
        }

        /**
         * Step Counter 기반 거리 계산
         *
         * @param stepCount 현재 걸음 수
         * @return Step Counter 기반 계산 거리 (미터)
         */
        private fun calculateStepBasedDistance(stepCount: Int): Float {
            val stepsTaken = stepCount - initialStepCount
            if (stepsTaken <= 0) return 0f

            // 평균 보폭이 있으면 사용, 없으면 기본값 사용
            val stepLength = averageStepLength ?: 0.7f // 기본 보폭: 0.7m (성인 평균)

            return stepsTaken * stepLength
        }

        /**
         * GPS 속도 계산 (m/s) - 3점 이동 평균 방식
         * 마지막 3개 위치 포인트를 사용하여 더 부드러운 속도 계산
         *
         * 개선 사항:
         * - 3점 이동 평균 방식으로 노이즈 감소 (2점 대비 10-30% 개선)
         * - coerceAtLeast를 나눗셈 후에 적용하여 정확한 시간 차이 사용
         * - 최소 시간 간격(100ms)을 설정하여 노이즈 제거
         * - 가중 평균 사용 (최근 구간 70%, 이전 구간 30%)
         *
         * 장점:
         * - 노이즈 감소: 단일 측정값의 오차가 평균화됨
         * - 더 안정적인 속도 추정: 급격한 변화 완화
         * - GPS 정확도가 낮은 환경(도심, 건물 주변)에서 더 유의미한 개선
         *
         * 단점:
         * - 계산 복잡도 약간 증가
         * - 반응 속도 약간 감소 (과거 데이터 사용)
         */
        private fun calculateGpsSpeed(points: List<LocationPoint>): Float {
            // 3점 미만이면 2점 방식으로 폴백
            if (points.size < 3) {
                if (points.size < 2) return 0f

                val lastPoint = points.last()
                val secondLastPoint = points[points.size - 2]

                val distance =
                    calculateDistanceBetweenPoints(
                        secondLastPoint.latitude,
                        secondLastPoint.longitude,
                        lastPoint.latitude,
                        lastPoint.longitude,
                    )

                val timeDiffSeconds =
                    ((lastPoint.timestamp - secondLastPoint.timestamp) / 1000f)
                        .coerceAtLeast(0.1f)

                return distance / timeDiffSeconds
            }

            // 3점 이상이면 이동 평균 방식 사용
            val lastPoint = points.last()
            val secondLastPoint = points[points.size - 2]
            val thirdLastPoint = points[points.size - 3]

            // 첫 번째 구간 (thirdLast -> secondLast)
            val distance1 =
                calculateDistanceBetweenPoints(
                    thirdLastPoint.latitude,
                    thirdLastPoint.longitude,
                    secondLastPoint.latitude,
                    secondLastPoint.longitude,
                )
            val timeDiff1 =
                ((secondLastPoint.timestamp - thirdLastPoint.timestamp) / 1000f)
                    .coerceAtLeast(0.1f)
            val speed1 = if (timeDiff1 > 0f) distance1 / timeDiff1 else 0f

            // 두 번째 구간 (secondLast -> last)
            val distance2 =
                calculateDistanceBetweenPoints(
                    secondLastPoint.latitude,
                    secondLastPoint.longitude,
                    lastPoint.latitude,
                    lastPoint.longitude,
                )
            val timeDiff2 =
                ((lastPoint.timestamp - secondLastPoint.timestamp) / 1000f)
                    .coerceAtLeast(0.1f)
            val speed2 = if (timeDiff2 > 0f) distance2 / timeDiff2 else 0f

            // 이동 평균: 두 구간의 가중 평균 (최근 데이터에 더 높은 가중치)
            // 가중치: 최근 구간 70%, 이전 구간 30%
            return if (speed1 > 0f && speed2 > 0f) {
                speed2 * 0.7f + speed1 * 0.3f
            } else if (speed2 > 0f) {
                speed2
            } else {
                0f
            }
        }

        /**
         * 두 지점 간 거리 계산 (Haversine 공식)
         */
        private fun calculateDistanceBetweenPoints(
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
         * 총 이동 거리 계산 (미터) - GPS만 사용
         * GPS 노이즈를 필터링하여 정확한 거리만 계산합니다.
         *
         * @deprecated 하이브리드 거리 계산을 위해 calculateHybridDistance 사용 권장
         */
        private fun calculateTotalDistance(points: List<LocationPoint>): Float {
            if (points.size < 2) return 0f

            var totalDistance = 0f
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]

                val distance =
                    calculateDistance(
                        prev.latitude,
                        prev.longitude,
                        curr.latitude,
                        curr.longitude,
                    )

                // GPS 노이즈 필터링: 5m 미만 이동은 무시
                // (LocationTrackingService에서 이미 3m 필터링을 했지만, 추가 안전장치)
                if (distance >= 5f) {
                    totalDistance += distance
                } else {
//                Timber.d("GPS 노이즈로 인한 작은 거리 무시: ${distance}m")
                }
            }
            return totalDistance
        }

        /**
         * 두 지점 간 거리 계산 (Haversine 공식)
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
         * 가속도계 기반 실시간 걸음 수 보간 업데이트
         * 실제 걸음 수가 업데이트되기 전까지 예상 걸음 수를 계산하여 부드러운 전환을 제공합니다.
         */
        private fun updateInterpolatedStepCount(currentRealStepCount: Int) {
            val currentTime = System.currentTimeMillis()

            // 실제 걸음 수가 업데이트되었는지 확인
            if (currentRealStepCount != lastRealStepCount) {
                // 실제 걸음 수가 업데이트됨 -> 보간된 값을 실제 값으로 점진적으로 수렴
                lastRealStepCount = currentRealStepCount
                lastRealStepCountTime = currentTime

                // 보간된 값이 실제 값과 다르면 점진적으로 수렴
                if (interpolatedStepCount != currentRealStepCount) {
                    val diff = currentRealStepCount - interpolatedStepCount
                    // 차이의 30%씩 수렴 (부드러운 전환)
                    interpolatedStepCount += (diff * 0.3f).toInt()
                    if (kotlin.math.abs(diff) < 2) {
                        // 차이가 작으면 즉시 실제 값으로 설정
                        interpolatedStepCount = currentRealStepCount
                    }
                } else {
                    interpolatedStepCount = currentRealStepCount
                }
            } else {
                // 실제 걸음 수가 업데이트되지 않음 -> 가속도계 기반으로 예상 걸음 수 계산
                if (estimatedStepsPerSecond > 0f && movementStartTime > 0L) {
                    val timeSinceMovementStart = (currentTime - movementStartTime) / 1000f // 초 단위
                    val timeSinceLastUpdate = (currentTime - lastRealStepCountTime) / 1000f // 초 단위

                    // 마지막 실제 걸음 수 업데이트 이후 경과 시간 동안 예상 걸음 수 추가
                    val estimatedAdditionalSteps = (timeSinceLastUpdate * estimatedStepsPerSecond).toInt()

                    // 보간된 걸음 수 = 마지막 실제 걸음 수 + 예상 추가 걸음 수
                    interpolatedStepCount = lastRealStepCount + estimatedAdditionalSteps

                    // 보간된 값이 실제 값보다 너무 많이 앞서지 않도록 제한 (최대 10걸음)
                    val maxAhead = lastRealStepCount + 10
                    if (interpolatedStepCount > maxAhead) {
                        interpolatedStepCount = maxAhead
                    }
                } else {
                    // 움직임이 없으면 보간된 값을 실제 값으로 유지
                    interpolatedStepCount = lastRealStepCount
                }
            }
        }

        /**
         * 상태 초기화 (리셋)
         */
        fun reset() {
            Timber.d("WalkingViewModel 상태 초기화")

            // 모든 Job 취소
            stepCountJob?.cancel()
            locationJob?.cancel()
            durationUpdateJob?.cancel()
            activityJob?.cancel()
            accelerometerJob?.cancel()

            // 추적 중지
            stepCounterManager.stopTracking()
            stopLocationTracking()
            accelerometerManager.stopTracking()
            activityRecognitionManager.stopTracking()

            // 상태 초기화
            currentSession = null
            locationPoints.clear()
            activityStatsList.clear()
            lastActivityState = null
            lastActivityChangeTime = 0L
            lastLocationForActivity = null
            currentMovementState = null
            pendingMovementState = null
            movementStateChangeTime = 0L
            stableMovementState = null
            initialStepCount = 0
            lastStepCount = 0
            lastGpsDistance = 0f
            averageStepLength = null
            lastRealStepCount = 0
            lastRealStepCountTime = 0L
            interpolatedStepCount = 0
            estimatedStepsPerSecond = 0f
            lastAcceleration = 0f
            movementStartTime = 0L

            // UI 상태를 Initial로 초기화
            _uiState.value = WalkingUiState.Initial

            Timber.d("WalkingViewModel 상태 초기화 완료")
        }

        /**
         * 위치 데이터 BroadcastReceiver 등록
         */
        private fun registerLocationReceiver() {
            locationReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        if (intent?.action == LocationTrackingService.ACTION_LOCATION_DATA) {
                            val locationsJson = intent.getStringExtra(LocationTrackingService.EXTRA_LOCATIONS) ?: return
                            try {
                                val newLocations = Json.decodeFromString<List<LocationPoint>>(locationsJson)

                                // 새로운 위치 포인트만 추가 (중복 제거)
                                newLocations.forEach { newPoint ->
                                    // 이미 존재하는 위치인지 확인 (타임스탬프와 좌표로 판단)
                                    val exists =
                                        locationPoints.any { existing ->
                                            existing.timestamp == newPoint.timestamp ||
                                                (
                                                    kotlin.math.abs(existing.latitude - newPoint.latitude) < 0.000001 &&
                                                        kotlin.math.abs(existing.longitude - newPoint.longitude) <
                                                        0.000001
                                                )
                                        }

                                    if (!exists) {
                                        locationPoints.add(newPoint)
                                    }
                                }

                                // 거리 계산 및 UI 업데이트
                                val currentState = _uiState.value
                                if (currentState is WalkingUiState.Walking) {
                                    val totalDistance = calculateHybridDistance(locationPoints, currentState.stepCount)
                                    _uiState.value = currentState.copy(distance = totalDistance)
                                    updateCurrentSession(stepCount = currentState.stepCount)
                                }

                                Timber.d("위치 데이터 수신: ${newLocations.size}개 포인트, 총 ${locationPoints.size}개 포인트")
                            } catch (e: Exception) {
                                Timber.e(e, "위치 데이터 파싱 실패")
                            }
                        }
                    }
                }

            val filter = IntentFilter(LocationTrackingService.ACTION_LOCATION_DATA)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                getApplication<Application>().registerReceiver(
                    locationReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                getApplication<Application>().registerReceiver(locationReceiver, filter)
            }
        }

        /**
         * 위치 데이터 BroadcastReceiver 해제
         */
        private fun unregisterLocationReceiver() {
            locationReceiver?.let {
                try {
                    getApplication<Application>().unregisterReceiver(it)
                } catch (e: Exception) {
                    Timber.e(e, "BroadcastReceiver 해제 실패")
                }
            }
            locationReceiver = null
        }

        override fun onCleared() {
            super.onCleared()
            unregisterLocationReceiver()
            stepCountJob?.cancel()
            locationJob?.cancel()
            durationUpdateJob?.cancel()
            activityJob?.cancel()
            activityRecognitionManager.stopTracking()
        }
    }

/**
 * Walking UI State
 */
sealed interface WalkingUiState {
    /**
     * 초기 상태
     */
    data object Initial : WalkingUiState

    /**
     * 산책 중
     */
    data class Walking(
        val stepCount: Int,
        val duration: Long,
        val distance: Float = 0f,
        val currentActivity: ActivityType? = null,
        val currentMovementState: team.swyp.sdu.domain.service.MovementState? = null, // 걷는 중/뛰는 중 상태
        val currentSpeed: Float = 0f, // 가속도계로 측정한 현재 속도 (m/s)
        // 검증용 디버그 정보
        val debugInfo: DebugInfo? = null,
    ) : WalkingUiState

    /**
     * 검증용 디버그 정보
     */
    data class DebugInfo(
        val acceleration: Float, // 가속도 (m/s²)
        val stepsPerSecond: Float, // 걸음 수/초
        val averageStepLength: Float?, // 평균 보폭 (m)
        val realStepCount: Int, // 실제 걸음 수
        val interpolatedStepCount: Int, // 보간된 걸음 수
        val gpsDistance: Float, // GPS 거리 (m)
        val stepBasedDistance: Float, // Step Counter 거리 (m)
        val locationPointCount: Int, // 위치 포인트 개수
        val lastLocation: LocationPoint?, // 마지막 위치
    )

    /**
     * 산책 완료
     */
    data class Completed(
        val session: WalkingSession,
    ) : WalkingUiState

    /**
     * 오류 상태
     */
    data class Error(
        val message: String,
    ) : WalkingUiState
}
