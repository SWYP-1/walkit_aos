package team.swyp.sdu.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import team.swyp.sdu.domain.model.CharacterCustomization
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.ItemCategory
import team.swyp.sdu.domain.repository.CosmeticItemRepository
import team.swyp.sdu.domain.repository.PurchaseEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * 캐릭터 꾸미기 아이템 ViewModel
 */
@HiltViewModel
class CosmeticItemViewModel @Inject constructor(
    private val repository: CosmeticItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CosmeticItemUiState>(CosmeticItemUiState.Loading)
    val uiState: StateFlow<CosmeticItemUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    init {
        // 구매 이벤트 구독
        repository.purchaseEvents()
            .onEach { event ->
                handlePurchaseEvent(event)
            }
            .launchIn(viewModelScope)

        // 아이템 목록 로드
        loadItems()
    }

    /**
     * 아이템 목록 로드
     */
    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = CosmeticItemUiState.Loading
            try {
                combine(
                    repository.getAvailableItems(),
                    repository.getPurchasedItems(),
                    repository.getAppliedItems(),
                ) { availableItems, purchasedItems, appliedItems ->
                    CosmeticItemUiState.Success(
                        availableItems = availableItems,
                        purchasedItems = purchasedItems,
                        appliedItems = appliedItems,
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Timber.e(e, "아이템 목록 로드 실패")
                _uiState.value = CosmeticItemUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다")
            }
        }
    }

    /**
     * 구매 흐름 시작
     */
    fun startPurchaseFlow(activity: Activity, productId: String) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            val result = repository.startPurchaseFlow(activity, productId)
            result.fold(
                onSuccess = {
                    // 구매 흐름 시작 성공
                    // 구매 결과는 purchaseEvents Flow를 통해 처리됨
                    Timber.d("구매 흐름 시작 성공: $productId")
                },
                onFailure = { exception ->
                    Timber.e(exception, "구매 흐름 시작 실패")
                    _purchaseState.value = PurchaseState.Error(exception.message ?: "구매를 시작할 수 없습니다")
                },
            )
        }
    }

    /**
     * 구매 이벤트 처리
     */
    private fun handlePurchaseEvent(event: PurchaseEvent) {
        when (event) {
            is PurchaseEvent.Success -> {
                _purchaseState.value = PurchaseState.Success(event.productId)
                // 아이템 목록 새로고침
                loadItems()
            }
            is PurchaseEvent.Pending -> {
                _purchaseState.value = PurchaseState.Pending(event.productId)
            }
            is PurchaseEvent.Error -> {
                _purchaseState.value = PurchaseState.Error(event.message)
            }
            is PurchaseEvent.Canceled -> {
                _purchaseState.value = PurchaseState.Idle
            }
        }
    }

    /**
     * 아이템 적용
     */
    fun applyItem(productId: String, category: ItemCategory) {
        viewModelScope.launch {
            try {
                val result = repository.applyItem(productId, category)
                result.fold(
                    onSuccess = {
                        Timber.d("아이템 적용 성공: $productId")
                        loadItems() // 목록 새로고침
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "아이템 적용 실패")
                    },
                )
            } catch (e: Exception) {
                Timber.e(e, "아이템 적용 중 오류")
            }
        }
    }

    /**
     * 아이템 제거
     */
    fun removeItem(category: ItemCategory) {
        viewModelScope.launch {
            try {
                val result = repository.removeItem(category)
                result.fold(
                    onSuccess = {
                        Timber.d("아이템 제거 성공: ${category.name}")
                        loadItems() // 목록 새로고침
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "아이템 제거 실패")
                    },
                )
            } catch (e: Exception) {
                Timber.e(e, "아이템 제거 중 오류")
            }
        }
    }

    /**
     * 구매 상태 초기화
     */
    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}

/**
 * UI State
 */
sealed interface CosmeticItemUiState {
    data object Loading : CosmeticItemUiState

    data class Success(
        val availableItems: List<CosmeticItem>,
        val purchasedItems: List<CosmeticItem>,
        val appliedItems: CharacterCustomization,
    ) : CosmeticItemUiState

    data class Error(val message: String) : CosmeticItemUiState
}

/**
 * 구매 State
 */
sealed interface PurchaseState {
    data object Idle : PurchaseState

    data object Loading : PurchaseState

    /**
     * 구매 대기 중 (결제 처리 중)
     */
    data class Pending(val productId: String) : PurchaseState

    data class Success(val productId: String) : PurchaseState

    data class Error(val message: String) : PurchaseState
}








