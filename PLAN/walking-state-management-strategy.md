# Walking 상태 관리 하이브리드 전략

## 핵심 전략: 하이브리드 접근
"적절한 수준의 추상화"와 "단일 소스"의 균형

## 전체 플로우

```
┌─────────────────────────────────────────────────────────┐
│  진행 중 (Walking 중)                                   │
│  ────────────────────────                               │
│  • 별도 StateFlow 사용 (성능 최적화)                    │
│  • 빠른 업데이트 (GPS 업데이트마다)                      │
│  • 타입 안정성 (nullable 최소화)                        │
│  • 동시성 안전 (독립 업데이트)                           │
│                                                          │
│  StateFlow: _locations (MutableStateFlow)               │
│  - GPS 업데이트마다 즉시 반영                            │
│  - UI에서 실시간 경로 표시                               │
└─────────────────────────────────────────────────────────┘
                        ↓ stopWalking()
┌─────────────────────────────────────────────────────────┐
│  완료 시점 (stopWalking())                              │
│  ────────────────────────                               │
│  • StateFlow 값 수집                                     │
│  • WalkingSession 생성                                   │
│  • Completed(session) 상태로 전환                       │
│                                                          │
│  createCompletedSession():                              │
│  - collectedLocations = _locations.value                │
│  - session = WalkingSession(locations=collectedLocations)│
│  - _uiState.value = Completed(session)                  │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  결과 화면 (WalkingResultScreen)                        │
│  ────────────────────────                               │
│  • session만 사용 (단일 소스)                            │
│  • 중복 파라미터 제거                                    │
│  • session.locations, session.localImagePath 사용        │
│                                                          │
│  변경 전:                                                │
│  - locations: List<LocationPoint> (별도 파라미터)      │
│  - currentSession.locations (중복)                       │
│                                                          │
│  변경 후:                                                │
│  - currentSession.locations만 사용                       │
│  - locations 파라미터 제거                               │
└─────────────────────────────────────────────────────────┘
```

## 구현 세부사항

### 1. 진행 중 (Walking 중) - 별도 StateFlow 사용

**위치**: `WalkingViewModel.kt`

```kotlin
// Location 리스트를 StateFlow로 노출 (Shared ViewModel을 위한)
private val _locations = MutableStateFlow<List<LocationPoint>>(emptyList())
val locations: StateFlow<List<LocationPoint>> = _locations.asStateFlow()

// GPS 업데이트마다 즉시 반영
private fun handleLocationUpdate(newLocations: List<LocationPoint>) {
    val currentLocations = _locations.value.toMutableList()
    
    // 새로운 위치 포인트 추가 (중복 제거)
    newLocations.forEach { newPoint ->
        val exists = currentLocations.any { existing ->
            existing.timestamp == newPoint.timestamp ||
            (kotlin.math.abs(existing.latitude - newPoint.latitude) < 0.000001 &&
            kotlin.math.abs(existing.longitude - newPoint.longitude) < 0.000001)
        }
        
        if (!exists) {
            currentLocations.add(newPoint)
        }
    }
    
    _locations.value = currentLocations
}
```

**장점**:
- ✅ 빠른 업데이트: GPS 업데이트마다 즉시 반영
- ✅ 성능 최적화: `WalkingUiState.Walking`은 경량 유지 (stepCount, duration만)
- ✅ 타입 안정성: nullable 최소화
- ✅ 동시성 안전: 독립적인 StateFlow 업데이트

### 2. 완료 시점 (stopWalking()) - StateFlow 값 수집 및 세션 생성

**위치**: `WalkingViewModel.kt`

```kotlin
suspend fun stopWalking() {
    tracking.stopTracking()
    durationJob?.cancel()
    
    // 완료된 세션 생성 (현재 메모리 데이터로 즉시 생성)
    val completedSession = createCompletedSession()
    
    // DB에 저장하고 localId를 받아옴
    currentSessionLocalId = walkingSessionRepository.createSessionPartial(completedSession)
    
    // Completed 상태로 변경
    _uiState.value = WalkingUiState.Completed(completedSession)
}

private fun createCompletedSession(): WalkingSession {
    val preEmotion = _preWalkingEmotion.value
        ?: throw IllegalStateException("산책 전 감정이 선택되지 않았습니다")
    
    val postEmotion = _postWalkingEmotion.value ?: preEmotion
    
    val endTime = System.currentTimeMillis()
    val collectedLocations = _locations.value  // ⭐ StateFlow 값 수집
    val totalDistance = calculateTotalDistance(collectedLocations)
    
    return WalkingSession(
        startTime = startTimeMillis,
        endTime = endTime,
        stepCount = lastStepCount,
        locations = collectedLocations,  // ⭐ 수집된 locations 사용
        totalDistance = totalDistance,
        preWalkEmotion = preEmotion,
        postWalkEmotion = postEmotion,
        note = null,
        imageUrl = null,  // Deprecated 필드
        localImagePath = null,  // 나중에 업데이트
        serverImageUrl = null,  // ⚠️ 서버 동기화 후에만 받음 (walking 과정에서는 사용 안 함)
        createdDate = DateUtils.formatToIsoDateTime(startTimeMillis)
    )
}
```

**핵심 포인트**:
- ✅ `_locations.value`를 한 번만 수집하여 세션 생성
- ✅ `Completed(session)` 상태로 전환하여 단일 소스 확보
- ⚠️ `serverImageUrl`은 서버 동기화 후에만 받기 때문에 walking 과정에서는 사용되지 않음

### 3. 결과 화면 (WalkingResultScreen) - session만 사용

**변경 전**:
```kotlin
@Composable
fun WalkingResultScreen(
    currentSession: WalkingSession?,
    locations: List<LocationPoint>,  // ❌ 중복 파라미터
    // ...
) {
    // locations와 currentSession.locations 둘 다 사용 (중복)
    PathThumbnail(
        locations = locations.ifEmpty { currentSession.locations },
        // ...
    )
}
```

**변경 후**:
```kotlin
@Composable
fun WalkingResultScreen(
    currentSession: WalkingSession?,
    // locations 파라미터 제거 ✅
    // ...
) {
    // currentSession.locations만 사용 (단일 소스)
    PathThumbnail(
        locations = currentSession?.locations ?: emptyList(),
        // ...
    )
    
    // 이미지도 session에서 가져옴
    val imagePath = currentSession?.localImagePath
    // serverImageUrl은 서버 동기화 후에만 사용 (walking 과정에서는 사용 안 함)
}
```

**변경 사항**:
1. ✅ `WalkingResultScreen`에서 `locations` 파라미터 제거
2. ✅ `currentSession.locations`만 사용
3. ✅ `WalkingResultRoute`에서 `locations` 전달 제거
4. ✅ 이미지 경로도 `session.localImagePath` 사용

## serverImageUrl 처리 전략

### ⚠️ 중요: serverImageUrl은 walking 과정에서 사용되지 않음

**이유**:
- `serverImageUrl`은 서버에 이미지를 업로드한 후에만 response로 받음
- walking 전체 과정에서는 전혀 활용할 일이 없음
- 서버 동기화는 `syncSessionToServer()` 호출 시에만 발생

**처리 방식**:
```kotlin
// WalkingSession 생성 시
WalkingSession(
    // ...
    localImagePath = null,  // 나중에 업데이트 (스냅샷 생성 후)
    serverImageUrl = null,  // 서버 동기화 후에만 업데이트
)

// 서버 동기화 후 (WalkingSessionRepository에서)
// serverImageUrl이 response로 받아와서 DB에 업데이트됨
// 하지만 walking 과정에서는 사용되지 않음
```

## 파일별 변경 사항

### 1. WalkingViewModel.kt
- ✅ 이미 구현됨 (변경 불필요)
- `_locations` StateFlow 사용
- `createCompletedSession()`에서 `_locations.value` 수집

### 2. WalkingResultRoute.kt
**변경 필요**:
```kotlin
// 변경 전
val locations by viewModel.locations.collectAsStateWithLifecycle()

WalkingResultScreen(
    locations = locations,  // ❌ 제거
    // ...
)

// 변경 후
WalkingResultScreen(
    // locations 파라미터 제거 ✅
    // ...
)
```

### 3. WalkingResultScreen.kt
**변경 필요**:
```kotlin
// 변경 전
@Composable
fun WalkingResultScreen(
    locations: List<LocationPoint>,  // ❌ 제거
    // ...
)

// 변경 후
@Composable
fun WalkingResultScreen(
    // locations 파라미터 제거 ✅
    // ...
) {
    // currentSession.locations만 사용
    PathThumbnail(
        locations = currentSession?.locations ?: emptyList(),
        // ...
    )
    
    KakaoMapView(
        locations = currentSession?.locations ?: emptyList(),
        // ...
    )
}
```

## 장점 요약

### 1. 성능 최적화
- ✅ Walking 중: 경량 `WalkingUiState.Walking` (stepCount, duration만)
- ✅ 빠른 업데이트: GPS 업데이트마다 즉시 반영
- ✅ 메모리 효율: locations는 별도 StateFlow로 관리

### 2. 단일 소스 원칙
- ✅ 결과 화면: `session.locations`만 사용
- ✅ 중복 제거: `locations` 파라미터 제거
- ✅ 일관성: 모든 데이터는 `session`에서 가져옴

### 3. 타입 안정성
- ✅ nullable 최소화: `currentSession?.locations ?: emptyList()`
- ✅ 명확한 데이터 흐름: StateFlow → Session → UI

### 4. 유지보수성
- ✅ 단일 소스: 데이터 변경 시 한 곳만 수정
- ✅ 명확한 책임: 각 단계별 명확한 역할
- ✅ 테스트 용이: 각 단계별 독립적 테스트 가능

## 테스트 시나리오

### 1. Walking 중 업데이트 테스트
```kotlin
// GPS 업데이트마다 _locations StateFlow 업데이트 확인
viewModel.handleLocationUpdate(listOf(location1, location2))
assertEquals(2, viewModel.locations.value.size)
```

### 2. stopWalking() 세션 생성 테스트
```kotlin
// stopWalking() 호출 시 세션 생성 확인
viewModel.stopWalking()
val state = viewModel.uiState.value
assertTrue(state is WalkingUiState.Completed)
assertEquals(collectedLocations, state.session.locations)
```

### 3. 결과 화면 단일 소스 테스트
```kotlin
// WalkingResultScreen에서 session.locations만 사용 확인
WalkingResultScreen(
    currentSession = session,
    // locations 파라미터 없음 ✅
)
// session.locations만 사용됨
```

## 마이그레이션 체크리스트

- [ ] `WalkingResultScreen`에서 `locations` 파라미터 제거
- [ ] `WalkingResultScreen` 내부에서 `currentSession.locations`만 사용
- [ ] `WalkingResultRoute`에서 `locations` StateFlow 수집 제거
- [ ] `WalkingResultRoute`에서 `locations` 파라미터 전달 제거
- [ ] `KakaoMapView`에 `currentSession.locations` 전달
- [ ] `PathThumbnail`에 `currentSession.locations` 전달
- [ ] 이미지 경로도 `session.localImagePath` 사용 확인
- [ ] 테스트 코드 업데이트

## 결론

하이브리드 접근 전략을 통해:
1. **진행 중**: 별도 StateFlow로 빠른 업데이트 (성능 최적화)
2. **완료 시점**: StateFlow 값 수집하여 세션 생성 (단일 소스 확보)
3. **결과 화면**: session만 사용 (중복 제거, 단일 소스)

이 전략은 성능과 단일 소스 원칙의 균형을 맞추는 최적의 접근입니다.





