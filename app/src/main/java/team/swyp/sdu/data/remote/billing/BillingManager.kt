package team.swyp.sdu.data.remote.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Play Billing Manager
 *
 * BillingClient를 래핑하여 사용하기 쉽게 만든 클래스입니다.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var billingClient: BillingClient? = null
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var isInitialized = false

    /**
     * BillingClient 초기화 (Application에서 호출)
     */
    fun initialize() {
        if (isInitialized) {
            Timber.d("BillingManager가 이미 초기화되었습니다")
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                // 리스너가 설정되어 있으면 전달
                purchasesUpdatedListener?.onPurchasesUpdated(billingResult, purchases)
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("BillingClient 연결 성공")
                    isInitialized = true
                } else {
                    Timber.e("BillingClient 연결 실패: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Timber.w("BillingClient 연결 끊김")
                // 재연결 시도
                billingClient?.startConnection(this)
            }
        })
    }

    /**
     * 구매 업데이트 리스너 설정 (ViewModel에서 호출)
     *
     * @param listener 구매 업데이트 리스너
     */
    fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        purchasesUpdatedListener = listener
        Timber.d("구매 업데이트 리스너 설정 완료")
    }

    /**
     * 제품 상세 정보 조회
     *
     * @param productIds 제품 ID 목록
     * @return 제품 상세 정보 목록
     */
    suspend fun queryProductDetails(productIds: List<String>): Result<List<ProductDetails>> {
        val client = billingClient
        if (client == null || !client.isReady) {
            return Result.failure(IllegalStateException("BillingClient가 준비되지 않았습니다"))
        }

        return try {
            val productList = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP) // 소모성 아이템
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            // suspend 함수로 직접 호출 (Kotlin extension)
            val productDetailsResult = client.queryProductDetails(params)

            if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = productDetailsResult.productDetailsList ?: emptyList()
                Timber.d("제품 상세 정보 조회 성공: ${productDetailsList.size}개")
                Result.success(productDetailsList)
            } else {
                Timber.e("제품 상세 정보 조회 실패: ${productDetailsResult.billingResult.debugMessage}")
                Result.failure(
                    Exception("제품 조회 실패: ${productDetailsResult.billingResult.debugMessage}"),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "제품 상세 정보 조회 중 오류")
            Result.failure(e)
        }
    }

    /**
     * 구매 흐름 시작
     *
     * @param activity Activity
     * @param productDetails 제품 상세 정보
     * @return BillingResult
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
    ): BillingResult {
        val client = billingClient
        if (client == null || !client.isReady) {
            Timber.e("BillingClient가 준비되지 않았습니다")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
                .setDebugMessage("BillingClient가 준비되지 않았습니다")
                .build()
        }

        val productDetailsParamsList = listOf(
            com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build(),
        )

        val flowParams = com.android.billingclient.api.BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = client.launchBillingFlow(activity, flowParams)
        Timber.d("구매 흐름 시작: ${billingResult.responseCode}")
        return billingResult
    }

    /**
     * 구매 소비 (소모성 아이템)
     *
     * @param purchaseToken 구매 토큰
     * @return Result<Unit>
     */
    suspend fun consumePurchase(purchaseToken: String): Result<Unit> {
        val client = billingClient
        if (client == null || !client.isReady) {
            return Result.failure(IllegalStateException("BillingClient가 준비되지 않았습니다"))
        }

        return try {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()

            // suspend 함수로 직접 호출 (Kotlin extension)
            val consumeResult = client.consumePurchase(consumeParams)

            if (consumeResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.d("구매 소비 성공: $purchaseToken")
                Result.success(Unit)
            } else {
                Timber.e("구매 소비 실패: ${consumeResult.billingResult.debugMessage}")
                Result.failure(
                    Exception("구매 소비 실패: ${consumeResult.billingResult.debugMessage}"),
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "구매 소비 중 오류")
            Result.failure(e)
        }
    }

    /**
     * 구매 내역 조회
     * TODO: queryPurchasesAsync API 확인 후 구현 필요
     *
     * @return 구매 내역 목록
     */
    suspend fun queryPurchases(): Result<List<Purchase>> {
        // TODO: queryPurchasesAsync API 확인 후 구현
        return Result.failure(UnsupportedOperationException("아직 구현되지 않았습니다"))
    }

    /**
     * BillingClient 종료
     */
    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        purchasesUpdatedListener = null
    }
}










