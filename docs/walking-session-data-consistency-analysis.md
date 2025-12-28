# WalkingSession 메모리 vs Room 이원화 분석

## 개요

WalkingSession 데이터의 메모리(UiState)와 Room 데이터베이스 저장 간의 이원화 현상을 분석하고, Flow 기반 반응형 아키텍처를 통한 개선 방안을 제시합니다.

## 현재 아키텍처 구조

### 데이터 모델

#### WalkingSession (도메인 모델)
```kotlin
data class WalkingSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val stepCount: Int = 0,
    val locations: List<LocationPoint> = emptyList(),
    val totalDistance: Float = 0f,
    val preWalkEmotion: EmotionType,
    val postWalkEmotion: EmotionType,
    val note: String? = null,
    val localImagePath: String? = null,
    val serverImageUrl: String? = null,
    val createdDate: String
)
```

#### WalkingSessionEntity (Room 엔티티)
```kotlin
@Entity(tableName = "walking_sessions")
data class WalkingSessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long,
    val stepCount: Int = 0,
    val locationsJson: String = "[]",
    val totalDistance: Float = 0f,
    val syncState: SyncState,
    val preWalkEmotion: String,
    val postWalkEmotion: String,
    val note: String? = null,
    val localImagePath: String? = null,
    val serverImageUrl: String? = null,
    val createdDate: String
)
```

## 데이터 흐름 분석

### 산책 시작 → WalkingResultScreen까지의 흐름

#### 1. 메모리(UiState)에서의 WalkingSession 관리

**산책 시작 단계 (`WalkingViewModel.startWalking()`)**
- 실시간 센서 데이터 수집 (걸음 수, 위치, 시간 등)
- `WalkingUiState.Walking` 상태로 실시간 UI 업데이트

**산책 종료 단계 (`WalkingViewModel.stopWalking()`)**
```kotlin
suspend fun stopWalking() {
    // 1. 메모리에서 즉시 WalkingSession 생성
    val completedSession = createCompletedSession()

    // 2. UI 상태를 SavingSession으로 변경 (로딩 화면)
    _uiState.value = WalkingUiState.SavingSession(completedSession)

    // 3. DB에 부분 저장
    currentSessionLocalId = walkingSessionRepository.createSessionPartial(completedSession)

    // 4. UI 상태를 Completed로 변경
    _uiState.value = WalkingUiState.Completed(completedSession)
}
```

#### 2. Room에 저장되는 WalkingSession 관리

**부분 저장 (`createSessionPartial`)**
- 기본 데이터만 저장: ID, 시간, 걸음 수, 위치, 거리, 감정
- `syncState = PENDING` 상태
- 이미지와 노트는 `null` 상태

**점진적 업데이트**
- 산책 후 감정: `updatePostWalkEmotion()`
- 이미지/노트: `updateSessionImageAndNote()`
- 서버 동기화: `syncSessionToServer()`

## 이원화 문제점

### 1. 메모리 vs DB 데이터 불일치

**메모리 세션 (Completed 상태)**
```kotlin
WalkingSession(
    id = "session-123",
    startTime = 1000000L,
    endTime = 2000000L,
    stepCount = 1500,
    locations = [...],
    totalDistance = 1200.5f,
    preWalkEmotion = EmotionType.HAPPY,
    postWalkEmotion = EmotionType.HAPPY, // 기본값
    note = null,                          // 아직 입력되지 않음
    localImagePath = null,                // 아직 추가되지 않음
    serverImageUrl = null                 // 서버 동기화 전
)
```

**DB 세션 (업데이트 후)**
```kotlin
WalkingSession(
    id = "session-123",
    startTime = 1000000L,
    endTime = 2000000L,
    stepCount = 1500,
    locations = [...],
    totalDistance = 1200.5f,
    preWalkEmotion = EmotionType.HAPPY,
    postWalkEmotion = EmotionType.CONTENT, // 사용자가 선택한 값
    note = "오늘 산책 너무 좋았다!",       // 사용자가 입력한 메모
    localImagePath = "/storage/...",      // 저장된 이미지 경로
    serverImageUrl = "https://..."        // 서버 URL
)
```

### 2. WalkingResultScreen 데이터 로딩 전략의 문제

```kotlin
// WalkingResultRoute.kt - 현재 구현
LaunchedEffect(currentSessionLocalId) {
    val localId = currentSessionLocalId
    if (localId != null) {
        // DB에서 최신 세션 조회 (권장)
        val loadedSession = viewModel.getSessionById(localId)
        session = loadedSession
    } else {
        // 메모리 Completed 상태 사용 (Fallback - 문제 가능성)
        when (val state = uiState) {
            is WalkingUiState.Completed -> {
                session = state.session // 오래된 메모리 데이터
            }
        }
    }
}
```

**문제점:**
- 일회성 조회로 인한 실시간 업데이트 불가
- 메모리 fallback 시 오래된 데이터 사용
- LaunchedEffect 재실행 시 불필요한 DB I/O 발생

### 3. 데이터 일관성 이슈

| 시점 | 메모리(UiState) | Room DB | 상태 |
|------|----------------|---------|------|
| 산책 종료 직후 | `Completed(session)` - 기본 데이터 | `PENDING` - 기본 데이터 | **일치** |
| 감정 선택 후 | `Completed(session)` - **구버전** | `PENDING` - **최신 데이터** | **불일치** |
| 이미지/노트 추가 후 | `Completed(session)` - **구버전** | `PENDING` - **최신 데이터** | **불일치** |
| 서버 동기화 후 | `Completed(session)` - **구버전** | `SYNCED` - **최신 데이터** | **불일치** |

## 잠재적 문제 상황 (우선순위별)

### 🔴 높은 우선순위

| 문제 | 발생 가능성 | 영향도 | 설명 |
|------|------------|--------|------|
| **UI에서 오래된 데이터 표시** | 높음 | 중간 | DB 업데이트 후 메모리 상태가 갱신되지 않아 사용자가 잘못된 정보를 봄 |

### 🟡 중간 우선순위

| 문제 | 발생 가능성 | 영향도 | 설명 |
|------|------------|--------|------|
| **앱 강제 종료 시 데이터 손실** | 낮음 | 높음 | 메모리 데이터가 DB에 저장되기 전 앱 종료 시 실시간 데이터 손실 (현재는 즉시 저장으로 완화됨) |
| **에러 복구 시 데이터 불일치** | 중간 | 중간 | DB 저장 실패 시 메모리와 DB 상태 불일치 |

### 🟢 낮은 우선순위

| 문제 | 발생 가능성 | 영향도 | 설명 |
|------|------------|--------|------|
| **동시성 문제** | 낮음 | 낮음 | 단일 사용자 앱 특성상 동시 접근 가능성 낮음 |

## 개선 방안: Flow 기반 반응형 아키텍처

### 핵심 원칙

1. **DB를 단일 진실 공급원(Single Source of Truth)으로**
2. **Flow를 통한 자동 UI 갱신**
3. **메모리 상태는 화면 전환 플래그로만 활용**

### 1. Repository에 Flow 추가

```kotlin
interface WalkingSessionRepository {
    // 기존 메서드들...
    
    // 🆕 세션 관찰 Flow 추가
    fun observeSessionById(id: String): Flow<WalkingSession?>
    
    // 기존 일회성 조회는 유지 (특정 상황에서 필요)
    suspend fun getSessionById(id: String): WalkingSession?
}

class WalkingSessionRepositoryImpl(
    private val walkingSessionDao: WalkingSessionDao
) : WalkingSessionRepository {
    
    override fun observeSessionById(id: String): Flow<WalkingSession?> {
        return walkingSessionDao.observeSessionById(id)
            .map { entity -> entity?.toDomain() }
    }
}
```

### 2. DAO에 Flow 쿼리 추가

```kotlin
@Dao
interface WalkingSessionDao {
    // 기존 메서드들...
    
    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    fun observeSessionById(id: String): Flow<WalkingSessionEntity?>
    
    @Query("SELECT * FROM walking_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): WalkingSessionEntity?
}
```

### 3. ViewModel 재구조화

#### UiState 단순화

```kotlin
sealed class WalkingUiState {
    data object Idle : WalkingUiState()
    
    data class Walking(
        val stepCount: Int,
        val elapsedTime: Long,
        val distance: Float,
        val locations: List<LocationPoint>
    ) : WalkingUiState()
    
    data object SavingSession : WalkingUiState()
    
    // ✅ 세션 데이터를 제거하고 상태 플래그로만 사용
    data object SessionSaved : WalkingUiState()
}
```

#### ViewModel 구현

```kotlin
class WalkingViewModel(
    private val walkingSessionRepository: WalkingSessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<WalkingUiState>(WalkingUiState.Idle)
    val uiState: StateFlow<WalkingUiState> = _uiState.asStateFlow()
    
    private val _currentSessionLocalId = MutableStateFlow<String?>(null)
    
    // ✅ 현재 세션을 Flow로 관찰
    val currentSession: StateFlow<WalkingSession?> = _currentSessionLocalId
        .flatMapLatest { id ->
            if (id != null) {
                walkingSessionRepository.observeSessionById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    suspend fun stopWalking() {
        val completedSession = createCompletedSession()
        
        // 1. 저장 중 상태로 변경
        _uiState.value = WalkingUiState.SavingSession
        
        try {
            // 2. DB에 저장
            val sessionId = walkingSessionRepository.createSessionPartial(completedSession)
            _currentSessionLocalId.value = sessionId
            
            // 3. 저장 완료 상태로 변경 (세션 데이터 없음)
            _uiState.value = WalkingUiState.SessionSaved
            
            // ✅ currentSession Flow가 자동으로 DB 데이터를 방출함
            
        } catch (e: Exception) {
            Timber.e(e, "세션 저장 실패")
            _uiState.value = WalkingUiState.Idle
        }
    }
    
    // ✅ DB만 업데이트, Flow가 자동으로 UI 갱신
    fun updatePostWalkEmotion(postWalkEmotion: EmotionType) {
        viewModelScope.launch {
            try {
                val localId = _currentSessionLocalId.value
                    ?: throw IllegalStateException("저장된 세션이 없습니다")
                
                // DB만 업데이트
                walkingSessionRepository.updatePostWalkEmotion(localId, postWalkEmotion)
                
                // Flow가 자동으로 UI에 전파 (별도 메모리 업데이트 불필요)
                
            } catch (e: Exception) {
                Timber.e(e, "산책 후 감정 업데이트 실패")
                throw e
            }
        }
    }
    
    fun updateSessionImageAndNote(imageUri: Uri?, note: String?) {
        viewModelScope.launch {
            try {
                val localId = _currentSessionLocalId.value ?: return@launch
                
                // DB만 업데이트
                walkingSessionRepository.updateSessionImageAndNote(
                    id = localId,
                    imageUri = imageUri,
                    note = note
                )
                
                // Flow가 자동으로 UI에 전파
                
            } catch (e: Exception) {
                Timber.e(e, "이미지/노트 업데이트 실패")
            }
        }
    }
}
```

### 4. WalkingResultScreen 수정

```kotlin
@Composable
fun WalkingResultRoute(
    viewModel: WalkingViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // ✅ Flow로 세션 관찰 (자동 갱신)
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    
    // 저장 중일 때만 로딩 표시
    if (uiState is WalkingUiState.SavingSession) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // ✅ session이 null이면 로딩, 있으면 화면 표시
    if (session == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        WalkingResultScreen(
            session = session!!, // Flow에서 자동으로 최신 데이터 제공
            onPostWalkEmotionSelected = viewModel::updatePostWalkEmotion,
            onImageAndNoteUpdated = viewModel::updateSessionImageAndNote,
            onSaveComplete = {
                viewModel.completeSession()
                onNavigateToHome()
            },
            onNavigateToHistory = onNavigateToHistory,
            modifier = modifier
        )
    }
}
```

### 5. 효율적인 부분 업데이트

```kotlin
@Dao
interface WalkingSessionDao {
    // 전체 엔티티 업데이트 (기본)
    @Update
    suspend fun update(session: WalkingSessionEntity)
    
    // 특정 필드만 업데이트하는 쿼리들
    @Query("UPDATE walking_sessions SET postWalkEmotion = :emotion WHERE id = :id")
    suspend fun updatePostWalkEmotion(id: String, emotion: String)
    
    @Query("""
        UPDATE walking_sessions 
        SET localImagePath = :localImagePath, 
            note = :note 
        WHERE id = :id
    """)
    suspend fun updateImageAndNote(
        id: String, 
        localImagePath: String?, 
        note: String?
    )
    
    @Query("UPDATE walking_sessions SET serverImageUrl = :url WHERE id = :id")
    suspend fun updateServerImageUrl(id: String, url: String?)
    
    @Query("""
        UPDATE walking_sessions 
        SET syncState = :state 
        WHERE id = :id
    """)
    suspend fun updateSyncState(id: String, state: String)
}
```

**권장 사항:**
- 단순 업데이트는 기존 `@Update` 활용
- 자주 사용되는 특정 필드 업데이트는 별도 쿼리로 최적화
- 각 업데이트는 독립적으로 실행되며 Flow가 변경을 자동 감지

### 6. 에러 처리 전략

```kotlin
sealed class SessionUpdateResult {
    data class Success(val session: WalkingSession) : SessionUpdateResult()
    data class Error(val exception: Exception, val previousSession: WalkingSession?) : SessionUpdateResult()
}

fun updatePostWalkEmotionWithResult(postWalkEmotion: EmotionType): Flow<SessionUpdateResult> = flow {
    try {
        val localId = _currentSessionLocalId.value
            ?: throw IllegalStateException("저장된 세션이 없습니다")
        
        val previousSession = currentSession.value
        
        walkingSessionRepository.updatePostWalkEmotion(localId, postWalkEmotion)
        
        // 업데이트된 세션을 한 번 조회하여 방출
        val updatedSession = walkingSessionRepository.getSessionById(localId)
        if (updatedSession != null) {
            emit(SessionUpdateResult.Success(updatedSession))
        }
        
    } catch (e: Exception) {
        Timber.e(e, "산책 후 감정 업데이트 실패")
        emit(SessionUpdateResult.Error(e, currentSession.value))
    }
}
```

## 개선 후 데이터 흐름

### 업데이트 시퀀스

```
사용자 액션 (감정 선택)
    ↓
ViewModel.updatePostWalkEmotion()
    ↓
Repository.updatePostWalkEmotion()
    ↓
Room DB 업데이트
    ↓
DAO Flow가 변경 감지
    ↓
Repository Flow가 도메인 모델로 변환
    ↓
ViewModel.currentSession Flow 방출
    ↓
WalkingResultScreen 자동 재구성
    ↓
UI에 최신 데이터 표시
```

### 데이터 일관성 보장

| 시점 | Room DB | Flow 방출 | UI 표시 | 상태 |
|------|---------|-----------|---------|------|
| 산책 종료 직후 | 기본 데이터 | 기본 데이터 | 기본 데이터 | **일치** |
| 감정 선택 후 | **최신 데이터** | **최신 데이터** | **최신 데이터** | **일치** |
| 이미지/노트 추가 후 | **최신 데이터** | **최신 데이터** | **최신 데이터** | **일치** |
| 서버 동기화 후 | **최신 데이터** | **최신 데이터** | **최신 데이터** | **일치** |

## 성능 고려사항

### Flow vs 일회성 조회

**Flow의 이점:**
- DB 변경 시 자동 갱신 (추가 코드 불필요)
- Room이 최적화된 관찰자 패턴 제공
- 메모리 누수 없음 (Lifecycle 인식)

**잠재적 우려:**
- 과도한 재구성? → Room의 diffing으로 실제 변경 시에만 방출
- 메모리 오버헤드? → StateFlow + WhileSubscribed로 구독자 없을 때 자동 정리

### 벤치마크 예상

```kotlin
// 일회성 조회 (현재 방식)
// - DB 쿼리: 매번 실행
// - 메모리 동기화: 수동 관리 필요
// - 코드 복잡도: 높음

// Flow 기반 (개선안)
// - DB 쿼리: 초기 1회 + 변경 시에만
// - 메모리 동기화: 자동
// - 코드 복잡도: 낮음
```

## 마이그레이션 체크리스트

### Phase 1: Repository 레이어
- [ ] `observeSessionById()` Flow 메서드 추가
- [ ] DAO에 `Flow<WalkingSessionEntity?>` 쿼리 추가
- [ ] 기존 `getSessionById()` 유지 (특정 상황용)

### Phase 2: ViewModel 레이어
- [ ] `WalkingUiState.Completed` 제거, `SessionSaved`로 단순화
- [ ] `currentSession` StateFlow 추가
- [ ] 업데이트 메서드에서 메모리 동기화 코드 제거

### Phase 3: UI 레이어
- [ ] `WalkingResultRoute`에서 `collectAsStateWithLifecycle()` 사용
- [ ] 수동 `LaunchedEffect` 제거
- [ ] null 체크 및 로딩 처리 추가

### Phase 4: 테스트
- [ ] Flow 방출 테스트
- [ ] UI 자동 갱신 테스트
- [ ] 에러 시나리오 테스트

## 결론

### 현재 구조의 문제점
- **데이터 이원화**: 메모리와 DB 간 불일치로 인한 버그 가능성
- **수동 동기화**: 개발자가 메모리 상태를 수동으로 업데이트해야 함
- **복잡성 증가**: 동기화 로직 관리가 어렵고 누락 가능성 높음

### 개선안의 장점
- **단일 진실 공급원**: DB만 관리하면 되므로 일관성 보장
- **자동 UI 갱신**: Flow가 변경을 자동으로 전파하여 수동 동기화 불필요
- **코드 간결성**: 보일러플레이트 코드 감소
- **테스트 용이성**: 데이터 흐름이 명확하여 테스트 작성 쉬움
- **성능**: Room의 최적화된 관찰자 패턴 활용

### 권장 구현 순서

1. **Repository에 Flow 메서드 추가** (기존 코드 영향 없음)
2. **ViewModel에 currentSession Flow 추가** (기존 상태와 병행 가능)
3. **UI에서 Flow 사용** (점진적 마이그레이션)
4. **기존 메모리 동기화 코드 제거** (검증 후)

이러한 개선을 통해 코드의 복잡도를 낮추고 데이터 일관성을 보장하면서도, Room의 강력한 반응형 기능을 최대한 활용할 수 있습니다.

## 관련 파일
- `WalkingViewModel.kt`: Flow 기반 상태 관리로 전환
- `WalkingSessionRepository.kt`: observeSessionById() 추가
- `WalkingSessionDao.kt`: Flow 쿼리 추가
- `WalkingResultRoute.kt`: collectAsStateWithLifecycle() 사용
- `WalkingSessionMapper.kt`: 도메인 ↔ 엔티티 변환 (변경 없음)
