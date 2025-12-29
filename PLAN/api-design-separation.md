# API 설계: 메타데이터와 리소스 URL 분리

## 🎯 핵심 원칙

**메타데이터 조회와 리소스 URL 제공을 완전히 분리하여 URL 노출을 방지합니다.**

## 문제점

만약 메타데이터에 URL이 포함되면:
- ❌ 모든 아이템의 URL이 노출됨
- ❌ 구매하지 않은 아이템도 접근 가능
- ❌ 앱 수정으로 무단 아이템 표시 가능

## 해결 방안

### API 분리 설계

#### 1. 메타데이터 조회 API (공개)
```http
GET /api/cosmetic-items
```

**목적**: 상점 화면에서 아이템 목록 표시

**인증**: 불필요 (공개 API)

**응답**:
```json
{
  "items": [
    {
      "productId": "shoes_05",
      "name": "신발 5번",
      "description": "멋진 신발입니다",
      "category": "SHOES",
      "rarity": "RARE",
      "price": "₩1,000",
      "thumbnailUrl": "https://cdn.example.com/thumbnails/shoes_05.png"
      // ⚠️ resourceUrl 없음!
      // ⚠️ resourcePath 없음!
    }
  ]
}
```

**특징**:
- 썸네일만 포함 (CDN 사용, 공개 가능)
- 리소스 URL은 절대 포함하지 않음
- 경로 정보도 포함하지 않음

#### 2. 구매한 아이템 리소스 URL 조회 API (인증 필요)
```http
GET /api/cosmetic-items/purchased/resources
Authorization: Bearer {accessToken}
```

**목적**: 구매한 아이템에 대해서만 리소스 URL 제공

**인증**: 필수 (사용자 인증 토큰)

**응답**:
```json
{
  "items": [
    {
      "productId": "shoes_05",
      "resourceUrl": "https://firebasestorage.googleapis.com/.../shoes_05.png?token=abc123",
      "thumbnailUrl": "https://firebasestorage.googleapis.com/.../shoes_05_thumb.png?token=abc123",
      "expiresAt": 1735689600000
    }
  ]
}
```

**특징**:
- 구매 검증 후에만 제공 (필수!)
- Signed URL 사용 (만료 시간 포함, 추가 보안)
- 구매한 아이템에 대해서만 반환

**⚠️ 중요**: Signed URL만으로는 안전하지 않습니다!
- Signed URL은 "임시 접근 권한"을 제공하지만
- **구매 검증이 필수**입니다
- 자세한 내용: [signed-url-explanation.md](mdc:PLAN/signed-url-explanation.md)

## 클라이언트 구현

### API Service
```kotlin
interface CosmeticItemApiService {
    /**
     * 모든 아이템 메타데이터 조회 (공개)
     * URL은 포함하지 않음
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
}

// 메타데이터 (URL 없음)
data class ItemMetadataDto(
    val productId: String,
    val name: String,
    val description: String,
    val category: String,
    val rarity: String,
    val price: String,
    val thumbnailUrl: String?  // 썸네일만 (CDN)
    // ⚠️ resourceUrl, resourcePath 없음!
)

// 리소스 URL (구매한 아이템에만)
data class PurchasedResourceDto(
    val productId: String,
    val resourceUrl: String,      // Signed URL
    val thumbnailUrl: String,
    val expiresAt: Long          // 만료 시간
)
```

### Repository 구현
```kotlin
class CosmeticItemRepositoryImpl @Inject constructor(
    private val apiService: CosmeticItemApiService,
    private val authRepository: AuthRepository
) : CosmeticItemRepository {
    
    // 리소스 URL 캐시 (productId -> resourceUrl)
    private val resourceUrlCache = mutableMapOf<String, String>()
    
    /**
     * 구매 가능한 모든 아이템 메타데이터 조회
     */
    override fun getAvailableItems(): Flow<List<CosmeticItem>> = flow {
        val response = apiService.getAvailableItems()
        
        if (response.isSuccessful) {
            val metadataList = response.body()?.items ?: emptyList()
            
            val items = metadataList.map { metadata ->
                CosmeticItem(
                    productId = metadata.productId,
                    name = metadata.name,
                    description = metadata.description,
                    category = ItemCategory.valueOf(metadata.category),
                    rarity = ItemRarity.valueOf(metadata.rarity),
                    price = metadata.price,
                    thumbnailUrl = metadata.thumbnailUrl,
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
        // 1. 로컬 DB에서 구매한 productId 목록
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
            emit(emptyList())
        }
    }
}
```

## 보안 효과

### ✅ URL 노출 방지
- 메타데이터에는 URL이 없으므로 노출되지 않음
- 구매한 아이템에 대해서만 URL 제공

### ✅ 구매 검증 필수
- 서버에서 구매 여부 검증 후 URL 제공
- 무단 접근 불가능

### ✅ Signed URL 사용
- 만료 시간이 있는 URL 사용
- 만료 후 재요청 필요

## 데이터 흐름 요약

```
상점 화면:
  GET /api/cosmetic-items
  → 메타데이터만 (URL 없음)
  → 썸네일만 표시

구매 완료 후:
  GET /api/cosmetic-items/purchased/resources
  → 구매 검증 후 Signed URL 제공
  → 캐릭터 표시에 사용
```

**결론**: 메타데이터와 리소스 URL을 완전히 분리하여 URL 노출을 방지합니다.











