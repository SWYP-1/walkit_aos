# 데이터 흐름도

이 문서는 산책 시작부터 종료까지의 전체 데이터 흐름을 단계별로 설명합니다.

## 1. 산책 시작 (startWalking)

```
사용자: "산책 시작" 버튼 클릭
    │
    ▼
WalkingScreen.kt
    │
    ▼
WalkingViewModel.startWalking()
    │
    ├─▶ StepCounterManager.startTracking()
    │       │
    │       └─▶ SensorManager.registerListener()
    │               (TYPE_STEP_DETECTOR 센서 등록)
    │
    ├─▶ LocationTrackingService 시작
    │       │
    │       ├─▶ Intent(ACTION_START_TRACKING) 전송
    │       │
    │       └─▶ Foreground Service 시작
    │               │
    │               └─▶ FusedLocationProviderClient.requestLocationUpdates()
    │
    ├─▶ ActivityRecognitionManager.startTracking()
    │       │
    │       └─▶ ActivityRecognitionClient.requestActivityUpdates()
    │               (1초마다 활동 상태 감지 - P0: 즉각 피드백)
    │
    └─▶ AccelerometerManager.startTracking()
            │
            └─▶ SensorManager.registerListener()
                    (TYPE_ACCELEROMETER 센서 등록 - 실시간 움직임 감지)
```

### 상세 설명

1. **사용자 액션**: WalkingScreen에서 "산책 시작" 버튼 클릭
2. **ViewModel 초기화**: 
   - `WalkingSession` 생성
   - `WalkingUiState.Walking` 상태로 변경
3. **StepCounterManager 시작**:
   - 하드웨어 센서 리스너 등록
   - `getStepCountUpdates()` Flow 시작
4. **LocationTrackingService 시작**:
   - Foreground Service로 시작 (백그라운드 실행 보장)
   - FusedLocationProviderClient로 위치 업데이트 요청
5. **ActivityRecognitionManager 시작**:
   - Google Play Services API로 활동 감지 시작
   - 1초마다 활동 상태 업데이트 (P0: 즉각 피드백)
6. **AccelerometerManager 시작**:
   - 가속도계 센서 리스너 등록
   - 실시간 움직임 감지 (Activity Recognition보다 빠름)

## 2. 실시간 데이터 수집

### 2-1. 걸음 수 업데이트

```
StepCounterManager
    │
    │ SensorEventListener.onSensorChanged()
    │ (실제 걸음 감지 시 호출)
    │
    ▼
callbackFlow { trySend(stepCount) }
    │
    ▼
WalkingViewModel
    │
    │ stepCounterManager.getStepCountUpdates()
    │     .onEach { stepCount -> ... }
    │
    ▼
_uiState.value = state.copy(stepCount = stepCount)
    │
    ▼
WalkingScreen (자동 업데이트)
    │
    └─▶ UI에 걸음 수 표시
```

### 2-2. 위치 업데이트

```
LocationTrackingService
    │
    │ LocationCallback.onLocationResult()
    │ (GPS 위치 업데이트 시 호출)
    │
    ├─▶ GPS 정확도 필터링 (>50m 무시)
    ├─▶ 최소 거리 필터링 (<3m 무시)
    │
    ▼
locationPoints.add(newLocationPoint)
    │
    ▼
sendNewLocationDataBroadcast()
    │
    │ Intent(ACTION_LOCATION_DATA)
    │     .putExtra(EXTRA_LOCATIONS, JSON)
    │
    ▼
Broadcast 전송
    │
    ▼
WalkingViewModel.registerLocationReceiver()
    │
    │ BroadcastReceiver.onReceive()
    │
    ├─▶ JSON 디코딩
    ├─▶ locationPoints에 추가
    ├─▶ 거리 계산
    │
    ▼
_uiState.value = state.copy(distance = totalDistance)
    │
    ▼
WalkingScreen (자동 업데이트)
    │
    └─▶ UI에 거리 표시
```

### 2-3. 활동 상태 업데이트

```
ActivityRecognitionManager
    │
    │ Google Play Services
    │ (1초마다 활동 상태 감지 - P0: 즉각 피드백)
    │
    ▼
BroadcastReceiver.onReceive()
    │
    │ ActivityRecognitionResult 처리
    │
    ├─▶ 가장 높은 신뢰도의 활동 선택
    ├─▶ ActivityState 생성
    │
    ▼
callbackFlow { trySend(activityState) }
    │
    ▼
WalkingViewModel
    │
    │ activityRecognitionManager.getActivityUpdates()
    │     .onEach { activityState -> ... }
    │
    ├─▶ handleActivityStateChange(activityState)
    │       │
    │       ├─▶ 활동 상태 통계 업데이트
    │       ├─▶ UI State 업데이트
    │       │
    │       └─▶ LocationTrackingService에 Broadcast 전송
    │               │
    │               │ Intent(ACTION_ACTIVITY_UPDATE)
    │               │     .putExtra(EXTRA_ACTIVITY_TYPE, ...)
    │               │
    │               ▼
    │       LocationTrackingService.registerActivityReceiver()
    │               │
    │               │ BroadcastReceiver.onReceive()
    │               │
    │               ├─▶ currentActivityType 업데이트
    │               │
    │               └─▶ updateLocationRequest()
    │                       │
    │                       ├─▶ 기존 LocationRequest 제거
    │                       ├─▶ 활동 상태 + 배터리 상태에 맞는 새 간격 계산
    │                       │   (기본: 걷기 3초, 달리기 2초, 정지 8초)
    │                       │   (배터리 부족 시: 간격 1.5-2배 증가)
    │                       │
    │                       └─▶ 새 LocationRequest 등록
    │
    └─▶ WalkingScreen (자동 업데이트)
            │
            └─▶ UI에 활동 상태 표시 (아이콘 + 텍스트)

─────────────────────────────────────────────────────────────

AccelerometerManager (즉각 피드백)
    │
    │ SensorEventListener.onSensorChanged()
    │ (실시간 가속도계 업데이트 - 수십 밀리초)
    │
    ▼
callbackFlow { trySend(MovementDetection) }
    │
    │ 움직임 상태 감지 (STILL, WALKING, RUNNING)
    │ 이동 평균 필터링으로 노이즈 제거
    │
    ▼
WalkingViewModel
    │
    │ accelerometerManager.getMovementUpdates()
    │     .onEach { detection -> ... }
    │
    └─▶ 즉각적인 움직임 피드백 (Activity Recognition보다 빠름)
            │
            └─▶ UI에 즉시 반영 가능 (선택적)
```

## 3. 활동 상태 변경 시 지연 시간 분석

### 시나리오: 정지 → 달리기

```
시간축: 0초 ──────────────────────────────────────────> 4초

0초: 사용자가 달리기 시작
    │
    ▼
0~2초: Activity Recognition API가 활동 상태 감지 중
    │   (최대 2초 소요 - DETECTION_INTERVAL_MS)
    │
    ▼
2초: Activity Recognition이 "RUNNING" 감지
    │
    ├─▶ ActivityRecognitionManager
    │       └─▶ callbackFlow { trySend(RUNNING) }
    │
    ├─▶ WalkingViewModel
    │       └─▶ handleActivityStateChange(RUNNING)
    │               │
    │               └─▶ Broadcast 전송 (수십 밀리초)
    │
    └─▶ LocationTrackingService
            └─▶ BroadcastReceiver.onReceive()
                    │
                    └─▶ updateLocationRequest() (즉시 실행)
                            │
                            ├─▶ 기존 LocationRequest 제거
                            └─▶ 새 LocationRequest 등록 (RUNNING: 2초 간격)
    │
    ▼
2~4초: 새로운 LocationRequest 적용 대기
    │   (최대 2초 소요 - RUNNING 간격)
    │
    ▼
4초: 첫 번째 위치 업데이트 수신
    │
    └─▶ UI에 위치 반영
```

**총 지연 시간**: 최대 4초 (2초 + 2초)

### 최선의 경우

```
시간축: 0초 ──────────────────────────────────────────> 2초

0초: 사용자가 달리기 시작
    │
    ▼
0초: Activity Recognition이 즉시 "RUNNING" 감지
    │   (운 좋게 바로 감지)
    │
    └─▶ LocationRequest 즉시 업데이트
    │
    ▼
0~2초: 위치 업데이트 대기
    │
    ▼
2초: 첫 번째 위치 업데이트 수신
```

**총 지연 시간**: 최대 2초

## 4. 산책 종료 (stopWalking)

```
사용자: "측정 종료" 버튼 클릭
    │
    ▼
WalkingScreen.onStopClick
    │
    ├─▶ viewModel.stopWalking()
    │       │
    │       ├─▶ StepCounterManager.stopTracking()
    │       │       └─▶ SensorManager.unregisterListener()
    │       │
    │       ├─▶ ActivityRecognitionManager.stopTracking()
    │       │       └─▶ ActivityRecognitionClient.removeActivityUpdates()
    │       │
    │       ├─▶ AccelerometerManager.stopTracking()
    │       │       └─▶ SensorManager.unregisterListener()
    │       │
    │       ├─▶ LocationTrackingService 중지
    │       │       │
    │       │       ├─▶ Intent(ACTION_STOP_TRACKING) 전송
    │       │       │
    │       │       └─▶ stopTracking()
    │       │               │
    │       │               ├─▶ FusedLocationClient.removeLocationUpdates()
    │       │               │
    │       │               └─▶ sendLocationDataBroadcast()
    │       │                       (전체 위치 데이터 전송)
    │       │
    │       ├─▶ 최종 통계 계산
    │       │       │
    │       │       ├─▶ 총 거리 계산
    │       │       ├─▶ 활동 상태별 통계 계산
    │       │       │
    │       │       └─▶ WalkingSession 완성
    │       │
    │       ├─▶ 세션 저장 (비동기)
    │       │       │
    │       │       └─▶ WalkingSessionRepository.saveSession()
    │       │               │
    │       │               ├─▶ WalkingSessionMapper.toEntity()
    │       │               │       (LocationPoint, ActivityStats JSON 직렬화)
    │       │               │
    │       │               ├─▶ WalkingSessionDao.insert()
    │       │               │       (Room Database 저장)
    │       │               │
    │       │               └─▶ 서버 동기화 시도 (스텁)
    │       │
    │       └─▶ _uiState.value = WalkingUiState.Completed(session)
    │
    └─▶ onNavigateToResult()
            │
            ▼
        WalkingResultScreen으로 이동
            │
            └─▶ 지도에 경로 표시 + 통계 카드 표시
```

### 상태 초기화 플로우

```
WalkingResultScreen에서 뒤로가기
    │
    ▼
onNavigateBack()
    │
    └─▶ navController.navigate(Screen.Main.route)
            │
            ▼
        MainScreen (기록 측정 탭)
            │
            └─▶ WalkingScreen 재구성
                    │
                    ▼
                LaunchedEffect(Unit)
                    │
                    └─▶ uiState가 Completed인지 확인
                            │
                            ├─▶ Completed이면 viewModel.reset()
                            │       │
                            │       ├─▶ 모든 Job 취소
                            │       ├─▶ 모든 Manager/Service 중지
                            │       ├─▶ 상태 변수 초기화
                            │       │
                            │       └─▶ _uiState.value = WalkingUiState.Initial
                            │
                            └─▶ Initial 상태로 복귀
                                    │
                                    └─▶ "산책을 시작하세요" 화면 표시
```

## 5. 데이터 흐름 요약

### 실시간 업데이트 (지속적)

1. **걸음 수**: 센서 이벤트 → Flow → ViewModel → UI
2. **위치**: GPS 업데이트 → Service → Broadcast → ViewModel → UI
3. **활동 상태**: Google API (1초마다) → Flow → ViewModel → UI + Service
4. **가속도계 움직임**: 센서 이벤트 (실시간) → Flow → ViewModel (즉각 피드백)

### 상태 변경 시 (이벤트 기반)

1. **활동 상태 변경**: Activity Recognition → ViewModel → Service (Broadcast)
2. **위치 업데이트 간격 변경**: Service 내부에서 즉시 처리

## 다음 단계

- [컴포넌트 상세 설명](./03-components.md)에서 각 컴포넌트의 구현을 확인하세요.
- [타이밍 다이어그램](./04-timing-diagram.md)에서 시간 기반 분석을 확인하세요.
