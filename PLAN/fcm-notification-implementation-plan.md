# FCM 알림 플로우 구현 계획

## 📋 개요

Android 앱에서 FCM(Firebase Cloud Messaging) 기반 알림 시스템을 구현합니다.
알림 권한 요청, FCM 토큰 관리, 서버 연동을 포함한 전체 플로우를 구현합니다.

---

## 🏗️ 아키텍처 구조

```
app/
├── data/
│   ├── api/
│   │   └── notification/
│   │       └── NotificationApi.kt          # 알림 관련 API 인터페이스
│   ├── local/
│   │   └── datastore/
│   │       └── FcmTokenDataStore.kt        # FCM 토큰 로컬 저장 (DataStore)
│   ├── remote/
│   │   └── notification/
│   │       ├── NotificationRemoteDataSource.kt  # 서버 API 호출
│   │       └── dto/
│   │           ├── FcmTokenRequestDto.kt   # FCM 토큰 등록 요청 DTO
│   │           └── NotificationSettingsDto.kt   # 알림 설정 DTO
│   └── repository/
│       └── NotificationRepository.kt       # Repository 레이어
├── domain/
│   └── service/
│       └── FcmTokenManager.kt              # FCM 토큰 관리 서비스
├── presentation/
│   └── viewmodel/
│       ├── NotificationPermissionViewModel.kt   # 알림 권한 요청 ViewModel
│       └── NotificationSettingsViewModel.kt     # 알림 설정 ViewModel (기존 확장)
└── ui/
    ├── notification/
    │   ├── NotificationPermissionDialog.kt  # 알림 권한 안내 다이얼로그
    │   └── components/
    │       └── NotificationPermissionContent.kt
    ├── home/
    │   └── HomeScreen.kt                   # 홈 화면 (다이얼로그 표시)
    └── mypage/
        └── settings/
            └── NotificationSettingsScreen.kt    # 기존 화면 확장
```

---

## 📦 필요한 의존성

### 1. Firebase BOM 및 FCM
```kotlin
// build.gradle.kts
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-messaging-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")
```

### 2. DataStore (이미 있음)
```kotlin
implementation(libs.androidx.datastore.preferences)
```

---

## 🔧 구현 단계

### Phase 1: Firebase 설정 및 FCM 토큰 관리

#### 1.1 Firebase 프로젝트 설정
- `google-services.json` 파일 추가 (이미 있음)
- `build.gradle.kts`에 Firebase 플러그인 추가

#### 1.2 FCM 토큰 관리 서비스
**파일**: `domain/service/FcmTokenManager.kt`

**책임**:
- FCM 토큰 발급 및 로컬 저장
- 토큰 갱신 감지 및 처리
- 로그인 상태 확인 및 서버 동기화

**주요 메서드**:
```kotlin
class FcmTokenManager @Inject constructor(
    private val firebaseMessaging: FirebaseMessaging,
    private val fcmTokenDataStore: FcmTokenDataStore,
    private val notificationRepository: NotificationRepository,
    private val tokenProvider: TokenProvider, // 로그인 상태 확인용
) {
    suspend fun initializeToken() // 앱 최초 실행 시 토큰 발급
    suspend fun refreshToken() // 토큰 갱신 시 호출
    suspend fun syncTokenToServer() // 서버에 토큰 등록/업데이트
}
```

#### 1.3 FCM 토큰 로컬 저장소
**파일**: `data/local/datastore/FcmTokenDataStore.kt`

**책임**:
- FCM 토큰을 DataStore에 저장
- 토큰 읽기/쓰기

```kotlin
@Singleton
class FcmTokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore
    
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
}
```

#### 1.4 FirebaseMessagingService 구현
**파일**: `domain/service/WalkItFirebaseMessagingService.kt`

**책임**:
- FCM 토큰 갱신 감지 (`onNewToken`)
- 푸시 알림 수신 처리 (`onMessageReceived`)
- 알림 클릭 시 특정 화면으로 이동

```kotlin
@AndroidEntryPoint
class WalkItFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // 토큰 갱신 처리
        // 로그인 상태 확인 후 서버 업데이트 또는 로컬 저장
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 알림 수신 처리
        // NotificationChannel 생성 및 알림 표시
    }
}
```

---

### Phase 2: 서버 API 연동

#### 2.1 API 인터페이스
**파일**: `data/api/notification/NotificationApi.kt`

```kotlin
interface NotificationApi {
    @POST("/fcm/token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequestDto
    ): Response<Unit>
    
    @GET("/notification/settings")
    suspend fun getNotificationSettings(): NotificationSettingsDto
    
    @PATCH("/notification/settings")
    suspend fun updateNotificationSettings(
        @Body request: UpdateNotificationSettingsRequest
    ): Response<Unit>
}
```

#### 2.2 DTO 정의
**파일**: `data/remote/notification/dto/FcmTokenRequestDto.kt`

```kotlin
@Serializable
data class FcmTokenRequestDto(
    @SerialName("token")
    val token: String,
    @SerialName("deviceType")
    val deviceType: String = "AOS",
    @SerialName("deviceId")
    val deviceId: String? = null,
)
```

**파일**: `data/remote/notification/dto/NotificationSettingsDto.kt`

```kotlin
@Serializable
data class NotificationSettingsDto(
    @SerialName("notificationEnabled")
    val notificationEnabled: Boolean,
    @SerialName("goalNotificationEnabled")
    val goalNotificationEnabled: Boolean,
    @SerialName("newMissionNotificationEnabled")
    val newMissionNotificationEnabled: Boolean,
)
```

#### 2.3 RemoteDataSource
**파일**: `data/remote/notification/NotificationRemoteDataSource.kt`

```kotlin
@Singleton
class NotificationRemoteDataSource @Inject constructor(
    private val notificationApi: NotificationApi,
) {
    suspend fun registerFcmToken(token: String, deviceId: String?): Result<Unit>
    suspend fun getNotificationSettings(): Result<NotificationSettingsDto>
    suspend fun updateNotificationSettings(settings: UpdateNotificationSettingsRequest): Result<Unit>
}
```

#### 2.4 Repository
**파일**: `data/repository/NotificationRepository.kt`

```kotlin
@Singleton
class NotificationRepository @Inject constructor(
    private val remoteDataSource: NotificationRemoteDataSource,
) {
    suspend fun registerFcmToken(token: String, deviceId: String?): Result<Unit>
    suspend fun getNotificationSettings(): Result<NotificationSettingsDto>
    suspend fun updateNotificationSettings(settings: UpdateNotificationSettingsRequest): Result<Unit>
}
```

---

### Phase 3: 알림 권한 요청 플로우

#### 3.1 알림 권한 안내 다이얼로그
**파일**: `ui/notification/NotificationPermissionDialog.kt`

**UI 구성**:
- 알림을 받으면 좋은 이유 설명 텍스트
- "알림 켜기" 버튼 (Primary)
- "나중에" 버튼 (Secondary)

**표시 조건**:
- 홈 화면에서 표시
- 알림 권한이 아직 요청되지 않은 경우
- Android 13+ 에서만 표시

**구현**:
- `AlertDialog` 또는 커스텀 다이얼로그 사용
- `ActivityResultContracts.RequestPermission` 사용

```kotlin
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 권한") },
        text = {
            Column {
                Text("알림을 받으면 좋은 이유 설명...")
            }
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("알림 켜기")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("나중에")
            }
        },
    )
}
```

#### 3.2 ViewModel
**파일**: `presentation/viewmodel/NotificationPermissionViewModel.kt`

**책임**:
- 알림 권한 상태 확인
- 다이얼로그 표시 여부 관리
- 권한 요청 로직 처리
- 권한 결과에 따른 서버 동의 여부 전송

**State 정의**:
```kotlin
sealed interface NotificationPermissionUiState {
    data object Idle : NotificationPermissionUiState
    data object Checking : NotificationPermissionUiState
    data object ShouldShowDialog : NotificationPermissionUiState  // 다이얼로그 표시 필요
    data object Requesting : NotificationPermissionUiState
    data object Granted : NotificationPermissionUiState
    data object Denied : NotificationPermissionUiState
}
```

**주요 메서드**:
```kotlin
fun checkShouldShowDialog() // 다이얼로그 표시 여부 확인
fun showDialog() // 다이얼로그 표시
fun dismissDialog() // 다이얼로그 닫기
fun requestPermission() // 권한 요청
fun handlePermissionResult(granted: Boolean) // 권한 결과 처리
fun skipPermission() // 나중에 버튼 클릭
```

#### 3.3 홈 화면 통합
**파일**: `ui/home/HomeScreen.kt`

**추가 기능**:
- 홈 화면 진입 시 알림 권한 다이얼로그 표시 여부 확인
- `NotificationPermissionViewModel` 주입
- 다이얼로그 표시 로직 통합

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    permissionViewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    val permissionUiState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    
    // 홈 화면 UI
    
    // 알림 권한 다이얼로그 표시
    when (permissionUiState) {
        is NotificationPermissionUiState.ShouldShowDialog -> {
            NotificationPermissionDialog(
                onDismiss = permissionViewModel::dismissDialog,
                onRequestPermission = permissionViewModel::requestPermission,
                onSkip = permissionViewModel::skipPermission,
            )
        }
        else -> {}
    }
}
```

---

### Phase 4: 알림 설정 화면 확장

#### 4.1 기존 ViewModel 확장
**파일**: `ui/mypage/settings/NotificationSettingsViewModel.kt`

**추가 기능**:
- 서버에서 알림 설정 조회
- 서버에 알림 설정 업데이트
- OS 알림 권한 상태 확인
- 알림이 꺼져 있으면 설정 화면으로 이동

**주요 메서드**:
```kotlin
fun loadSettings() // 서버에서 설정 조회
fun updateSettings(settings: NotificationSettings) // 서버에 설정 업데이트
fun openNotificationSettings() // OS 알림 설정 화면으로 이동
```

#### 4.2 UI 업데이트
**파일**: `ui/mypage/settings/NotificationSettingsScreen.kt`

**추가 기능**:
- 알림 권한이 거절된 경우 안내 메시지 표시
- "알림 설정 열기" 버튼 추가
- OS 권한 다이얼로그 재요청 ❌ (요구사항)

---

### Phase 5: 통합 및 초기화

#### 5.1 Application 초기화
**파일**: `WalkingBuddyApplication.kt`

**추가**:
- Firebase 초기화
- FcmTokenManager 초기화
- NotificationChannel 생성

```kotlin
override fun onCreate() {
    super.onCreate()
    
    // Firebase 초기화
    Firebase.initialize(this)
    
    // FCM 토큰 초기화
    fcmTokenManager.initializeToken()
    
    // NotificationChannel 생성
    createNotificationChannel()
}
```

#### 5.2 로그인 성공 시점 통합
**파일**: `ui/login/LoginScreen.kt` 또는 `presentation/viewmodel/UserViewModel.kt`

**추가**:
- 로그인 성공 시 FCM 토큰 서버 등록
- 알림 권한 다이얼로그 표시 여부를 상태로 관리 (홈 화면에서 표시)

```kotlin
fun onLoginSuccess() {
    // 로그인 처리
    // FCM 토큰 서버 등록
    fcmTokenManager.syncTokenToServer()
    
    // 알림 권한 다이얼로그 표시 여부 확인
    // 홈 화면에서 다이얼로그를 표시하도록 상태 설정
    notificationPermissionViewModel.checkShouldShowDialog()
}
```

**주의사항**:
- `navigateToNotificationPermission()` 호출 ❌
- 다이얼로그는 홈 화면에서 표시
- 로그인 성공 후 홈 화면으로 이동하면 자동으로 다이얼로그 표시

#### 5.3 NotificationChannel 생성
**파일**: `domain/service/NotificationChannelManager.kt`

```kotlin
@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "앱 알림"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
```

---

## 🔄 플로우 다이어그램

### 앱 최초 실행 플로우
```
앱 시작
  ↓
Firebase 초기화
  ↓
FCM 토큰 발급 → 로컬 저장
  ↓
로그인 화면 표시
  ↓
로그인 성공
  ↓
FCM 토큰 서버 등록
  ↓
홈 화면으로 이동
  ↓
홈 화면 진입 시 알림 권한 다이얼로그 표시 여부 확인 (Android 13+)
  ↓
다이얼로그 표시 (홈 화면 위에 오버레이)
  ↓
"알림 켜기" 클릭 → OS 권한 요청
  ↓
권한 허용 → 서버에 notification-consent = true
권한 거절 → 서버에 notification-consent = false
```

### FCM 토큰 갱신 플로우
```
onNewToken() 호출
  ↓
로그인 상태 확인
  ↓
로그인 상태 → 즉시 서버 업데이트
로그아웃 상태 → 로컬에만 저장
```

### 알림 수신 플로우
```
FCM 메시지 수신
  ↓
onMessageReceived() 호출
  ↓
NotificationChannel 확인/생성
  ↓
Notification 생성 및 표시
  ↓
알림 클릭 → PendingIntent로 특정 화면 이동
```

---

## 📝 구현 체크리스트

### Phase 1: Firebase 설정
- [ ] Firebase 프로젝트 설정 및 `google-services.json` 추가
- [ ] `build.gradle.kts`에 Firebase 의존성 추가
- [ ] `FcmTokenManager` 구현
- [ ] `FcmTokenDataStore` 구현
- [ ] `WalkItFirebaseMessagingService` 구현
- [ ] AndroidManifest에 서비스 등록

### Phase 2: 서버 API 연동
- [ ] `NotificationApi` 인터페이스 정의
- [ ] DTO 클래스 정의 (`FcmTokenRequestDto`, `NotificationSettingsDto`)
- [ ] `NotificationRemoteDataSource` 구현
- [ ] `NotificationRepository` 구현
- [ ] NetworkModule에 API 등록

### Phase 3: 알림 권한 요청
- [ ] `NotificationPermissionDialog` 구현 (다이얼로그 형식)
- [ ] `NotificationPermissionViewModel` 구현
- [ ] 홈 화면에 다이얼로그 통합
- [ ] 권한 요청 로직 통합
- [ ] Android 13+ 체크 로직
- [ ] 권한 결과 처리 및 서버 동의 여부 전송

### Phase 4: 알림 설정 화면
- [ ] 기존 `NotificationSettingsViewModel` 확장
- [ ] 서버 API 연동 추가
- [ ] OS 알림 설정 화면 이동 기능
- [ ] UI 업데이트

### Phase 5: 통합
- [ ] Application에서 Firebase 초기화
- [ ] FCM 토큰 초기화
- [ ] NotificationChannel 생성
- [ ] 로그인 성공 시점 통합 (상태 관리, navigate ❌)
- [ ] 홈 화면에서 다이얼로그 표시 로직 통합
- [ ] 테스트 및 검증

---

## 🎯 주요 고려사항

### 1. Android 버전별 처리
- **Android 13+**: POST_NOTIFICATIONS 권한 요청 필요
- **Android 12 이하**: 권한 요청 생략, 안내 화면만 표시

### 2. 로그인 상태 관리
- `TokenProvider`를 통해 로그인 상태 확인
- 로그인 상태에 따라 FCM 토큰 서버 동기화 여부 결정

### 3. 알림 권한 UX
- 커스텀 안내 화면 먼저 표시
- OS 권한 다이얼로그는 "알림 켜기" 버튼 클릭 시에만 표시
- 설정 화면에서는 OS 권한 다이얼로그 재요청 ❌

### 4. 에러 처리
- FCM 토큰 발급 실패 시 재시도 로직
- 서버 API 호출 실패 시 로컬 저장 후 재시도
- 네트워크 오류 처리

### 5. 테스트
- FCM 토큰 발급 테스트
- 권한 요청 플로우 테스트
- 서버 API 연동 테스트
- 알림 수신 테스트

---

## 📚 참고 자료

- [Firebase Cloud Messaging 문서](https://firebase.google.com/docs/cloud-messaging)
- [Android 알림 권한 가이드](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [ActivityResultContracts 문서](https://developer.android.com/training/basics/intents/result)

---

## 🚀 다음 단계

1. Firebase 프로젝트 설정 확인
2. Phase 1부터 순차적으로 구현
3. 각 Phase 완료 후 테스트
4. 전체 플로우 통합 테스트

