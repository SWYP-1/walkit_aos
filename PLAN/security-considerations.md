# 보안 고려사항 및 대응 방안

## 🚨 보안 문제점

### 문제 1: URL 노출로 인한 무단 접근
- **위험**: Firebase Storage 다운로드 URL이 클라이언트에 노출되면
  - 악의적인 사용자가 URL을 직접 사용하여 구매하지 않은 아이템 이미지 다운로드 가능
  - 앱을 수정하여 하드코딩된 URL로 구매하지 않은 아이템 표시 가능
  - 클라이언트에서만 처리하는 것은 보안상 취약함

### 문제 2: 클라이언트 검증만으로는 부족
- **위험**: 로컬 DB에만 구매 정보를 저장하면
  - 앱을 수정하여 구매하지 않은 아이템을 DB에 추가 가능
  - 서버 검증 없이는 실제 구매 여부 확인 불가

## ✅ 보안 강화 방안

### 방안 1: 서버 API를 통한 리소스 제공 (권장)

#### 구조
```
클라이언트 → 서버 API → Firebase Storage
         ← (구매 검증된 URL만) ←
```

#### 서버 API 설계 (메타데이터와 리소스 URL 분리)

**핵심**: 메타데이터 조회와 리소스 URL 제공을 완전히 분리합니다.

##### 1. 메타데이터 조회 API (공개, URL 없음)
```kotlin
// 모든 아이템의 메타데이터 조회 (상점 표시용)
GET /api/cosmetic-items
// 인증 불필요 (공개 API)

Response:
{
  "items": [
    {
      "productId": "shoes_05",
      "name": "신발 5번",
      "description": "멋진 신발",
      "category": "SHOES",
      "rarity": "RARE",
      "price": "₩1,000",
      // ⚠️ URL은 포함하지 않음!
      // resourcePath도 포함하지 않음 (경로 노출 방지)
      "thumbnailUrl": "https://cdn.example.com/thumbnails/shoes_05.png"  // 썸네일만 CDN 사용
    }
  ]
}
```

##### 2. 구매한 아이템 리소스 URL 조회 API (인증 필요)
```kotlin
// 구매한 아이템에 대해서만 Signed URL 제공
GET /api/cosmetic-items/purchased/resources
Headers: Authorization: Bearer {accessToken}

Response:
{
  "items": [
    {
      "productId": "shoes_05",
      "resourceUrl": "https://firebasestorage.../shoes_05.png?token=...", // Signed URL
      "thumbnailUrl": "https://firebasestorage.../shoes_05_thumb.png?token=...",
      "expiresAt": 1234567890  // URL 만료 시간
    }
  ]
}
```

#### 클라이언트 구현
```kotlin
// data/remote/api/CosmeticItemApiService.kt
interface CosmeticItemApiService {
    /**
     * 모든 아이템 메타데이터 조회 (상점 표시용)
     * URL은 포함하지 않음 (보안)
     */
    @GET("/api/cosmetic-items")
    suspend fun getAvailableItems(): Response<AvailableItemsResponse>
    
    /**
     * 구매한 아이템의 리소스 URL만 조회
     * 서버에서 구매 검증 후 Signed URL 제공
     */
    @GET("/api/cosmetic-items/purchased/resources")
    suspend fun getPurchasedItemResources(
        @Header("Authorization") token: String
    ): Response<PurchasedResourcesResponse>
    
    /**
     * 아이템 적용 요청
     * 서버에서 구매 여부 재검증
     */
    @POST("/api/cosmetic-items/apply")
    suspend fun applyItem(
        @Header("Authorization") token: String,
        @Body request: ApplyItemRequest
    ): Response<ApplyItemResponse>
}

// 메타데이터 (URL 없음)
data class AvailableItemsResponse(
    val items: List<ItemMetadataDto>
)

data class ItemMetadataDto(
    val productId: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val price: String,
    val thumbnailUrl: String?  // 썸네일만 (CDN 사용, 공개 가능)
    // ⚠️ resourceUrl, resourcePath 없음!
)

// 리소스 URL (구매한 아이템에만 제공)
data class PurchasedResourcesResponse(
    val items: List<PurchasedResourceDto>
)

data class PurchasedResourceDto(
    val productId: String,
    val resourceUrl: String,      // 서버에서 제공하는 Signed URL
    val thumbnailUrl: String,
    val expiresAt: Long          // URL 만료 시간
)
```

#### Repository 수정 (메타데이터와 리소스 URL 분리)
```kotlin
// data/repository/CosmeticItemRepositoryImpl.kt
class CosmeticItemRepositoryImpl @Inject constructor(
    private val purchasedItemDao: PurchasedItemDao,
    private val appliedItemDao: AppliedItemDao,
    private val apiService: CosmeticItemApiService,
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager
) : CosmeticItemRepository {
    
    // 메타데이터와 리소스 URL을 별도로 관리
    private val resourceUrlCache = mutableMapOf<String, String>()  // productId -> resourceUrl
    
    /**
     * 구매 가능한 모든 아이템 메타데이터 조회 (URL 없음)
     */
    override fun getAvailableItems(): Flow<List<CosmeticItem>> = flow {
        val response = apiService.getAvailableItems()
        
        if (response.isSuccessful) {
            val metadataList = response.body()?.items ?: emptyList()
            
            // 메타데이터만 반환 (URL 없음)
            val items = metadataList.map { metadata ->
                CosmeticItem(
                    productId = metadata.productId,
                    name = metadata.name,
                    description = metadata.description,
                    category = ItemCategory.valueOf(metadata.category),
                    rarity = ItemRarity.valueOf(metadata.rarity),
                    price = metadata.price,
                    thumbnailUrl = metadata.thumbnailUrl,  // 썸네일만 (CDN)
                    resourceUrl = null,  // ⚠️ 리소스 URL은 없음!
                    isPurchased = false
                )
            }
            
            emit(items)
        } else {
            emit(emptyList())
        }
    }
    
    /**
     * 구매한 아이템 목록 조회 (메타데이터 + 리소스 URL)
     */
    override fun getPurchasedItems(): Flow<List<CosmeticItem>> = flow {
        // 1. 로컬 DB에서 구매한 productId 목록 가져오기
        val purchasedProductIds = purchasedItemDao.getAllPurchasedProductIds()
        
        if (purchasedProductIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        
        // 2. 메타데이터 조회 (공개 API)
        val metadataResponse = apiService.getAvailableItems()
        val allMetadata = metadataResponse.body()?.items ?: emptyList()
        
        // 3. 구매한 아이템의 리소스 URL 조회 (인증 필요)
        val accessToken = authRepository.getAccessToken()
            ?: throw IllegalStateException("인증 토큰이 없습니다")
        
        val resourcesResponse = apiService.getPurchasedItemResources("Bearer $accessToken")
        
        if (resourcesResponse.isSuccessful) {
            val resources = resourcesResponse.body()?.items ?: emptyList()
            
            // 리소스 URL 캐시 업데이트
            resources.forEach { resource ->
                resourceUrlCache[resource.productId] = resource.resourceUrl
            }
            
            // 4. 메타데이터와 리소스 URL 결합
            val purchasedItems = purchasedProductIds.mapNotNull { productId ->
                val metadata = allMetadata.find { it.productId == productId }
                val resourceUrl = resourceUrlCache[productId]
                
                if (metadata != null && resourceUrl != null) {
                    CosmeticItem(
                        productId = metadata.productId,
                        name = metadata.name,
                        description = metadata.description,
                        category = ItemCategory.valueOf(metadata.category),
                        rarity = ItemRarity.valueOf(metadata.rarity),
                        price = metadata.price,
                        thumbnailUrl = metadata.thumbnailUrl,
                        resourceUrl = resourceUrl,  // 서버에서 제공한 Signed URL
                        isPurchased = true
                    )
                } else null
            }
            
            emit(purchasedItems)
        } else {
            // 서버 검증 실패 시 빈 리스트 반환
            emit(emptyList())
        }
    }
}
```

### 방안 2: Firebase Storage 보안 규칙 설정

#### ⚠️ 중요: Firebase Storage Rules는 Signed URL을 생성하지 않습니다!

**Firebase Storage Rules의 역할**:
- 클라이언트가 직접 Storage에 접근할 수 있는지 여부를 결정
- Signed URL은 **백엔드에서 생성**합니다

**자세한 설명**: [signed-url-generation.md](mdc:PLAN/signed-url-generation.md) 참고

#### Firebase Storage 보안 규칙
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // 기본 캐릭터는 모든 사용자 접근 가능
    match /cosmetic-items/base/{fileName} {
      allow read: if true;
    }
    
    // 아이템 리소스는 클라이언트 직접 접근 차단
    match /cosmetic-items/{category}/{fileName} {
      // ❌ 클라이언트에서 직접 접근 불가
      // ✅ 서버에서 생성한 Signed URL은 접근 가능 (서버 권한으로 생성했으므로)
      allow read: if false;  // 클라이언트 직접 접근 차단
      
      // 쓰기: 서버 관리자만 가능
      allow write: if request.auth != null 
                   && request.auth.token.admin == true;
    }
    
    // 썸네일도 동일하게 처리
    match /cosmetic-items/{category}/thumbnails/{fileName} {
      allow read: if false;
      allow write: if request.auth != null 
                   && request.auth.token.admin == true;
    }
  }
}
```

**효과**:
- 클라이언트가 직접 Storage에 접근하려고 하면 차단됨
- 하지만 서버에서 생성한 Signed URL은 접근 가능 (서버 권한으로 생성했으므로)

### 방안 3: Signed URL 사용 (만료 시간 설정)

#### ⚠️ 중요: Signed URL만으로는 부족합니다!

**Signed URL의 특징**:
- 만료 시간이 있는 임시 URL
- URL에 서명(signature) 포함되어 변조 불가능
- 만료 후 자동으로 접근 불가

**하지만**:
- URL이 노출되면 만료 시간 전까지 접근 가능
- 구매 검증 없이 Signed URL을 생성하면 여전히 무단 접근 가능

**따라서**: 구매 검증 + Signed URL 조합이 필수입니다!

#### 서버에서 Signed URL 생성
```javascript
// 서버 코드 (Node.js 예시)
const admin = require('firebase-admin');
const { getStorage } = require('firebase-admin/storage');

async function getSignedUrl(storagePath, expiresInSeconds = 3600) {
  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  
  const [url] = await file.getSignedUrl({
    action: 'read',
    expires: Date.now() + expiresInSeconds * 1000  // 1시간 후 만료
  });
  
  return url;
}

// ⚠️ 구매 검증 후에만 Signed URL 생성
async function getPurchasedItemUrls(userId, productIds) {
  // 1. 사용자의 구매 내역 검증 (필수!)
  const purchasedItems = await verifyPurchases(userId, productIds);
  
  // 2. 구매한 아이템에 대해서만 Signed URL 생성
  const urls = {};
  for (const item of purchasedItems) {
    // ✅ 구매 검증 완료된 아이템만
    if (item.isPurchased) {
      urls[item.productId] = {
        resourceUrl: await getSignedUrl(`cosmetic-items/${item.category}/${item.fileName}`),
        thumbnailUrl: await getSignedUrl(`cosmetic-items/${item.category}/thumbnails/${item.fileName}`),
        expiresAt: Date.now() + 3600 * 1000
      };
    }
  }
  
  return urls;
}
```

**자세한 설명**: [signed-url-explanation.md](mdc:PLAN/signed-url-explanation.md) 참고

### 방안 4: 구매 검증 플로우

#### 구매 완료 후 서버 검증
```kotlin
// data/repository/CosmeticItemRepositoryImpl.kt
override suspend fun purchaseItem(productId: String): Result<Purchase> {
    // 1. Google Play Billing으로 구매 시작
    val purchaseResult = billingManager.launchPurchaseFlow(...)
    
    // 2. 구매 완료 후 서버에 검증 요청
    val accessToken = authRepository.getAccessToken()
        ?: return Result.failure(IllegalStateException("인증 토큰이 없습니다"))
    
    val verifyResponse = apiService.verifyPurchase(
        token = "Bearer $accessToken",
        request = VerifyPurchaseRequest(
            productId = productId,
            purchaseToken = purchaseResult.purchaseToken
        )
    )
    
    if (!verifyResponse.isSuccessful) {
        // 서버 검증 실패 시 구매 취소
        billingManager.consumePurchase(purchaseResult.purchaseToken)
        return Result.failure(Exception("구매 검증 실패"))
    }
    
    // 3. 서버 검증 성공 시 로컬 DB 저장
    purchasedItemDao.insert(
        PurchasedItemEntity(
            productId = productId,
            purchaseToken = purchaseResult.purchaseToken,
            purchaseTime = System.currentTimeMillis(),
            isConsumed = false
        )
    )
    
    // 4. 구매 소비 (소모성 아이템)
    billingManager.consumePurchase(purchaseResult.purchaseToken)
    
    return Result.success(purchaseResult)
}
```

## 🔒 최종 보안 아키텍처

```
┌─────────────┐
│   클라이언트  │
└──────┬──────┘
       │
       │ 1. 구매 요청 (productId)
       ↓
┌─────────────────┐
│ Google Play API │
└──────┬──────────┘
       │
       │ 2. 구매 완료 (purchaseToken)
       ↓
┌─────────────┐
│   클라이언트  │
└──────┬──────┘
       │
       │ 3. 서버에 구매 검증 요청
       │    (productId + purchaseToken)
       ↓
┌─────────────┐
│   서버 API   │
└──────┬──────┘
       │
       │ 4. Google Play API로 구매 검증
       ↓
┌─────────────────┐
│ Google Play API │
└──────┬──────────┘
       │
       │ 5. 구매 검증 완료
       ↓
┌─────────────┐
│   서버 API   │
└──────┬──────┘
       │
       │ 6. Firebase Storage에서 Signed URL 생성
       │    (구매한 아이템에 대해서만)
       ↓
┌──────────────────┐
│ Firebase Storage  │
└──────┬───────────┘
       │
       │ 7. Signed URL 반환 (만료 시간 포함)
       ↓
┌─────────────┐
│   서버 API   │
└──────┬──────┘
       │
       │ 8. 구매 내역 + Signed URL 반환
       ↓
┌─────────────┐
│   클라이언트  │
└─────────────┘
```

## 📋 구현 체크리스트

### Phase 1: 서버 API 구현 (우선순위 높음)
- [ ] 구매 검증 API (`POST /api/purchases/verify`)
- [ ] 구매한 아이템 목록 조회 API (`GET /api/cosmetic-items/purchased`)
- [ ] Firebase Storage Signed URL 생성 로직
- [ ] Google Play API 연동 (구매 검증)

### Phase 2: 클라이언트 수정
- [ ] 서버 API 호출 로직 추가
- [ ] 구매 완료 후 서버 검증 필수화
- [ ] Signed URL 만료 시간 처리
- [ ] 오프라인 모드 처리 (서버 검증 실패 시)

### Phase 3: Firebase 설정
- [ ] Firebase Storage 보안 규칙 설정
- [ ] 클라이언트 직접 접근 차단
- [ ] 서버 관리자만 쓰기 권한

### Phase 4: 테스트
- [ ] 구매 검증 테스트
- [ ] URL 만료 시간 테스트
- [ ] 무단 접근 차단 테스트
- [ ] 오프라인 모드 테스트

## ⚠️ 임시 방안 (서버 구현 전)

서버가 아직 구현되지 않은 경우, 최소한의 보안 조치:

```kotlin
// 임시: 클라이언트에서 직접 Storage 접근 차단
// 대신 로컬 assets에 기본 아이템만 포함

// 1. 구매한 아이템만 로컬 assets에서 로드
// 2. 서버 구현 후 즉시 API 연동으로 전환
// 3. 이 기간 동안은 기본 아이템만 제공
```

**주의**: 이 방법은 임시 방안이며, 서버 검증이 필수입니다.










