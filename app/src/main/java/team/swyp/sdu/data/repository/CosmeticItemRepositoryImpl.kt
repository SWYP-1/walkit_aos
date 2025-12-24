package team.swyp.sdu.data.repository

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import team.swyp.sdu.data.local.dao.AppliedItemDao
import team.swyp.sdu.data.local.dao.PurchasedItemDao
import team.swyp.sdu.data.local.entity.AppliedItemEntity
import team.swyp.sdu.data.local.entity.PurchasedItemEntity
import team.swyp.sdu.data.remote.billing.BillingManager
import team.swyp.sdu.domain.model.CharacterCustomization
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.ItemCategory
import team.swyp.sdu.domain.repository.CosmeticItemRepository
import team.swyp.sdu.domain.repository.PurchaseEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 꾸미기 아이템 Repository 구현
 *
 * TODO: 서버 API 연동 시 메타데이터는 서버에서 가져오도록 수정 필요
 */
@Singleton
class CosmeticItemRepositoryImpl @Inject constructor(
    private val purchasedItemDao: PurchasedItemDao,
    private val appliedItemDao: AppliedItemDao,
    private val billingManager: BillingManager,
) : CosmeticItemRepository {
    /**
     * 구매 이벤트를 Flow로 노출
     */
    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
    override fun purchaseEvents(): Flow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    /**
     * Repository 전용 코루틴 스코프
     * (구매 처리 등 백그라운드 작업에 사용)
     */
    private val repositoryScope = CoroutineScope(SupervisorJob())

    /**
     * 구매 업데이트 리스너
     * (BillingManager에서 호출됨)
     */
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    // 구매 내역 처리 (코루틴 스코프에서 처리)
                    purchases.forEach { purchase ->
                        repositoryScope.launch {
                            processPurchase(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("사용자가 구매를 취소했습니다")
                _purchaseEvents.tryEmit(PurchaseEvent.Canceled)
            }
            else -> {
                Timber.e("구매 실패: ${billingResult.debugMessage}")
                _purchaseEvents.tryEmit(PurchaseEvent.Error(billingResult.debugMessage))
            }
        }
    }

    init {
        // BillingManager의 구매 업데이트 리스너 설정
        // Repository에서 모든 구매 이벤트를 관리
        billingManager.setPurchasesUpdatedListener(purchasesUpdatedListener)
    }
    /**
     * 구매 가능한 아이템 목록 (임시 하드코딩)
     * TODO: 서버 API에서 메타데이터 가져오기
     */
    private val availableItems = listOf(
        CosmeticItem(
            productId = "shoes_01",
            name = "기본 신발",
            description = "기본 신발입니다",
            category = ItemCategory.SHOES,
            rarity = team.swyp.sdu.domain.model.ItemRarity.COMMON,
            price = "₩1,000",
            thumbnailUrl = null,
        ),
        CosmeticItem(
            productId = "shoes_02",
            name = "고급 신발",
            description = "고급 신발입니다",
            category = ItemCategory.SHOES,
            rarity = team.swyp.sdu.domain.model.ItemRarity.RARE,
            price = "₩2,000",
            thumbnailUrl = null,
        ),
        CosmeticItem(
            productId = "hat_01",
            name = "기본 모자",
            description = "기본 모자입니다",
            category = ItemCategory.HAT,
            rarity = team.swyp.sdu.domain.model.ItemRarity.COMMON,
            price = "₩1,000",
            thumbnailUrl = null,
        ),
        CosmeticItem(
            productId = "gloves_01",
            name = "기본 장갑",
            description = "기본 장갑입니다",
            category = ItemCategory.GLOVES,
            rarity = team.swyp.sdu.domain.model.ItemRarity.COMMON,
            price = "₩1,000",
            thumbnailUrl = null,
        ),
        CosmeticItem(
            productId = "necklace_01",
            name = "기본 목걸이",
            description = "기본 목걸이입니다",
            category = ItemCategory.NECKLACE,
            rarity = team.swyp.sdu.domain.model.ItemRarity.COMMON,
            price = "₩1,000",
            thumbnailUrl = null,
        ),
    )

    override fun getAvailableItems(): Flow<List<CosmeticItem>> = flow {
        // TODO: 서버 API에서 메타데이터 가져오기
        emit(availableItems)
    }

    override fun getPurchasedItems(): Flow<List<CosmeticItem>> {
        return purchasedItemDao.getAll().map { purchasedEntities ->
            val purchasedProductIds = purchasedEntities
                .filter { !it.isConsumed }
                .map { it.productId }
                .toSet()

            // 구매한 아이템만 필터링하여 반환
            availableItems
                .filter { it.productId in purchasedProductIds }
                .map { it.copy(isPurchased = true) }
        }
    }

    /**
     * 구매 흐름 시작
     */
    override suspend fun startPurchaseFlow(activity: Activity, productId: String): Result<Unit> {
        return try {
            // 제품 상세 정보 조회
            val productDetailsResult = billingManager.queryProductDetails(listOf(productId))
            productDetailsResult.fold(
                onSuccess = { productDetailsList ->
                    val productDetails = productDetailsList.firstOrNull()
                    if (productDetails != null) {
                        // 구매 흐름 시작
                        val billingResult = billingManager.launchPurchaseFlow(activity, productDetails)
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Timber.d("구매 흐름 시작 성공: $productId")
                            Result.success(Unit)
                        } else {
                            Timber.e("구매 흐름 시작 실패: ${billingResult.debugMessage}")
                            Result.failure(Exception("구매 시작 실패: ${billingResult.debugMessage}"))
                        }
                    } else {
                        Result.failure(Exception("제품을 찾을 수 없습니다: $productId"))
                    }
                },
                onFailure = { exception ->
                    Timber.e(exception, "제품 정보 조회 실패")
                    Result.failure(exception)
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "구매 흐름 시작 중 오류")
            Result.failure(e)
        }
    }

    /**
     * 구매 완료 처리 (내부 메서드 - BillingManager 리스너에서 호출)
     */
    private suspend fun processPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: ""

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                // 구매 대기 중 (결제 처리 중)
                Timber.d("구매 대기 중: $productId")
                _purchaseEvents.tryEmit(PurchaseEvent.Pending(productId))
            }
            Purchase.PurchaseState.PURCHASED -> {
                // 구매 완료
                Timber.d("구매 완료: $productId")
                try {
                    // 구매 내역 저장
                    val handleResult = handlePurchase(purchase)
                    handleResult.fold(
                        onSuccess = {
                            // 소모성 아이템이므로 구매 소비
                            consumePurchase(purchase.purchaseToken)
                            _purchaseEvents.tryEmit(PurchaseEvent.Success(productId))
                            Timber.d("구매 완료 처리 성공: $productId")
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "구매 완료 처리 실패")
                            _purchaseEvents.tryEmit(
                                PurchaseEvent.Error(
                                    exception.message ?: "구매 처리에 실패했습니다",
                                ),
                            )
                        },
                    )
                } catch (e: Exception) {
                    Timber.e(e, "구매 완료 처리 중 오류")
                    _purchaseEvents.tryEmit(
                        PurchaseEvent.Error(
                            e.message ?: "구매 처리 중 오류가 발생했습니다",
                        ),
                    )
                }
            }
            Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                Timber.w("구매 상태 불명: $productId")
                _purchaseEvents.tryEmit(PurchaseEvent.Error("구매 상태를 확인할 수 없습니다"))
            }
        }
    }

    override suspend fun handlePurchase(purchase: Purchase): Result<Unit> {
        return try {
            // 구매 내역을 로컬 DB에 저장
            purchase.products.forEach { productId ->
                val entity = PurchasedItemEntity(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    purchaseTime = purchase.purchaseTime,
                    quantity = purchase.quantity,
                    isConsumed = false,
                )
                purchasedItemDao.insert(entity)
                Timber.d("구매 내역 저장 완료: $productId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "구매 내역 저장 실패")
            Result.failure(e)
        }
    }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> {
        return try {
            // 먼저 로컬 DB에서 purchaseToken으로 구매 내역 찾기
            val allPurchased = purchasedItemDao.getAll().first()
            val entities = allPurchased.filter { it.purchaseToken == purchaseToken }

            if (entities.isEmpty()) {
                Timber.w("구매 내역을 찾을 수 없습니다: purchaseToken=$purchaseToken")
                return Result.failure(Exception("구매 내역을 찾을 수 없습니다"))
            }

            // BillingManager로 구매 소비
            val consumeResult = billingManager.consumePurchase(purchaseToken)
            consumeResult.fold(
                onSuccess = {
                    // 소비 성공 시 로컬 DB 업데이트
                    entities.forEach { entity ->
                        purchasedItemDao.update(entity.copy(isConsumed = true))
                        Timber.d("구매 소비 상태 업데이트 완료: ${entity.productId}")
                    }
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Timber.e(exception, "구매 소비 실패")
                    Result.failure(exception)
                },
            )
        } catch (e: Exception) {
            Timber.e(e, "구매 소비 처리 중 오류")
            Result.failure(e)
        }
    }

    override suspend fun applyItem(productId: String, category: ItemCategory): Result<Unit> {
        return try {
            val entity = AppliedItemEntity(
                category = category.name,
                productId = productId,
            )
            appliedItemDao.insert(entity)
            Timber.d("아이템 적용 완료: $productId (${category.name})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "아이템 적용 실패")
            Result.failure(e)
        }
    }

    override fun getAppliedItems(): Flow<CharacterCustomization> {
        return appliedItemDao.getAll().map { appliedEntities ->
            val appliedMap = appliedEntities.associate { it.category to it.productId }

            CharacterCustomization(
                shoes = appliedMap[ItemCategory.SHOES.name]?.let { productId ->
                    availableItems.firstOrNull { it.productId == productId }
                },
                hat = appliedMap[ItemCategory.HAT.name]?.let { productId ->
                    availableItems.firstOrNull { it.productId == productId }
                },
                gloves = appliedMap[ItemCategory.GLOVES.name]?.let { productId ->
                    availableItems.firstOrNull { it.productId == productId }
                },
                necklace = appliedMap[ItemCategory.NECKLACE.name]?.let { productId ->
                    availableItems.firstOrNull { it.productId == productId }
                },
            )
        }
    }

    override suspend fun removeItem(category: ItemCategory): Result<Unit> {
        return try {
            appliedItemDao.deleteByCategory(category.name)
            Timber.d("아이템 제거 완료: ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "아이템 제거 실패")
            Result.failure(e)
        }
    }
}








