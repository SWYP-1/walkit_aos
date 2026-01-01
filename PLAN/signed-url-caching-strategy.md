# Signed URL과 이미지 캐싱 전략 (상업 앱 수준)

## 문제점

Signed URL의 만료 시간(예: 1시간) 때문에 매번 이미지를 다시 다운로드해야 하는가?

**답변**: 아니요! 이미지는 캐싱되고, URL만 갱신합니다.

## 상업 앱 수준의 해결 방안

### 방안 1: 이미지 캐싱 + URL 갱신 (권장)

#### 핵심 원리

```
이미지 파일 자체는 동일함
→ Coil이 파일 내용을 기반으로 캐싱
→ URL이 바뀌어도 같은 파일이면 캐시 재사용
```

#### 구현 방법

##### 1. Coil의 자동 캐싱 활용

```kotlin
// Coil은 URL이 아닌 파일 내용을 기반으로 캐싱합니다
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(signedUrl)  // URL이 바뀌어도
        .diskCachePolicy(CachePolicy.ENABLED)  // 디스크 캐싱
        .memoryCachePolicy(CachePolicy.ENABLED)  // 메모리 캐싱
        .build(),
    contentDescription = "아이템"
)
```

**동작 방식**:
- 첫 번째 로드: Signed URL로 이미지 다운로드 → 디스크 캐시 저장
- URL 만료 후: 새 Signed URL 받음 → 같은 파일이면 캐시 재사용
- **이미지는 다시 다운로드하지 않음!**

##### 2. URL 갱신 전략

```kotlin
// Repository에서 URL 갱신 로직
class CosmeticItemRepositoryImpl {
    private val resourceUrlCache = mutableMapOf<String, CachedUrl>()
    
    data class CachedUrl(
        val url: String,
        val expiresAt: Long  // 만료 시간
    )
    
    /**
     * 구매한 아이템의 리소스 URL 조회
     * 만료 시간 전에 미리 갱신
     */
    suspend fun getPurchasedItemResources(): List<PurchasedResourceDto> {
        val now = System.currentTimeMillis()
        
        // 만료 시간이 10분 이하로 남았으면 갱신
        val needsRefresh = resourceUrlCache.values.any { 
            it.expiresAt - now < 10 * 60 * 1000  // 10분
        }
        
        if (needsRefresh) {
            // 서버에서 새 Signed URL 받기
            val newUrls = apiService.getPurchasedItemResources(token)
            
            // 캐시 업데이트
            newUrls.forEach { resource ->
                resourceUrlCache[resource.productId] = CachedUrl(
                    url = resource.resourceUrl,
                    expiresAt = resource.expiresAt
                )
            }
        }
        
        return resourceUrlCache.values.map { 
            PurchasedResourceDto(url = it.url, expiresAt = it.expiresAt)
        }
    }
}
```

### 방안 2: 더 긴 만료 시간 사용

#### 만료 시간 선택

```javascript
// 서버에서 Signed URL 생성 시 만료 시간 설정
const [url] = await file.getSignedUrl({
  action: 'read',
  expires: Date.now() + (7 * 24 * 60 * 60 * 1000)  // 7일
  // 또는
  expires: Date.now() + (30 * 24 * 60 * 60 * 1000)  // 30일
});
```

**권장 만료 시간**:
- **1시간**: 보안 우선 (URL 노출 시 피해 최소화)
- **24시간**: 일반적인 앱 (하루에 한 번 갱신)
- **7일**: 사용자 경험 우선 (일주일에 한 번 갱신)
- **30일**: 장기 캐싱 (한 달에 한 번 갱신)

**트레이드오프**:
- 짧은 만료 시간: 보안 강화, 하지만 자주 갱신 필요
- 긴 만료 시간: 사용자 경험 향상, 하지만 URL 노출 시 피해 큼

### 방안 3: 백그라운드 URL 갱신

#### 앱 시작 시 또는 주기적으로 갱신

```kotlin
class CosmeticItemViewModel @Inject constructor(
    private val repository: CosmeticItemRepository
) : ViewModel() {
    
    init {
        // 앱 시작 시 URL 갱신
        refreshResourceUrls()
        
        // 주기적으로 갱신 (예: 1시간마다)
        viewModelScope.launch {
            while (true) {
                delay(60 * 60 * 1000)  // 1시간
                refreshResourceUrls()
            }
        }
    }
    
    private fun refreshResourceUrls() {
        viewModelScope.launch {
            repository.refreshPurchasedItemResources()
        }
    }
}
```

**효과**:
- 사용자가 앱을 사용하는 동안 URL이 만료되지 않음
- 백그라운드에서 미리 갱신하여 끊김 없음

## 실제 상업 앱 사례

### Netflix, Spotify 등

```
1. 콘텐츠 URL은 만료 시간이 있음 (보안)
2. 하지만 이미지/썸네일은 긴 만료 시간 사용 (7일 ~ 30일)
3. 또는 CDN을 사용하여 캐싱
```

### 게임 앱 (캐릭터 커스터마이징)

```
1. 구매한 아이템의 Signed URL 생성 (만료 시간: 7일)
2. Coil로 이미지 로드 → 자동 디스크 캐싱
3. 7일 후 URL 갱신 → 같은 파일이면 캐시 재사용
4. 사용자는 이미지 재다운로드를 느끼지 못함
```

## 최종 권장 전략

### 1. 이미지 캐싱 (Coil 자동)

```kotlin
// Coil 설정 (Application 클래스)
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)  // 50MB
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)  // 메모리의 25%
                    .build()
            }
            .build()
        
        Coil.setImageLoader(imageLoader)
    }
}
```

**효과**:
- 이미지는 디스크에 캐싱됨
- URL이 바뀌어도 같은 파일이면 캐시 재사용
- 네트워크 요청 없이 즉시 표시

### 2. URL 갱신 전략

```kotlin
// 만료 시간 전에 미리 갱신
class ResourceUrlManager {
    private val urlCache = mutableMapOf<String, CachedUrl>()
    
    suspend fun getUrl(productId: String): String {
        val cached = urlCache[productId]
        val now = System.currentTimeMillis()
        
        // 만료 시간이 1시간 이하로 남았으면 갱신
        if (cached == null || cached.expiresAt - now < 60 * 60 * 1000) {
            val newUrl = refreshUrl(productId)
            urlCache[productId] = CachedUrl(newUrl, now + 7 * 24 * 60 * 60 * 1000)
            return newUrl
        }
        
        return cached.url
    }
}
```

### 3. 만료 시간 설정

```javascript
// 서버에서 Signed URL 생성
const [url] = await file.getSignedUrl({
  action: 'read',
  expires: Date.now() + (7 * 24 * 60 * 60 * 1000)  // 7일
});
```

**권장**:
- **캐릭터 커스터마이징 아이템**: 7일 ~ 30일
- **자주 변경되는 콘텐츠**: 1시간 ~ 24시간
- **민감한 콘텐츠**: 1시간

## 성능 비교

### ❌ 매번 다운로드 (비효율적)

```
사용자가 앱 열 때마다
→ Signed URL 받기
→ 이미지 다운로드
→ 느림, 데이터 사용량 많음
```

### ✅ 캐싱 + URL 갱신 (효율적)

```
첫 번째 로드:
→ Signed URL 받기
→ 이미지 다운로드
→ 디스크 캐싱

이후 로드:
→ Signed URL 갱신 (만료 시간 전)
→ 캐시된 이미지 사용
→ 빠름, 데이터 사용량 적음
```

## 구현 예시

### Repository 구현

```kotlin
class CosmeticItemRepositoryImpl @Inject constructor(
    private val apiService: CosmeticItemApiService,
    private val authRepository: AuthRepository
) : CosmeticItemRepository {
    
    // URL 캐시 (productId -> CachedUrl)
    private val urlCache = mutableMapOf<String, CachedUrl>()
    
    data class CachedUrl(
        val url: String,
        val expiresAt: Long
    )
    
    /**
     * 구매한 아이템의 리소스 URL 조회
     * 캐시된 URL이 만료 시간 전이면 재사용
     */
    suspend fun getPurchasedItemResources(): List<PurchasedResourceDto> {
        val now = System.currentTimeMillis()
        
        // 만료 시간이 1시간 이하로 남았으면 갱신
        val needsRefresh = urlCache.isEmpty() || 
            urlCache.values.any { it.expiresAt - now < 60 * 60 * 1000 }
        
        if (needsRefresh) {
            val accessToken = authRepository.getAccessToken()
                ?: throw IllegalStateException("인증 토큰이 없습니다")
            
            val response = apiService.getPurchasedItemResources("Bearer $accessToken")
            
            if (response.isSuccessful) {
                val resources = response.body()?.items ?: emptyList()
                
                // 캐시 업데이트
                resources.forEach { resource ->
                    urlCache[resource.productId] = CachedUrl(
                        url = resource.resourceUrl,
                        expiresAt = resource.expiresAt
                    )
                }
            }
        }
        
        return urlCache.map { (productId, cached) ->
            PurchasedResourceDto(
                productId = productId,
                resourceUrl = cached.url,
                expiresAt = cached.expiresAt
            )
        }
    }
}
```

### ViewModel 구현

```kotlin
class CosmeticItemViewModel @Inject constructor(
    private val repository: CosmeticItemRepository
) : ViewModel() {
    
    val purchasedItems: StateFlow<List<CosmeticItem>> = flow {
        // 1. 메타데이터 조회
        val metadata = repository.getAvailableItems().first()
        
        // 2. 리소스 URL 조회 (캐시 활용)
        val resources = repository.getPurchasedItemResources()
        
        // 3. 결합
        val items = metadata.mapNotNull { meta ->
            val resource = resources.find { it.productId == meta.productId }
            resource?.let {
                meta.copy(
                    resourceUrl = it.resourceUrl,
                    isPurchased = true
                )
            }
        }
        
        emit(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        // 주기적으로 URL 갱신 (백그라운드)
        viewModelScope.launch {
            while (true) {
                delay(6 * 60 * 60 * 1000)  // 6시간마다
                repository.refreshPurchasedItemResources()
            }
        }
    }
}
```

## 요약

### ✅ 해결 방안

1. **이미지 캐싱**: Coil이 자동으로 디스크 캐싱
   - URL이 바뀌어도 같은 파일이면 캐시 재사용
   - 이미지는 다시 다운로드하지 않음

2. **URL 갱신**: 만료 시간 전에 미리 갱신
   - 백그라운드에서 갱신하여 끊김 없음
   - 사용자는 느끼지 못함

3. **만료 시간**: 적절한 시간 설정
   - 캐릭터 커스터마이징: 7일 ~ 30일 권장
   - 보안과 사용자 경험의 균형

### ✅ 최종 효과

- ✅ 이미지는 한 번만 다운로드 (캐싱)
- ✅ URL만 갱신 (가벼운 작업)
- ✅ 사용자는 재다운로드를 느끼지 못함
- ✅ 데이터 사용량 최소화
- ✅ 빠른 로딩 속도

**결론**: Signed URL의 만료 시간 때문에 매번 이미지를 다운로드하는 것이 아닙니다. 이미지는 캐싱되고, URL만 갱신합니다!












