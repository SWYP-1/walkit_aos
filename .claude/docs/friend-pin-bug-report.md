# 친구 핀(Friend Pin) 미표시 버그 분석 보고서

## 개요

인터랙티브 지도 화면에서 장소 핀(SPOT)은 정상 표시되나, 친구 핀(FRIEND)이 보이지 않는 문제.

---

## 원인 1: 렌더링 오버레이 고정 (renderState stuck)

### 현상
- `[drawMarkers] 완료 — 2개 그리기 성공` 로그는 찍히지만 지도에서 핀이 안 보임

### 원인
`KakaoMapView`는 `renderState != MapRenderState.Complete`이면 **50% 불투명 검은 오버레이**를 지도 위에 표시한다.

`InteractiveMapScreen`은 경로가 없으므로 `locations = emptyList()`를 전달하는데, 이때:

```
setLocations([])  →  renderState = Idle
updateMapFromState()  →  startCameraMove()  →  renderState = MovingCamera
실제 카메라 이동 없음 (빈 locations skip)
onCameraMoveEndListener 콜백 미발생
renderState 영원히 MovingCamera 상태 유지
isLoading = true  →  오버레이 표시
```

핀은 그려졌지만 오버레이로 가려진 상태.

### 수정
- `setLocations([])` → `renderState = Complete` 즉시 설정
- `updateMapFromState()` → `locations.isEmpty()` 시 early return (startCameraMove 호출 안 함)

---

## 원인 2: Lottie 캐릭터 Bitmap이 투명하게 렌더링

### 현상
- `[drawMarkers] 친구 핀(userId=28) — 캐릭터 Bitmap 사용` 로그는 찍힘
- 핀이 렌더링되나 투명/비가시 상태

### 원인 (2-A): 백그라운드 스레드에서 LottieDrawable.draw() 호출

```kotlin
// 기존 코드 - Dispatchers.Default에서 draw() 호출
async(Dispatchers.Default) {
    drawable.draw(Canvas(bitmap))  // ← 백그라운드에서 호출 시 투명 bitmap 생성
}
```

`LottieDrawable.draw()`는 메인 스레드에서 호출해야 정상 렌더링된다.

### 수정 (2-A)
```kotlin
withContext(Dispatchers.Main) {
    drawable.draw(Canvas(bitmap))
}
```

---

### 원인 (2-B): Lottie ImageAssetDelegate 누락으로 이미지 로드 실패

`LottieImageProcessor.replaceAssetP()`는 asset의 `"p"` 필드를 아래 형식으로 저장한다:

```
"p": "data:image/png;base64,iVBORw0KGgo..."
```

그런데 Lottie SDK는 `"p"` 필드를 **파일명**으로 해석하여 실제 파일을 찾으려 한다.  
`"data:image/png;base64,..."` 문자열로 파일을 찾을 수 없으므로 이미지가 렌더링되지 않아 투명한 bitmap 생성.

`LottieAnimationView`(아바타 행에서 사용)는 내부적으로 커스텀 이미지 로딩을 처리하지만,  
`LottieDrawable.draw()`는 이를 처리하지 못한다.

### 수정 (2-B)
```kotlin
drawable.setImageAssetDelegate { asset ->
    val fileName = asset.fileName ?: return@setImageAssetDelegate null
    if (fileName.startsWith("data:")) {
        val bytes = Base64.decode(fileName.substringAfter("base64,"), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } else null
}
```

`setImageAssetDelegate`에서 `"data:"` 접두사를 감지하면 직접 base64 디코딩하여 Bitmap을 반환.

---

## 원인 3: 투명 Bitmap을 non-null로 판단하여 폴백 미동작

### 현상
- 투명한 캐릭터 Bitmap이 생성되어도 `!= null`이므로 `ic_pin_friend` 폴백이 실행되지 않음
- 결과적으로 투명한 마커가 지도에 추가됨 (= 보이지 않음)

### 수정
```kotlin
// KakaoMapView.drawMarkers 내부
if (cachedBitmap != null && !cachedBitmap.isRecycled && !cachedBitmap.isTransparent()) {
    // 캐릭터 bitmap 사용
} else {
    // ic_pin_friend 폴백
}

// 투명 여부: 중앙+4귀 5개 샘플 픽셀 alpha=0 확인
private fun Bitmap.isTransparent(): Boolean { ... }
```

---

## 수정 요약

| # | 위치 | 원인 | 수정 |
|---|------|------|------|
| 1 | `KakaoMapViewModel` + `KakaoMapView` | `renderState` MovingCamera에 stuck → 오버레이 고정 | `locations` 빈 경우 즉시 Complete 처리 |
| 2-A | `InteractiveMapViewModel.renderLottieFirstFrame` | 백그라운드에서 `LottieDrawable.draw()` 호출 | `withContext(Dispatchers.Main)` |
| 2-B | `InteractiveMapViewModel.renderLottieFirstFrame` | `ImageAssetDelegate` 없어 base64 이미지 로드 실패 | `setImageAssetDelegate`로 직접 디코딩 |
| 3 | `KakaoMapView.drawMarkers` | 투명 Bitmap을 non-null로 판단해 폴백 미동작 | `isTransparent()` 샘플 체크 후 폴백 |
