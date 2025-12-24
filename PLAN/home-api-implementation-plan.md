# 홈 화면 API 통합 구현 계획

## 개요
홈 화면에서 `/pages/home` API를 호출하여 모든 데이터를 한 번에 가져오는 기능을 구현합니다.

## API 스펙

### 엔드포인트
```
GET /pages/home
```

### 쿼리 파라미터
- `lat` (required): 위도 (Double)
- `lon` (required): 경도 (Double)

### 응답 구조
```json
{
  "characterDto": {
    "headImageName": "string",
    "bodyImageName": "string",
    "feetImageName": "string",
    "characterImageName": "string",
    "backgroundImageName": "string",
    "level": 0,
    "grade": "SEED",
    "nickName": "string"
  },
  "walkProgressPercentage": "string",
  "todaySteps": 0,
  "weatherDto": {
    "nx": 0,
    "ny": 0,
    "generatedAt": "2025-12-23T07:55:04.960Z",
    "tempC": 0,
    "rain1hMm": 0,
    "precipType": "NONE",
    "sky": "SUNNY"
  },
  "weeklyMissionDto": {
    "userWeeklyMissionId": 0,
    "missionId": 0,
    "title": "string",
    "description": "string",
    "category": "string",
    "type": "string",
    "status": "string",
    "rewardPoints": 0,
    "assignedConfigJson": "string",
    "weekStart": "2025-12-23",
    "weekEnd": "2025-12-23",
    "completedAt": "2025-12-23T07:55:04.961Z",
    "failedAt": "2025-12-23T07:55:04.961Z"
  },
  "walkResponseDto": [
    {
      "id": 0,
      "preWalkEmotion": "JOYFUL",
      "postWalkEmotion": "JOYFUL",
      "note": "string",
      "imageUrl": "string",
      "startTime": 0,
      "endTime": 0,
      "totalTime": 0,
      "stepCount": 0,
      "totalDistance": 0,
      "createdDate": "2025-12-23T07:55:04.961Z",
      "points": [
        {
          "latitude": 0,
          "longitude": 0,
          "timestampMillis": 0
        }
      ]
    }
  ]
}
```

### Grade Enum
- `SEED`: 새싹
- `SPROUT`: 새싹 (성장)
- `TREE`: 나무

### PrecipType Enum (강수 형태)
- `NONE`: 강수 없음
- `RAIN`: 비
- `RAIN_SNOW`: 비/눈
- `SNOW`: 눈
- `SHOWER`: 소나기
- `UNKNOWN`: 알 수 없음

### Sky Enum (하늘 상태)
- `SUNNY`: 맑음
- `CLOUDY_MANY`: 구름 많음
- `OVERCAST`: 흐림
- `UNKNOWN`: 알 수 없음

---

## 위치 처리 전략

### 1. 위치 권한 상태 확인

#### 시나리오 A: 위치 권한 없음 (첫 진입)
- **상황**: 앱 첫 실행 또는 위치 권한을 거부한 경우
- **처리**: 기본 위치 사용
  - **기본 위치**: 서울시청
    - 위도: `37.5665`
    - 경도: `126.9780`
- **동작**: 기본 위치로 API 호출하여 초기 데이터 로드

#### 시나리오 B: 위치 권한 있음
- **상황**: 위치 권한이 이미 부여된 경우
- **처리**: 
  1. `LocationManager.getCurrentLocationOrLast()` 호출
  2. 위치 획득 성공 시 → 실제 위치로 API 호출
  3. 위치 획득 실패 시 → 기본 위치(서울시청)로 API 호출

### 2. 위치 권한 요청 플로우

```
[홈 화면 진입]
    ↓
[위치 권한 확인]
    ↓
[권한 없음?] ──Yes──→ [기본 위치(서울시청) 사용]
    │                      ↓
    No                  [API 호출]
    ↓                      ↓
[위치 획득 시도]      [데이터 표시]
    ↓
[위치 획득 성공?]
    │
    ├─ Yes ──→ [실제 위치로 API 호출]
    │              ↓
    │          [데이터 표시]
    │
    └─ No ──→ [기본 위치(서울시청) 사용]
                  ↓
              [API 호출]
                  ↓
              [데이터 표시]
```

### 3. 위치 권한 요청 후 처리

- **권한 수락**: 
  - 위치 획득 시도
  - 성공 시 실제 위치로 API 재호출
  - 실패 시 기본 위치로 API 호출

- **권한 거부**: 
  - 기본 위치(서울시청)로 API 호출
  - 사용자에게 위치 권한이 필요함을 안내 (선택사항)

---

## 구현 단계

### Phase 1: DTO 생성

#### 1.1 기본 위치 상수 정의
**파일**: `app/src/main/java/team/swyp/sdu/utils/LocationConstants.kt`
```kotlin
object LocationConstants {
    // 서울시청 좌표 (기본 위치)
    const val DEFAULT_LATITUDE = 37.5665
    const val DEFAULT_LONGITUDE = 126.9780
}
```

#### 1.2 DTO 생성 (`data/remote/home/dto/`)

**HomeData.kt** - 메인 응답 DTO
```kotlin
@Serializable
data class HomeData(
    @SerialName("characterDto")
    val characterDto: CharacterDto,
    
    @SerialName("walkProgressPercentage")
    val walkProgressPercentage: String,
    
    @SerialName("todaySteps")
    val todaySteps: Int,
    
    @SerialName("weatherDto")
    val weatherDto: WeatherDto,
    
    @SerialName("weeklyMissionDto")
    val weeklyMissionDto: WeeklyMissionDto?,
    
    @SerialName("walkResponseDto")
    val walkResponseDto: List<WalkResponseDto> = emptyList(),
)
```

**CharacterDto.kt** - 캐릭터 정보 (기존 파일 확인 필요)
```kotlin
@Serializable
data class CharacterDto(
    @SerialName("headImageName")
    val headImageName: String? = null,
    
    @SerialName("bodyImageName")
    val bodyImageName: String? = null,
    
    @SerialName("feetImageName")
    val feetImageName: String? = null,
    
    @SerialName("characterImageName")
    val characterImageName: String? = null,
    
    @SerialName("backgroundImageName")
    val backgroundImageName: String? = null,
    
    @SerialName("level")
    val level: Int = 0,
    
    @SerialName("grade")
    val grade: Grade = Grade.SEED,
    
    @SerialName("nickName")
    val nickName: String? = null,
)
```

**Grade.kt** - 등급 Enum
```kotlin
@Serializable
enum class Grade {
    @SerialName("SEED")
    SEED,
    
    @SerialName("SPROUT")
    SPROUT,
    
    @SerialName("TREE")
    TREE,
}
```

**PrecipType.kt** - 강수 형태 Enum
```kotlin
@Serializable
enum class PrecipType {
    @SerialName("NONE")
    NONE,
    
    @SerialName("RAIN")
    RAIN,
    
    @SerialName("RAIN_SNOW")
    RAIN_SNOW,
    
    @SerialName("SNOW")
    SNOW,
    
    @SerialName("SHOWER")
    SHOWER,
    
    @SerialName("UNKNOWN")
    UNKNOWN,
}
```

**Sky.kt** - 하늘 상태 Enum
```kotlin
@Serializable
enum class Sky {
    @SerialName("SUNNY")
    SUNNY,
    
    @SerialName("CLOUDY_MANY")
    CLOUDY_MANY,
    
    @SerialName("OVERCAST")
    OVERCAST,
    
    @SerialName("UNKNOWN")
    UNKNOWN,
}
```

**WeatherDto.kt** - 날씨 정보
```kotlin
@Serializable
data class WeatherDto(
    @SerialName("nx")
    val nx: Int = 0,
    
    @SerialName("ny")
    val ny: Int = 0,
    
    @SerialName("generatedAt")
    val generatedAt: String? = null,
    
    @SerialName("tempC")
    val tempC: Int = 0,
    
    @SerialName("rain1hMm")
    val rain1hMm: Int = 0,
    
    @SerialName("precipType")
    val precipType: PrecipType = PrecipType.NONE,
    
    @SerialName("sky")
    val sky: Sky = Sky.SUNNY,
)
```

**WeeklyMissionDto.kt** - 주간 미션 정보
```kotlin
@Serializable
data class WeeklyMissionDto(
    @SerialName("userWeeklyMissionId")
    val userWeeklyMissionId: Long = 0,
    
    @SerialName("missionId")
    val missionId: Long = 0,
    
    @SerialName("title")
    val title: String = "",
    
    @SerialName("description")
    val description: String = "",
    
    @SerialName("category")
    val category: String = "",
    
    @SerialName("type")
    val type: String = "",
    
    @SerialName("status")
    val status: String = "",
    
    @SerialName("rewardPoints")
    val rewardPoints: Int = 0,
    
    @SerialName("assignedConfigJson")
    val assignedConfigJson: String? = null,
    
    @SerialName("weekStart")
    val weekStart: String? = null,
    
    @SerialName("weekEnd")
    val weekEnd: String? = null,
    
    @SerialName("completedAt")
    val completedAt: String? = null,
    
    @SerialName("failedAt")
    val failedAt: String? = null,
)
```

**WalkResponseDto.kt** - 산책 기록 정보
```kotlin
@Serializable
data class WalkResponseDto(
    @SerialName("id")
    val id: Long = 0,
    
    @SerialName("preWalkEmotion")
    val preWalkEmotion: String = "JOYFUL",
    
    @SerialName("postWalkEmotion")
    val postWalkEmotion: String = "JOYFUL",
    
    @SerialName("note")
    val note: String? = null,
    
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    
    @SerialName("startTime")
    val startTime: Long = 0,
    
    @SerialName("endTime")
    val endTime: Long = 0,
    
    @SerialName("totalTime")
    val totalTime: Long = 0,
    
    @SerialName("stepCount")
    val stepCount: Int = 0,
    
    @SerialName("totalDistance")
    val totalDistance: Double = 0.0,
    
    @SerialName("createdDate")
    val createdDate: String? = null,
    
    @SerialName("points")
    val points: List<PointDto> = emptyList(),
)
```

**PointDto.kt** - 위치 좌표 포인트
```kotlin
@Serializable
data class PointDto(
    @SerialName("latitude")
    val latitude: Double = 0.0,
    
    @SerialName("longitude")
    val longitude: Double = 0.0,
    
    @SerialName("timestampMillis")
    val timestampMillis: Long = 0,
)
```

---

### Phase 2: API 인터페이스 생성

**파일**: `app/src/main/java/team/swyp/sdu/data/api/home/HomeApi.kt`
```kotlin
package team.swyp.sdu.data.api.home

import retrofit2.http.GET
import retrofit2.http.Query
import team.swyp.sdu.data.remote.home.dto.HomeData

/**
 * 홈 화면 API
 */
interface HomeApi {
    /**
     * 홈 화면 데이터 조회
     *
     * @param lat 위도
     * @param lon 경도
     * @return 홈 화면 데이터
     */
    @GET("/pages/home")
    suspend fun getHomeData(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
    ): HomeData
}
```

---

### Phase 3: RemoteDataSource 생성

**파일**: `app/src/main/java/team/swyp/sdu/data/remote/home/HomeRemoteDataSource.kt`
```kotlin
package team.swyp.sdu.data.remote.home

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.api.home.HomeApi
import team.swyp.sdu.data.remote.home.dto.HomeData
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRemoteDataSource @Inject constructor(
    private val homeApi: HomeApi,
) {
    suspend fun getHomeData(lat: Double, lon: Double): Result<HomeData> {
        return try {
            val data = homeApi.getHomeData(lat, lon)
            Result.Success(data)
        } catch (e: Exception) {
            Timber.e(e, "홈 데이터 조회 실패")
            Result.Error(e, e.message ?: "홈 데이터를 불러오지 못했습니다")
        }
    }
}
```

---

### Phase 4: Repository 생성

**파일**: `app/src/main/java/team/swyp/sdu/domain/repository/HomeRepository.kt`
```kotlin
package team.swyp.sdu.domain.repository

import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.home.HomeRemoteDataSource
import team.swyp.sdu.data.remote.home.dto.HomeData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val homeRemoteDataSource: HomeRemoteDataSource,
) {
    suspend fun getHomeData(lat: Double, lon: Double): Result<HomeData> {
        return homeRemoteDataSource.getHomeData(lat, lon)
    }
}
```

---

### Phase 5: ViewModel 업데이트

**파일**: `app/src/main/java/team/swyp/sdu/ui/home/HomeViewModel.kt`

#### 5.1 HomeUiState 업데이트
```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        // 기존 필드들...
        val characterDto: CharacterDto? = null,
        val walkProgressPercentage: String = "0",
        val todaySteps: Int = 0,
        val weatherDto: WeatherDto? = null,
        val weeklyMissionDto: WeeklyMissionDto? = null,
        val walkResponseDto: List<WalkResponseDto> = emptyList(),
        // ... 기존 필드들
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

#### 5.2 위치 처리 로직 추가
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val locationManager: LocationManager,
    // ... 기존 dependencies
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    /**
     * 홈 데이터 로드 (위치 기반)
     */
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            // 위치 획득 시도
            val location = getLocationForApi()
            
            // API 호출
            val result = homeRepository.getHomeData(
                lat = location.latitude,
                lon = location.longitude
            )
            
            when (result) {
                is Result.Success -> {
                    // UI State 업데이트
                    _uiState.value = HomeUiState.Success(
                        characterDto = result.data.characterDto,
                        walkProgressPercentage = result.data.walkProgressPercentage,
                        todaySteps = result.data.todaySteps,
                        weatherDto = result.data.weatherDto,
                        weeklyMissionDto = result.data.weeklyMissionDto,
                        walkResponseDto = result.data.walkResponseDto,
                        // ... 기존 필드 매핑
                    )
                }
                is Result.Error -> {
                    _uiState.value = HomeUiState.Error(
                        result.message ?: "데이터를 불러오지 못했습니다"
                    )
                }
                Result.Loading -> {
                    // 이미 Loading 상태
                }
            }
        }
    }
    
    /**
     * API 호출을 위한 위치 획득
     * 
     * 1. 위치 권한 확인
     * 2. 권한 있음 → 현재 위치 획득 시도
     * 3. 권한 없음 또는 위치 획득 실패 → 기본 위치(서울시청) 반환
     */
    private suspend fun getLocationForApi(): LocationPoint {
        return if (locationManager.hasLocationPermission()) {
            // 위치 권한 있음 → 현재 위치 획득 시도
            locationManager.getCurrentLocationOrLast()
                ?: getDefaultLocation()
        } else {
            // 위치 권한 없음 → 기본 위치 사용
            getDefaultLocation()
        }
    }
    
    /**
     * 기본 위치 반환 (서울시청)
     */
    private fun getDefaultLocation(): LocationPoint {
        return LocationPoint(
            latitude = LocationConstants.DEFAULT_LATITUDE,
            longitude = LocationConstants.DEFAULT_LONGITUDE,
            timestamp = System.currentTimeMillis(),
        )
    }
    
    /**
     * 위치 권한 요청 후 데이터 재로드
     */
    fun reloadAfterPermissionGranted() {
        loadHomeData()
    }
}
```

---

### Phase 6: DI 모듈 업데이트

**파일**: `app/src/main/java/team/swyp/sdu/di/NetworkModule.kt`
```kotlin
@Provides
@Singleton
fun provideHomeApi(
    @Named("walkit") retrofit: Retrofit,
): HomeApi = retrofit.create(HomeApi::class.java)
```

---

### Phase 7: UI 업데이트 (위치 권한 처리)

**파일**: `app/src/main/java/team/swyp/sdu/ui/home/HomeScreen.kt`

```kotlin
@Composable
fun HomeScreen(
    onClickWalk: () -> Unit,
    onClickGoal: () -> Unit,
    onClickMission: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 위치 권한 상태
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    
    // 위치 권한 요청 Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 수락 → 데이터 재로드
            viewModel.reloadAfterPermissionGranted()
        } else {
            // 권한 거부 → 기본 위치로 이미 로드된 데이터 유지
            // (이미 기본 위치로 로드되었으므로 추가 처리 불필요)
        }
    }
    
    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            // 권한 있음 → 바로 로드
            viewModel.loadHomeData()
        } else {
            // 권한 없음 → 기본 위치로 로드
            viewModel.loadHomeData()
            
            // 권한 요청 (선택사항 - 사용자 경험에 따라 결정)
            // permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    // UI 렌더링
    when (uiState) {
        is HomeUiState.Loading -> {
            // 로딩 UI
        }
        is HomeUiState.Success -> {
            // 성공 UI
        }
        is HomeUiState.Error -> {
            // 에러 UI
        }
    }
}
```

---

## 구현 순서

1. ✅ **Phase 1**: DTO 생성
   - LocationConstants.kt 생성
   - 모든 DTO 파일 생성

2. ✅ **Phase 2**: API 인터페이스 생성
   - HomeApi.kt 생성

3. ✅ **Phase 3**: RemoteDataSource 생성
   - HomeRemoteDataSource.kt 생성

4. ✅ **Phase 4**: Repository 생성
   - HomeRepository.kt 생성

5. ✅ **Phase 5**: ViewModel 업데이트
   - HomeUiState 업데이트
   - 위치 처리 로직 추가

6. ✅ **Phase 6**: DI 모듈 업데이트
   - NetworkModule에 HomeApi 등록

7. ✅ **Phase 7**: UI 업데이트
   - 위치 권한 처리 추가

---

## 주의사항

1. **위치 권한 처리**
   - 첫 진입 시 권한 없어도 기본 위치로 API 호출 가능
   - 권한 수락 후 실제 위치로 재호출

2. **기본 위치**
   - 서울시청 좌표: `37.5665, 126.9780`
   - LocationConstants로 관리하여 일관성 유지

3. **에러 처리**
   - 위치 획득 실패 시 기본 위치 사용
   - API 호출 실패 시 Error 상태 표시

4. **성능 최적화**
   - 위치 권한 확인은 비동기로 처리
   - 불필요한 API 재호출 방지

---

## 테스트 시나리오

1. **첫 진입 (권한 없음)**
   - 기본 위치로 API 호출 확인
   - 데이터 정상 표시 확인

2. **권한 수락 후**
   - 실제 위치로 API 재호출 확인
   - 데이터 업데이트 확인

3. **권한 거부**
   - 기본 위치로 API 호출 확인
   - 데이터 정상 표시 확인

4. **위치 획득 실패**
   - 기본 위치로 Fallback 확인
   - 에러 없이 데이터 표시 확인

