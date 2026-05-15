# 버그 분석: 시스템 Navigation Bar 영역에 Bottom Sheet 내용이 비쳐 보이는 현상

## 개요

| 항목 | 내용 |
|------|------|
| 증상 | 지도 탭에서 장소 검색 Bottom Sheet를 열었을 때, 시스템 Navigation Bar 영역에 시트 내용이 희미하게 비쳐 보임 |
| 플랫폼 | Android (API 35 타겟, Edge-to-Edge 적용) |
| 영향 범위 | 지도 탭 전체 (`InteractiveMapRoute`) |
| 해결 난이도 | 높음 — 원인이 3개 레이어에 걸쳐 복합적으로 얽혀 있어 단순 색상 설정으로 해결되지 않음 |

---

## 증상

- 지도 화면에서 장소 검색 / 최근 검색어 상태로 Bottom Sheet가 확장될 때, 시스템 Navigation Bar 영역(화면 최하단)에 시트 UI(검색바, 텍스트 등)가 반투명하게 비쳐 보임
- 다른 탭(홈, 기록, 마이페이지)에서는 동일 현상 없음

---

## 원인 분석

### 시도 1 — 가설: `window.navigationBarColor` 미적용 (API 35 deprecated)

`MainActivity`에서 `window.navigationBarColor = white`로 설정했으나 API 35에서 해당 API는 deprecated되어 실제로 적용되지 않음. 또한 `isNavigationBarContrastEnforced = false`로 시스템의 Nav Bar 대비 강제를 꺼둔 상태였음.

**조치**: `isNavigationBarContrastEnforced = false` 제거 및 `BottomSheetScaffold`에 `sheetContainerColor = Color.White` 명시.

**결과**: 여전히 비쳐 보임 → 근본 원인이 다른 곳에 있음을 확인.

---

### 시도 2 — 레이아웃 계층 전체 추적

화면 구조를 레이어별로 재추적:

```
MainActivity (Edge-to-Edge)
└── NavGraph
    └── MainScreen
        └── Scaffold (bottomBar = CustomBottomNavigation)
            └── Box (padding = paddingValues)
                └── InteractiveMapRoute
                    └── BottomSheetScaffold
                        └── Sheet (SpotBottomSheetContent)
```

**핵심 발견**: `BottomSheetScaffold`의 시트는 Compose의 일반적인 clip 경계를 넘어서 화면 전체에 그려질 수 있음 (시스템 바 뒤까지 확장). 문제는 시트가 Nav Bar 영역에 그려지더라도 그 앞을 막아줄 불투명 배경이 없다는 것.

---

### 진짜 원인: `CustomBottomNavigation`의 Modifier 순서 오류

```kotlin
// 수정 전 (버그 있음)
Box(
    modifier = modifier
        .padding(bottom = navBarHeight)  // 1) 먼저 padding 적용
        .background(Color.White)         // 2) 이후 배경 → padding 영역(Nav Bar)은 흰색 미적용
)
```

Compose에서 Modifier는 **선언 순서대로** 적용된다. `padding` 이후에 `background`를 선언하면 배경은 padding이 제거된 영역(Nav Bar 위)에만 그려진다. **Nav Bar 영역 자체는 흰색으로 덮이지 않아 투명한 상태**로 남음.

`BottomSheetScaffold`의 시트는 이 투명한 Nav Bar 영역 뒤까지 그려지고 있었으므로, 시트 내용이 Nav Bar를 통해 비쳐 보이는 현상이 발생한 것.

---

## 해결

`CustomBottomNavigation`에서 흰색 배경을 Nav Bar padding보다 **먼저** 적용하고, padding은 내부 `Row`로 이동:

```kotlin
// 수정 후
Box(
    modifier = modifier
        .fillMaxWidth()
        .background(Color.White)  // Nav Bar 영역 포함 전체에 흰색 배경 먼저 적용
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = navBarHeight),  // 아이템 위치만 Nav Bar 위로 올림
        ...
    )
}
```

이렇게 하면:
- `Box`의 흰색 배경이 Nav Bar 영역까지 완전히 덮음
- 아이템은 기존과 동일하게 Nav Bar 위에 배치됨
- `BottomSheetScaffold` 시트가 Nav Bar 영역까지 그려져도 흰색 `Box`가 가림

---

## 배운 점

### 1. Compose Modifier 순서는 시각적 결과에 직접적인 영향을 미친다
`padding → background`와 `background → padding`은 완전히 다른 결과를 낸다. 특히 시스템 바 영역처럼 "안 보이는 공간"에도 배경을 채워야 할 때 순서 실수가 눈에 잘 띄지 않는 버그를 만든다.

### 2. `BottomSheetScaffold`는 부모 clip 경계를 넘어 그릴 수 있다
Material3의 `BottomSheetScaffold` 시트는 `SubcomposeLayout` 기반으로 화면 전체에 오버레이되기 때문에, 부모 컨테이너의 clip 경계를 벗어나 시스템 바 영역까지 그려진다. 바텀시트 관련 버그는 레이아웃 계층 전체를 봐야 한다.

### 3. Edge-to-Edge에서 `window.navigationBarColor`는 API 35에서 동작하지 않는다
API 35에서 Nav Bar 색상을 제어하려면 `enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(...))` 또는 레이아웃 레벨에서 직접 커버하는 방식을 사용해야 한다.

### 4. "색상 문제처럼 보이는 버그가 레이아웃 구조 문제인 경우"
증상이 "투명해 보임"이라 처음엔 색상 API 문제로 접근했으나, 실제 원인은 배경이 칠해지는 범위 자체가 잘못된 레이아웃 구조 문제였다. 비슷한 증상의 버그는 색상보다 **어느 레이어에서 어느 범위까지 배경이 그려지는지**를 먼저 확인하는 게 빠르다.
