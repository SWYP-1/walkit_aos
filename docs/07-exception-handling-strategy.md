# Android 릴리즈 빌드 예외 처리 전략

## 개요

이 문서는 Android 앱의 릴리즈 빌드에서 예외 처리를 어떻게 할지에 대한 전략을 정리합니다. 
**"모든 예외를 잡아서 크래시를 막는다"**는 접근법의 문제점을 명확히 하고, 
올바른 예외 처리 전략을 제시합니다.

---

## 1. Debug 빌드 vs Release 빌드: 예외 처리 철학의 차이

### Debug 빌드의 목적
- **개발자의 빠른 피드백**: 문제를 즉시 발견하고 수정
- **상세한 에러 정보**: 스택 트레이스, 로그, 디버거 연결
- **빠른 실패(Fail Fast)**: 문제를 숨기지 않고 즉시 노출

```kotlin
// Debug 빌드: 예외를 그대로 던져서 개발자가 즉시 인지
fun loadUserData() {
    val data = api.getUserData() // 예외 발생 시 즉시 크래시
    // 개발자가 즉시 문제를 발견하고 수정 가능
}
```

### Release 빌드의 목적
- **사용자 경험 보호**: 예상치 못한 크래시로 인한 앱 종료 방지
- **데이터 손실 방지**: 사용자가 작업 중인 데이터 보호
- **안정성**: 복구 가능한 오류는 조용히 처리

```kotlin
// Release 빌드: 복구 가능한 예외는 처리하되, 치명적 오류는 여전히 크래시
fun loadUserData() {
    try {
        val data = api.getUserData()
        // 성공 처리
    } catch (e: NetworkException) {
        // 네트워크 오류: 사용자에게 재시도 옵션 제공
        showRetryDialog()
    } catch (e: ServerException) {
        // 서버 오류: 사용자에게 알림
        showErrorMessage("서버 오류가 발생했습니다")
    }
    // 치명적 오류(예: NullPointerException)는 catch하지 않음 → 크래시
}
```

### 핵심 차이점

| 항목 | Debug 빌드 | Release 빌드 |
|------|-----------|--------------|
| **목적** | 개발자 피드백 | 사용자 경험 보호 |
| **예외 처리** | 최소한만 처리 | 복구 가능한 것만 처리 |
| **크래시 허용** | ✅ 즉시 크래시 | ⚠️ 치명적 오류만 크래시 |
| **로깅** | 상세 로그 | Crashlytics에 기록 |
| **에러 메시지** | 기술적 상세 정보 | 사용자 친화적 메시지 |

---

## 2. Firebase Crashlytics: Fatal vs Non-Fatal

### Fatal (Crash) - 앱이 강제 종료된 경우

**정의**: 처리되지 않은 예외(Uncaught Exception)로 인해 앱이 종료된 경우

**집계 기준**:
- `Thread.setDefaultUncaughtExceptionHandler`가 처리하지 않은 예외
- 앱의 메인 스레드나 백그라운드 스레드에서 발생한 uncaught exception
- Android 시스템이 앱을 강제 종료시킨 경우

**Crashlytics 대시보드 표시**:
- **Crash-free users**: 크래시가 발생하지 않은 사용자 비율
- **Crash count**: 크래시 발생 횟수
- **Affected users**: 크래시를 경험한 사용자 수

```kotlin
// Fatal이 되는 경우
fun processData() {
    val data: String = null // NullPointerException 발생
    val length = data.length // 앱 크래시 → Fatal로 기록됨
}
```

### Non-Fatal - 앱이 계속 실행되는 경우

**정의**: 개발자가 명시적으로 `recordException()`을 호출한 경우

**집계 기준**:
- `FirebaseCrashlytics.getInstance().recordException(throwable)` 호출
- 앱이 계속 실행 중인 상태에서 기록된 예외
- 사용자가 앱을 사용할 수 있는 상태

**Crashlytics 대시보드 표시**:
- **Non-fatal exceptions**: 별도 섹션에 표시
- **Crash-free users에 영향 없음**: 크래시로 집계되지 않음
- **통계 왜곡**: 실제 크래시가 Non-fatal로 기록되면 통계가 왜곡됨

```kotlin
// Non-Fatal로 기록되는 경우
fun processData() {
    try {
        val data: String = null
        val length = data.length // NullPointerException 발생
    } catch (e: NullPointerException) {
        // 예외를 잡아서 Non-Fatal로 기록
        FirebaseCrashlytics.getInstance().recordException(e)
        // 앱은 계속 실행됨 → 하지만 잘못된 상태일 수 있음
    }
}
```

### 왜 이 구분이 중요한가?

**통계의 신뢰성**:
- Fatal: 실제로 사용자가 경험한 크래시
- Non-Fatal: 개발자가 기록한 예외 (크래시가 아닐 수도 있음)

**우선순위 결정**:
- Fatal 크래시는 즉시 수정해야 할 치명적 문제
- Non-Fatal은 중요도가 낮을 수 있음 (하지만 무시하면 안 됨)

**문제점**: 모든 예외를 Non-Fatal로 기록하면
- 실제 크래시율이 낮아 보임 (통계 왜곡)
- 치명적 버그가 숨겨짐
- 우선순위 결정이 어려워짐

---

## 3. 모든 예외를 Throwable로 잡을 경우 발생하는 문제점

### 3.1 Crash 통계 왜곡

**문제 상황**:
```kotlin
// 모든 예외를 잡아버리는 잘못된 패턴
fun processCriticalData() {
    try {
        val data = loadData()
        processData(data)
        saveData(data)
    } catch (e: Throwable) { // 모든 예외를 잡음
        FirebaseCrashlytics.getInstance().recordException(e)
        // 앱은 계속 실행되지만 잘못된 상태
    }
}
```

**발생하는 문제**:
- 실제로는 앱이 크래시해야 할 상황인데 Non-Fatal로 기록됨
- Crash-free users 비율이 비정상적으로 높아짐
- 실제 사용자 경험과 통계가 불일치
- **개발팀이 실제 문제의 심각성을 인지하지 못함**

**예시**:
```
실제 상황: 100명 중 10명이 크래시 경험
잘못된 처리: 모든 예외를 잡아서 Non-Fatal로 기록
통계 결과: Crash-free users 100% (완전히 거짓)
```

### 3.2 치명적 상태 오류 은폐

**문제 상황**:
```kotlin
fun initializeApp() {
    try {
        val database = DatabaseManager.initialize() // null 반환 가능
        val config = ConfigManager.load() // null 반환 가능
        
        // database나 config가 null이면 앱이 제대로 작동할 수 없음
        // 하지만 예외를 잡아버리면 앱이 계속 실행됨
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        // 앱은 실행되지만 완전히 망가진 상태
    }
}
```

**발생하는 문제**:
- 앱이 초기화되지 않았는데도 계속 실행됨
- 사용자는 앱이 작동하지 않는 것을 경험하지만 크래시는 안 남
- **디버깅이 매우 어려워짐**: 크래시가 없어서 문제를 발견하기 어려움
- **데이터 손실 위험**: 잘못된 상태에서 데이터를 저장하면 더 큰 문제 발생

**실제 사례**:
```
시나리오: 데이터베이스 초기화 실패
- 올바른 처리: 크래시 → 개발자가 즉시 수정
- 잘못된 처리: 예외를 잡아서 계속 실행 → 사용자는 앱이 작동 안 함을 경험
              → 개발자는 크래시 로그가 없어서 문제를 발견하기 어려움
```

### 3.3 UX 악화

**문제 상황**:
```kotlin
fun saveUserData() {
    try {
        database.save(userData) // 실패할 수 있음
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        // 사용자에게 아무것도 알리지 않음
        // 사용자는 데이터가 저장되었다고 생각하지만 실제로는 저장 안 됨
    }
}
```

**발생하는 문제**:
- 사용자에게 피드백이 없음 (에러 메시지, 토스트 등)
- 사용자는 작업이 성공했다고 생각하지만 실제로는 실패
- **데이터 손실**: 사용자가 입력한 데이터가 사라짐
- **신뢰도 하락**: 사용자가 앱을 신뢰하지 않게 됨

**올바른 처리**:
```kotlin
fun saveUserData() {
    try {
        database.save(userData)
        showSuccessMessage("저장되었습니다")
    } catch (e: DatabaseException) {
        FirebaseCrashlytics.getInstance().recordException(e)
        showErrorMessage("저장에 실패했습니다. 다시 시도해주세요")
        // 사용자에게 명확한 피드백 제공
    }
}
```

### 3.4 디버깅 어려움

**문제 상황**:
```kotlin
fun complexOperation() {
    try {
        step1()
        step2()
        step3()
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        // 어떤 단계에서 실패했는지 알 수 없음
    }
}
```

**발생하는 문제**:
- 스택 트레이스만으로는 정확한 실패 지점 파악 어려움
- 로컬 변수 상태를 확인할 수 없음
- 재현이 어려운 버그의 경우 디버깅이 거의 불가능

**올바른 처리**:
```kotlin
fun complexOperation() {
    try {
        step1()
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().log("Failed at step1")
        FirebaseCrashlytics.getInstance().recordException(e)
        throw e // 또는 적절한 복구 로직
    }
    
    try {
        step2()
    } catch (e: Exception) {
        FirebaseCrashlytics.getInstance().log("Failed at step2")
        FirebaseCrashlytics.getInstance().recordException(e)
        throw e
    }
    // 각 단계별로 명확하게 처리
}
```

---

## 4. 릴리즈 빌드에서 권장되는 예외 분류 기준

### 4.1 복구 가능한 예외 (Recoverable Exceptions)

**정의**: 일시적인 문제이거나 사용자 액션으로 해결 가능한 예외

**특징**:
- 네트워크 연결 문제
- 서버 일시적 오류 (5xx 에러)
- 권한 요청 거부
- 사용자 입력 검증 실패
- 외부 서비스 일시적 장애

**처리 방법**:
- 예외를 catch하여 처리
- 사용자에게 명확한 에러 메시지 제공
- 재시도 옵션 제공
- Non-Fatal로 기록 (선택적)

```kotlin
// 복구 가능한 예외 처리 예시
suspend fun loadUserData(): Result<UserData> {
    return try {
        val response = api.getUserData()
        Result.Success(response)
    } catch (e: IOException) {
        // 네트워크 오류: 사용자에게 재시도 옵션 제공
        FirebaseCrashlytics.getInstance().log("Network error: ${e.message}")
        Result.Error("인터넷 연결을 확인해주세요", retryable = true)
    } catch (e: HttpException) {
        when (e.code()) {
            in 500..599 -> {
                // 서버 오류: 일시적 문제일 수 있음
                FirebaseCrashlytics.getInstance().log("Server error: ${e.code()}")
                Result.Error("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요", retryable = true)
            }
            in 400..499 -> {
                // 클라이언트 오류: 사용자 액션 필요
                Result.Error("요청을 처리할 수 없습니다", retryable = false)
            }
            else -> throw e // 예상치 못한 오류는 다시 던짐
        }
    }
}
```

### 4.2 복구 불가능한 예외 (Unrecoverable Exceptions)

**정의**: 앱의 논리적 오류나 치명적인 상태 오류

**특징**:
- NullPointerException (예상치 못한 null)
- IllegalStateException (앱 상태가 잘못됨)
- ClassCastException (타입 캐스팅 오류)
- 초기화 실패 (앱이 제대로 작동할 수 없음)
- 데이터 무결성 오류

**처리 방법**:
- **예외를 catch하지 않음** → 크래시 허용
- 또는 catch하더라도 즉시 앱 종료
- Fatal로 기록되어야 함

```kotlin
// 복구 불가능한 예외 처리 예시
fun initializeApp() {
    val database = DatabaseManager.initialize()
        ?: throw IllegalStateException("Database initialization failed")
    // null이면 앱이 작동할 수 없으므로 즉시 크래시
    
    val config = ConfigManager.load()
        ?: throw IllegalStateException("Config loading failed")
    // 마찬가지로 크래시
    
    // 초기화가 완료된 후에만 정상 동작 가능
}

fun processData(data: UserData) {
    // data가 null이면 논리적 오류
    requireNotNull(data) { "UserData must not be null" }
    // requireNotNull은 IllegalArgumentException을 던지고, 이는 크래시로 이어짐
}
```

### 4.3 예외 분류 의사결정 트리

```
예외 발생
    │
    ├─ 네트워크 오류? 
    │   └─ YES → 복구 가능 (재시도 가능)
    │
    ├─ 서버 오류 (5xx)?
    │   └─ YES → 복구 가능 (일시적 문제)
    │
    ├─ 사용자 입력 오류?
    │   └─ YES → 복구 가능 (사용자에게 알림)
    │
    ├─ 권한 거부?
    │   └─ YES → 복구 가능 (권한 요청)
    │
    ├─ NullPointerException?
    │   └─ YES → 복구 불가능 (논리 오류)
    │
    ├─ IllegalStateException?
    │   └─ YES → 복구 불가능 (상태 오류)
    │
    ├─ 초기화 실패?
    │   └─ YES → 복구 불가능 (앱 작동 불가)
    │
    └─ 기타 예외
        └─ 케이스별 판단 필요
```

---

## 5. 릴리즈에서도 일부 예외는 의도적으로 크래시를 유지해야 하는 이유

### 5.1 빠른 문제 발견 (Fail Fast 원칙)

**이유**: 크래시가 발생하면 개발팀이 즉시 문제를 인지하고 수정할 수 있음

**예시**:
```kotlin
// 잘못된 처리: 예외를 잡아서 숨김
fun saveData(data: Data) {
    try {
        database.save(data)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        // 크래시는 안 나지만 데이터도 저장 안 됨
        // 개발자는 문제를 발견하기 어려움
    }
}

// 올바른 처리: 치명적 오류는 크래시 허용
fun saveData(data: Data) {
    val database = getDatabase()
        ?: throw IllegalStateException("Database not initialized")
    // 초기화 안 된 상태면 즉시 크래시 → 개발자가 즉시 수정 가능
    
    database.save(data) // 저장 실패 시 예외 발생 → 크래시
    // 크래시가 발생하면 개발자가 즉시 문제를 발견하고 수정
}
```

### 5.2 데이터 무결성 보호

**이유**: 잘못된 상태에서 데이터를 저장하면 더 큰 문제가 발생할 수 있음

**예시**:
```kotlin
// 잘못된 처리: 잘못된 데이터도 저장 시도
fun saveUserProfile(user: User?) {
    try {
        if (user == null) {
            FirebaseCrashlytics.getInstance().log("User is null")
            return // 조용히 종료
        }
        database.save(user)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }
}

// 올바른 처리: null이면 즉시 크래시
fun saveUserProfile(user: User?) {
    requireNotNull(user) { "User must not be null when saving" }
    // null이면 즉시 크래시 → 잘못된 데이터 저장 방지
    
    database.save(user)
}
```

### 5.3 사용자 경험 보호

**이유**: 크래시가 나는 것보다 앱이 작동하지 않는 상태로 계속 실행되는 것이 더 나쁨

**예시**:
```
시나리오: 메인 화면 초기화 실패

잘못된 처리:
- 예외를 잡아서 앱은 실행됨
- 하지만 화면이 비어있거나 오류 상태
- 사용자는 앱이 작동하지 않는다고 느낌
- 재시작해도 같은 문제 반복
- 사용자 경험: 매우 나쁨

올바른 처리:
- 초기화 실패 시 즉시 크래시
- 사용자가 앱을 재시작하면 문제 해결 (일시적 문제인 경우)
- 또는 개발자가 수정 후 업데이트 배포
- 사용자 경험: 크래시 후 재시작하면 정상 작동
```

### 5.4 통계의 신뢰성

**이유**: 실제 크래시율을 정확히 파악해야 우선순위를 올바르게 설정할 수 있음

**예시**:
```
시나리오: 100명의 사용자 중 10명이 실제로 크래시 경험

잘못된 처리 (모든 예외를 잡음):
- Crash-free users: 100% (거짓)
- 개발팀: "크래시가 없네? 안정적이구나"
- 실제로는 10명이 문제를 경험 중
- 우선순위 설정 불가능

올바른 처리:
- Crash-free users: 90% (정확)
- 개발팀: "10%의 사용자가 크래시를 경험하고 있구나"
- 즉시 수정 작업 시작
- 우선순위 명확
```

---

## 6. 실무에서 사용할 수 있는 권장 패턴

### 6.1 Result 타입을 활용한 예외 처리

```kotlin
// Result 타입 정의 (프로젝트에 이미 존재)
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>
    data object Loading : Result<Nothing>
}

// Repository 레이어에서 예외 처리
class UserRepository(
    private val api: UserApi,
    private val crashlytics: FirebaseCrashlytics
) {
    suspend fun getUserData(): Result<UserData> {
        return try {
            val response = api.getUserData()
            Result.Success(response)
        } catch (e: IOException) {
            // 네트워크 오류: 복구 가능
            crashlytics.log("Network error in getUserData")
            Result.Error("인터넷 연결을 확인해주세요", e)
        } catch (e: HttpException) {
            when (e.code()) {
                in 500..599 -> {
                    // 서버 오류: 일시적 문제
                    crashlytics.log("Server error: ${e.code()}")
                    Result.Error("서버 오류가 발생했습니다", e)
                }
                else -> {
                    // 예상치 못한 HTTP 오류
                    crashlytics.recordException(e)
                    Result.Error("오류가 발생했습니다", e)
                }
            }
        } catch (e: Exception) {
            // 예상치 못한 예외: Non-Fatal로 기록하되 Result로 반환
            crashlytics.recordException(e)
            Result.Error("알 수 없는 오류가 발생했습니다", e)
        }
        // NullPointerException, IllegalStateException 등은 catch하지 않음
        // → 크래시로 이어져서 개발자가 즉시 수정 가능
    }
}
```

### 6.2 ViewModel에서의 예외 처리

```kotlin
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<Result<UserData>>(Result.Loading)
    val uiState: StateFlow<Result<UserData>> = _uiState.asStateFlow()
    
    fun loadUser() {
        viewModelScope.launch {
            _uiState.value = Result.Loading
            
            // Repository가 이미 예외를 처리했으므로 Result만 처리
            _uiState.value = repository.getUserData()
        }
        // ViewModel에서는 비즈니스 로직만 처리
        // 예외 처리는 Repository에서 완료
    }
}
```

### 6.3 UI에서의 예외 처리

```kotlin
@Composable
fun UserScreen(
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (val state = uiState) {
        is Result.Loading -> {
            LoadingIndicator()
        }
        is Result.Success -> {
            UserContent(state.data)
        }
        is Result.Error -> {
            ErrorContent(
                message = state.message,
                onRetry = { viewModel.loadUser() }
            )
        }
    }
}
```

### 6.4 초기화 실패 시 크래시 패턴

```kotlin
class AppInitializer(
    private val context: Context,
    private val crashlytics: FirebaseCrashlytics
) {
    fun initialize() {
        // 초기화 실패 시 즉시 크래시
        val database = initializeDatabase()
            ?: throw IllegalStateException("Database initialization failed")
        
        val config = loadConfig()
            ?: throw IllegalStateException("Config loading failed")
        
        // 초기화 성공 시에만 정상 진행
        crashlytics.log("App initialization completed")
    }
    
    private fun initializeDatabase(): Database? {
        return try {
            DatabaseManager.initialize(context)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            null // 실패 시 null 반환 → initialize()에서 크래시 발생
        }
    }
}
```

### 6.5 네트워크 요청 재시도 패턴

```kotlin
suspend fun <T> retryOnNetworkError(
    maxRetries: Int = 3,
    delayMillis: Long = 1000,
    block: suspend () -> T
): Result<T> {
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            val result = block()
            return Result.Success(result)
        } catch (e: IOException) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(delayMillis * (attempt + 1)) // 지수 백오프
            }
        } catch (e: Exception) {
            // 네트워크 오류가 아닌 경우 즉시 실패
            FirebaseCrashlytics.getInstance().recordException(e)
            return Result.Error("요청 처리 중 오류가 발생했습니다", e)
        }
    }
    
    // 모든 재시도 실패
    FirebaseCrashlytics.getInstance().log("Network retry failed after $maxRetries attempts")
    return Result.Error("네트워크 연결을 확인해주세요", lastException)
}
```

### 6.6 전역 예외 핸들러 (선택적)

```kotlin
class CustomUncaughtExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val crashlytics: FirebaseCrashlytics
) : Thread.UncaughtExceptionHandler {
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        // 치명적 예외만 여기로 옴 (복구 가능한 예외는 이미 처리됨)
        
        // 추가 정보 기록
        crashlytics.setCustomKey("thread_name", thread.name)
        crashlytics.setCustomKey("thread_id", thread.id.toString())
        
        // 예외 기록 (Fatal로 자동 기록됨)
        crashlytics.recordException(exception)
        
        // 기본 핸들러 호출 (크래시 발생)
        defaultHandler?.uncaughtException(thread, exception)
    }
}

// Application에서 설정
class WalkingBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            CustomUncaughtExceptionHandler(defaultHandler, FirebaseCrashlytics.getInstance())
        )
    }
}
```

---

## 7. 체크리스트: 예외 처리 검토 항목

코드 리뷰 시 다음 항목을 확인하세요:

### ✅ 올바른 패턴

- [ ] 네트워크 오류는 catch하여 사용자에게 재시도 옵션 제공
- [ ] 서버 오류(5xx)는 catch하여 일시적 문제임을 알림
- [ ] 사용자 입력 검증 실패는 catch하여 명확한 에러 메시지 제공
- [ ] NullPointerException, IllegalStateException 등은 catch하지 않음
- [ ] 초기화 실패는 즉시 크래시 발생
- [ ] 복구 가능한 예외는 Result 타입으로 반환
- [ ] 사용자에게 항상 명확한 피드백 제공

### ❌ 피해야 할 패턴

- [ ] `catch (e: Throwable)`로 모든 예외를 잡는 것
- [ ] 예외를 잡고 아무것도 하지 않는 것 (조용한 실패)
- [ ] 예외를 잡고 사용자에게 알리지 않는 것
- [ ] 치명적 오류를 Non-Fatal로 기록하는 것
- [ ] 잘못된 상태에서 앱을 계속 실행하는 것

---

## 8. 결론

### 핵심 원칙

1. **복구 가능한 예외만 catch**: 네트워크, 서버 오류 등
2. **치명적 오류는 크래시 허용**: NullPointerException, IllegalStateException 등
3. **사용자에게 항상 피드백 제공**: 에러 메시지, 재시도 옵션
4. **통계의 신뢰성 유지**: 실제 크래시율을 정확히 파악

### 최종 메시지

**"모든 예외를 잡아서 크래시를 막는다"**는 접근법은:

- ❌ 통계를 왜곡시킴
- ❌ 치명적 버그를 숨김
- ❌ 디버깅을 어렵게 만듦
- ❌ 사용자 경험을 악화시킴

**올바른 접근법**은:

- ✅ 복구 가능한 예외만 처리
- ✅ 치명적 오류는 크래시 허용
- ✅ 사용자에게 명확한 피드백 제공
- ✅ 통계를 신뢰할 수 있게 유지

**결과**: 더 안정적이고 신뢰할 수 있는 앱을 만들 수 있습니다.

---

## 참고 자료

- [Firebase Crashlytics 문서](https://firebase.google.com/docs/crashlytics)
- [Android 예외 처리 가이드](https://developer.android.com/topic/libraries/architecture/coroutines#exceptions)
- 프로젝트 내 관련 문서:
  - `docs/05-faq.md` - FAQ 및 일반적인 질문
  - `app/src/main/java/swyp/team/walkit/utils/CrashReportingHelper.kt` - Crashlytics 헬퍼
