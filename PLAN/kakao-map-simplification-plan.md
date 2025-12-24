# 카카오 맵 뷰 간편화 플랜

## 목표
스냅샷 기능을 "기록 완료" 버튼 클릭 시에만 PixelCopy로 생성하도록 변경
- 자동 스냅샷 생성 제거
- 복잡한 fallback 로직 제거 (OpenGL, View 방식 제거)
- PixelCopy 방식만 사용
- 두 가지 케이스 지원:
  1. 지도 + 경로가 그려진 화면 캡처
  2. 사진 + 경로가 그려진 화면 캡처
- 생성된 스냅샷을 localImagePath에 저장

## 현재 구조 분석

### 1. KakaoMapView.kt
- **현재 스냅샷 관련 기능:**
  - `captureSnapshot()` - 스냅샷 캡처 시작 (자동 호출)
  - `captureUsingPixelCopy()` - PixelCopy 방식 스냅샷 ✅ (유지)
  - `captureFromGLSurface()` - OpenGL 방식 스냅샷 ❌ (제거)
  - `captureFromView()` - View 방식 스냅샷 (fallback) ❌ (제거)
  - `saveSnapshotToFile()` - 스냅샷 파일 저장 ✅ (유지, 수정)
  - `findGLSurfaceView()` - GLSurfaceView 찾기 ✅ (PixelCopy용으로 유지)
  - `createBitmapFromGLSurface()` - OpenGL 비트맵 생성 ❌ (제거)
  - `waitForFramesToRender()` - 렌더링 대기 ❌ (제거, 단순화)
  - `waitForRouteLineRender()` - RouteLine 렌더링 대기 ❌ (제거)
  - 자동 스냅샷 생성 로직 ❌ (제거)

- **유지할 기능:**
  - `drawPath()` - 경로를 routeLine으로 표시 ✅
  - `moveCameraToPath()` - 카메라 자동 이동 ✅
  - `createRouteLineOptions()` - RouteLine 옵션 생성 ✅
  - MapView 초기화 및 표시 ✅ (항상 VISIBLE)

- **추가할 기능:**
  - `captureSnapshotOnDemand()` - 수동 스냅샷 생성 함수 (버튼 클릭 시 호출)
  - 스냅샷 생성 완료 콜백 (파일 경로 반환)

### 2. KakaoMapViewModel.kt
- **변경할 상태:**
  - `_snapshotState` - 제거하거나 수동 생성용으로 변경
  - `setSnapshot()` - 수동 호출용으로 변경

- **단순화할 상태:**
  - `MapRenderState` - Ready 상태에서 자동 스냅샷 생성 제거
    - 현재: Idle → MovingCamera → DrawingPath → Ready (자동 스냅샷 생성)
    - 변경: Idle → MovingCamera → DrawingPath → Complete (스냅샷 생성 없음)

- **유지할 기능:**
  - `setLocations()` - 경로 설정 ✅
  - `calculateCameraSettings()` - 카메라 설정 계산 ✅
  - `calculateZoomLevel()` - 줌 레벨 계산 ✅
  - `startCameraMove()` - 카메라 이동 시작 ✅
  - `onCameraMoveComplete()` - 카메라 이동 완료 ✅
  - `onPathDrawComplete()` - 경로 그리기 완료 ✅

- **추가할 기능:**
  - `captureSnapshot()` - 수동 스냅샷 생성 요청 함수

### 3. WalkingResultScreen.kt
- **현재 구조:**
  - 사진(emotionPhotoUri)이 있으면 사진 표시
  - 없으면 지도 스냅샷 표시
  - PathThumbnail로 경로 그리기

- **변경할 부분:**
  - 스냅샷 자동 생성 제거
  - "기록 완료" 버튼 클릭 시 스냅샷 생성
  - 두 가지 케이스 처리:
    1. **사진이 있는 경우:**
       - KakaoMapView 로딩하지 않음 (성능 최적화) ✅
       - 사진 + PathThumbnail만 표시
       - 사진 위에 경로가 그려진 Box의 PixelCopy
    2. **사진이 없는 경우:**
       - KakaoMapView 로딩 및 표시
       - 지도 + 경로 표시
       - KakaoMapView의 PixelCopy
  - 생성된 스냅샷을 localImagePath에 저장

### 4. 사진 + 경로 스냅샷 생성
- **새로운 컴포넌트 필요:**
  - 사진 + 경로가 그려진 Box를 PixelCopy로 캡처
  - PathThumbnail이 그려진 영역 포함

### 5. RouteThumbnailMap.kt
- 별도 파일이므로 영향 확인 필요
- 스냅샷 기능을 사용하는지 확인 후 필요시 수정

## 수정 계획

### Phase 1: KakaoMapView.kt 간편화

#### 1.1 자동 스냅샷 생성 제거
- [ ] `MapRenderState.Ready` 처리에서 자동 스냅샷 생성 제거
- [ ] `waitForRouteLineRender()` 제거 (자동 스냅샷용)
- [ ] 경로 그리기 완료 후 바로 Complete 상태로 변경

#### 1.2 스냅샷 함수 단순화
- [ ] `captureSnapshot()` → `captureSnapshotOnDemand()`로 변경 (수동 호출)
- [ ] `captureUsingPixelCopy()` 유지 및 단순화 ✅
- [ ] `captureFromGLSurface()` 삭제 ❌
- [ ] `captureFromView()` 삭제 ❌
- [ ] `createBitmapFromGLSurface()` 삭제 ❌
- [ ] `waitForFramesToRender()` 단순화 (PixelCopy 전용)
- [ ] `findGLSurfaceView()` 유지 (PixelCopy용) ✅
- [ ] `saveSnapshotToFile()` 유지 및 수정 (파일 경로 반환) ✅

#### 1.3 컴포저블 파라미터 수정
- [ ] `onSnapshotCaptured` 파라미터 제거 (자동 콜백)
- [ ] `showMapView` 파라미터 제거 (항상 true로 고정)
- [ ] `captureSnapshot()` 함수 추가 (수동 호출용)
- [ ] 스냅샷 생성 완료 시 파일 경로 반환 콜백 추가

#### 1.4 렌더링 로직 단순화
- [ ] `MapRenderState.Ready` → `Complete`로 변경
- [ ] 스냅샷 Image 표시 제거, MapView만 표시
- [ ] MapView는 항상 VISIBLE로 표시

### Phase 2: KakaoMapViewModel.kt 수정

#### 2.1 스냅샷 상태 변경
- [ ] `_snapshotState` 유지 (수동 생성용)
- [ ] `setSnapshot()` 함수 유지 (수동 호출용)
- [ ] 자동 스냅샷 생성 로직 제거

#### 2.2 렌더링 상태 단순화
- [ ] `MapRenderState.Ready` → `MapRenderState.Complete`로 변경
- [ ] 자동 스냅샷 생성 관련 로직 제거

#### 2.3 수동 스냅샷 생성 함수 추가
- [ ] `captureSnapshot()` suspend 함수 추가
- [ ] 스냅샷 생성 완료 시 파일 경로 반환

### Phase 3: WalkingResultScreen.kt 수정

#### 3.1 스냅샷 자동 생성 제거
- [ ] `snapshotState` 구독 제거 (자동 생성용)
- [ ] 자동 스냅샷 Image 표시 로직 제거

#### 3.2 지도/사진 표시 변경
- [ ] 사진이 있으면 KakaoMapView 로딩하지 않음 (조건부 로딩)
- [ ] 사진이 없으면 KakaoMapView 직접 표시 (항상 VISIBLE)
- [ ] 사진 + 경로 Box 직접 표시 (사진이 있는 경우)

#### 3.3 "기록 완료" 버튼 수정
- [ ] 버튼 클릭 시 스냅샷 생성 함수 호출
- [ ] 두 가지 케이스 처리:
  - [ ] 케이스 1: **사진이 있는 경우** → 사진+경로 Box의 PixelCopy (맵뷰 로딩 없음)
  - [ ] 케이스 2: **사진이 없는 경우** → KakaoMapView의 PixelCopy
- [ ] 생성된 스냅샷을 파일로 저장
- [ ] localImagePath에 저장
- [ ] 저장 완료 후 서버 동기화

#### 3.4 사진 + 경로 스냅샷 생성
- [ ] 사진 + PathThumbnail이 그려진 Box를 PixelCopy로 캡처
- [ ] Box에 Modifier를 추가하여 캡처 가능하도록 설정

### Phase 4: 스냅샷 생성 유틸리티 함수 추가

#### 4.1 공통 스냅샷 생성 함수
- [ ] `captureViewSnapshot()` 함수 생성 (View를 PixelCopy로 캡처)
- [ ] 두 가지 케이스 모두에서 사용 가능하도록 범용 함수로 구현

#### 4.2 파일 저장 및 경로 반환
- [ ] 스냅샷을 파일로 저장하는 함수 수정
- [ ] 저장된 파일 경로를 반환하여 localImagePath에 저장 가능하도록

#### 4.3 상수 정리
- [ ] MapSnapshotConstants에서 불필요한 상수 제거
- [ ] PixelCopy 관련 상수만 유지
- [ ] RouteLine 관련 상수 유지

#### 4.4 Import 정리
- [ ] OpenGL 관련 import 제거
- [ ] View fallback 관련 import 제거
- [ ] PixelCopy 관련 import 유지

## 최종 구조

### KakaoMapView.kt (간편화 후)
```kotlin
@Composable
fun KakaoMapView(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    viewModel: KakaoMapViewModel = hiltViewModel(),
) {
    // MapView 항상 표시 (VISIBLE)
    // 경로를 routeLine으로 표시
    // 자동으로 카메라 이동 및 축척
    // 스냅샷은 수동으로만 생성
}

// 수동 스냅샷 생성 함수
suspend fun captureMapSnapshot(
    mapView: MapView,
    context: Context,
    onComplete: (String?) -> Unit // 파일 경로 반환
) {
    // PixelCopy로만 캡처
    // 파일로 저장 후 경로 반환
}
```

### KakaoMapViewModel.kt (수정 후)
```kotlin
class KakaoMapViewModel {
    val uiState: StateFlow<KakaoMapUiState>
    val renderState: StateFlow<MapRenderState>
    
    fun setLocations(locations: List<LocationPoint>)
    fun onPathDrawComplete() // 스냅샷 생성 없이 바로 Complete
    
    // 수동 스냅샷 생성
    suspend fun captureSnapshot(mapView: MapView, context: Context): String? // 파일 경로 반환
}
```

### MapRenderState (간편화 후)
```kotlin
sealed class MapRenderState {
    data object Idle : MapRenderState()
    data object MovingCamera : MapRenderState()
    data object DrawingPath : MapRenderState()
    data object Complete : MapRenderState() // Ready → Complete (자동 스냅샷 없음)
}
```

### WalkingResultScreen.kt (수정 후)
```kotlin
// 조건부 맵뷰 로딩
if (emotionPhotoUri == null) {
    // 사진이 없을 때만 맵뷰 로딩
    KakaoMapView(
        locations = locations,
        viewModel = mapViewModel
    )
} else {
    // 사진이 있으면 맵뷰 로딩하지 않음
    // 사진 + 경로만 표시
    PhotoWithPathView(
        photoUri = emotionPhotoUri,
        locations = locations
    )
}

// "기록 완료" 버튼 클릭 시
Button(onClick = {
    viewModelScope.launch {
        val imagePath = if (emotionPhotoUri != null) {
            // 케이스 1: 사진 + 경로 스냅샷 (맵뷰 로딩 없음)
            capturePhotoWithPathSnapshot(photoWithPathBox, context)
        } else {
            // 케이스 2: 지도 + 경로 스냅샷
            mapViewModel.captureSnapshot(mapView, context)
        }
        
        // localImagePath에 저장
        if (imagePath != null) {
            walkingSessionRepository.updateSessionImageAndNote(
                localId = currentSessionLocalId,
                imageUri = Uri.fromFile(File(imagePath)),
                note = null
            )
        }
        
        // 서버 동기화
        viewModel.syncSessionToServer()
        onNavigateBack()
    }
}) {
    Text("기록완료")
}
```

## 예상 효과

1. **코드 간소화:**
   - 약 200줄 이상의 불필요한 스냅샷 관련 코드 제거 (OpenGL, View fallback)
   - 복잡한 렌더링 상태 머신 단순화
   - 자동 스냅샷 생성 로직 제거

2. **성능 개선:**
   - 불필요한 자동 스냅샷 생성 오버헤드 제거
   - 사용자가 원할 때만 스냅샷 생성
   - PixelCopy만 사용하여 단순화

3. **유지보수성 향상:**
   - 코드 복잡도 감소
   - 스냅샷 생성 시점 명확화
   - 버그 가능성 감소

4. **사용자 경험:**
   - 실시간 지도 상호작용 가능 (스냅샷 대신 실제 MapView)
   - 줌/팬 등 제스처 지원
   - "기록 완료" 시점에만 스냅샷 생성하여 성능 최적화

5. **성능 최적화:**
   - 사진이 있으면 맵뷰 로딩하지 않아 메모리 및 초기화 시간 절약
   - 불필요한 리소스 사용 방지

5. **기능 확장:**
   - 사진 + 경로 스냅샷 지원
   - 두 가지 케이스 모두 동일한 방식으로 처리 가능

## 주의사항

1. **RouteThumbnailMap.kt 확인 필요:**
   - 스냅샷 기능을 사용하는지 확인
   - 사용한다면 별도 처리 필요

2. **다른 화면에서 스냅샷 사용 여부 확인:**
   - `snapshotState`를 구독하는 다른 화면이 있는지 확인
   - 있다면 해당 화면도 수정 필요

3. **테스트:**
   - 경로 표시 정상 동작 확인
   - 자동 축척/카메라 이동 정상 동작 확인
   - 여러 경로 길이에 대한 테스트

## 구현 순서

1. ✅ 플랜 작성 (현재 단계)
2. ⏳ KakaoMapView.kt 간편화
   - [ ] 자동 스냅샷 생성 제거
   - [ ] OpenGL, View fallback 제거
   - [ ] PixelCopy만 사용하도록 단순화
   - [ ] 수동 스냅샷 생성 함수 추가
3. ⏳ KakaoMapViewModel.kt 수정
   - [ ] 렌더링 상태 단순화 (Ready → Complete)
   - [ ] 수동 스냅샷 생성 함수 추가
4. ⏳ 공통 스냅샷 유틸리티 함수 생성
   - [ ] `captureViewSnapshot()` 함수 생성
   - [ ] View를 PixelCopy로 캡처하는 범용 함수
5. ⏳ WalkingResultScreen.kt 수정
   - [ ] 자동 스냅샷 표시 제거
   - [ ] 조건부 맵뷰 로딩 (사진이 있으면 맵뷰 로딩하지 않음)
   - [ ] 사진이 있으면 사진 + 경로만 표시
   - [ ] 사진이 없으면 MapView 직접 표시
   - [ ] "기록 완료" 버튼에 스냅샷 생성 로직 추가
   - [ ] 두 가지 케이스 처리 (지도/사진)
   - [ ] localImagePath에 저장
6. ⏳ RouteThumbnailMap.kt 확인 및 수정 (필요시)
7. ⏳ 테스트 및 검증
   - [ ] 지도 + 경로 스냅샷 생성 테스트
   - [ ] 사진 + 경로 스냅샷 생성 테스트
   - [ ] 파일 저장 및 localImagePath 저장 테스트

