# 산책/러닝 앱 집중모드 구현 설계 문서

## 현재 코드베이스 분석 결과

### 기존 컴포넌트 구조
- **LocationTrackingService**: Foreground Service로 위치 추적 기능 제공
- **MainActivity**: Single Activity로 Compose Navigation 사용
- **MainScreen**: 홈 탭 화면으로 WalkingFloatingActionButton 포함
- **NavGraph**: Navigation 구조 관리
- **HomeRoute**: 메인 홈 화면

### 현재 Walking 시작 플로우
```
MainScreen → FloatingActionButton 클릭 → 위치 권한 확인 → Walking 화면으로 이동
```

## 집중모드 구현 설계

### 1. LocationTrackingService 수정

**파일**: `app/src/main/java/team/swyp/sdu/domain/service/LocationTrackingService.kt`

**변경사항**:
```kotlin
companion object {
    // 기존 companion object에 추가
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // ... 기존 코드 ...
}

override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
        ACTION_START_TRACKING -> {
            _isRunning.value = true  // 상태 변경 추가
            serviceScope.launch {
                startTracking()
            }
        }
        ACTION_STOP_TRACKING -> {
            _isRunning.value = false  // 상태 변경 추가
            serviceScope.launch {
                stopTracking()
            }
        }
        // ... 기존 코드 ...
    }
    return START_NOT_STICKY
}
```

### 2. MainActivity 수정

**파일**: `app/src/main/java/team/swyp/sdu/MainActivity.kt`

**변경사항**:
```xml
<!-- AndroidManifest.xml -->
<activity
    android:name=".MainActivity"
    android:launchMode="singleTop"  <!-- 추가 -->
    ... />
```

```kotlin
// MainActivity.kt
setContent {
    WalkItTheme {
        val userViewModel: UserViewModel = hiltViewModel()
        val navController = rememberNavController()

        // LocationService 상태 구독 및 자동 네비게이션
        val isWorkoutActive by LocationTrackingService.isRunning.collectAsStateWithLifecycle()

        LaunchedEffect(isWorkoutActive) {
            if (isWorkoutActive) {
                // 집중모드로 이동
                navController.navigate(Screen.FocusMode.route) {
                    popUpTo(Screen.Main.route) { saveState = true }
                    launchSingleTop = true
                }
            } else {
                // 현재 집중모드 화면이면 홈으로 복귀
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute == Screen.FocusMode.route) {
                    navController.popBackStack(Screen.Main.route, false)
                }
            }
        }

        NavGraph(navController = navController, userViewModel = userViewModel)
    }
}
```

### 3. Navigation 구조 추가

**파일**: `app/src/main/java/team/swyp/sdu/navigation/NavGraph.kt`

**변경사항**:
```kotlin
sealed class Screen(val route: String) {
    // ... 기존 코드 ...
    data object FocusMode : Screen("focus_mode")  // 추가
}

// NavGraph 함수 내에 추가
composable(Screen.FocusMode.route) {
    FocusModeScreen(
        onStopWorkout = {
            // LocationService 중지
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    )
}
```

### 4. FocusModeScreen 구현

**새 파일**: `app/src/main/java/team/swyp/sdu/ui/focusmode/FocusModeScreen.kt`

```kotlin
@Composable
fun FocusModeScreen(
    onStopWorkout: () -> Unit
) {
    // 백버튼 차단
    BackHandler {
        // 아무 동작도 하지 않음
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "운동 중",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Button(
                onClick = onStopWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("운동 종료", color = Color.White)
            }
        }
    }
}
```

### 5. MainScreen 시작 버튼 수정

**파일**: `app/src/main/java/team/swyp/sdu/ui/screens/MainScreen.kt`

**변경사항**:
```kotlin
WalkingFloatingActionButton(
    onClick = {
        // 위치 권한 확인 후 LocationService 시작
        if (locationViewModel.hasLocationPermission()) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START_TRACKING
            }
            context.startService(intent)
            // 네비게이션은 MainActivity의 LaunchedEffect에서 자동 처리
        } else {
            locationViewModel.checkShouldShowDialog()
        }
    }
)
```

## 구현 시나리오 검증

### 1. 정상 시작 시나리오
```
사용자 클릭 → LocationService 시작 → isRunning = true →
LaunchedEffect 감지 → focus_mode로 자동 이동 → FocusModeScreen 표시
```

### 2. 앱 종료 후 재시작 시나리오
```
앱 종료 → LocationService 계속 실행 → 앱 재시작 →
MainActivity 생성 → isRunning = true 구독 →
LaunchedEffect 감지 → focus_mode로 자동 이동
```

### 3. 알림 클릭 시나리오
```
알림 클릭 → MainActivity(singleTop) 실행 →
isRunning = true 구독 → LaunchedEffect 감지 → focus_mode로 자동 이동
```

### 4. 운동 종료 시나리오
```
종료 버튼 클릭 → LocationService 중지 → isRunning = false →
LaunchedEffect 감지 → focus_mode에서 popBackStack → Main 화면 복귀
```

## 기술적 고려사항

1. **StateFlow 스레딩**: LocationTrackingService에서 StateFlow 업데이트는 메인 스레드에서 안전하게 처리
2. **Lifecycle 관리**: collectAsStateWithLifecycle로 메모리 누수 방지
3. **Navigation 상태**: popUpTo와 launchSingleTop으로 백스택 적절히 관리
4. **권한 처리**: 기존 위치 권한 로직 유지
5. **배터리 최적화**: LocationTrackingService의 기존 배터리 최적화 로직 활용

## 구현 순서

1. LocationTrackingService에 isRunning StateFlow 추가
2. MainActivity에 launchMode="singleTop" 설정 및 자동 네비게이션 로직 추가
3. NavGraph에 FocusMode 화면 추가
4. FocusModeScreen 컴포넌트 구현
5. MainScreen의 FloatingActionButton에서 LocationService 시작하도록 수정
6. 각 시나리오별 테스트 진행
