# 전체 아키텍처 개요

## 시스템 구조

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              MainScreen.kt                           │  │
│  │  - 상단 탭: 기록 측정 / 달린 기록                     │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │  WalkingScreen.kt                             │   │  │
│  │  │  - 산책 측정 UI                                │   │  │
│  │  │  - 기록 종료 버튼 클릭 시 결과 화면으로 이동   │   │  │
│  │  └───────────────────────────────────────────────┘   │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │  WalkingSessionListScreen.kt                   │   │  │
│  │  │  - 저장된 산책 기록 리스트 표시                │   │  │
│  │  └───────────────────────────────────────────────┘   │  │
│  │  ┌───────────────────────────────────────────────┐   │  │
│  │  │  WalkingResultScreen.kt                       │   │  │
│  │  │  - 산책 완료 후 결과 표시                      │   │  │
│  │  │  - 지도 터치 최적화 (스크롤 분리)              │   │  │
│  │  └───────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │ observe StateFlow
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Presentation Layer                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │          WalkingViewModel.kt                          │  │
│  │  - UI State 관리 (StateFlow)                           │  │
│  │  - 비즈니스 로직 조율                                  │  │
│  │  - 여러 Manager/Service 조합                           │  │
│  │  - 상태 초기화 (reset)                                 │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │    WalkingSessionListViewModel.kt                     │  │
│  │  - 저장된 산책 기록 리스트 관리                        │  │
│  └───────────────────────────────────────────────────────┘  │
└───────────┬───────────────┬───────────────┬───────────────┬─┘
            │               │               │               │
            │               │               │               │
    ┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │   Step       │ │  Activity   │ │ Accelerometer│ │  Location   │
    │   Counter    │ │ Recognition │ │   Manager   │ │  Tracking   │
    │   Manager    │ │   Manager   │ │             │ │   Service   │
    └──────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
            │               │               │               │
            │               │               │               │
    ┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
    │   Android    │ │   Google   │ │   Android   │ │   Google    │
    │   Sensor     │ │   Play     │ │   Sensor    │ │   Play      │
    │   API        │ │  Services  │ │   API       │ │  Services   │
    │              │ │            │ │             │ │             │
    │ TYPE_STEP_   │ │ Activity   │ │ TYPE_       │ │ Fused       │
    │ DETECTOR     │ │ Recognition│ │ ACCELEROMETER│ │ Location    │
    │              │ │   API      │ │             │ │   API       │
    │              │ │ (1초 간격)  │ │             │ │             │
    └──────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
                            │
                            ▼
            ┌───────────────────────────────┐
            │      Data Layer               │
            │  ┌─────────────────────────┐ │
            │  │ WalkingSessionRepository│ │
            │  │  - 세션 저장/조회         │ │
            │  │  - 서버 동기화 (스텁)     │ │
            │  └─────────────────────────┘ │
            │  ┌─────────────────────────┐ │
            │  │ WalkingSessionDao       │ │
            │  │  - Room Database CRUD   │ │
            │  └─────────────────────────┘ │
            │  ┌─────────────────────────┐ │
            │  │ AppDatabase (Room)      │ │
            │  │  - 로컬 데이터 저장      │ │
            │  └─────────────────────────┘ │
            └───────────────────────────────┘
```

## 주요 컴포넌트

### 1. WalkingViewModel (Presentation Layer)
**역할**: 전체 산책 세션의 상태를 관리하고 UI에 표시할 데이터를 제공

**주요 책임**:
- UI State 관리 (`WalkingUiState`)
- 여러 Manager/Service 조합 및 조율
- 위치 데이터 수집 및 하이브리드 거리 계산 (GPS + Step Counter)
- 활동 상태 통계 관리

**생명주기**: ViewModel (화면 회전 시에도 유지)

### 2. StepCounterManager (Domain Layer)
**역할**: Android 하드웨어 센서를 사용하여 걸음 수 측정

**주요 책임**:
- `Sensor.TYPE_STEP_DETECTOR` 센서 관리
- 실시간 걸음 수 Flow 제공
- 센서 안정성 모니터링 및 재등록

**특징**:
- 제자리 움직임 자동 필터링
- 센서 타임아웃 감지 및 복구

### 3. ActivityRecognitionManager (Domain Layer)
**역할**: Google Play Services API를 사용하여 사용자 활동 상태 감지

**주요 책임**:
- 걷기, 달리기, 정지, 차량 등 활동 상태 감지
- 1초마다 활동 상태 업데이트 (P0: 즉각 피드백을 위해 단축)
- 활동 상태 Flow 제공

**특징**:
- Google Play Services 의존성
- 배터리 효율적인 감지 (하드웨어 가속)

### 4. AccelerometerManager (Domain Layer) - 신규 추가
**역할**: 가속도계 센서를 사용하여 즉각적인 움직임 감지

**주요 책임**:
- 실시간 가속도계 모니터링 (`Sensor.TYPE_ACCELEROMETER`)
- 움직임 상태 감지 (STILL, WALKING, RUNNING)
- Activity Recognition보다 빠른 즉각 피드백 제공

**특징**:
- 실시간 업데이트 (수십 밀리초)
- Activity Recognition API (1초 간격)보다 빠름
- 이동 평균 필터링으로 노이즈 제거

### 5. LocationTrackingService (Domain Layer)
**역할**: Foreground Service로 백그라운드에서 위치 추적

**주요 책임**:
- FusedLocationProviderClient를 통한 위치 업데이트
- 활동 상태 + 배터리 상태에 따른 동적 업데이트 간격 조정
- 위치 데이터 Broadcast 전송
- 배터리 레벨 모니터링 및 저전력 모드 감지

**특징**:
- Foreground Service (백그라운드 실행 보장)
- 활동 상태 변경 시 즉시 LocationRequest 업데이트
- 배터리 상태 기반 적응형 간격 조정
- GPS 정확도 필터링 및 최소 거리 필터링

### 6. WalkingSessionRepository (Data Layer)
**역할**: 산책 세션 데이터의 저장 및 조회를 담당

**주요 책임**:
- Room Database를 통한 로컬 저장
- 세션 저장/조회/삭제
- 서버 동기화 (현재는 스텁 구현)

**특징**:
- 하이브리드 저장 (로컬 + 서버 동기화)
- 복잡한 객체 (LocationPoint, ActivityStats) JSON 직렬화
- 비동기 처리 (suspend 함수)

### 7. LocationTestData (Utils)
**역할**: 테스트용 위치 데이터 제공

**주요 책임**:
- 서울 중심 기준 테스트용 위치 20개 생성
- 위치 포인트가 부족할 때 지도 표시용

**특징**:
- 원형 경로 생성 (약 500m 반경)
- KakaoMapView에서 위치 포인트가 0개 또는 1개일 때 사용

## 컴포넌트 간 통신 방식

### 1. ViewModel → Manager/Service
- **직접 호출**: `stepCounterManager.startTracking()`
- **Flow 수신**: `stepCounterManager.getStepCountUpdates().collect { ... }`

### 2. Service → ViewModel
- **Broadcast**: `LocationTrackingService`가 위치 데이터를 Broadcast로 전송
- **BroadcastReceiver**: `WalkingViewModel`이 Broadcast를 수신하여 위치 데이터 수집

### 3. ViewModel → Service
- **Broadcast**: 활동 상태 변경 시 `LocationTrackingService`에 Broadcast 전송
- **Intent**: 서비스 시작/중지 시 Intent 사용

## 데이터 모델

### WalkingSession
```kotlin
data class WalkingSession(
    val startTime: Long,
    val endTime: Long? = null,
    val stepCount: Int = 0,
    val locations: List<LocationPoint> = emptyList(),
    val activityStats: List<ActivityStats> = emptyList()
)
```

### LocationPoint
```kotlin
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float? = null,
    val provider: String? = null,
    val isIndoor: Boolean? = null,
    val speed: Float? = null
)
```

### ActivityState
```kotlin
data class ActivityState(
    val type: ActivityType,  // WALKING, RUNNING, STILL 등
    val confidence: Int,      // 신뢰도 (0-100)
    val timestamp: Long
)
```

## 주요 기능

### 하이브리드 거리 계산
- GPS 기반 거리 + Step Counter 기반 거리 결합
- GPS 정확도에 따라 자동으로 가중치 조정
- 평균 보폭 자동 계산 및 업데이트

### 배터리 최적화
- 배터리 레벨 모니터링 (1분마다)
- 저전력 모드 자동 감지
- 배터리 상태에 따른 적응형 업데이트 간격
- GPS 우선순위 동적 조정 (HIGH_ACCURACY ↔ BALANCED_POWER_ACCURACY)

### 세션 저장 및 관리
- Room Database를 통한 로컬 저장
- 산책 완료 시 자동 저장
- 기록 리스트 화면에서 조회 가능
- 서버 동기화 준비 (현재는 스텁)

### 상태 관리 개선
- WalkingScreen 단순화: LaunchedEffect에서 navigate 제거
- 기록 종료 버튼 클릭 시 명시적 navigate
- 뒤로가기 처리: 화면 사라질 때 상태 초기화
- 기록 종료 후 자동 초기화: 화면 재표시 시 Completed 상태 감지

### UI 개선
- MainScreen에 탭 추가 (기록 측정 / 달린 기록)
- WalkingResultScreen 지도 터치 최적화 (스크롤 분리)
- 테스트용 위치 데이터 제공 (LocationTestData)

## 다음 단계

- [데이터 흐름도](./02-data-flow.md)에서 실제 동작 과정을 확인하세요.
- [컴포넌트 상세 설명](./03-components.md)에서 각 컴포넌트의 구현 세부사항을 확인하세요.
- [배터리 최적화](./06-battery-optimization.md)에서 배터리 최적화 전략을 확인하세요.
