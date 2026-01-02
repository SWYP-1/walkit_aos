# 로띠 캐릭터 이미지 교체 작업 가이드 (Android → iOS)

## 📋 개요

WalkIt 앱에서 사용자가 캐릭터를 커스터마이징하고 실시간으로 변경사항을 확인할 수 있는 로띠(Lottie) 애니메이션 기반 캐릭터 시스템을 구현했습니다.

이 문서는 Android에서 구현된 사용자 경험 시나리오를 중심으로 iOS 포팅 가이드를 제공합니다.

## 🔄 시나리오별 데이터 흐름 및 구현 플로우

### 시나리오 1: 앱 실행 시 캐릭터 표시

**데이터 흐름**:
```
사용자 로그인 확인 → 캐릭터 정보 API 호출 → 캐릭터 데이터 수신
      ↓
캐릭터 등급 추출 → Base Lottie JSON 로드 → 기본 이미지들 적용
      ↓
Lottie JSON 수정 → 캐릭터 애니메이션 표시
```

**구체적인 구현 플로우**:

1. **앱 초기화 단계**
   ```kotlin
   // ViewModel.init() 또는 앱 시작 시점
   fun initializeCharacter() {
       // 1. 현재 로그인된 사용자 확인
       val currentUser = userRepository.getCurrentUser()
       if (currentUser == null) return // 로그인 안됨

       // 2. 캐릭터 정보 조회 API 호출
       viewModelScope.launch {
           _characterState.value = LottieCharacterState(isLoading = true)
           val character = characterRepository.getCharacter(currentUser.id)
       }
   }
   ```

2. **캐릭터 데이터 수신 후 처리**
   ```kotlin
   private fun processCharacterData(character: Character) {
       // 3. 캐릭터 등급에 따른 Base Lottie JSON 로드
       val baseJson = loadBaseLottieJson(character.grade)

       // 4. 캐릭터 파트별 기본 이미지 적용
       val characterJson = updateCharacterPartsInLottie(baseJson, character)

       // 5. UI 상태 업데이트
       _characterState.value = LottieCharacterState(
           baseJson = baseJson.toString(),
           modifiedJson = characterJson.toString(),
           isLoading = false
       )
   }
   ```

### 시나리오 2: 드레싱룸 아이템 미리보기

**데이터 흐름**:
```
아이템 선택 → 아이템 이미지 URL 추출 → 이미지 다운로드
      ↓
현재 캐릭터 JSON 복사 → 선택된 아이템만 적용 → 미리보기 표시
      ↓
선택 취소 시 → 원래 캐릭터 JSON 복원
```

**구체적인 구현 플로우**:

1. **아이템 선택 시 미리보기 생성**
   ```kotlin
   fun previewCosmeticItem(item: CosmeticItem) {
       viewModelScope.launch {
           try {
               // 1. 현재 캐릭터 상태 가져오기
               val currentState = _characterState.value
               val baseJson = JSONObject(currentState?.modifiedJson ?: return)

               // 2. 선택된 아이템 이미지 다운로드
               val imageBytes = imageDownloader.downloadPngImage(item.imageUrl)
               val base64Image = imageBytes.toBase64DataUrl()

               // 3. Lottie JSON에서 해당 파트 asset 교체
               val assetId = getAssetIdForItem(item)
               val previewJson = replaceAssetInLottie(baseJson, assetId, base64Image)

               // 4. 미리보기 상태로 UI 업데이트 (저장하지 않음)
               _previewState.value = LottieCharacterState(
                   modifiedJson = previewJson.toString(),
                   isPreview = true
               )
           } catch (e: Exception) {
               // 에러 처리
           }
       }
   }
   ```

2. **미리보기 취소**
   ```kotlin
   fun cancelPreview() {
       // 미리보기 상태 초기화
       _previewState.value = null
       // UI는 자동으로 원래 캐릭터로 돌아감
   }
   ```

### 시나리오 3: 아이템 구매 후 착용 적용

**데이터 흐름**:
```
아이템 구매 완료 → 서버 착용 상태 동기화 → 모든 착용 아이템 조회
      ↓
착용 아이템별 이미지 다운로드 → 캐릭터 전체 재구성 → 로컬 저장
      ↓
UI 업데이트 → 모든 화면에서 변경된 캐릭터 표시
```

**구체적인 구현 플로우**:

1. **구매 완료 후 캐릭터 업데이트**
   ```kotlin
   fun applyPurchasedItem(itemId: String) {
       viewModelScope.launch {
           try {
               // 1. 서버에서 새로운 착용 상태 동기화
               val wornItems = characterRepository.getWornItems()

               // 2. 현재 캐릭터 상태 가져오기
               val currentState = _characterState.value
               val baseJson = JSONObject(currentState?.baseJson ?: return)

               // 3. 착용된 모든 아이템 적용하여 캐릭터 재구성
               val updatedJson = applyAllWornItems(baseJson, wornItems)

               // 4. 로컬에 변경사항 저장
               characterRepository.saveCharacterState(updatedJson.toString())

               // 5. UI 상태 업데이트
               _characterState.value = currentState.copy(
                   modifiedJson = updatedJson.toString()
               )
           } catch (e: Exception) {
               // 에러 처리 및 롤백
           }
       }
   }
   ```

### 시나리오 4: 캐릭터 등급 변경 시 업데이트

**데이터 흐름**:
```
등급 변경 감지 → 새로운 Base Lottie JSON 로드 → 모든 착용 아이템 재적용
      ↓
캐릭터 완전 재구성 → 변경사항 저장 → UI 업데이트
```

**구체적인 구현 플로우**:

1. **등급 변경 처리**
   ```kotlin
   fun handleGradeUpgrade(newGrade: Int) {
       viewModelScope.launch {
           try {
               // 1. 새로운 등급의 Base Lottie JSON 로드
               val newBaseJson = loadBaseLottieJson(newGrade)

               // 2. 현재 착용된 모든 아이템 조회
               val wornItems = characterRepository.getWornItems()

               // 3. 새로운 Base JSON에 모든 아이템 재적용
               val updatedJson = applyAllWornItems(newBaseJson, wornItems)

               // 4. 캐릭터 데이터 업데이트
               val updatedCharacter = characterRepository.updateCharacterGrade(newGrade)

               // 5. UI 상태 완전 업데이트
               _characterState.value = LottieCharacterState(
                   baseJson = newBaseJson.toString(),
                   modifiedJson = updatedJson.toString(),
                   character = updatedCharacter
               )
           } catch (e: Exception) {
               // 에러 처리
           }
       }
   }
   ```

## 📊 핵심 데이터 구조

### 캐릭터 정보 모델
```kotlin
// 서버에서 내려주는 캐릭터 기본 정보
data class Character(
    val id: String,
    val grade: Int,              // 1-4 등급 (등급별 다른 Lottie 파일 사용)
    val headImageUrl: String?,   // 머리 이미지 URL
    val bodyImageUrl: String?,   // 몸통 이미지 URL
    val feetImageUrl: String?    // 발 이미지 URL
)
```

### 착용 아이템 정보
```kotlin
// 사용자가 구매한 코스메틱 아이템들
data class CosmeticItem(
    val id: String,
    val imageUrl: String,        // 아이템 이미지 URL
    val tags: String?,          // "TOP", "DECOR" 등의 태그 (파트별 세부 구분)
    val slot: EquipSlot         // HEAD, BODY, FEET 중 하나
)
```

### 캐릭터 파트 구분
```kotlin
enum class CharacterPart {
    HEAD,   // 머리 (모자, 헤어 등)
    BODY,   // 몸통 (상의, 드레스 등)
    FEET    // 발 (신발, 양말 등)
}
```

## 🔄 기술 구현 세부사항

### Lottie JSON 구조 및 Asset 교체 메커니즘

**Base Lottie JSON 구조**:
```json
{
  "v": "5.7.4",
  "fr": 30,
  "ip": 0,
  "op": 180,
  "w": 512,
  "h": 512,
  "assets": [
    {
      "id": "head_asset",
      "w": 512,
      "h": 512,
      "p": ["data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."],
      "u": "images/"
    },
    {
      "id": "body_asset",
      "w": 512,
      "h": 512,
      "p": ["data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."],
      "u": "images/"
    }
  ],
  "layers": [...]
}
```

**Asset 교체 알고리즘**:
```kotlin
fun replaceLottieAsset(json: JSONObject, assetId: String, base64Image: String): JSONObject {
    val assets = json.getJSONArray("assets")

    // 1. assets 배열에서 해당 assetId 찾기
    for (i in 0 until assets.length()) {
        val asset = assets.getJSONObject(i)
        if (asset.getString("id") == assetId) {
            // 2. p 배열의 첫 번째 요소 교체
            val imagePaths = asset.getJSONArray("p")
            imagePaths.put(0, base64Image) // Base64 데이터 삽입
            break
        }
    }

    return json
}
```

### 데이터 변환 파이프라인

**이미지 URL → Lottie Asset 변환 플로우**:

```
이미지 URL (String)
    ↓ HTTP 요청 (ImageDownloader)
이미지 바이너리 (ByteArray)
    ↓ PNG 검증 (isValidPng)
유효한 PNG 데이터
    ↓ Base64 인코딩 (Base64.NO_WRAP)
"data:image/png;base64,..." (String)
    ↓ Lottie JSON assets.p[0] 삽입
수정된 Lottie JSON (JSONObject)
    ↓ JSON.stringify()
최종 Lottie JSON 문자열
    ↓ LottieAnimation composable
업데이트된 캐릭터 애니메이션 표시
```

**구현 코드**:
```kotlin
suspend fun processImageForLottie(imageUrl: String): String {
    return withContext(Dispatchers.IO) {
        try {
            // 1. 이미지 다운로드
            val imageData = imageDownloader.downloadPngImage(imageUrl)

            // 2. PNG 포맷 검증
            require(imageData.isValidPngFormat()) { "Invalid PNG format" }

            // 3. Base64 변환 (NO_WRAP으로 한 줄로 만들기)
            val base64 = Base64.encodeToString(imageData, Base64.NO_WRAP)

            // 4. Data URL 포맷으로 반환
            "data:image/png;base64,$base64"
        } catch (e: Exception) {
            Timber.e(e, "이미지 처리 실패: $imageUrl")
            throw e
        }
    }
}
```

### 캐릭터 파트 매핑 시스템

**Asset ID 결정 로직**:
```kotlin
enum class CharacterPart(val assetId: String, vararg val lottieAssetIds: String) {
    HEAD("head", "headtop", "headdocor"),
    BODY("body", "body"),
    FEET("feet", "foot");

    fun getLottieAssetId(tags: String? = null): String {
        if (tags.isNullOrBlank()) return lottieAssetIds.first()

        return when (this) {
            HEAD -> when {
                tags.contains("TOP", ignoreCase = true) -> "headtop"
                tags.contains("DECOR", ignoreCase = true) -> "headdocor"
                else -> lottieAssetIds.first()
            }
            BODY -> lottieAssetIds.first()
            FEET -> lottieAssetIds.first()
        }
    }
}
```

### ViewModel 상태 관리 아키텍처

**DressingRoomState 구조**:
```kotlin
data class DressingRoomState(
    // 캐릭터 기본 정보
    val character: Character? = null,

    // Lottie JSON 상태들
    val baseLottieJson: String? = null,          // 원본 Lottie JSON
    val currentLottieJson: String? = null,       // 현재 적용된 Lottie JSON
    val previewLottieJson: String? = null,       // 미리보기용 Lottie JSON

    // 아이템 상태
    val wornItems: List<CosmeticItem> = emptyList(),     // 착용된 아이템들
    val selectedItems: List<CosmeticItem> = emptyList(), // 선택된 아이템들

    // UI 상태
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**상태 전이 플로우**:
```
초기 상태 (Loading)
    ↓ 캐릭터 데이터 로드 성공
성공 상태 (baseJson, currentJson 설정)
    ↓ 아이템 선택
미리보기 상태 (previewJson 설정)
    ↓ 선택 취소
원래 상태로 복귀
    ↓ 구매 완료
업데이트 상태 (모든 아이템 재적용)
    ↓ 저장 완료
최종 성공 상태
```

### 에러 처리 및 복원 전략

**네트워크 타임아웃 처리**:
```kotlin
suspend fun safeImageDownload(url: String, timeoutMs: Long = 10000): Result<String> {
    return withTimeoutOrNull(timeoutMs) {
        try {
            val base64Image = processImageForLottie(url)
            Result.Success(base64Image)
        } catch (e: IOException) {
            Result.Error(e, "네트워크 오류: ${e.message}")
        } catch (e: TimeoutCancellationException) {
            Result.Error(e, "시간 초과")
        }
    } ?: Result.Error(Exception("Timeout"), "요청 시간이 초과되었습니다")
}
```

**Lottie JSON 파싱 안전장치**:
```kotlin
fun safeJsonModification(originalJson: JSONObject, assetUpdates: Map<String, String>): JSONObject {
    return try {
        val resultJson = JSONObject(originalJson.toString()) // Deep copy

        assetUpdates.forEach { (assetId, base64Image) ->
            try {
                replaceLottieAsset(resultJson, assetId, base64Image)
            } catch (e: Exception) {
                Timber.w(e, "Asset 교체 실패: $assetId, 원본 유지")
                // 개별 asset 실패해도 전체 작업 계속
            }
        }

        resultJson
    } catch (e: JSONException) {
        Timber.e(e, "JSON 수정 실패, 원본 반환")
        originalJson // 실패 시 원본 반환
    }
}
```

### 메모리 및 성능 최적화

**이미지 캐싱 전략**:
```kotlin
class LottieImageCache {
    private val cache = LruCache<String, String>(50) // URL -> Base64 매핑

    suspend fun getOrLoadImage(url: String): String {
        return cache.get(url) ?: run {
            val base64Image = processImageForLottie(url)
            cache.put(url, base64Image)
            base64Image
        }
    }
}
```

**비동기 처리 패턴**:
```kotlin
fun updateCharacterPreview(item: CosmeticItem) {
    viewModelScope.launch {
        // 1. 현재 상태 스냅샷
        val currentJson = _state.value.currentLottieJson ?: return

        // 2. 백그라운드에서 이미지 처리
        val base64Image = async(Dispatchers.IO) {
            lottieImageProcessor.processImageForLottie(item.imageUrl)
        }

        try {
            // 3. 메인 스레드에서 UI 업데이트
            val newJson = withContext(Dispatchers.Main) {
                lottieImageProcessor.replaceAssetInLottie(
                    JSONObject(currentJson),
                    getAssetIdForItem(item),
                    base64Image.await()
                ).toString()
            }

            // 4. 상태 업데이트
            _state.update { it.copy(previewLottieJson = newJson) }
        } catch (e: Exception) {
            // 에러 처리
        }
    }
}
```

## 🎭 캐릭터 표시 로직

### 캐릭터 파트별 Asset ID 매핑

```kotlin
private fun createCharacterAssetMap(character: Character): Map<String, LottieAsset> {
    val assetMap = mutableMapOf<String, LottieAsset>()

    // HEAD 파트
    character.headImageUrl?.let { url ->
        val assetId = CharacterPart.HEAD.getLottieAssetId(/* tags */)
        assetMap[assetId] = LottieAsset(
            id = assetId,
            imageData = downloadAndConvertToBase64(url),
            width = 512,  // Lottie asset 크기
            height = 512
        )
    }

    // BODY 파트
    character.bodyImageUrl?.let { url ->
        val assetId = CharacterPart.BODY.getLottieAssetId()
        assetMap[assetId] = LottieAsset(...)
    }

    // FEET 파트
    character.feetImageUrl?.let { url ->
        val assetId = CharacterPart.FEET.getLottieAssetId()
        assetMap[assetId] = LottieAsset(...)
    }

    return assetMap
}
```

### 코스메틱 아이템 적용

```kotlin
suspend fun updateAssetsForWornItems(
    baseLottieJson: JSONObject,
    wornItems: List<CosmeticItem>
): JSONObject {
    var resultJson = baseLottieJson

    for (item in wornItems) {
        val assetId = getAssetIdForCosmeticItem(item)
        resultJson = replaceAssetWithImageUrl(
            lottieJson = resultJson,
            assetId = assetId,
            imageUrl = item.imageUrl
        )
    }

    return resultJson
}
```

## 📱 iOS 구현 가이드 - 구현 플로우 중심

### 필수 인프라 구축

#### 1. 의존성 및 파일 구조
```ruby
# Podfile
pod 'lottie-ios'
pod 'Alamofire'  # HTTP 클라이언트
```

```
📁 WalkIt/
├── Models/
│   ├── Character.swift
│   ├── CosmeticItem.swift
│   └── LottieAsset.swift
├── Services/
│   ├── CharacterService.swift
│   ├── LottieImageProcessor.swift
│   └── ImageDownloader.swift
├── ViewModels/
│   ├── CharacterViewModel.swift
│   └── DressingRoomViewModel.swift
└── Views/
    ├── CharacterView.swift
    └── DressingRoomView.swift
```

#### 2. Base Lottie 파일 관리
```
📁 Assets/Lottie/
├── seed.json  // 1등급 캐릭터
├── sprout.json  // 2등급 캐릭터
├── tree.json  // 3등급 캐릭터
```

### 데이터 흐름별 구현 패턴

#### Pattern 1: 캐릭터 초기 로드 플로우
```
ViewModel.init() → API 캐릭터 조회 → Base JSON 로드
      ↓
캐릭터 파트별 이미지 다운로드 → Lottie JSON 수정
      ↓
UI 상태 업데이트 → 캐릭터 애니메이션 표시
```

**구현 코드**:
```swift
class CharacterViewModel: ObservableObject {
    @Published var characterState: CharacterState = .loading

    func loadCharacter() async {
        do {
            // 1. 캐릭터 정보 조회
            let character = try await characterService.getCharacter()

            // 2. Base Lottie JSON 로드
            let baseJsonData = try loadLottieJson(for: character.grade)
            var baseJson = try JSONSerialization.jsonObject(with: baseJsonData) as! [String: Any]

            // 3. 캐릭터 기본 이미지들 적용
            let defaultImages = try await downloadDefaultImages(for: character)
            baseJson = try await lottieProcessor.applyImages(defaultImages, to: baseJson)

            // 4. UI 상태 업데이트
            await MainActor.run {
                self.characterState = .loaded(
                    character: character,
                    lottieJson: baseJson
                )
            }
        } catch {
            await MainActor.run {
                self.characterState = .error(error.localizedDescription)
            }
        }
    }
}
```

#### Pattern 2: 실시간 미리보기 플로우
```
아이템 선택 → 이미지 다운로드 → 현재 JSON 복사
      ↓
선택 아이템만 적용 → 미리보기 JSON 생성
      ↓
UI 즉시 업데이트 (저장하지 않음)
```

**구현 코드**:
```swift
class DressingRoomViewModel: ObservableObject {
    @Published var previewState: PreviewState?

    func previewItem(_ item: CosmeticItem) async {
        do {
            // 1. 현재 캐릭터 JSON 가져오기
            guard let currentJson = characterState?.lottieJson else { return }

            // 2. 선택된 아이템 이미지 다운로드
            let base64Image = try await imageDownloader.downloadAsBase64(from: item.imageUrl)

            // 3. 현재 JSON을 복사해서 미리보기용으로 수정
            var previewJson = currentJson
            previewJson = try lottieProcessor.replaceAsset(
                in: previewJson,
                assetId: item.getAssetId(),
                with: base64Image
            )

            // 4. 미리보기 상태 업데이트 (저장하지 않음)
            await MainActor.run {
                self.previewState = PreviewState(
                    item: item,
                    modifiedJson: previewJson,
                    isPreview: true
                )
            }
        } catch {
            // 에러 처리
        }
    }
}
```

#### Pattern 3: 아이템 구매 적용 플로우
```
구매 완료 → 서버 착용 상태 동기화 → 모든 착용 아이템 다운로드
      ↓
캐릭터 전체 재구성 → 로컬 저장 → 모든 UI 업데이트
```

**구현 코드**:
```swift
func applyPurchasedItem(_ itemId: String) async {
    do {
        // 1. 서버에서 최신 착용 상태 조회
        let wornItems = try await characterService.getWornItems()

        // 2. Base Lottie JSON 로드
        let baseJson = try await loadBaseLottieJson()

        // 3. 착용된 모든 아이템 적용
        var characterJson = baseJson
        for item in wornItems {
            let base64Image = try await imageDownloader.downloadAsBase64(from: item.imageUrl)
            characterJson = try lottieProcessor.replaceAsset(
                in: characterJson,
                assetId: item.getAssetId(),
                with: base64Image
            )
        }

        // 4. 로컬 저장 및 UI 업데이트
        try await characterService.saveCharacterState(characterJson)

        await MainActor.run {
            self.characterState = .updated(json: characterJson)
            // 다른 화면들도 업데이트하도록 NotificationCenter 등으로 알림
            NotificationCenter.default.post(name: .characterUpdated, object: nil)
        }
    } catch {
        // 에러 처리 및 롤백
    }
}
```

### 핵심 서비스 클래스 구현

#### LottieImageProcessor (이미지 처리 코어)
```swift
class LottieImageProcessor {
    func downloadAndConvertImage(url: String) async throws -> String {
        // 1. URL에서 이미지 다운로드
        let imageData = try await downloadImage(from: url)

        // 2. PNG 검증
        guard isValidPNG(imageData) else {
            throw LottieError.invalidImageFormat
        }

        // 3. Base64 변환
        return "data:image/png;base64," + imageData.base64EncodedString()
    }

    func replaceAsset(in json: [String: Any], assetId: String, with base64Image: String) throws -> [String: Any] {
        var modifiedJson = json

        // assets 배열에서 해당 assetId 찾기
        if var assets = modifiedJson["assets"] as? [[String: Any]] {
            for (index, var asset) in assets.enumerated() {
                if asset["id"] as? String == assetId {
                    // p 배열의 첫 번째 요소 교체
                    if var imagePaths = asset["p"] as? [Any], !imagePaths.isEmpty {
                        imagePaths[0] = base64Image
                        assets[index]["p"] = imagePaths
                    }
                    break
                }
            }
            modifiedJson["assets"] = assets
        }

        return modifiedJson
    }
}
```

#### CharacterService (데이터 관리)
```swift
class CharacterService {
    private let apiClient: APIClient
    private let storage: CharacterStorage

    func getCharacter() async throws -> Character {
        // API 호출 또는 캐시에서 가져오기
        if let cached = storage.getCachedCharacter() {
            return cached
        }
        return try await apiClient.fetchCharacter()
    }

    func getWornItems() async throws -> [CosmeticItem] {
        // 착용된 아이템 목록 조회
        try await apiClient.fetchWornItems()
    }

    func saveCharacterState(_ json: [String: Any]) async throws {
        // 캐릭터 상태 영구 저장
        try await storage.saveCharacterJson(json)
    }
}
```

### UI 컴포넌트 구현

#### CharacterView (캐릭터 표시)
```swift
struct CharacterView: View {
    let characterState: CharacterState

    var body: some View {
        switch characterState {
        case .loading:
            ProgressView()
        case .loaded(let character, let lottieJson):
            LottieCharacterView(json: lottieJson)
        case .error(let message):
            ErrorView(message: message)
        case .preview(let previewState):
            LottieCharacterView(json: previewState.modifiedJson)
        }
    }
}

struct LottieCharacterView: View {
    let json: [String: Any]

    var body: some View {
        if let data = try? JSONSerialization.data(withJSONObject: json),
           let animation = try? AnimationView(json: data) {
            LottieView(animation: animation)
                .loopMode(.loop)
        } else {
            // 폴백
            LottieView(name: "default_character")
        }
    }
}
```

## ⚠️ 시나리오별 주의사항

### 앱 실행 시 캐릭터 로드
- **네트워크 타임아웃**: 캐릭터 로드 실패 시 기본 캐릭터 표시
- **캐릭터 등급 확인**: 잘못된 등급의 Lottie 파일 로드하지 않도록 검증
- **초기 로딩 UX**: 로딩 중일 때 placeholder 캐릭터 표시

### 드레싱룸 실시간 미리보기
- **빠른 응답성**: 아이템 선택 후 0.5초 이내에 미리보기 표시
- **메모리 관리**: 미리보기 JSON은 캐시하지 말고 즉시 폐기
- **선택 취소**: 미리보기 상태에서 원래 상태로 빠르게 복원

### 아이템 구매 후 적용
- **트랜잭션 안정성**: 구매 완료 전까지 기존 캐릭터 유지
- **오프라인 처리**: 네트워크 실패 시 로컬에 구매 상태 저장
- **UI 일관성**: 구매 완료 즉시 모든 화면에서 캐릭터 업데이트

### 캐릭터 변경사항 동기화
- **실시간 반영**: 서버 변경사항을 푸시로 수신
- **충돌 처리**: 로컬 변경사항과 서버 상태 충돌 시 사용자 선택
- **버전 관리**: 캐릭터 상태 변경 히스토리 유지

## 🔧 문제 해결 가이드

### 캐릭터가 표시되지 않는 경우
1. Lottie JSON 파일이 올바르게 로드되었는지 확인
2. Base64 이미지 데이터가 유효한지 검증
3. assets 배열의 id와 p 구조가 맞는지 확인

### 이미지가 깨져서 표시되는 경우
1. PNG 포맷 검증 (헤더: 89 50 4E 47)
2. Base64 인코딩이 올바른지 확인
3. 이미지 크기가 Lottie asset 크기와 맞는지 확인

### 성능 이슈 발생 시
1. 이미지 다운로드 캐싱 구현
2. Base64 변환을 백그라운드에서 처리
3. Lottie 애니메이션 메모리 관리 최적화

## 📱 테스트 시나리오

### 기본 기능 테스트
- [ ] 앱 실행 시 캐릭터 정상 표시
- [ ] 드레싱룸에서 아이템 선택 시 미리보기 작동
- [ ] 아이템 구매 후 캐릭터 즉시 변경
- [ ] 오프라인 상태에서 캐릭터 유지

### 에러 상황 테스트
- [ ] 네트워크 실패 시 기본 캐릭터 표시
- [ ] 잘못된 이미지 URL 처리
- [ ] 캐릭터 등급 변경 시 Lottie 파일 교체
- [ ] 메모리 부족 상황에서의 안정성

## 📋 구현 플로우별 체크리스트

### Phase 1: 인프라 구축
- [ ] Lottie-ios 라이브러리 설치 및 프로젝트 설정
- [ ] 캐릭터 등급별 Base Lottie JSON 파일 준비
- [ ] Character, CosmeticItem, LottieAsset 모델 클래스 구현
- [ ] ImageDownloader, LottieImageProcessor 서비스 클래스 구현
- [ ] CharacterService 및 API 연동 로직 구현

### Phase 2: 기본 캐릭터 표시 플로우
- [ ] 앱 실행 시 캐릭터 정보 조회 API 호출
- [ ] 캐릭터 등급에 따른 Base Lottie JSON 로드
- [ ] 캐릭터 파트별 기본 이미지 다운로드 및 변환
- [ ] Lottie JSON asset 교체 및 캐릭터 표시
- [ ] 로딩 및 에러 상태 UI 처리

### Phase 3: 실시간 미리보기 플로우
- [ ] 드레싱룸 화면 및 아이템 선택 UI 구현
- [ ] 아이템 선택 시 이미지 다운로드 (비동기)
- [ ] 현재 캐릭터 JSON 복사 및 수정
- [ ] 미리보기 상태로 UI 즉시 업데이트
- [ ] 선택 취소 시 원래 상태 복원 로직

### Phase 4: 아이템 구매 적용 플로우
- [ ] 아이템 구매 완료 이벤트 처리
- [ ] 서버에서 착용 상태 재동기화
- [ ] 모든 착용 아이템 이미지 일괄 다운로드
- [ ] 캐릭터 전체 재구성 및 로컬 저장
- [ ] 모든 화면에서의 캐릭터 업데이트

### Phase 5: 에러 처리 및 최적화
- [ ] 네트워크 타임아웃 및 실패 처리
- [ ] 이미지 포맷 검증 및 폴백 처리
- [ ] Lottie JSON 파싱 에러 처리
- [ ] 이미지 캐싱 구현으로 성능 최적화
- [ ] 메모리 관리 및 리소스 정리

### Phase 6: 고급 기능 (선택)
- [ ] 캐릭터 등급 변경 시 Lottie 파일 동적 교체
- [ ] 다중 아이템 동시 미리보기
- [ ] 캐릭터 상태 변경 히스토리 관리
- [ ] 오프라인 모드 지원
- [ ] 캐릭터 애니메이션 커스터마이징

---

이 가이드를 참고하여 iOS에서 동일한 로띠 캐릭터 이미지 교체 기능을 구현하시면 됩니다. 추가 질문이 있으시면 언제든지 문의해주세요! 🚀
