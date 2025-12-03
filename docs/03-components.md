# 컴포넌트 상세 설명

각 컴포넌트의 역할, 구현 세부사항, 주요 메서드를 설명합니다.

## 1. WalkingViewModel

### 역할
전체 산책 세션의 상태를 관리하고, 여러 Manager/Service를 조합하여 UI에 표시할 데이터를 제공합니다. 산책 완료 시 세션을 저장하고, 상태 초기화를 담당합니다.

### 주요 책임

1. **UI State 관리**
   ```kotlin
   private val _uiState = MutableStateFlow<WalkingUiState>(WalkingUiState.Initial)
   val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()
   ```

2. **여러 데이터 소스 조합**
   - 걸음 수: `StepCounterManager.getStepCountUpdates()`
   - 위치: `LocationTrackingService`의 Broadcast
   - 활동 상태: `ActivityRecognitionManager.getActivityUpdates()`

3. **비즈니스 로직**
   - 하이브리드 거리 계산 (`calculateHybridDistance` - GPS + Step Counter)
   - 활동 상태별 통계 관리 (`updateActivityStats`)
   - 시간 업데이트 (1초마다)
   - 가속도계 기반 즉각 피드백 수신
   - 세션 저장 (`WalkingSessionRepository`)

4. **상태 관리**
   - 상태 초기화 (`reset()`)
   - 뒤로가기 처리 (DisposableEffect)
   - 화면 재표시 시 자동 초기화 (LaunchedEffect)

### 주요 메서드

#### startWalking()
```kotlin
fun startWalking() {
    // 1. 센서 사용 가능 여부 확인
    if (!stepCounterManager.isStepCounterAvailable()) {
        _uiState.value = WalkingUiState.Error("...")
        return
    }

    // 2. 세션 초기화
    val startTime = System.currentTimeMillis()
    currentSession = WalkingSession(startTime = startTime)
    locationPoints.clear()

    // 3. 모든 추적 시작
    stepCounterManager.startTracking()
    startLocationTracking()  // LocationTrackingService 시작
    startActivityTracking()   // ActivityRecognitionManager 시작
    startAccelerometerTracking()  // AccelerometerManager 시작 (즉각 피드백)

    // 4. UI State 초기화
    _uiState.value = WalkingUiState.Walking(...)

    // 5. Flow 수신 시작
    stepCountJob = stepCounterManager.getStepCountUpdates()
        .onEach { stepCount -> /* UI 업데이트 */ }
        .launchIn(viewModelScope)
}
```

#### stopWalking()
```kotlin
fun stopWalking() {
    // 1. 모든 추적 중지
    stepCounterManager.stopTracking()
    activityRecognitionManager.stopTracking()
    accelerometerManager.stopTracking()
    stopLocationTracking()

    // 2. 최종 통계 계산
    val completedSession = session.copy(
        endTime = System.currentTimeMillis(),
        locations = locationPoints.toList(),
        totalDistance = calculateHybridDistance(locationPoints, stepCount),
        activityStats = calculateFinalActivityStats(locationPoints)
    )

    // 3. 세션 저장 (비동기)
    viewModelScope.launch {
        try {
            walkingSessionRepository.saveSession(completedSession)
        } catch (e: Exception) {
            Timber.e(e, "산책 세션 저장 실패")
        }
    }

    // 4. UI State를 Completed로 변경
    _uiState.value = WalkingUiState.Completed(completedSession)
}
```

#### reset()
```kotlin
fun reset() {
    // 1. 모든 Job 취소
    stepCountJob?.cancel()
    locationJob?.cancel()
    durationUpdateJob?.cancel()
    activityJob?.cancel()
    accelerometerJob?.cancel()
    
    // 2. 모든 추적 중지
    stepCounterManager.stopTracking()
    stopLocationTracking()
    accelerometerManager.stopTracking()
    activityRecognitionManager.stopTracking()
    
    // 3. 상태 변수 초기화
    currentSession = null
    locationPoints.clear()
    activityStatsList.clear()
    // ... 기타 상태 변수 초기화
    
    // 4. UI State를 Initial로 초기화
    _uiState.value = WalkingUiState.Initial
}
```

#### handleActivityStateChange()
```kotlin
private fun handleActivityStateChange(newState: ActivityState) {
    // 1. 이전 활동 상태의 시간 기록
    if (lastActivityState != null && lastActivityChangeTime > 0) {
        val duration = currentTime - lastActivityChangeTime
        updateActivityStats(lastActivityState!!.type, duration, 0f)
    }

    // 2. 현재 활동 상태 업데이트
    lastActivityState = newState
    lastActivityChangeTime = currentTime

    // 3. LocationTrackingService에 즉시 알림 (지연 해결)
    val intent = Intent(LocationTrackingService.ACTION_ACTIVITY_UPDATE).apply {
        setPackage(getApplication<Application>().packageName)
        putExtra(LocationTrackingService.EXTRA_ACTIVITY_TYPE, newState.type.ordinal)
    }
    getApplication<Application>().sendBroadcast(intent)

    // 4. UI 업데이트
    val state = _uiState.value
    if (state is WalkingUiState.Walking) {
        val totalDistance = calculateTotalDistance(locationPoints)
        _uiState.value = state.copy(
            currentActivity = newState.type,
            distance = totalDistance
        )
    }
}
```

#### calculateHybridDistance() - 하이브리드 거리 계산
```kotlin
private fun calculateHybridDistance(points: List<LocationPoint>, stepCount: Int): Float {
    // GPS 기반 거리 계산
    val gpsDistance = calculateTotalDistance(points)
    
    // Step Counter 기반 거리 계산
    val stepBasedDistance = calculateStepBasedDistance(stepCount)
    
    // GPS 정확도 확인
    val lastPoint = points.lastOrNull()
    val isGpsAccurate = lastPoint?.accuracy?.let { it > 0 && it <= 20f } ?: false
    
    // GPS가 정확하면 GPS 거리 우선 사용
    if (isGpsAccurate && gpsDistance > 0f) {
        // Step Counter 거리와 비교하여 보정
        val difference = abs(gpsDistance - stepBasedDistance)
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
        return stepBasedDistance
    }
}
```

**특징**:
- GPS 정확도에 따라 자동으로 가중치 조정
- 평균 보폭 자동 계산 및 업데이트
- 실내 환경에서도 정확한 거리 측정

### 데이터 수집 방식

#### 1. 걸음 수 (Flow)
```kotlin
stepCounterManager.getStepCountUpdates()
    .onEach { stepCount ->
        // 실시간 걸음 수 업데이트
    }
    .launchIn(viewModelScope)
```

#### 2. 위치 (Broadcast)
```kotlin
private fun registerLocationReceiver() {
    locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val locationsJson = intent?.getStringExtra(EXTRA_LOCATIONS)
            val newLocations = Json.decodeFromString<List<LocationPoint>>(locationsJson)
            
            // 위치 데이터 추가 및 거리 계산
            locationPoints.addAll(newLocations)
            val totalDistance = calculateTotalDistance(locationPoints)
            // UI 업데이트
        }
    }
    
    val filter = IntentFilter(LocationTrackingService.ACTION_LOCATION_DATA)
    registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
}
```

#### 3. 활동 상태 (Flow)
```kotlin
activityRecognitionManager.getActivityUpdates()
    .onEach { activityState ->
        handleActivityStateChange(activityState)
    }
    .launchIn(viewModelScope)
```

---

## 2. StepCounterManager

### 역할
Android 하드웨어 센서를 사용하여 걸음 수를 측정합니다.

### 센서 종류

1. **TYPE_STEP_DETECTOR** (우선 사용)
   - 실제 걸음만 감지
   - 제자리 움직임 자동 필터링
   - 각 걸음마다 이벤트 발생

2. **TYPE_STEP_COUNTER** (백업)
   - 누적 걸음 수 제공
   - 초기값 기준으로 증가량 계산 필요

### 주요 메서드

#### startTracking()
```kotlin
fun startTracking() {
    if (isTracking) return
    
    isTracking = true
    stepCount = 0
    Timber.d("걸음 수 추적 시작")
}
```

#### getStepCountUpdates()
```kotlin
fun getStepCountUpdates(): Flow<Int> = callbackFlow {
    // 1. 센서 사용 가능 여부 확인
    if (!isStepCounterAvailable()) {
        close()
        return@callbackFlow
    }

    var initialStepCount: Float = 0f
    var isInitialValueSet = false
    var lastUpdateTime = System.currentTimeMillis()
    val SENSOR_TIMEOUT_MS = 10000L // 10초 타임아웃

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (!isTracking) return

            event?.let {
                lastUpdateTime = System.currentTimeMillis()
                
                when (it.sensor.type) {
                    Sensor.TYPE_STEP_DETECTOR -> {
                        // 실제 걸음 감지
                        stepCount++
                        trySend(stepCount)
                    }
                    Sensor.TYPE_STEP_COUNTER -> {
                        // 누적값 사용 (백업)
                        val totalSteps = it.values[0]
                        
                        if (!isInitialValueSet) {
                            initialStepCount = totalSteps
                            isInitialValueSet = true
                            stepCount = 0
                            return
                        }

                        val currentStepCount = (totalSteps - initialStepCount).toInt()
                        if (currentStepCount != stepCount) {
                            stepCount = currentStepCount
                            trySend(stepCount)
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // 센서 정확도 변경 처리
        }
    }

    // 2. 센서 등록
    val sensorToUse = stepDetectorSensor ?: stepCounterSensor
    sensorManager.registerListener(
        listener,
        sensorToUse,
        SensorManager.SENSOR_DELAY_FASTEST
    )

    // 3. 센서 타임아웃 모니터링 (네트워크 변경 등으로 센서가 멈출 수 있음)
    val timeoutJob = launch(coroutineContext) {
        while (isTracking) {
            delay(5000) // 5초마다 확인
            val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime
            if (timeSinceLastUpdate > SENSOR_TIMEOUT_MS) {
                // 센서 재등록 시도
                sensorManager.unregisterListener(listener)
                sensorManager.registerListener(
                    listener,
                    sensorToUse,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }
        }
    }

    awaitClose {
        timeoutJob.cancel()
        sensorManager.unregisterListener(listener)
    }
}
```

### 특징

- **실시간 업데이트**: `SENSOR_DELAY_FASTEST` 사용
- **안정성**: 센서 타임아웃 감지 및 재등록
- **정확도**: Step Detector 우선 사용 (제자리 움직임 필터링)

---

## 3. ActivityRecognitionManager

### 역할
Google Play Services의 Activity Recognition API를 사용하여 사용자 활동 상태를 감지합니다.

### 감지 가능한 활동

- `WALKING`: 걷기
- `RUNNING`: 달리기
- `STILL`: 정지
- `IN_VEHICLE`: 차량
- `ON_BICYCLE`: 자전거
- `ON_FOOT`: 도보
- `UNKNOWN`: 알 수 없음

### 주요 메서드

#### startTracking()
```kotlin
fun startTracking() {
    if (isTracking) return
    
    isTracking = true
    
    // BroadcastReceiver 등록
    registerActivityReceiver()
    
    // Activity Recognition 업데이트 요청
    requestActivityUpdates()
}
```

#### requestActivityUpdates()
```kotlin
private fun requestActivityUpdates() {
    pendingIntent = createPendingIntent()
    pendingIntent?.let { intent ->
        activityRecognitionClient.requestActivityUpdates(
            DETECTION_INTERVAL_MS,  // 2초마다 업데이트
            intent
        ).addOnSuccessListener {
            Timber.d("Activity Recognition 업데이트 요청 성공")
        }.addOnFailureListener { e ->
            Timber.e(e, "Activity Recognition 업데이트 요청 실패")
        }
    }
}
```

#### getActivityUpdates()
```kotlin
fun getActivityUpdates(): Flow<ActivityState> = callbackFlow {
    if (!isTracking) {
        close()
        return@callbackFlow
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ACTIVITY_UPDATES) {
                val result = ActivityRecognitionResult.extractResult(intent)
                result?.let {
                    val detectedActivity = it.mostProbableActivity
                    val activityType = convertToActivityType(detectedActivity.type)
                    val activityState = ActivityState(
                        type = activityType,
                        confidence = detectedActivity.confidence,
                        timestamp = System.currentTimeMillis()
                    )
                    trySend(activityState)
                }
            }
        }
    }

    val filter = IntentFilter(ACTION_ACTIVITY_UPDATES)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        context.registerReceiver(receiver, filter)
    }

    awaitClose {
        context.unregisterReceiver(receiver)
    }
}
```

### 특징

- **업데이트 간격**: 1초 (`DETECTION_INTERVAL_MS = 1000L`, P0: 즉각 피드백)
- **배터리 효율**: 하드웨어 가속 사용
- **신뢰도**: 각 활동에 대한 신뢰도(0-100) 제공

---

## 4. AccelerometerManager (신규 추가)

### 역할
가속도계 센서를 사용하여 즉각적인 움직임 감지를 제공합니다.

### 주요 책임

1. **실시간 가속도계 모니터링**
   - `Sensor.TYPE_ACCELEROMETER` 센서 사용
   - `SENSOR_DELAY_FASTEST`로 최대 속도 업데이트

2. **움직임 상태 감지**
   - STILL: 정지 (1.5 m/s² 이하)
   - WALKING: 걷기 (1.5 ~ 3.0 m/s²)
   - RUNNING: 달리기 (3.0 ~ 5.0 m/s² 이상)

3. **노이즈 필터링**
   - 이동 평균 필터링 (5개 샘플 윈도우)
   - 부정확한 움직임 감지 제거

### 주요 메서드

#### getMovementUpdates()
```kotlin
fun getMovementUpdates(): Flow<MovementDetection> = callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                
                // 총 가속도 계산
                val acceleration = sqrt(x * x + y * y + z * z)
                
                // 이동 평균 적용 (노이즈 필터링)
                accelerationBuffer.add(acceleration)
                if (accelerationBuffer.size > MOVING_AVERAGE_WINDOW) {
                    accelerationBuffer.removeAt(0)
                }
                
                val averageAcceleration = accelerationBuffer.average().toFloat()
                
                // 움직임 상태 판단
                val state = when {
                    averageAcceleration <= STILL_THRESHOLD -> MovementState.STILL
                    averageAcceleration <= WALKING_THRESHOLD -> MovementState.WALKING
                    averageAcceleration <= RUNNING_THRESHOLD -> MovementState.RUNNING
                    else -> MovementState.RUNNING
                }
                
                trySend(MovementDetection(state, averageAcceleration))
            }
        }
    }
    
    sensorManager.registerListener(
        listener,
        accelerometerSensor,
        SensorManager.SENSOR_DELAY_FASTEST
    )
    
    awaitClose {
        sensorManager.unregisterListener(listener)
    }
}
```

### 특징

- **실시간 업데이트**: 수십 밀리초 내 움직임 감지
- **Activity Recognition보다 빠름**: 1초 간격 API보다 즉각적
- **노이즈 필터링**: 이동 평균으로 부정확한 감지 제거

---

## 5. LocationTrackingService

### 역할
Foreground Service로 백그라운드에서 위치를 추적하고, 활동 상태에 따라 동적으로 업데이트 간격을 조정합니다.

### 주요 책임

1. **위치 추적**
   - FusedLocationProviderClient 사용
   - GPS 정확도 필터링 (>50m 무시)
   - 최소 거리 필터링 (<3m 무시)

2. **동적 업데이트 간격 조정**
   - 활동 상태 + 배터리 상태 변경 시 즉시 LocationRequest 업데이트
   - 활동 상태별 기본 간격:
     - 걷기: 3초
     - 달리기: 2초
     - 정지: 8초
     - 차량/자전거: 5초
   - 배터리 상태별 조정:
     - 배터리 > 50%: 기본 간격 유지
     - 배터리 20-50%: 간격 1.5배 증가
     - 배터리 < 20%: 간격 2배 증가
     - 저전력 모드: 간격 2배 증가

3. **배터리 모니터링**
   - 1분마다 배터리 레벨 확인
   - 저전력 모드 감지
   - 배터리 상태 변경 시 LocationRequest 자동 업데이트

3. **위치 데이터 전송**
   - 새로운 위치만 Broadcast로 전송 (성능 최적화)

### 주요 메서드

#### startTracking()
```kotlin
fun startTracking() {
    if (isTracking) return

    locationPoints.clear()
    lastSentIndex = 0
    startForeground(NOTIFICATION_ID, createNotification())
    isTracking = true

    locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                // GPS 정확도 필터링
                val accuracy = location.accuracy
                if (accuracy > 0 && accuracy > 50f) {
                    Timber.w("GPS 정확도가 낮아 위치를 무시합니다: ${accuracy}m")
                    return@let
                }
                
                val point = LocationPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = location.time,
                    accuracy = if (accuracy > 0) accuracy else null
                )
                
                // 최소 거리 필터링
                if (locationPoints.isNotEmpty()) {
                    val lastPoint = locationPoints.last()
                    val distance = calculateDistance(...)
                    
                    if (distance < 3f) {
                        Timber.d("최소 거리 미만으로 위치를 무시합니다: ${distance}m")
                        return@let
                    }
                }
                
                locationPoints.add(point)
                sendNewLocationDataBroadcast()  // 새로운 위치만 전송
            }
        }
    }

    locationRequest?.let { request ->
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            android.os.Looper.getMainLooper()
        )
    }
}
```

#### updateLocationRequest()
```kotlin
private fun updateLocationRequest() {
    if (!isTracking || locationCallback == null) return

    val updateInterval = getUpdateIntervalForActivity()  // 활동 + 배터리 상태 고려
    val priority = getPriorityForBatteryAndActivity()  // 배터리 상태 고려

    val newLocationRequest = LocationRequest.Builder(priority, updateInterval)
        .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
        .setMaxUpdateDelayMillis(updateInterval * 2)
        .setWaitForAccurateLocation(false)  // 빠른 응답을 위해 false
        .build()

    // 기존 업데이트 제거 후 새로 등록
    locationCallback?.let { callback ->
        fusedLocationClient.removeLocationUpdates(callback)
        try {
            fusedLocationClient.requestLocationUpdates(
                newLocationRequest,
                callback,
                android.os.Looper.getMainLooper()
            )
            locationRequest = newLocationRequest
            Timber.d("LocationRequest 업데이트 완료: 간격=${updateInterval}ms, 활동=${currentActivityType}, 배터리=${currentBatteryLevel}%, 저전력모드=$isPowerSaveMode, 우선순위=$priority")
        } catch (e: SecurityException) {
            Timber.e(e, "LocationRequest 업데이트 실패")
        }
    }
}
```

#### getPriorityForBatteryAndActivity()
```kotlin
private fun getPriorityForBatteryAndActivity(): Priority {
    // 저전력 모드이거나 배터리가 20% 미만이면 BALANCED_POWER_ACCURACY 사용
    if (isPowerSaveMode || currentBatteryLevel < 20) {
        return Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }
    
    // 배터리가 20-50%이고 정지 상태면 BALANCED_POWER_ACCURACY
    if (currentBatteryLevel < BATTERY_MEDIUM_THRESHOLD && 
        currentActivityType == ActivityType.STILL) {
        return Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }
    
    // 그 외에는 HIGH_ACCURACY
    return Priority.PRIORITY_HIGH_ACCURACY
}
```

#### registerActivityReceiver()
```kotlin
private fun registerActivityReceiver() {
    activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_ACTIVITY_UPDATE) {
                val activityTypeOrdinal = intent.getIntExtra(EXTRA_ACTIVITY_TYPE, -1)
                if (activityTypeOrdinal >= 0) {
                    val newActivityType = ActivityType.values().getOrNull(activityTypeOrdinal)
                    if (newActivityType != null && newActivityType != currentActivityType) {
                        currentActivityType = newActivityType
                        Timber.d("활동 상태 변경 수신: $newActivityType - LocationRequest 즉시 업데이트")
                        // 즉시 LocationRequest 업데이트 (지연 해결)
                        updateLocationRequest()
                    }
                }
            }
        }
    }

    val filter = IntentFilter(ACTION_ACTIVITY_UPDATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        registerReceiver(activityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(activityReceiver, filter)
    }
}
```

### 특징

- **Foreground Service**: 백그라운드 실행 보장
- **동적 간격 조정**: 활동 상태 + 배터리 상태 변경 시 즉시 반영
- **배터리 최적화**: 배터리 레벨 모니터링 및 저전력 모드 감지
- **필터링**: GPS 정확도 및 최소 거리 필터링으로 노이즈 제거

---

## 6. WalkingScreen (UI Layer)

### 역할
산책 측정 화면을 표시하고 사용자 인터랙션을 처리합니다.

### 주요 특징

1. **단순화된 네비게이션**
   - LaunchedEffect에서 자동 navigate 제거
   - 기록 종료 버튼 클릭 시 명시적 navigate
   - 상태 기반 자동 네비게이션 제거

2. **상태 관리**
   - 화면 재표시 시 Completed 상태 감지 및 자동 초기화
   - 뒤로가기 처리: DisposableEffect로 상태 초기화
   - 측정 중(Walking) 상태는 초기화하지 않음

### 주요 메서드

#### 기록 종료 처리
```kotlin
onStopClick = {
    viewModel.stopWalking()
    onNavigateToResult()  // 명시적 navigate
}
```

#### 상태 초기화 감지
```kotlin
LaunchedEffect(Unit) {
    val currentState = viewModel.uiState.value
    if (currentState is WalkingUiState.Completed) {
        viewModel.reset()  // 자동 초기화
    }
}
```

---

## 7. WalkingResultScreen (UI Layer)

### 역할
산책 완료 후 결과를 표시하는 화면입니다.

### 주요 특징

1. **지도 터치 최적화**
   - 지도는 스크롤 가능한 영역 밖에 배치
   - 터치 이벤트가 스크롤에 가로채이지 않음
   - RouteDetailScreen과 동일한 매끄러운 터치 경험

2. **레이아웃 구조**
   - 지도: 고정 높이 (400.dp), 스크롤 없음
   - 나머지 콘텐츠: 별도 스크롤 가능한 Column

### 레이아웃 구조
```kotlin
Column {
    // 지도 (스크롤 없음)
    Card {
        KakaoMapView(locations = session.locations)
    }
    
    // 나머지 콘텐츠 (스크롤 가능)
    Column(modifier = Modifier.verticalScroll(...)) {
        Button("다시 시작하기")
    }
}
```

---

## 8. LocationTestData (Utils)

### 역할
테스트용 위치 데이터를 제공합니다.

### 주요 메서드

#### getSeoulTestLocations()
```kotlin
fun getSeoulTestLocations(): List<LocationPoint> {
    val baseLat = 37.5665 // 서울시청 위도
    val baseLon = 126.9780 // 서울시청 경도
    
    // 작은 원형 경로 생성 (약 500m 반경)
    val radius = 0.0045
    val centerTime = System.currentTimeMillis()
    
    return (0 until 20).map { index ->
        val angle = (index * 18.0) * Math.PI / 180.0
        val lat = baseLat + radius * Math.sin(angle)
        val lon = baseLon + radius * Math.cos(angle)
        
        LocationPoint(
            latitude = lat,
            longitude = lon,
            timestamp = centerTime + (index * 10000L),
            accuracy = 10f
        )
    }
}
```

### 사용 위치
- `KakaoMapView`: 위치 포인트가 0개 또는 1개일 때 테스트용 위치 사용
- 지도 표시 테스트 및 개발 편의성 제공

---

## 9. WalkingSessionRepository (Data Layer)

### 역할
산책 세션 데이터의 저장 및 조회를 담당합니다.

### 주요 메서드

#### saveSession()
```kotlin
suspend fun saveSession(session: WalkingSession): Long {
    val entity = WalkingSessionMapper.toEntity(session, isSynced = false)
    val id = walkingSessionDao.insert(entity)
    
    // 서버 동기화 시도 (비동기, 실패해도 로컬 저장은 유지)
    try {
        syncToServer(session)
    } catch (e: Exception) {
        Timber.w(e, "서버 동기화 실패 (로컬 저장은 유지됨)")
    }
    
    return id
}
```

#### getAllSessions()
```kotlin
fun getAllSessions(): Flow<List<WalkingSession>> {
    return walkingSessionDao.getAllSessions().map { entities ->
        entities.map { WalkingSessionMapper.toDomain(it) }
    }
}
```

### 특징
- 하이브리드 저장: 로컬 저장 + 서버 동기화 (스텁)
- 복잡한 객체 JSON 직렬화 (LocationPoint, ActivityStats)
- 비동기 처리 (suspend 함수, Flow)

---

## 다음 단계

- [타이밍 다이어그램](./04-timing-diagram.md)에서 시간 기반 분석을 확인하세요.
- [FAQ](./05-faq.md)에서 자주 묻는 질문을 확인하세요.
