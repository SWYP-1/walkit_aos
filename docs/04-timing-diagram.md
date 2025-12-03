# 타이밍 다이어그램

이 문서는 활동 상태 변경 시 발생하는 지연 시간을 시각적으로 설명합니다.

## 활동 상태 변경 시 지연 시간 분석

### 시나리오 1: 정지 → 달리기 (최악의 경우)

```
시간축: 0초 ──────────────────────────────────────────> 4초

0초: 사용자가 달리기 시작
    │
    │ [가속도계: 즉시 움직임 감지 (수십 밀리초)]
    │ [Activity Recognition API가 활동 상태를 감지하는 중...]
    │ (최대 1초 소요 - DETECTION_INTERVAL_MS = 1000L, P0: 즉각 피드백)
    │
    ▼
1초: Activity Recognition이 "RUNNING" 감지
    │
    ├─▶ ActivityRecognitionManager
    │       │
    │       └─▶ BroadcastReceiver.onReceive()
    │               │
    │               └─▶ callbackFlow { trySend(ActivityState(RUNNING)) }
    │                       │
    │                       └─▶ 약 10-50ms 소요
    │
    ├─▶ WalkingViewModel
    │       │
    │       └─▶ handleActivityStateChange(ActivityState(RUNNING))
    │               │
    │               ├─▶ 활동 상태 통계 업데이트 (약 1ms)
    │               │
    │               ├─▶ UI State 업데이트 (약 1ms)
    │               │       │
    │               │       └─▶ WalkingScreen 자동 업데이트
    │               │               │
    │               │               └─▶ UI에 "달리기" 표시 (즉시)
    │               │
    │               └─▶ Broadcast 전송 (약 10-50ms)
    │                       │
    │                       │ Intent(ACTION_ACTIVITY_UPDATE)
    │                       │     .putExtra(EXTRA_ACTIVITY_TYPE, RUNNING.ordinal)
    │                       │
    │                       └─▶ sendBroadcast()
    │
    └─▶ LocationTrackingService
            │
            └─▶ registerActivityReceiver()
                    │
                    └─▶ BroadcastReceiver.onReceive()
                            │
                            ├─▶ currentActivityType = RUNNING (약 1ms)
                            │
                            └─▶ updateLocationRequest() (즉시 실행)
                                    │
                                    ├─▶ 기존 LocationRequest 제거 (약 10-50ms)
                                    │       │
                                    │       └─▶ fusedLocationClient.removeLocationUpdates()
                                    │
                                    ├─▶ 새 LocationRequest 생성 (약 1ms)
                                    │       │
                                    │       └─▶ 간격: 2초 (RUNNING)
                                    │
                                    └─▶ 새 LocationRequest 등록 (약 10-50ms)
                                            │
                                            └─▶ fusedLocationClient.requestLocationUpdates()
    │
    │ [새로운 LocationRequest가 적용되어 위치 업데이트를 기다리는 중...]
    │ (최대 2초 소요 - RUNNING 간격)
    │
    ▼
4초: 첫 번째 위치 업데이트 수신
    │
    ├─▶ LocationCallback.onLocationResult()
    │       │
    │       ├─▶ GPS 정확도 필터링
    │       ├─▶ 최소 거리 필터링
    │       │
    │       └─▶ locationPoints.add(newLocationPoint)
    │               │
    │               └─▶ sendNewLocationDataBroadcast()
    │                       │
    │                       └─▶ Broadcast 전송
    │
    └─▶ WalkingViewModel
            │
            └─▶ registerLocationReceiver()
                    │
                    └─▶ BroadcastReceiver.onReceive()
                            │
                            ├─▶ locationPoints에 추가
                            ├─▶ 거리 계산
                            │
                            └─▶ UI 업데이트
                                    │
                                    └─▶ WalkingScreen에 거리 표시
```

**총 지연 시간**: 최대 3초
- 가속도계 즉각 피드백: 수십 밀리초 (선택적 UI 반영)
- Activity Recognition 감지: 최대 1초 (P0: 1초로 단축)
- 위치 업데이트 대기: 최대 2초 (RUNNING 간격)

---

### 시나리오 2: 정지 → 달리기 (최선의 경우)

```
시간축: 0초 ──────────────────────────────────────────> 2초

0초: 사용자가 달리기 시작
    │
    │ [운 좋게 Activity Recognition이 바로 감지]
    │ (0초에 감지 - DETECTION_INTERVAL_MS의 시작 시점과 일치)
    │
    ▼
0초: Activity Recognition이 즉시 "RUNNING" 감지
    │
    ├─▶ ActivityRecognitionManager → WalkingViewModel
    │       (약 10-50ms)
    │
    ├─▶ WalkingViewModel → LocationTrackingService
    │       (Broadcast 전송, 약 10-50ms)
    │
    └─▶ LocationTrackingService
            │
            └─▶ updateLocationRequest() (즉시 실행, 약 60-100ms)
    │
    │ [새로운 LocationRequest가 적용되어 위치 업데이트를 기다리는 중...]
    │ (최대 2초 소요)
    │
    ▼
2초: 첫 번째 위치 업데이트 수신
    │
    └─▶ UI에 위치 반영
```

**총 지연 시간**: 최대 2초
- Activity Recognition 감지: 0초 (즉시)
- 위치 업데이트 대기: 최대 2초

---

## 지연 시간 요약

### 활동 상태 변경 감지

| 단계 | 소요 시간 | 설명 |
|------|----------|------|
| 가속도계 움직임 감지 | 수십 밀리초 | 즉각 피드백 (선택적) |
| Activity Recognition 감지 | 0~1초 | `DETECTION_INTERVAL_MS = 1000L` (P0: 1초로 단축) |
| Broadcast 전송 | 10~50ms | ViewModel → Service |
| LocationRequest 업데이트 | 60~100ms | 기존 제거 + 새로 등록 (배터리 상태 고려) |

### 위치 업데이트 대기

| 활동 상태 | 업데이트 간격 | 최대 대기 시간 |
|----------|--------------|---------------|
| 달리기 (RUNNING) | 2초 | 2초 |
| 걷기 (WALKING) | 3초 | 3초 |
| 정지 (STILL) | 8초 | 8초 |
| 차량/자전거 | 5초 | 5초 |

### 총 지연 시간

| 시나리오 | 가속도계 | Activity Recognition | 위치 업데이트 | 총 지연 |
|---------|---------|---------------------|--------------|---------|
| 최악의 경우 | 즉시 (선택적) | 1초 | 2초 (RUNNING) | **3초** |
| 최선의 경우 | 즉시 | 0초 | 2초 (RUNNING) | **2초** |
| 평균 | 즉시 | 0.5초 | 2초 (RUNNING) | **2.5초** |

---

## 개선 전후 비교

### 개선 전 (문제 상황)

```
정지 상태 (8초 간격) → 달리기 시작
    │
    │ [Activity Recognition이 상태 변경 감지: 최대 2초]
    │
    ▼
2초: 상태 변경 감지
    │
    │ [하지만 LocationRequest가 업데이트되지 않음]
    │ [기존 8초 간격을 계속 사용]
    │
    ▼
10초: 다음 위치 업데이트 (8초 후)
```

**총 지연**: 최대 10초 (2초 + 8초)

### 개선 후 (현재)

```
정지 상태 (8초 간격) → 달리기 시작
    │
    │ [Activity Recognition이 상태 변경 감지: 최대 2초]
    │
    ▼
2초: 상태 변경 감지
    │
    ├─▶ LocationRequest 즉시 업데이트 (약 100ms)
    │       │
    │       └─▶ 간격: 8초 → 2초로 변경
    │
    ▼
4초: 첫 번째 위치 업데이트 (2초 후)
```

**총 지연**: 최대 4초 (2초 + 2초)

**개선 효과**: 10초 → 4초 (60% 단축)

---

## 실시간 데이터 업데이트 타이밍

### 걸음 수 업데이트

```
시간축: 0초 ──────────────────────────────────────────> 10초

0초: 사용자가 걸음 시작
    │
    ▼
즉시: SensorEventListener.onSensorChanged()
    │   (실제 걸음 감지 시 즉시 호출)
    │
    └─▶ StepCounterManager
            │
            └─▶ callbackFlow { trySend(stepCount) }
                    │
                    └─▶ 약 1-5ms
    │
    ▼
즉시: WalkingViewModel
    │
    └─▶ _uiState.value = state.copy(stepCount = stepCount)
            │
            └─▶ 약 1ms
    │
    ▼
즉시: WalkingScreen (자동 업데이트)
    │
    └─▶ UI에 걸음 수 표시
```

**지연 시간**: 거의 없음 (1-5ms)

### 위치 업데이트

```
시간축: 0초 ──────────────────────────────────────────> 3초

0초: GPS 위치 업데이트
    │
    ▼
즉시: LocationCallback.onLocationResult()
    │
    ├─▶ GPS 정확도 필터링 (약 1ms)
    ├─▶ 최소 거리 필터링 (약 1ms)
    │
    └─▶ locationPoints.add(point)
            │
            └─▶ sendNewLocationDataBroadcast()
                    │
                    └─▶ 약 10-50ms
    │
    ▼
즉시: WalkingViewModel
    │
    └─▶ BroadcastReceiver.onReceive()
            │
            ├─▶ JSON 디코딩 (약 1-5ms)
            ├─▶ locationPoints에 추가 (약 1ms)
            ├─▶ 거리 계산 (약 1-5ms)
            │
            └─▶ _uiState.value = state.copy(distance = totalDistance)
                    │
                    └─▶ 약 1ms
    │
    ▼
즉시: WalkingScreen (자동 업데이트)
    │
    └─▶ UI에 거리 표시
```

**지연 시간**: 약 15-65ms (거의 실시간)

---

## 결론

### 활동 상태 변경 시
- **최대 지연**: 3초 (Activity Recognition 1초 + 위치 업데이트 2초)
- **평균 지연**: 2.5초
- **최소 지연**: 2초
- **즉각 피드백**: 가속도계로 수십 밀리초 내 감지 (선택적)

### 실시간 데이터 업데이트
- **걸음 수**: 거의 즉시 (1-5ms)
- **위치**: 거의 즉시 (15-65ms)
- **활동 상태**: 최대 1초 (Activity Recognition 간격, P0: 1초로 단축)
- **가속도계 움직임**: 거의 즉시 (수십 밀리초)

### 개선 효과
- 활동 상태 변경 시 지연: 10초 → 3초 (70% 단축)
- Activity Recognition 간격: 2초 → 1초 (50% 단축)
- 가속도계 즉각 피드백 추가
- 실시간 반영: 거의 즉시

---

## 다음 단계

- [FAQ](./05-faq.md)에서 자주 묻는 질문을 확인하세요.
- [컴포넌트 상세 설명](./03-components.md)에서 구현 세부사항을 확인하세요.
