# FAQ (자주 묻는 질문)

## 일반적인 질문

### Q1: 왜 Activity Recognition API를 사용하나요?

**A**: Activity Recognition API는 Google Play Services에서 제공하는 하드웨어 가속 API입니다. 

**장점**:
- 배터리 효율적 (하드웨어 가속)
- 정확한 활동 감지 (걷기, 달리기, 정지 등)
- 다양한 활동 타입 지원

**대안**:
- 가속도계 센서 직접 사용 (더 복잡하고 배터리 소모 큼)
- GPS 속도 기반 추정 (정확도 낮음)

---

### Q2: 왜 Broadcast를 사용하나요? Flow를 사용하면 안 되나요?

**A**: `LocationTrackingService`는 별도의 프로세스에서 실행되는 Foreground Service입니다.

**Broadcast를 사용하는 이유**:
- Service와 ViewModel이 서로 다른 프로세스/컨텍스트에서 실행
- Android의 표준 IPC (Inter-Process Communication) 방식
- 앱이 백그라운드에 있어도 데이터 전송 가능

**Flow를 사용할 수 없는 이유**:
- Service와 ViewModel이 같은 프로세스라도 독립적인 생명주기
- Service가 ViewModel의 Flow를 직접 구독하기 어려움

**참고**: `StepCounterManager`와 `ActivityRecognitionManager`는 ViewModel과 같은 프로세스에서 실행되므로 Flow를 사용합니다.

---

### Q3: 왜 걸음 수는 Flow이고 위치는 Broadcast인가요?

**A**: 컴포넌트의 실행 위치와 생명주기에 따라 다릅니다.

| 데이터 | 컴포넌트 | 실행 위치 | 통신 방식 |
|--------|---------|----------|----------|
| 걸음 수 | StepCounterManager | ViewModel과 같은 프로세스 | Flow |
| 활동 상태 | ActivityRecognitionManager | ViewModel과 같은 프로세스 | Flow |
| 위치 | LocationTrackingService | 별도 프로세스 (Foreground Service) | Broadcast |

**이유**:
- **같은 프로세스**: Flow 사용 가능 (직접 참조 가능)
- **별도 프로세스**: Broadcast 필요 (Android IPC)

---

### Q4: GPS 정확도 필터링이 왜 필요한가요?

**A**: GPS는 환경에 따라 정확도가 크게 달라집니다.

**문제 상황**:
- 실내: 정확도 50-100m (위치가 크게 튀는 현상)
- 건물 사이: 정확도 20-50m (반사파로 인한 오차)
- 실외 (맑은 하늘): 정확도 3-10m (정확함)

**해결 방법**:
```kotlin
// 정확도가 50m 이하인 경우만 사용
if (accuracy > 0 && accuracy > 50f) {
    Timber.w("GPS 정확도가 낮아 위치를 무시합니다: ${accuracy}m")
    return@let
}
```

**효과**:
- 부정확한 위치 데이터 제거
- 거리 측정 정확도 향상

---

### Q5: 최소 거리 필터링(3m)이 왜 필요한가요?

**A**: GPS 노이즈를 제거하기 위함입니다.

**문제 상황**:
- GPS는 정지 상태에서도 위치가 약간씩 변함 (노이즈)
- 예: 제자리에 서 있어도 1-2m씩 위치가 변함
- 이로 인해 거리가 부정확하게 증가

**해결 방법**:
```kotlin
// 이전 위치와 3m 이상 떨어진 경우만 추가
val distance = calculateDistance(lastPoint, newPoint)
if (distance < 3f) {
    Timber.d("최소 거리 미만으로 위치를 무시합니다: ${distance}m")
    return@let
}
```

**효과**:
- GPS 노이즈 제거
- 정지 상태에서 거리 증가 방지

---

### Q6: 왜 활동 상태 변경 시 지연이 발생하나요?

**A**: 두 가지 지연이 있습니다.

#### 1. Activity Recognition 감지 지연 (최대 1초)
- Activity Recognition API는 1초마다 업데이트 (`DETECTION_INTERVAL_MS = 1000L`, P0: 즉각 피드백)
- 사용자가 달리기 시작해도 최대 1초 후에 감지

#### 2. 위치 업데이트 대기 시간 (최대 2-16초)
- 활동 상태 + 배터리 상태에 따라 위치 업데이트 간격이 다름
- 기본: 달리기 2초, 걷기 3초, 정지 8초
- 배터리 부족 시: 간격 1.5-2배 증가

#### 3. 가속도계 즉각 피드백 (선택적)
- 가속도계는 실시간으로 움직임 감지 (수십 밀리초)
- Activity Recognition보다 빠르지만 정확도는 낮음
- UI에 즉시 반영 가능 (선택적 구현)

**총 지연**: 최대 3초 (1초 + 2초)

**개선 방법**:
- ✅ `DETECTION_INTERVAL_MS`를 1초로 단축 (구현 완료)
- ✅ 가속도계 즉각 피드백 추가 (구현 완료)
- 배터리 상태 기반 적응형 간격 조정 (구현 완료)

---

### Q7: 센서 타임아웃 모니터링이 왜 필요한가요?

**A**: 네트워크 변경, 배터리 최적화 등으로 센서가 멈출 수 있습니다.

**문제 상황**:
- 네트워크 변경 시 센서 리스너가 비활성화될 수 있음
- 배터리 최적화로 센서 업데이트가 중단될 수 있음
- 사용자가 걸음 수가 멈추는 현상 경험

**해결 방법**:
```kotlin
// 10초 동안 센서 업데이트가 없으면 재등록
val timeoutJob = launch(coroutineContext) {
    while (isTracking) {
        delay(5000) // 5초마다 확인
        val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime
        if (timeSinceLastUpdate > SENSOR_TIMEOUT_MS) {
            // 센서 재등록
            sensorManager.unregisterListener(listener)
            sensorManager.registerListener(listener, sensorToUse, ...)
        }
    }
}
```

**효과**:
- 센서가 멈춰도 자동으로 복구
- 사용자 경험 향상

---

### Q8: Foreground Service가 왜 필요한가요?

**A**: 백그라운드에서 위치 추적을 계속하기 위함입니다.

**일반 Service의 문제**:
- Android 8.0 (API 26) 이상에서 백그라운드 실행 제한
- 앱이 백그라운드로 가면 Service가 종료될 수 있음

**Foreground Service의 장점**:
- 백그라운드 실행 보장
- 사용자에게 알림 표시 (투명성)
- 위치 추적이 중단되지 않음

**필수 권한**:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
```

---

### Q9: 왜 Step Detector를 Step Counter보다 우선 사용하나요?

**A**: Step Detector가 더 정확하고 실시간입니다.

| 센서 | 특징 | 장점 | 단점 |
|------|------|------|------|
| **Step Detector** | 각 걸음마다 이벤트 발생 | - 실시간 업데이트<br>- 제자리 움직임 필터링 | - 일부 기기에서 미지원 |
| **Step Counter** | 누적 걸음 수 제공 | - 모든 기기 지원 | - 초기값 설정 필요<br>- 실시간성이 낮음 |

**우선순위**:
```kotlin
val sensorToUse = stepDetectorSensor ?: stepCounterSensor
```

**효과**:
- 가능하면 Step Detector 사용 (더 정확)
- 없으면 Step Counter 사용 (호환성)

---

### Q10: 활동 상태별 업데이트 간격을 왜 다르게 설정하나요?

**A**: 배터리 효율성과 정확도의 균형을 맞추기 위함입니다.

**이유**:
- **달리기 (2초)**: 빠르게 움직이므로 자주 업데이트 필요
- **걷기 (3초)**: 적당한 속도이므로 중간 간격
- **정지 (8초)**: 움직이지 않으므로 자주 업데이트 불필요

**효과**:
- 배터리 소모 감소 (정지 상태에서 8초 간격)
- 정확도 유지 (달리기 상태에서 2초 간격)

---

## 기술적인 질문

### Q11: callbackFlow는 어떻게 동작하나요?

**A**: `callbackFlow`는 콜백 기반 API를 Flow로 변환하는 코루틴 빌더입니다.

**동작 원리**:
```kotlin
fun getStepCountUpdates(): Flow<Int> = callbackFlow {
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // 콜백에서 Flow로 데이터 전송
            trySend(stepCount)
        }
    }
    
    sensorManager.registerListener(listener, sensor, ...)
    
    // Flow가 취소되면 리스너 해제
    awaitClose {
        sensorManager.unregisterListener(listener)
    }
}
```

**장점**:
- 콜백 기반 API를 Flow로 변환
- 자동 리소스 정리 (`awaitClose`)
- 코루틴과 통합 가능

---

### Q12: StateFlow와 MutableStateFlow의 차이는?

**A**: `StateFlow`는 읽기 전용, `MutableStateFlow`는 읽기/쓰기 가능합니다.

**사용 예시**:
```kotlin
// ViewModel 내부
private val _uiState = MutableStateFlow<WalkingUiState>(WalkingUiState.Initial)
val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()

// 외부에서 사용
viewModel.uiState.collect { state ->
    // 읽기만 가능
}
```

**이유**:
- 캡슐화: 외부에서 직접 수정 불가
- 안전성: ViewModel을 통해서만 상태 변경

---

### Q13: BroadcastReceiver의 RECEIVER_NOT_EXPORTED는 무엇인가요?

**A**: Android 14 (API 34) 이상에서 필수인 보안 플래그입니다.

**문제**:
- Android 14부터 BroadcastReceiver 등록 시 명시적 플래그 필요
- 없으면 `SecurityException` 발생

**해결**:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
} else {
    registerReceiver(receiver, filter)
}
```

**의미**:
- `RECEIVER_NOT_EXPORTED`: 다른 앱에서 접근 불가 (내부용)
- `RECEIVER_EXPORTED`: 다른 앱에서 접근 가능 (공개용)

---

### Q14: 왜 WalkingScreen에서 LaunchedEffect의 navigate를 제거했나요?

**A**: 코드 단순화와 명시적 사용자 액션 처리를 위함입니다.

**이전 방식 (문제점)**:
- LaunchedEffect에서 상태 변화 감지 시 자동 navigate
- 복잡한 상태 추적 로직 (`previousState`, `hasNavigatedToResult`)
- 예상치 못한 네비게이션 발생 가능

**현재 방식 (개선)**:
- 기록 종료 버튼 클릭 시 명시적 navigate
- 코드 단순화 및 예측 가능한 동작
- 사용자 액션에 직접 반응

**효과**:
- 코드 가독성 향상
- 버그 가능성 감소
- 명시적 사용자 경험

---

### Q15: WalkingResultScreen에서 지도 터치가 안 먹히는 이유는?

**A**: 스크롤 가능한 영역 안에 지도가 있어서 터치 이벤트가 스크롤로 가로채졌기 때문입니다.

**문제 상황**:
- 전체 Column에 `verticalScroll` 적용
- 지도 터치가 스크롤 제스처로 인식됨
- 지도 확대/축소/이동이 불가능

**해결 방법**:
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

**효과**:
- 지도 터치가 정상 작동
- RouteDetailScreen과 동일한 매끄러운 경험

---

### Q16: 테스트용 위치 데이터(LocationTestData)는 언제 사용되나요?

**A**: 위치 포인트가 0개 또는 1개일 때 지도 표시를 위해 사용됩니다.

**사용 위치**:
- `KakaoMapView`: `if (locations.size <= 1)` 조건에서 사용
- 지도에 경로를 표시하기 위한 최소 데이터 제공

**특징**:
- 서울 중심 기준 원형 경로 (약 500m 반경)
- 20개 위치 포인트 생성
- 개발 및 테스트 편의성 제공

**실제 사용 시나리오**:
- 산책 시작 직후 (위치 포인트가 아직 적을 때)
- GPS 신호가 약한 환경
- 테스트 목적

---

### Q17: 세션 저장은 언제 이루어지나요?

**A**: 산책 종료(`stopWalking()`) 시 자동으로 저장됩니다.

**저장 플로우**:
1. 사용자가 "측정 종료" 버튼 클릭
2. `stopWalking()` 호출
3. 최종 통계 계산 및 `WalkingSession` 완성
4. `WalkingSessionRepository.saveSession()` 호출 (비동기)
5. Room Database에 저장
6. 서버 동기화 시도 (현재는 스텁)

**저장 데이터**:
- 시작/종료 시간
- 걸음 수
- 위치 좌표 리스트 (JSON 직렬화)
- 총 거리
- 활동 상태별 통계 (JSON 직렬화)
- 주요 활동 상태

**조회 방법**:
- `WalkingSessionListScreen`에서 저장된 세션 리스트 확인
- `WalkingSessionRepository.getAllSessions()` 사용

---

### Q18: 상태 초기화는 언제 이루어지나요?

**A**: 여러 시점에서 자동으로 초기화됩니다.

**초기화 시점**:

1. **화면 재표시 시** (LaunchedEffect)
   ```kotlin
   LaunchedEffect(Unit) {
       if (uiState is WalkingUiState.Completed) {
           viewModel.reset()
       }
   }
   ```
   - WalkingScreen이 다시 표시될 때 Completed 상태면 초기화

2. **뒤로가기 시** (DisposableEffect)
   ```kotlin
   DisposableEffect(Unit) {
       onDispose {
           if (currentState !is WalkingUiState.Completed && 
               currentState !is WalkingUiState.Walking) {
               viewModel.reset()
           }
       }
   }
   ```
   - 화면이 사라질 때 초기화 (단, 측정 중이 아닐 때만)

3. **명시적 초기화**
   - "다시 시작하기" 버튼 클릭 시
   - 에러 발생 시 재시도

**초기화 내용**:
- 모든 Job 취소
- 모든 Manager/Service 중지
- 상태 변수 초기화
- UI State를 `Initial`로 변경

---

## 다음 단계

- [전체 아키텍처 개요](./01-architecture-overview.md)로 돌아가기
- [데이터 흐름도](./02-data-flow.md)에서 전체 흐름 확인
- [컴포넌트 상세 설명](./03-components.md)에서 구현 확인
