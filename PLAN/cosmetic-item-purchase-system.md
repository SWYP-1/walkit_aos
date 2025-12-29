# 캐릭터 꾸미기 아이템 구매 시스템 설계 계획

## 1. 아키텍처 구조

```
domain/
├── model/
│   ├── CosmeticItem.kt          # 도메인 모델 (신발, 모자, 장갑, 목걸이 등)
│   └── CharacterCustomization.kt # 현재 적용된 아이템 정보
├── repository/
│   └── CosmeticItemRepository.kt # Repository 인터페이스
└── usecase/
    ├── GetAvailableItemsUseCase.kt
    ├── PurchaseItemUseCase.kt
    ├── ConsumePurchaseUseCase.kt
    └── ApplyItemUseCase.kt

data/
├── local/
│   ├── entity/
│   │   ├── PurchasedItemEntity.kt  # 구매한 아이템 저장
│   │   └── AppliedItemEntity.kt     # 현재 적용된 아이템 저장
│   ├── dao/
│   │   ├── PurchasedItemDao.kt
│   │   └── AppliedItemDao.kt
│   └── mapper/
│       └── CosmeticItemMapper.kt
├── remote/
│   └── billing/
│       └── BillingManager.kt        # Google Play Billing 래퍼
└── repository/
    └── CosmeticItemRepositoryImpl.kt

presentation/
├── ui/
│   ├── screens/
│   │   ├── ShopScreen.kt           # 아이템 구매 화면
│   │   └── CustomizationScreen.kt  # 캐릭터 꾸미기 화면
│   └── components/
│       ├── ItemCard.kt
│       └── CharacterView.kt        # Lottie/PNG 렌더링
└── viewmodel/
    └── CosmeticItemViewModel.kt
```

## 2. 데이터 모델 설계

### 2.1 Domain Model
```kotlin
// domain/model/CosmeticItem.kt
enum class ItemCategory {
    SHOES,      // 신발
    HAT,        // 모자
    GLOVES,     // 장갑
    NECKLACE    // 목걸이
}

enum class ItemRarity {
    COMMON,     // 일반
    RARE,       // 희귀
    EPIC,       // 영웅
    LEGENDARY   // 전설
}

data class CosmeticItem(
    val productId: String,           // Google Play 제품 ID
    val name: String,
    val description: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val price: String,               // 가격 (예: "₩1,000")
    val resourceUrl: String,         // Firebase Storage 다운로드 URL (PNG/Lottie)
    val thumbnailUrl: String,       // 썸네일 다운로드 URL
    val isPurchased: Boolean = false,
    val isApplied: Boolean = false
)

data class CharacterCustomization(
    val shoes: CosmeticItem? = null,
    val hat: CosmeticItem? = null,
    val gloves: CosmeticItem? = null,
    val necklace: CosmeticItem? = null
)
```

### 2.2 Local Entity
```kotlin
// data/local/entity/PurchasedItemEntity.kt
@Entity(tableName = "purchased_items")
data class PurchasedItemEntity(
    @PrimaryKey
    val productId: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val quantity: Int = 1,
    val isConsumed: Boolean = false  // 소비 여부
)

// data/local/entity/AppliedItemEntity.kt
@Entity(tableName = "applied_items")
data class AppliedItemEntity(
    @PrimaryKey
    val category: String,  // ItemCategory.name
    val productId: String
)
```

## 3. Google Play Billing 통합

### 3.1 BillingManager (BillingClient 래퍼)
```kotlin
// data/remote/billing/BillingManager.kt
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private lateinit var billingClient: BillingClient
    
    // 초기화
    fun initialize(listener: PurchasesUpdatedListener)
    
    // 제품 목록 조회
    suspend fun queryProductDetails(productIds: List<String>): Result<List<ProductDetails>>
    
    // 구매 시작
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails): BillingResult
    
    // 구매 소비 (소모성 아이템)
    suspend fun consumePurchase(purchaseToken: String): Result<Unit>
    
    // 구매 내역 조회
    suspend fun queryPurchases(): Result<List<Purchase>>
}
```

## 4. Repository 패턴

### 4.1 Repository 인터페이스
```kotlin
// domain/repository/CosmeticItemRepository.kt
interface CosmeticItemRepository {
    // 구매 가능한 아이템 목록 조회
    fun getAvailableItems(): Flow<List<CosmeticItem>>
    
    // 구매한 아이템 목록 조회
    fun getPurchasedItems(): Flow<List<CosmeticItem>>
    
    // 아이템 구매
    suspend fun purchaseItem(productId: String): Result<Purchase>
    
    // 구매 소비
    suspend fun consumePurchase(purchaseToken: String): Result<Unit>
    
    // 아이템 적용
    suspend fun applyItem(productId: String, category: ItemCategory): Result<Unit>
    
    // 현재 적용된 아이템 조회
    fun getAppliedItems(): Flow<CharacterCustomization>
    
    // 아이템 제거
    suspend fun removeItem(category: ItemCategory): Result<Unit>
}
```

## 5. ViewModel 설계

### 5.1 UI State
```kotlin
sealed interface CosmeticItemUiState {
    data object Loading : CosmeticItemUiState
    data class Success(
        val availableItems: List<CosmeticItem>,
        val purchasedItems: List<CosmeticItem>,
        val appliedItems: CharacterCustomization
    ) : CosmeticItemUiState
    data class Error(val message: String) : CosmeticItemUiState
}
```

### 5.2 ViewModel 함수
```kotlin
class CosmeticItemViewModel @Inject constructor(
    private val repository: CosmeticItemRepository,
    private val billingManager: BillingManager
) : ViewModel() {
    
    val uiState: StateFlow<CosmeticItemUiState>
    
    // 아이템 목록 로드
    fun loadItems()
    
    // 구매 흐름 시작
    fun startPurchaseFlow(activity: Activity, productId: String)
    
    // 구매 완료 처리 (onPurchasesUpdated에서 호출)
    fun handlePurchase(purchase: Purchase)
    
    // 아이템 적용
    fun applyItem(productId: String, category: ItemCategory)
    
    // 아이템 제거
    fun removeItem(category: ItemCategory)
}
```

## 6. UI 컴포넌트 설계

### 6.1 ShopScreen
- 카테고리별 탭 (신발, 모자, 장갑, 목걸이)
- 아이템 그리드 표시
- 구매/적용 버튼
- 구매 상태 표시

### 6.2 CustomizationScreen
- 캐릭터 미리보기 (Lottie/PNG)
- 적용된 아이템 표시
- 아이템 변경/제거

### 6.3 CharacterView (Lottie/PNG 렌더링)
```kotlin
@Composable
fun CharacterView(
    customization: CharacterCustomization,
    modifier: Modifier = Modifier
) {
    // 기본 캐릭터
    // + 신발 레이어
    // + 장갑 레이어
    // + 목걸이 레이어
    // + 모자 레이어 (최상단)
}
```

## 7. 구매 흐름 처리

```
1. 사용자가 아이템 선택
   ↓
2. ViewModel.startPurchaseFlow()
   ↓
3. BillingManager.launchPurchaseFlow()
   ↓
4. Google Play 구매 화면 표시
   ↓
5. onPurchasesUpdated() 콜백 수신
   ↓
6. ViewModel.handlePurchase()
   ↓
7. Repository.purchaseItem() - 로컬 DB 저장
   ↓
8. Repository.consumePurchase() - 구매 소비
   ↓
9. UI 업데이트 (구매 완료 표시)
```

## 8. 구현 단계

### Phase 1: 기본 구조
1. Domain 모델 생성
2. Local Entity 및 DAO 생성
3. AppDatabase에 Entity 추가
4. Repository 인터페이스 정의

### Phase 2: Billing 통합
1. BillingManager 구현
2. Google Play Billing 라이브러리 추가
3. BillingClient 초기화
4. 제품 목록 조회 구현

### Phase 3: 구매 흐름
1. 구매 흐름 시작 구현
2. 구매 완료 처리
3. 구매 소비 로직
4. 로컬 DB 저장

### Phase 4: UI 구현
1. ShopScreen 구현
2. CustomizationScreen 구현
3. CharacterView (Lottie/PNG) 구현
4. 아이템 적용/제거 UI

### Phase 5: 통합 및 테스트
1. ViewModel 통합
2. 구매 흐름 테스트
3. 아이템 적용 테스트
4. 서버 동기화 주석 추가 (향후 구현)

## 9. 보안 고려사항

**⚠️ 중요**: 리소스 URL 노출로 인한 보안 위험이 있습니다.

### 보안 문제점
- Firebase Storage URL이 클라이언트에 노출되면 구매하지 않은 아이템도 접근 가능
- 앱을 수정하여 하드코딩된 URL로 무단 아이템 표시 가능
- 클라이언트 검증만으로는 부족

### 보안 강화 방안
**자세한 내용은 [security-considerations.md](mdc:PLAN/security-considerations.md) 참고**

1. **서버 API를 통한 리소스 제공** (필수)
   - 구매 검증 후 서버에서 Signed URL 제공
   - 클라이언트는 직접 Storage 접근 불가

2. **구매 검증 필수화**
   - Google Play 구매 후 서버에서 검증
   - 검증 실패 시 구매 취소

3. **Firebase Storage 보안 규칙**
   - 클라이언트 직접 접근 차단
   - 서버 관리자만 쓰기 권한

4. **Signed URL 사용**
   - 만료 시간 설정 (예: 1시간)
   - 구매한 아이템에 대해서만 생성

### 구현 우선순위
```kotlin
// Phase 1: 서버 API 구현 (최우선)
// 1. 구매 검증 API (POST /api/purchases/verify)
// 2. 구매한 아이템 목록 조회 API (GET /api/cosmetic-items/purchased)
// 3. Firebase Storage Signed URL 생성 로직
// 4. Google Play API 연동 (구매 검증)

// Phase 2: 클라이언트 수정
// 1. 서버 API 호출 로직 추가
// 2. 구매 완료 후 서버 검증 필수화
// 3. Signed URL 만료 시간 처리
```

## 10. 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:8.1.0")
    
    // Lottie (이미 있다면 생략)
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    
    // Firebase Storage (이미지 다운로드용)
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
}
```











