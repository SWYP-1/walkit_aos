# CharacterView 리소스 관리 방식

## 문제 상황

사용자가 "3번 장갑", "5번 신발"을 구매했다고 가정:
- 구매한 아이템의 **이름**은 알고 있음 (productId: "gloves_03", "shoes_05")
- 하지만 **이미지/리소스 정보**는 어떻게 가져오는가?
- Firebase Storage에 PNG 파일이 저장되어 있다면 어떻게 접근하는가?

## 해결 방안

### 방안 1: Firebase Storage 다운로드 URL 사용 (권장)

#### 구조
```
Firebase Storage 구조:
cosmetic-items/
├── shoes/
│   ├── shoes_01.png
│   ├── shoes_02.png
│   └── shoes_05.png
├── gloves/
│   ├── gloves_01.png
│   ├── gloves_02.png
│   └── gloves_03.png
├── hats/
└── necklaces/
```

#### 데이터 모델 수정
```kotlin
data class CosmeticItem(
    val productId: String,           // "shoes_05", "gloves_03"
    val name: String,                 // "신발 5번", "장갑 3번"
    val description: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val price: String,
    
    // Firebase Storage 경로 또는 다운로드 URL
    val resourcePath: String,        // "cosmetic-items/shoes/shoes_05.png"
    val resourceUrl: String? = null, // Firebase Storage 다운로드 URL (캐싱용)
    
    val thumbnailPath: String,       // "cosmetic-items/shoes/thumbnails/shoes_05.png"
    val thumbnailUrl: String? = null,
    
    val isPurchased: Boolean = false,
    val isApplied: Boolean = false
)
```

#### Firebase Storage에서 URL 가져오기
```kotlin
// data/remote/storage/CosmeticItemStorageManager.kt
@Singleton
class CosmeticItemStorageManager @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Firebase Storage 경로에서 다운로드 URL 가져오기
     * URL은 캐싱되어 재사용 가능
     */
    suspend fun getDownloadUrl(storagePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = storage.reference.child(storagePath)
            val url = storageRef.downloadUrl.await()
            Result.success(url.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 여러 아이템의 URL을 한 번에 가져오기 (배치 처리)
     */
    suspend fun getDownloadUrls(paths: List<String>): Map<String, String> {
        return paths.associateWith { path ->
            getDownloadUrl(path).getOrNull() ?: ""
        }
    }
}
```

#### Repository에서 URL 포함하여 반환
```kotlin
// data/repository/CosmeticItemRepositoryImpl.kt
class CosmeticItemRepositoryImpl @Inject constructor(
    private val purchasedItemDao: PurchasedItemDao,
    private val appliedItemDao: AppliedItemDao,
    private val storageManager: CosmeticItemStorageManager,
    private val billingManager: BillingManager
) : CosmeticItemRepository {
    
    override fun getPurchasedItems(): Flow<List<CosmeticItem>> = flow {
        // 1. 로컬 DB에서 구매한 productId 목록 가져오기
        val purchasedProductIds = purchasedItemDao.getAllPurchasedProductIds()
        
        // 2. 각 productId에 대한 아이템 정보 가져오기
        // (이 정보는 Firebase Firestore나 로컬 JSON에서 가져올 수 있음)
        val items = purchasedProductIds.map { productId ->
            getItemInfo(productId) // 기본 정보 (이름, 카테고리 등)
        }
        
        // 3. Firebase Storage에서 다운로드 URL 가져오기
        val itemsWithUrls = items.map { item ->
            val resourceUrl = storageManager.getDownloadUrl(item.resourcePath).getOrNull()
            val thumbnailUrl = storageManager.getDownloadUrl(item.thumbnailPath).getOrNull()
            
            item.copy(
                resourceUrl = resourceUrl,
                thumbnailUrl = thumbnailUrl,
                isPurchased = true
            )
        }
        
        emit(itemsWithUrls)
    }
}
```

### 방안 2: 메타데이터와 리소스 URL 완전 분리 (권장)

#### ⚠️ 중요: 메타데이터에는 URL을 포함하지 않음

#### 서버 API 구조
```kotlin
// 1. 메타데이터 조회 API (공개, URL 없음)
GET /api/cosmetic-items
Response: {
  "items": [
    {
      "productId": "shoes_05",
      "name": "신발 5번",
      "category": "SHOES",
      "rarity": "RARE",
      "price": "₩1,000",
      "thumbnailUrl": "https://cdn.example.com/thumbnails/shoes_05.png",  // 썸네일만 (CDN)
      // ⚠️ resourceUrl, resourcePath 없음!
    }
  ]
}

// 2. 구매한 아이템 리소스 URL 조회 API (인증 필요)
GET /api/cosmetic-items/purchased/resources
Headers: Authorization: Bearer {accessToken}
Response: {
  "items": [
    {
      "productId": "shoes_05",
      "resourceUrl": "https://firebasestorage.../shoes_05.png?token=...",  // Signed URL
      "expiresAt": 1234567890
    }
  ]
}
```

#### 데이터 흐름
```
1. 상점 화면: 메타데이터만 조회 (URL 없음)
   GET /api/cosmetic-items
   → 썸네일만 표시 (CDN 사용)
   
2. 구매 완료 후: 리소스 URL 조회 (서버 검증)
   GET /api/cosmetic-items/purchased/resources
   → 구매한 아이템에 대해서만 Signed URL 제공
   
3. 캐릭터 표시: 구매한 아이템의 Signed URL 사용
   → CharacterView에서 resourceUrl로 이미지 로드
```

#### Repository 구현
```kotlin
// 메타데이터와 리소스 URL을 별도로 관리
class CosmeticItemRepositoryImpl {
    
    // 구매 가능한 아이템 메타데이터 (URL 없음)
    fun getAvailableItems(): Flow<List<CosmeticItem>> {
        // GET /api/cosmetic-items
        // resourceUrl = null
    }
    
    // 구매한 아이템 리소스 URL (서버 검증 후)
    fun getPurchasedItems(): Flow<List<CosmeticItem>> {
        // 1. 메타데이터 조회 (공개 API)
        // 2. 리소스 URL 조회 (인증 필요, 구매 검증)
        // 3. 결합하여 반환
    }
}
```

## CharacterView 구현

### 레이어별 이미지 합성
```kotlin
@Composable
fun CharacterView(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 1. 기본 캐릭터 (항상 표시)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("cosmetic-items/base/character_base.png")
                .build(),
            contentDescription = "기본 캐릭터",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // 2. 신발 레이어 (가장 아래)
        customization.shoes?.resourceUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .build(),
                contentDescription = "신발",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        // 3. 장갑 레이어
        customization.gloves?.resourceUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .build(),
                contentDescription = "장갑",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        // 4. 목걸이 레이어
        customization.necklace?.resourceUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .build(),
                contentDescription = "목걸이",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        
        // 5. 모자 레이어 (가장 위)
        customization.hat?.resourceUrl?.let { url ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .build(),
                contentDescription = "모자",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
```

### Lottie 애니메이션 지원
```kotlin
@Composable
fun CharacterView(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 기본 캐릭터
        AsyncImage(...)
        
        // 신발 (PNG 또는 Lottie)
        customization.shoes?.let { item ->
            when {
                item.resourceUrl.endsWith(".json") -> {
                    // Lottie 애니메이션
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.Url(item.resourceUrl)
                    )
                    LottieAnimation(
                        composition = composition,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    // PNG 이미지
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.resourceUrl)
                            .build(),
                        ...
                    )
                }
            }
        }
        
        // 나머지 레이어들...
    }
}
```

## 최종 데이터 흐름 (보안 강화)

```
1. 상점 화면: 메타데이터만 조회 (URL 없음)
   GET /api/cosmetic-items
   → 썸네일만 표시 (CDN, 공개 가능)
   
2. 사용자가 "shoes_05" 구매
   ↓
3. Google Play 구매 완료
   ↓
4. 서버에 구매 검증 요청
   POST /api/purchases/verify
   (productId + purchaseToken)
   ↓
5. 서버에서 Google Play API로 구매 검증
   ↓
6. 검증 성공 시 로컬 DB에 productId 저장
   ↓
7. 구매한 아이템 리소스 URL 조회
   GET /api/cosmetic-items/purchased/resources
   (서버에서 구매 검증 후 Signed URL 제공)
   ↓
8. 서버에서 Firebase Storage Signed URL 생성
   (구매한 아이템에 대해서만)
   ↓
9. CosmeticItem 객체에 Signed URL 포함하여 반환
   ↓
10. CharacterView에서 Signed URL로 이미지 로드
    - Coil의 AsyncImage 사용
    - 자동 캐싱 처리됨
```

**핵심**: 메타데이터 조회와 리소스 URL 제공이 완전히 분리되어 URL이 노출되지 않습니다.

## ⚠️ 보안 고려사항

**중요**: Firebase Storage URL을 클라이언트에 직접 노출하면 보안 위험이 있습니다.

### 보안 문제점
- URL이 노출되면 구매하지 않은 아이템도 접근 가능
- 앱을 수정하여 하드코딩된 URL로 무단 아이템 표시 가능
- 클라이언트 검증만으로는 부족

### 보안 강화 방안
**자세한 내용은 [security-considerations.md](mdc:PLAN/security-considerations.md) 참고**

1. **서버 API를 통한 리소스 제공** (권장)
   - 구매 검증 후 서버에서 Signed URL 제공
   - 클라이언트는 직접 Storage 접근 불가

2. **Firebase Storage 보안 규칙 설정**
   - 클라이언트 직접 접근 차단
   - 서버 관리자만 쓰기 권한

3. **Signed URL 사용**
   - 만료 시간 설정 (예: 1시간)
   - 구매한 아이템에 대해서만 생성

4. **구매 검증 필수화**
   - Google Play 구매 후 서버에서 검증
   - 검증 실패 시 구매 취소

## 권장 사항

1. **서버 API 우선**: 리소스 URL은 서버를 통해서만 제공
2. **구매 검증 필수**: 모든 구매는 서버에서 검증
3. **Signed URL 사용**: 만료 시간이 있는 URL 사용
4. **Firebase Storage 보안 규칙**: 클라이언트 직접 접근 차단
5. **Coil 사용**: 이미지 로딩은 Coil 사용 (이미 프로젝트에 포함됨)
6. **레이어 순서**: 기본 캐릭터 → 신발 → 장갑 → 목걸이 → 모자 순서로 렌더링

## 구현 예시

```kotlin
// ViewModel에서
fun loadPurchasedItems() {
    viewModelScope.launch {
        repository.getPurchasedItems()
            .collect { items ->
                // items에는 이미 resourceUrl이 포함되어 있음
                _uiState.value = _uiState.value.copy(
                    purchasedItems = items
                )
            }
    }
}

// UI에서
CharacterView(
    customization = uiState.appliedItems,
    modifier = Modifier.size(200.dp)
)
```











