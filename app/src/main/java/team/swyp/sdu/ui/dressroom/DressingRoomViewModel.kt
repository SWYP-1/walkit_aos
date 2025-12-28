package team.swyp.sdu.ui.dressroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.repository.CharacterRepository
import team.swyp.sdu.domain.repository.CosmeticItemRepository
import team.swyp.sdu.domain.repository.PointRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.service.LottieImageProcessor
import timber.log.Timber
import javax.inject.Inject

/**
 * DressingRoom ViewModel
 *
 * 코스메틱 아이템 관리 및 선택 상태를 담당합니다.
 */
@HiltViewModel
class DressingRoomViewModel @Inject constructor(
    private val cosmeticItemRepository: CosmeticItemRepository,
    private val characterRepository: CharacterRepository,
    private val pointRepository: PointRepository,
    private val userRepository: UserRepository,
    val lottieImageProcessor: LottieImageProcessor,
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<DressingRoomUiState>(DressingRoomUiState.Loading)
    val uiState: StateFlow<DressingRoomUiState> = _uiState.asStateFlow()

    // 장바구니 상태 (실제 아이템 객체)
    private val _cartItems = MutableStateFlow<LinkedHashSet<CosmeticItem>>(LinkedHashSet())
    val cartItems: StateFlow<LinkedHashSet<CosmeticItem>> = _cartItems.asStateFlow()

    private val _showOwnedOnly = MutableStateFlow(false)
    val showOwnedOnly: StateFlow<Boolean> = _showOwnedOnly.asStateFlow()


    init {
        loadDressingRoom()
    }

    /**
     * 캐릭터 + 코스메틱 아이템 병렬 로딩
     */
    fun loadDressingRoom(position: String? = null) {
        viewModelScope.launch {
            _uiState.value = DressingRoomUiState.Loading

            // 사용자 정보 확보
            var nickname: String? = null
            userRepository.getUser()
                .onSuccess { nickname = it.nickname }
                .onError { exception, message ->
                    Timber.e(exception, "사용자 정보 로드 실패: $message")
                    _uiState.value = DressingRoomUiState.Error(message ?: "사용자 정보 로드 실패")
                    return@launch
                }
            if (nickname == null) {
                Timber.e("사용자 정보가 없습니다.")
                _uiState.value = DressingRoomUiState.Error("사용자 정보가 없습니다.")
                return@launch
            }

            // 캐릭터 & 아이템 & 포인트 병렬 로딩
            val characterDeferred = async { characterRepository.getCharacter(nickname) }
            val itemsDeferred = async { cosmeticItemRepository.getCosmeticItems(position) }
            val pointDeferred = async { pointRepository.getUserPoint() }

            var character: Character? = null
            var items: List<CosmeticItem> = emptyList()
            var userPoint: Int = 0

            // 캐릭터 처리
            characterDeferred.await()
                .onSuccess { character = it }
                .onError { exception, message ->
                    Timber.e(exception, "캐릭터 로드 실패: $message")
                    _uiState.value = DressingRoomUiState.Error(message ?: "캐릭터 로드 실패")
                    return@launch
                }

            // 아이템 처리
            when (val result = itemsDeferred.await()) {
                is Result.Success -> items = result.data
                is Result.Error -> {
                    Timber.e(result.exception, "코스메틱 아이템 로드 실패")
                    _uiState.value = DressingRoomUiState.Error(result.message ?: "아이템 로드 실패")
                    return@launch
                }

                Result.Loading -> {}
            }

            // 포인트 처리
            when (val pointResult = pointDeferred.await()) {
                is Result.Success -> {
                    userPoint = pointResult.data
                    Timber.d("포인트 정보 로드 성공: $userPoint")
                }
                is Result.Error -> {
                    Timber.w(pointResult.exception, "포인트 정보 로드 실패: ${pointResult.message} - 기본값 0 사용")
                    userPoint = 0 // 실패 시 기본값 사용
                }
                Result.Loading -> {
                    // Loading 상태는 무시
                    userPoint = 0
                }
            }

            // UI 업데이트
            _uiState.value = DressingRoomUiState.Success(
                items = items,
                selectedItemId = null,
                selectedItemIdSet = LinkedHashSet(),
                currentPosition = position,
                availablePositions = listOf("HEAD", "BODY", "FEET"),
                character = character,
                myPoint = userPoint
            )
        }
    }

    /**
     * 드레싱룸 선택 UI (ID Set) 업데이트 + 장바구니 자동 담기
     * 선택하는 즉시 장바구니에 담김 (이미 소유한 아이템 제외)
     */
    fun selectItem(itemId: Int) {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val newSelectedSet = currentState.selectedItemIdSet.toMutableSet()
            val wasSelected = newSelectedSet.contains(itemId)

            if (wasSelected) {
                // 선택 해제: Set에서 제거 + 장바구니에서도 제거
                newSelectedSet.remove(itemId)
                removeFromCart(itemId)
            } else {
                // 선택: Set에 추가 + 장바구니에 담기 (미소유 아이템만)
                newSelectedSet.add(itemId)
                addToCartIfNotOwned(itemId, currentState.items)
            }

            _uiState.value = currentState.copy(
                selectedItemId = newSelectedSet.lastOrNull(),
                selectedItemIdSet = LinkedHashSet(newSelectedSet)
            )
            Timber.d("아이템 선택: $itemId, 선택됨: ${!wasSelected}, 현재 선택 Set: $newSelectedSet")
        }
    }

    /**
     * 장바구니에 추가 (미소유 아이템만)
     */
    private fun addToCartIfNotOwned(itemId: Int, items: List<CosmeticItem>) {
        val item = items.find { it.itemId == itemId }
        if (item != null && !item.owned) {
            // 미소유 아이템만 장바구니에 담기
            val currentCart = _cartItems.value
            if (!currentCart.contains(item)) {
                currentCart.add(item)
                _cartItems.value = currentCart
                Timber.d("장바구니 추가: ${item.name} (ID: $itemId)")
            }
        } else if (item?.owned == true) {
            Timber.d("이미 소유한 아이템은 장바구니에 담지 않음: ${item.name} (ID: $itemId)")
        }
    }

    /**
     * 장바구니에서 제거
     */
    private fun removeFromCart(itemId: Int) {
        val currentCart = _cartItems.value
        val itemToRemove = currentCart.find { it.itemId == itemId }
        if (itemToRemove != null) {
            currentCart.remove(itemToRemove)
            _cartItems.value = currentCart
            Timber.d("장바구니 제거: ${itemToRemove.name} (ID: $itemId)")
        }
    }

    fun clearSelection() {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            _uiState.value = currentState.copy(
                selectedItemId = null,
                selectedItemIdSet = LinkedHashSet()
            )
            Timber.d("모든 아이템 선택 해제")
        }
    }

    /**
     * 장바구니 추가/제거 (객체 Set)
     */
    fun toggleCartItem(item: CosmeticItem) {
        val currentCart = _cartItems.value
        if (!currentCart.add(item)) {
            currentCart.remove(item)
        }
        _cartItems.value = currentCart
        Timber.d("장바구니 상태: $currentCart")
    }

    /**
     * 장바구니 비우기
     */
    fun clearCart() {
        _cartItems.value = LinkedHashSet()
        Timber.d("장바구니 비움")
    }

    /**
     * 포지션 필터 변경
     */
    fun changePositionFilter(position: String?) {
        loadDressingRoom(position)
    }

    fun toggleShowOwnedOnly() {
        _showOwnedOnly.value = !_showOwnedOnly.value
        // UI 갱신
        refreshFilteredItems()
    }

    private fun refreshFilteredItems() {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val filtered = if (_showOwnedOnly.value) {
                currentState.items.filter { it.owned } // owned = true
            } else {
                currentState.items
            }
            _uiState.value = currentState.copy(
                items = filtered
            )
        }
    }


}
