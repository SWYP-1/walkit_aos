# WalkingSession 분리 저장 설계

## 요구사항

1. **stopWalking() 실행 시**: WalkingSessionEntity를 PENDING 상태로 즉시 저장
   - postWalkEmotion은 선택되지 않았으면 preWalkEmotion과 동일하게 설정
2. **PostWalkingEmotionScreen에서 감정 선택 시**: 저장된 세션의 postWalkEmotion 업데이트
3. **사진/텍스트 단계**: 저장된 세션의 localId를 찾아서 imageUrl과 note 업데이트
4. **기존 `completeAndSaveSession` 분리**: 단계별로 나누어 저장

## 현재 플로우

```
산책 종료 (stopWalking)
  └─> completeAndSaveSession()
      └─> 모든 데이터 수집 (감정, 위치, 걸음수 등)
      └─> walkingSessionRepository.saveSession() (한 번에 저장)
```

## 새로운 플로우

```
산책 종료 (stopWalking)
  └─> saveSessionPartial() 즉시 호출
      └─> walkingSessionRepository.createSessionPartial()
          └─> postWalkEmotion = preWalkEmotion (기본값)
          └─> PENDING 상태로 저장
          └─> localId 반환 및 ViewModel에 저장

PostWalkingEmotionScreen에서 감정 선택
  └─> updatePostWalkEmotion() 호출
      └─> walkingSessionRepository.updatePostWalkEmotion(localId, emotion)
          └─> 기존 세션 찾아서 postWalkEmotion 업데이트

사진/텍스트 단계
  └─> updateSessionImageAndNote() 호출
      └─> walkingSessionRepository.updateSessionImageAndNote(localId)
          └─> 기존 세션 찾아서 imageUrl, note 업데이트

WalkingResultScreen에서 "기록 완료" 버튼 클릭
  └─> syncSessionToServer() 호출
      └─> walkingSessionRepository.syncSessionToServer(localId)
          └─> 서버 동기화 시작
          └─> SyncState: PENDING → SYNCING → SYNCED (성공 시)
          └─> SyncState: PENDING → SYNCING → FAILED (실패 시)
```

## 설계 상세

### 1. Repository 레이어

#### WalkingSessionRepository에 추가할 메서드

```kotlin
/**
 * 부분 세션 생성 (stopWalking() 실행 시 즉시 호출)
 * 
 * @param session 기본 세션 데이터
 *   - postWalkEmotion: 선택되지 않았으면 preWalkEmotion과 동일
 *   - localImagePath: null (나중에 업데이트)
 *   - serverImageUrl: null (서버 동기화 후 업데이트)
 *   - note: null (나중에 업데이트)
 * @return 저장된 세션의 로컬 ID
 */
suspend fun createSessionPartial(session: WalkingSession): Long {
    // 1. 로컬 저장 (PENDING 상태)
    val entity = WalkingSessionMapper.toEntity(
        session = session,
        syncState = SyncState.PENDING
    )
    val localId = walkingSessionDao.insert(entity)
    
    // 서버 동기화는 하지 않음 (아직 완료되지 않았으므로)
    
    Timber.d("부분 세션 저장 완료: localId=$localId")
    return localId
}

/**
 * 세션의 산책 후 감정 업데이트 (PostWalkingEmotionScreen에서 선택 시)
 * 
 * @param localId 업데이트할 세션의 로컬 ID
 * @param postWalkEmotion 선택된 산책 후 감정
 */
suspend fun updatePostWalkEmotion(
    localId: Long,
    postWalkEmotion: EmotionType
) {
    val entity = walkingSessionDao.getSessionById(localId)
        ?: throw IllegalStateException("세션을 찾을 수 없습니다: ID=$localId")
    
    val updatedEntity = entity.copy(
        postWalkEmotion = postWalkEmotion.name
    )
    
    walkingSessionDao.update(updatedEntity)
    
    Timber.d("산책 후 감정 업데이트 완료: localId=$localId, emotion=$postWalkEmotion")
}

/**
 * URI를 파일로 복사하고 경로 반환
 * 
 * **두 가지 경우 처리:**
 * 1. **카메라 촬영**: MediaStore에 저장된 이미지 (content://media/...)
 * 2. **갤러리 선택**: 갤러리 앱의 이미지 (content://media/... 또는 content://com.android.providers.media.documents/...)
 * 
 * **왜 복사가 필요한가?**
 * - 카메라 촬영: MediaStore에 저장되어 있지만, 사용자가 갤러리에서 삭제할 수 있음
 * - 갤러리 선택: 다른 앱의 파일을 참조하므로 권한 문제나 파일 삭제 가능성 있음
 * - **앱 내부 저장소에 복사하면**: 앱과 함께 관리되며, 삭제 시점을 제어할 수 있음
 * 
 * @param uri 복사할 이미지 URI (content:// 또는 file://)
 * @return 저장된 파일의 절대 경로 (실패 시 null)
 */
private suspend fun copyImageUriToFile(uri: Uri): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        // URI 스킴 확인
        val scheme = uri.scheme
        Timber.d("이미지 URI 스킴: $scheme, URI: $uri")
        
        // Content URI인 경우 (카메라 촬영 또는 갤러리 선택)
        val inputStream = when {
            scheme == "content" -> {
                context.contentResolver.openInputStream(uri)
            }
            scheme == "file" -> {
                // File URI인 경우 (드물지만 가능)
                File(uri.path ?: return@withContext null).inputStream()
            }
            else -> {
                Timber.w("지원하지 않는 URI 스킴: $scheme")
                return@withContext null
            }
        } ?: return@withContext null
        
        // 파일명 생성 (타임스탬프 기반)
        val timestamp = System.currentTimeMillis()
        val fileName = "walking_image_${timestamp}.jpg"
        
        // 앱 내부 저장소의 Pictures 디렉토리에 저장
        // getExternalFilesDir: 앱 전용 외부 저장소 (앱 삭제 시 함께 삭제됨)
        // filesDir: 앱 내부 저장소 (항상 사용 가능)
        val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val file = File(fileDir, fileName)
        
        // 파일 복사
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        
        val absolutePath = file.absolutePath
        Timber.d("이미지 파일 복사 완료: $absolutePath (원본 URI: $uri)")
        absolutePath
    } catch (e: Exception) {
        Timber.e(e, "이미지 파일 복사 실패: $uri")
        null
    }
}

/**
 * 세션의 이미지와 노트 업데이트 (사진/텍스트 단계)
 * 
 * @param localId 업데이트할 세션의 로컬 ID
 * @param imageUri 이미지 URI (선택사항, null이면 기존 값 유지)
 * @param note 노트 텍스트 (선택사항, null이면 기존 값 유지)
 */
suspend fun updateSessionImageAndNote(
    localId: Long,
    imageUri: Uri? = null,
    note: String? = null
) {
    val entity = walkingSessionDao.getSessionById(localId)
        ?: throw IllegalStateException("세션을 찾을 수 없습니다: ID=$localId")
    
    // URI를 파일 경로로 변환하여 localImagePath에 저장
    val localImagePath = imageUri?.let { copyImageUriToFile(it) }
    
    val updatedEntity = entity.copy(
        localImagePath = localImagePath ?: entity.localImagePath, // 로컬 파일 경로 저장
        note = note ?: entity.note
    )
    
    walkingSessionDao.update(updatedEntity)
    
    Timber.d("세션 이미지/노트 업데이트 완료: localId=$localId, localImagePath=$localImagePath, note=$note")
    
    // 서버 동기화는 WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 처리
}

/**
 * 세션을 서버와 동기화 (WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 호출)
 * 
 * @param localId 동기화할 세션의 로컬 ID
 */
suspend fun syncSessionToServer(localId: Long) {
    val entity = walkingSessionDao.getSessionById(localId)
        ?: throw IllegalStateException("세션을 찾을 수 없습니다: ID=$localId")
    
    // 이미 동기화된 경우 스킵
    if (entity.syncState == SyncState.SYNCED) {
        Timber.d("이미 동기화된 세션: localId=$localId")
        return
    }
    
    // Domain 모델로 변환
    val session = WalkingSessionMapper.toDomain(entity)
    
    // 로컬 이미지 파일 경로를 URI로 변환 (서버 업로드용)
    val imageUri = entity.localImagePath?.let { imagePath ->
        try {
            val file = File(imagePath)
            if (file.exists()) {
                // FileProvider를 사용하여 URI 생성 (Android 7.0+ 호환)
                // FileProvider는 AndroidManifest.xml에 선언되어 있어야 함
                val authority = "${context.packageName}.fileprovider"
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    authority,
                    file
                )
            } else {
                Timber.w("이미지 파일이 존재하지 않음: $imagePath")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "이미지 URI 변환 실패: $imagePath")
            null
        }
    }
    
    // 동기화 상태를 SYNCING으로 변경
    walkingSessionDao.updateSyncState(localId, SyncState.SYNCING)
    
    try {
        // 서버 동기화 시도
        val serverResponse = syncToServer(session, imageUri, localId)
        
        // 서버 응답에서 받은 imageUrl을 serverImageUrl에 저장
        val updatedEntity = entity.copy(
            serverImageUrl = serverResponse.imageUrl  // 서버에서 받은 URL 저장
            // localImagePath는 유지 (오프라인 지원)
        )
        walkingSessionDao.update(updatedEntity)
        
        // 동기화 성공
        walkingSessionDao.updateSyncState(localId, SyncState.SYNCED)
        Timber.d("서버 동기화 성공: localId=$localId, serverImageUrl=${serverResponse.imageUrl}")
    } catch (e: Exception) {
        // 동기화 실패
        walkingSessionDao.updateSyncState(localId, SyncState.FAILED)
        Timber.e(e, "서버 동기화 실패: localId=$localId")
        throw e
    }
}
```

### 2. DAO 레이어

#### WalkingSessionDao에 추가할 메서드 (선택사항, 성능 최적화용)

```kotlin
/**
 * 산책 후 감정만 업데이트 (성능 최적화)
 */
@Query("""
    UPDATE walking_sessions 
    SET postWalkEmotion = :postWalkEmotion 
    WHERE id = :id
""")
suspend fun updatePostWalkEmotion(
    id: Long,
    postWalkEmotion: String
)

/**
 * 이미지 URL과 노트만 업데이트 (성능 최적화)
 */
@Query("""
    UPDATE walking_sessions 
    SET imageUrl = :imageUrl, note = :note 
    WHERE id = :id
""")
suspend fun updateImageAndNote(
    id: Long,
    imageUrl: String?,
    note: String?
)
```

### 3. ViewModel 레이어

#### WalkingViewModel 수정사항

```kotlin
// 현재 세션의 로컬 ID 저장
private var currentSessionLocalId: Long? = null

/**
 * 부분 세션 저장 (stopWalking() 실행 시 즉시 호출)
 * 
 * postWalkEmotion은 선택되지 않았으면 preWalkEmotion과 동일하게 설정
 */
private fun saveSessionPartial() {
    viewModelScope.launch {
        try {
            val preEmotion = _preWalkingEmotion.value
                ?: throw IllegalStateException("산책 전 감정이 선택되지 않았습니다")
            
            // postWalkEmotion이 선택되지 않았으면 preWalkEmotion과 동일하게 설정
            val postEmotion = _postWalkingEmotion.value ?: preEmotion
            
            val endTime = System.currentTimeMillis()
            val collectedLocations = _locations.value
            val totalDistance = calculateTotalDistance(collectedLocations)
            
            // 부분 세션 생성 (imageUrl과 note는 null)
            val partialSession = WalkingSession(
                startTime = startTimeMillis,
                endTime = endTime,
                stepCount = lastStepCount,
                locations = collectedLocations,
                totalDistance = totalDistance,
                preWalkEmotion = preEmotion,
                postWalkEmotion = postEmotion, // 기본값은 preWalkEmotion과 동일
                note = null, // 나중에 업데이트
                imageUrl = null, // 나중에 업데이트
                createdDate = DateUtils.formatToIsoDateTime(startTimeMillis)
            )
            
            // PENDING 상태로 저장
            currentSessionLocalId = walkingSessionRepository.createSessionPartial(partialSession)
            
            Timber.d("부분 세션 저장 완료: localId=$currentSessionLocalId, postEmotion=$postEmotion")
        } catch (e: Exception) {
            Timber.e(e, "부분 세션 저장 실패")
            throw e
        }
    }
}

/**
 * 산책 후 감정 업데이트 (PostWalkingEmotionScreen에서 선택 시 호출)
 * 
 * @param postWalkEmotion 선택된 산책 후 감정
 */
fun updatePostWalkEmotion(postWalkEmotion: EmotionType) {
    viewModelScope.launch {
        try {
            val localId = currentSessionLocalId
                ?: throw IllegalStateException("저장된 세션이 없습니다")
            
            walkingSessionRepository.updatePostWalkEmotion(
                localId = localId,
                postWalkEmotion = postWalkEmotion
            )
            
            // ViewModel 상태도 업데이트
            _postWalkingEmotion.value = postWalkEmotion
            
            Timber.d("산책 후 감정 업데이트 완료: localId=$localId, emotion=$postWalkEmotion")
        } catch (e: Exception) {
            Timber.e(e, "산책 후 감정 업데이트 실패")
            throw e
        }
    }
}

/**
 * 세션의 이미지와 노트 업데이트 (사진/텍스트 단계에서 호출)
 * 
 * URI를 파일로 복사하고 경로를 저장합니다.
 */
fun updateSessionImageAndNote() {
    viewModelScope.launch {
        try {
            val localId = currentSessionLocalId
                ?: throw IllegalStateException("저장된 세션이 없습니다")
            
            val imageUri = _emotionPhotoUri.value // URI 그대로 전달
            val note = _emotionText.value.ifEmpty { null }
            
            walkingSessionRepository.updateSessionImageAndNote(
                localId = localId,
                imageUri = imageUri, // URI를 전달하면 Repository에서 파일로 복사
                note = note
            )
            
            Timber.d("세션 이미지/노트 업데이트 완료: localId=$localId, imageUri=$imageUri, note=$note")
        } catch (e: Exception) {
            Timber.e(e, "세션 이미지/노트 업데이트 실패")
            throw e
        }
    }
}

/**
 * 세션을 서버와 동기화 (WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 호출)
 */
fun syncSessionToServer() {
    viewModelScope.launch {
        try {
            val localId = currentSessionLocalId
                ?: throw IllegalStateException("저장된 세션이 없습니다")
            
            walkingSessionRepository.syncSessionToServer(localId)
            
            Timber.d("서버 동기화 완료: localId=$localId")
        } catch (e: Exception) {
            Timber.e(e, "서버 동기화 실패")
            // TODO: 에러 처리 (사용자에게 알림)
            throw e
        }
    }
}

/**
 * 현재 세션의 로컬 ID 노출 (WalkingResultScreen에서 사용)
 */
val currentSessionLocalId: Long?
    get() = currentSessionLocalId

/**
 * stopWalking() 수정
 * - completeAndSaveSession() 제거
 * - saveSessionPartial() 즉시 호출
 */
fun stopWalking() {
    viewModelScope.launch {
        tracking.stopTracking()
    }
    durationJob?.cancel()
    
    // 센서 상태 업데이트
    updateSensorStatus()
    
    // 부분 세션 즉시 저장 (postWalkEmotion은 기본값으로 preWalkEmotion과 동일)
    saveSessionPartial()
    
    // UI State를 Completed로 변경 (기존 세션 데이터 사용)
    // TODO: UI State 업데이트 로직
}
```

### 4. UI 레이어

#### PostWalkingEmotionSelectScreen 수정

```kotlin
@Composable
fun PostWalkingEmotionSelectScreen(
    viewModel: WalkingViewModel,
    onNext: () -> Unit,
) {
    val selectedEmotion by viewModel.postWalkingEmotion.collectAsStateWithLifecycle()
    
    // ... 기존 UI 코드 ...
    
    Button(
        onClick = {
            if (selectedEmotion != null) {
                // 선택된 감정으로 세션 업데이트
                viewModel.updatePostWalkEmotion(selectedEmotion!!)
                onNext()
            }
        },
        enabled = selectedEmotion != null,
    ) {
        Text("다음")
    }
}
```

**참고**: 세션은 이미 `stopWalking()` 시점에 저장되었으므로, 여기서는 감정만 업데이트합니다.

#### 사진/텍스트 화면 수정

```kotlin
@Composable
fun EmotionPhotoAndTextScreen(
    viewModel: WalkingViewModel,
    onComplete: () -> Unit,
) {
    // ... 기존 UI 코드 ...
    
    Button(
        onClick = {
            viewModel.updateSessionImageAndNote()
            onComplete()
        }
    ) {
        Text("완료")
    }
}
```

#### WalkingResultScreen 수정

```kotlin
@Composable
fun WalkingResultScreen(
    viewModel: WalkingViewModel,
    onNavigateBack: () -> Unit,
) {
    val currentSessionLocalId = viewModel.currentSessionLocalId // ViewModel에 노출 필요
    
    // ... 기존 UI 코드 (지도, 통계 등) ...
    
    Button(
        onClick = {
            // "기록 완료" 버튼 클릭 시 서버 동기화 시작
            viewModel.syncSessionToServer()
            onNavigateBack()
        }
    ) {
        Text("기록 완료")
    }
}
```

## 데이터 흐름

### 시퀀스 다이어그램

```
WalkingViewModel    PostWalkingEmotionScreen    WalkingResultScreen    Repository              DAO
     |                      |                            |                      |                     |
     |-- stopWalking() ---->|                            |                      |                     |
     |                      |                            |                      |                     |
     |-- saveSessionPartial() ------------------------------------------------>|                     |
     |                      |                            |                      |-- createSessionPartial()|
     |                      |                            |                      |  (postWalkEmotion = preWalkEmotion)|
     |                      |                            |                      |-- insert() -------->|
     |                      |                            |                      |<-- localId ---------|
     |<-- localId 저장 -----|                            |                      |                     |
     |                      |                            |                      |                     |
     |                      |-- 감정 선택 --------------->|                      |                     |
     |<-- updatePostWalkEmotion()|                        |                      |                     |
     |                      |                            |                      |                     |
     |-- updatePostWalkEmotion() ---------------------------------------------->|                     |
     |                      |                            |                      |-- getSessionById() ->|
     |                      |                            |                      |<-- entity -----------|
     |                      |                            |                      |-- update() --------->|
     |                      |                            |                      |  (postWalkEmotion 업데이트)|
     |                      |                            |                      |                     |
     |                      |-- 사진/텍스트 입력 -------->|                      |                     |
     |<-- updateSessionImageAndNote()|                    |                      |                     |
     |                      |                            |                      |                     |
     |-- updateSessionImageAndNote() ------------------------------------------>|                     |
     |                      |                            |                      |-- getSessionById() ->|
     |                      |                            |                      |<-- copyImageUriToFile()|
     |                      |                            |                      |  (URI → 파일 경로)   |
     |                      |                            |                      |-- update() --------->|
     |                      |                            |                      |  (imageUrl, note 업데이트)|
     |                      |                            |                      |                     |
     |                      |                            |-- "기록 완료" 클릭 ->|                     |
     |                      |                            |                      |                     |
     |<-- syncSessionToServer()|                          |                      |                     |
     |                      |                            |                      |                     |
     |-- syncSessionToServer() ------------------------------------------------>|                     |
     |                      |                            |                      |-- getSessionById() ->|
     |                      |                            |                      |<-- entity -----------|
     |                      |                            |                      |-- updateSyncState() ->|
     |                      |                            |                      |  (SYNCING)          |
     |                      |                            |                      |-- syncToServer() --->|
     |                      |                            |                      |  (서버 API 호출)    |
     |                      |                            |                      |-- updateSyncState() ->|
     |                      |                            |                      |  (SYNCED 또는 FAILED)|
     |                      |                            |                      |                     |
```

## 주의사항

1. **에러 처리**: 세션 저장 실패 시 사용자에게 알림 필요
2. **중복 저장 방지**: PostWalkingEmotionSelectScreen에서 여러 번 호출되지 않도록 보호
3. **세션 ID 관리**: ViewModel이 재생성되면 localId를 잃을 수 있으므로, 필요시 DataStore나 다른 방법으로 저장 고려
4. **서버 동기화**: 모든 데이터가 완성된 후에만 서버 동기화 수행 (또는 각 단계마다 동기화)
5. **롤백 처리**: 중간 단계에서 실패 시 저장된 세션 삭제 여부 결정
6. **이미지 파일 관리**:
   - **모든 경우에 앱 내부 저장소에 복사**: 카메라 촬영, 갤러리 선택 모두 동일하게 처리
   - **이유**:
     - 카메라 촬영: MediaStore에 저장되어 있지만 사용자가 갤러리에서 삭제 가능
     - 갤러리 선택: 다른 앱의 파일을 참조하므로 권한 문제나 파일 삭제 가능성
     - 앱 내부 저장소에 복사하면 앱과 함께 관리되며, 삭제 시점을 제어할 수 있음
   - 파일 경로는 절대 경로로 저장 (예: `/data/data/package/files/Pictures/walking_image_1234567890.jpg`)
   - 파일 삭제 시 세션도 함께 삭제하거나, 세션 삭제 시 파일도 함께 삭제하는 정책 필요
   - 저장소 공간 관리 (오래된 파일 정리 등)
   - **저장 위치**: `getExternalFilesDir(Environment.DIRECTORY_PICTURES)` 또는 `filesDir`
     - 앱 전용 외부 저장소: 앱 삭제 시 함께 삭제됨 (권장)
     - 앱 내부 저장소: 항상 사용 가능 (fallback)

## 이미지 URL 관리 전략 (상업앱 기준)

### 문제 상황
- **로컬**: 앱 내부 저장소에 파일 경로 저장 (예: `/data/data/package/files/Pictures/walking_image_123.jpg`)
- **서버 응답**: 서버에서 제공하는 이미지 URL (예: `https://api.example.com/images/walking_123.jpg`)

### 고려사항

#### 시나리오별 분석

1. **오프라인 환경**
   - 로컬 파일 경로: ✅ 접근 가능
   - 서버 URL: ❌ 접근 불가능

2. **다중 기기 동기화**
   - 로컬 파일 경로: ❌ 다른 기기에는 파일이 없음
   - 서버 URL: ✅ 다른 기기에서도 접근 가능

3. **성능**
   - 로컬 파일: 즉시 로드 가능, 빠름
   - 서버 URL: 네트워크 필요, 느릴 수 있음 (CDN 사용 시 빠름)

4. **저장소 관리**
   - 로컬 파일: 저장소 공간 차지 (예: 5MB/이미지)
   - 서버 URL: 저장소 공간 차지 안 함

### 권장 솔루션: 변수 분리 (하이브리드 접근)

**변수를 분리하여 두 가지 모두 저장하는 것을 권장합니다.**

#### 이유
1. **오프라인 지원**: 상업앱에서 필수적인 UX
2. **다중 기기 동기화**: 서비스 확장성
3. **성능 최적화**: 로컬 파일 우선 사용으로 빠른 로딩
4. **유연성**: 상황에 맞게 선택 가능

#### 구현 방식

```kotlin
// Entity 수정
data class WalkingSessionEntity(
    // ... 기존 필드들 ...
    val localImagePath: String? = null,  // 로컬 파일 경로
    val serverImageUrl: String? = null,  // 서버 URL
    // 기존 imageUrl 필드는 deprecated 처리 또는 제거
)

// 이미지 로딩 로직 (스마트 캐싱)
fun getImageUri(): String? {
    // 1순위: 로컬 파일이 존재하면 사용 (빠름, 오프라인)
    if (localImagePath != null && File(localImagePath).exists()) {
        return localImagePath
    }
    // 2순위: 서버 URL 사용 (다중 기기 지원)
    return serverImageUrl
}
```

#### 서버 동기화 후 처리

```kotlin
suspend fun syncSessionToServer(localId: Long) {
    // ... 기존 동기화 로직 ...
    
    // 서버 응답에서 imageUrl 받음
    val serverResponse = walkRemoteDataSource.saveWalk(...)
    
    // 서버 URL 저장
    val updatedEntity = entity.copy(
        serverImageUrl = serverResponse.imageUrl  // 서버에서 받은 URL
        // localImagePath는 유지 (오프라인 지원)
    )
    
    walkingSessionDao.update(updatedEntity)
    
    // 저장소 관리 정책 (선택사항)
    // 옵션 A: 로컬 파일 유지 (오프라인 지원 강화) - 권장
    // 옵션 B: 서버 동기화 후 로컬 파일 삭제 (저장소 절약)
    // 옵션 C: 일정 기간 후 로컬 파일 삭제 (하이브리드)
}
```

#### 저장소 관리 정책 (선택)

**옵션 A: 로컬 파일 유지 (권장)**
- 장점: 오프라인 지원 강화, 빠른 로딩
- 단점: 저장소 공간 사용
- 사용 시나리오: 이미지가 중요한 기능인 경우

**옵션 B: 서버 동기화 후 로컬 파일 삭제**
- 장점: 저장소 절약
- 단점: 오프라인에서 접근 불가
- 사용 시나리오: 저장소가 제한적인 경우

**옵션 C: 일정 기간 후 로컬 파일 삭제 (하이브리드)**
- 장점: 균형잡힌 접근
- 단점: 복잡도 증가
- 사용 시나리오: 최근 세션은 빠르게, 오래된 세션은 서버에서 로드

### 마이그레이션 전략

기존 `imageUrl` 필드가 있는 경우:
1. `localImagePath`와 `serverImageUrl` 필드 추가
2. 기존 `imageUrl` 값을 `localImagePath`로 마이그레이션
3. 서버 동기화 시 `serverImageUrl` 업데이트
4. `imageUrl` 필드는 deprecated 처리 후 제거

## 구현 순서

1. ✅ Repository에 `createSessionPartial()` 메서드 추가
2. ✅ Repository에 `updatePostWalkEmotion()` 메서드 추가
3. ✅ Repository에 `updateSessionImageAndNote()` 메서드 추가
4. ✅ Repository에 `copyImageUriToFile()` 메서드 추가 (URI → 파일 경로 변환)
5. ✅ Repository에 `syncSessionToServer()` 메서드 추가 (서버 동기화)
6. ✅ DAO에 `updatePostWalkEmotion()` 메서드 추가 (선택사항, 성능 최적화용)
7. ✅ DAO에 `updateImageAndNote()` 메서드 추가 (선택사항, 성능 최적화용)
8. ✅ ViewModel에 `currentSessionLocalId` 추가
9. ✅ ViewModel에 `saveSessionPartial()` 메서드 추가 (private)
10. ✅ ViewModel에 `updatePostWalkEmotion()` 메서드 추가
11. ✅ ViewModel에 `updateSessionImageAndNote()` 메서드 추가
12. ✅ ViewModel에 `syncSessionToServer()` 메서드 추가
13. ✅ ViewModel에 `currentSessionLocalId` 노출 (WalkingResultScreen에서 사용)
14. ✅ ViewModel의 `stopWalking()`에서 `completeAndSaveSession()` 제거하고 `saveSessionPartial()` 호출
15. ✅ PostWalkingEmotionScreen에서 `updatePostWalkEmotion()` 호출
16. ✅ 사진/텍스트 화면에서 `updateSessionImageAndNote()` 호출
17. ✅ WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 `syncSessionToServer()` 호출
18. ✅ 테스트 및 검증

## 주요 변경사항 요약

1. **stopWalking() 시점**: 즉시 `saveSessionPartial()` 호출하여 세션 저장 (PENDING 상태)
2. **postWalkEmotion 기본값**: 선택되지 않았으면 `preWalkEmotion`과 동일하게 설정
3. **PostWalkingEmotionScreen**: 감정 선택 시 `updatePostWalkEmotion()` 호출하여 기존 세션 업데이트
4. **사진/텍스트 단계**: `updateSessionImageAndNote()` 호출하여 imageUrl과 note 업데이트
5. **WalkingResultScreen "기록 완료"**: `syncSessionToServer()` 호출하여 서버 동기화 시작
   - SyncState: PENDING → SYNCING → SYNCED (성공 시)
   - SyncState: PENDING → SYNCING → FAILED (실패 시, 나중에 재시도 가능)

