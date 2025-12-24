# LocationManager 사용 가이드

## 개요

`LocationManager`는 단순 위치 요청을 위한 유틸리티 클래스입니다.
- **연속 위치 추적**: `LocationTrackingService` 사용 (산책 중)
- **일회성 위치 요청**: `LocationManager` 사용 (홈화면, 지도 썸네일 등)

## 아키텍처 설계

### 책임 분리

```
LocationTrackingService (Foreground Service)
├─ 연속 위치 추적 (산책 중)
├─ 배터리 최적화
└─ 위치 업데이트를 Flow로 제공

LocationManager (Singleton)
├─ 일회성 위치 요청
├─ 현재 위치 가져오기
└─ 마지막 위치 가져오기
```

### 사용 시나리오

| 시나리오 | 사용 클래스 | 이유 |
|---------|------------|------|
| 산책 중 위치 추적 | `LocationTrackingService` | 연속 추적 필요, Foreground Service 필요 |
| 홈화면 현재 위치 표시 | `LocationManager` | 일회성 요청 |
| 지도 썸네일 현재 위치 | `LocationManager` | 일회성 요청 |
| 기타 일회성 위치 요청 | `LocationManager` | 단순 요청 |

## 사용 방법

### 1. ViewModel에서 사용

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationManager: LocationManager,
) : ViewModel() {
    
    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()
    
    fun loadCurrentLocation() {
        viewModelScope.launch {
            val location = locationManager.getCurrentLocationOrLast()
            _currentLocation.value = location
        }
    }
}
```

### 2. Composable에서 직접 사용 (EntryPoint)

```kotlin
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val locationManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LocationManagerEntryPoint::class.java
        ).locationManager()
    }
    
    var currentLocation by remember { mutableStateOf<LocationPoint?>(null) }
    
    LaunchedEffect(Unit) {
        currentLocation = locationManager.getCurrentLocationOrLast()
    }
    
    // UI 구현
}
```

### 3. RouteThumbnailMap에서 사용 (이미 적용됨)

```kotlin
// RouteThumbnailMap은 이미 LocationManager를 사용하도록 리팩토링됨
RouteThumbnailMap(
    locations = locations,
    locationManager = locationManager // 선택사항, 없으면 내부에서 생성
)
```

## API 메서드

### `getCurrentLocation(): LocationPoint?`

현재 위치를 가져옵니다 (고정밀도).

```kotlin
val location = locationManager.getCurrentLocation()
if (location != null) {
    // 위치 사용
}
```

### `getLastLocation(): LocationPoint?`

마지막으로 알려진 위치를 가져옵니다.

```kotlin
val location = locationManager.getLastLocation()
if (location != null) {
    // 위치 사용
}
```

### `getCurrentLocationOrLast(): LocationPoint?`

현재 위치를 가져오고, 실패하면 마지막 위치를 반환합니다.

```kotlin
val location = locationManager.getCurrentLocationOrLast()
// 항상 null이 아닐 수 있음 (권한이 있는 경우)
```

### `hasLocationPermission(): Boolean`

위치 권한이 있는지 확인합니다.

```kotlin
if (locationManager.hasLocationPermission()) {
    // 위치 요청 가능
}
```

## 권한 처리

`LocationManager`는 내부적으로 위치 권한을 확인합니다.
- 권한이 없으면 `null`을 반환
- 권한 확인은 `hasLocationPermission()` 메서드로 가능

## 주의사항

1. **연속 추적이 필요한 경우**: `LocationTrackingService` 사용
2. **권한 확인**: 위치 요청 전에 권한이 있는지 확인하는 것이 좋습니다
3. **null 처리**: 위치를 가져올 수 없는 경우 `null`을 반환하므로 null 체크 필요

## 마이그레이션 가이드

### 기존 코드 (RouteThumbnailMap)

```kotlin
// ❌ 이전: 직접 FusedLocationProviderClient 사용
val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
fusedLocationClient.getCurrentLocation(...)
```

```kotlin
// ✅ 이후: LocationManager 사용
val location = locationManager.getCurrentLocationOrLast()
```

## 예시: HomeScreen에서 사용

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentLocation()
    }
    
    // UI에서 currentLocation 사용
    currentLocation?.let { location ->
        Text("현재 위치: ${location.latitude}, ${location.longitude}")
    }
}
```

## 테스트

`LocationManager`는 단순한 유틸리티 클래스이므로 테스트가 쉽습니다.

```kotlin
@Test
fun `getCurrentLocation returns null when permission denied`() {
    // Given
    val locationManager = LocationManager(mockContext)
    whenever(mockContext.checkSelfPermission(...)).thenReturn(PERMISSION_DENIED)
    
    // When
    val result = locationManager.getCurrentLocation()
    
    // Then
    assertNull(result)
}
```





