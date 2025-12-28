# 권한 요청 전략 가이드

## 현재 상황 분석

### 필요한 권한 목록

| 권한 | 용도 | 필수 여부 | 요청 시점 |
|------|------|----------|----------|
| `ACTIVITY_RECOGNITION` | 걸음 수 측정 | 필수 | 산책 시작 전 |
| `HIGH_SAMPLING_RATE_SENSORS` | 가속도계 실시간 업데이트 | 필수 | 산책 시작 전 |
| `ACCESS_FINE_LOCATION` | 위치 추적 (산책 중) | 필수 | 산책 시작 전 |
| `ACCESS_COARSE_LOCATION` | 위치 추적 (대체) | 선택 | 산책 시작 전 |
| `POST_NOTIFICATIONS` | 알림 (Android 13+) | 선택 | 홈 화면 진입 시 |
| `READ_MEDIA_IMAGES` | 이미지 읽기 (Android 13+) | 선택 | 이미지 선택 시 |
| `CAMERA` | 카메라 촬영 | 선택 | 카메라 사용 시 |

### 현재 플로우

```
Splash → Login → Onboarding → Main (Home)
```

## 권장 권한 요청 전략

### ✅ 권장: **온보딩 마지막 단계에서 권한 요청**

#### 이유

1. **사용자 이해도**: 온보딩을 통해 앱의 목적과 기능을 이해한 후 권한 요청
2. **자연스러운 흐름**: 온보딩 완료 직전에 권한 요청 → 홈 화면 진입
3. **높은 승인률**: 앱의 가치를 이해한 상태에서 권한 요청 시 승인률 증가
4. **일관된 UX**: 모든 필수 권한을 한 번에 요청

#### 구현 위치

**온보딩 마지막 단계 (GoalStep 완료 후)**

```kotlin
// OnboardingScreen.kt 또는 OnboardingViewModel.kt
fun submitOnboarding() {
    // 1. 온보딩 데이터 저장
    // 2. 권한 요청 화면으로 이동 또는 권한 요청
    requestEssentialPermissions()
}
```

### 📋 권한 요청 순서

#### 1단계: 필수 권한 (온보딩 완료 시)

```
온보딩 완료 → 권한 요청 화면
├─ ACTIVITY_RECOGNITION (걸음 수 측정)
├─ ACCESS_FINE_LOCATION (위치 추적)
└─ HIGH_SAMPLING_RATE_SENSORS (가속도계)
```

**이유**: 산책 기능의 핵심 권한이므로 앱 사용 전에 필수

#### 2단계: 선택 권한 (필요 시점에)

```
홈 화면 진입 시
└─ POST_NOTIFICATIONS (알림) - 선택적

이미지 선택 시
└─ READ_MEDIA_IMAGES (갤러리) - 선택적

카메라 사용 시
└─ CAMERA (카메라) - 선택적
```

**이유**: 즉시 필요하지 않은 권한은 사용 시점에 요청

## 구현 예시

### 1. 권한 요청 화면 생성

```kotlin
// PermissionRequestScreen.kt
@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit,
    onSkip: () -> Unit, // 권한 거부 시에도 진행 가능
) {
    val context = LocalContext.current
    
    // 필수 권한 목록
    val essentialPermissions = remember {
        listOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
        )
    }
    
    // 권한 요청 Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            // 일부 권한 거부 시에도 진행 가능 (나중에 다시 요청)
            onSkip()
        }
    }
    
    // 권한 요청 화면 UI
    Column {
        Text("앱 사용을 위해 다음 권한이 필요합니다")
        // 권한 설명
        essentialPermissions.forEach { permission ->
            PermissionItem(permission = permission)
        }
        
        Button(onClick = {
            permissionLauncher.launch(essentialPermissions.toTypedArray())
        }) {
            Text("권한 허용")
        }
        
        TextButton(onClick = onSkip) {
            Text("나중에")
        }
    }
}
```

### 2. 온보딩 완료 시 권한 요청

```kotlin
// OnboardingViewModel.kt
fun submitOnboarding(onComplete: () -> Unit) {
    viewModelScope.launch {
        // 온보딩 데이터 저장
        // ...
        
        // 권한 요청 화면으로 이동
        _uiState.value = _uiState.value.copy(
            showPermissionRequest = true
        )
    }
}
```

### 3. 네비게이션 플로우 수정

```kotlin
// NavGraph.kt
composable(Screen.Onboarding.route) {
    OnboardingScreen(
        onFinish = {
            // 권한 요청 화면으로 이동
            navController.navigate(Screen.PermissionRequest.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        },
    )
}

composable(Screen.PermissionRequest.route) {
    PermissionRequestScreen(
        onPermissionsGranted = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.PermissionRequest.route) { inclusive = true }
            }
        },
        onSkip = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.PermissionRequest.route) { inclusive = true }
            }
        },
    )
}
```

## 대안: 홈 화면 진입 시 권한 요청

### 장점
- 온보딩 플로우가 간단함
- 사용자가 홈 화면을 먼저 볼 수 있음

### 단점
- 권한 요청이 갑작스러울 수 있음
- 사용자가 앱의 가치를 이해하기 전에 권한 요청

### 구현 예시

```kotlin
// HomeScreen.kt 또는 HomeViewModel.kt
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        // 홈 화면 진입 시 필수 권한 확인
        val missingPermissions = checkEssentialPermissions()
        if (missingPermissions.isNotEmpty()) {
            // 권한 요청 다이얼로그 표시
            showPermissionRequestDialog(missingPermissions)
        }
    }
    
    // 홈 화면 UI
}
```

## 권한 요청 UX 가이드라인

### 1. 권한 설명 제공

각 권한에 대해 **왜 필요한지** 명확히 설명:

```
📍 위치 정보
산책 경로를 기록하고 지도에 표시하기 위해 필요합니다.

👣 활동 인식
걸음 수를 정확하게 측정하기 위해 필요합니다.

⚡ 고속 센서
실시간으로 움직임을 감지하여 더 정확한 측정을 위해 필요합니다.
```

### 2. 권한 거부 시 처리

- **필수 권한 거부**: 기능 제한 안내 및 설정 화면으로 이동 유도
- **선택 권한 거부**: 기능 사용 불가 안내 (앱 사용은 가능)

### 3. 권한 재요청

- 사용자가 "다시 묻지 않음" 선택 시: 설정 화면으로 이동 유도
- 일정 시간 후 재요청 (선택적)

## 최종 권장사항

### ✅ **온보딩 마지막 단계에서 권한 요청** (권장)

**플로우:**
```
Splash → Login → Onboarding → PermissionRequest → Main (Home)
```

**이유:**
1. 사용자가 앱의 가치를 이해한 후 권한 요청
2. 높은 승인률 기대
3. 일관된 UX
4. 필수 권한을 한 번에 요청

### 선택 권한은 필요 시점에 요청

- **POST_NOTIFICATIONS**: 홈 화면 진입 시 (선택적)
- **READ_MEDIA_IMAGES**: 이미지 선택 시
- **CAMERA**: 카메라 사용 시

## 구현 체크리스트

- [ ] 권한 요청 화면 생성
- [ ] 온보딩 완료 시 권한 요청 화면으로 이동
- [ ] 권한 설명 UI 구현
- [ ] 권한 거부 시 처리 로직
- [ ] 홈 화면에서 선택 권한 요청 (POST_NOTIFICATIONS)
- [ ] 이미지 선택 시 권한 요청 (READ_MEDIA_IMAGES)
- [ ] 카메라 사용 시 권한 요청 (CAMERA)
- [ ] 권한 상태 확인 유틸리티 함수
- [ ] 설정 화면으로 이동 기능







