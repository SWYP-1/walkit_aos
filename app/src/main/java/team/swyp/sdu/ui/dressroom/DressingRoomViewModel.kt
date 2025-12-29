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
import team.swyp.sdu.domain.model.EquipSlot
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

    // 부위별 착용 아이템 추적
    private val _wornItemsByPosition = MutableStateFlow<Map<EquipSlot, Int>>(emptyMap())
    val wornItemsByPosition: StateFlow<Map<EquipSlot, Int>> = _wornItemsByPosition.asStateFlow()

    // 착용 요청 중 상태 (연속 클릭 방지)
    private val _isWearLoading = MutableStateFlow(false)
    val isWearLoading: StateFlow<Boolean> = _isWearLoading.asStateFlow()

    // 장바구니 다이얼로그 표시 상태
    private val _showCartDialog = MutableStateFlow(false)
    val showCartDialog: StateFlow<Boolean> = _showCartDialog.asStateFlow()


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
                    Timber.w(
                        pointResult.exception,
                        "포인트 정보 로드 실패: ${pointResult.message} - 기본값 0 사용"
                    )
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

            // 착용 상태 초기화 (빈 상태로 시작)
            _wornItemsByPosition.value = emptyMap()
        }
    }

    /**
     * 드레싱룸 선택 UI (ID Set) 업데이트 + 장바구니 자동 담기
     * 선택하는 즉시 장바구니에 담김 (이미 소유한 아이템 제외)
     */
    fun selectItem(itemId: Int) {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val item = currentState.items.find { it.itemId == itemId }
            if (item == null) return

            if (item.owned) {
                // 소유한 아이템: 착용/해제 토글
                val isCurrentlyWorn = _wornItemsByPosition.value[item.position] == itemId
                wearItem(itemId, !isCurrentlyWorn)
                Timber.d("아이템 ${if (!isCurrentlyWorn) "착용" else "해제"}: $itemId")
            } else {
                // 미소유 아이템: 장바구니 선택 로직
                val newSelectedSet = currentState.selectedItemIdSet.toMutableSet()
                val wasSelected = newSelectedSet.contains(itemId)

                if (wasSelected) {
                    // 선택 해제: Set에서 제거 + 장바구니에서도 제거
                    newSelectedSet.remove(itemId)
                    removeFromCart(itemId)
                } else {
                    // 선택: Set에 추가 + 장바구니에 담기
                    newSelectedSet.add(itemId)
                    addToCartIfNotOwned(itemId, currentState.items)
                }

                _uiState.value = currentState.copy(
                    selectedItemId = newSelectedSet.lastOrNull(),
                    selectedItemIdSet = LinkedHashSet(newSelectedSet)
                )
                Timber.d("장바구니 아이템 선택: $itemId, 선택됨: ${!wasSelected}, 현재 선택 Set: $newSelectedSet")
            }
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

    /**
     * 코스메틱 아이템 구매
     *
     * @param items 구매할 아이템 목록
     */
    fun purchaseItems() {
        viewModelScope.launch {
            // 구매 전 현재 UI 상태 저장 (실패 시 복원을 위해)
            val previousState = _uiState.value

            _uiState.value = DressingRoomUiState.Loading
            val items = cartItems.value.toList()
            Timber.d("코스메틱 아이템 구매 시작: ${items.size}개")

            val totalPrice = items.sumOf { it.point }

            when (val result = cosmeticItemRepository.purchaseItems(items, totalPrice)) {
                is Result.Success -> {
                    Timber.d("코스메틱 아이템 구매 성공")

                    // 구매 성공 시 장바구니에서 아이템 제거 및 UI 업데이트
                    val currentCart = _cartItems.value
                    val updatedCart = currentCart.filterNot { cartItem ->
                        items.any { purchasedItem -> purchasedItem.itemId == cartItem.itemId }
                    }.toCollection(LinkedHashSet())

                    _cartItems.value = updatedCart

                    // UI 상태 업데이트 (아이템 소유 상태 변경)
                    if (_uiState.value is DressingRoomUiState.Success) {
                        val currentState = _uiState.value as DressingRoomUiState.Success
                        val updatedItems = currentState.items.map { item ->
                            if (items.any { purchased -> purchased.itemId == item.itemId }) {
                                item.copy(owned = true)
                            } else {
                                item
                            }
                        }
                        // 포인트 정보 새로고침
                        val currentPoints = currentState.myPoint - totalPrice
                        _uiState.value =
                            currentState.copy(items = updatedItems, myPoint = currentPoints)
                    }
                    Timber.d("코스메틱 아이템 구매 완료 및 UI 업데이트")
                }

                is Result.Error -> {
                    Timber.e(result.exception, "코스메틱 아이템 구매 실패")
                    // 구매 실패 시 이전 상태로 복원
                    _uiState.value = previousState
                    Timber.d("구매 실패 - 이전 UI 상태로 복원: $previousState")
                    // TODO: 에러 처리 UI 표시 (Snackbar 등)
                }

                is Result.Loading -> {
                    // Loading 상태 유지
                }
            }
        }
    }

    /**
     * 코스메틱 아이템 착용/해제
     *
     * @param itemId 착용/해제할 아이템 ID
     * @param isWorn 착용 여부 (true: 착용, false: 해제)
     */
    fun wearItem(itemId: Int, isWorn: Boolean) {
        // 🚫 연속 클릭 방지
        if (_isWearLoading.value) {
            Timber.d("착용 요청 진행 중 - 무시: itemId=$itemId")
            return
        }

        val currentState = _uiState.value
        if (currentState !is DressingRoomUiState.Success) return

        val item = currentState.items.find { it.itemId == itemId } ?: return

        viewModelScope.launch {
            try {
                _isWearLoading.value = true
                Timber.d("코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 시작: itemId=$itemId")

                if (isWorn) {
                    // 착용: 같은 부위 다른 아이템들 해제
                    val currentlyWornItemId = _wornItemsByPosition.value[item.position]

                    if (currentlyWornItemId != null && currentlyWornItemId != itemId) {
                        // 같은 부위에 다른 아이템이 착용되어 있으면 해제
                        Timber.d("같은 부위 아이템 자동 해제: $currentlyWornItemId")
                        cosmeticItemRepository.wearItem(currentlyWornItemId, false)
                    }
                }

                // 현재 아이템 착용/해제 API 호출
                when (val result = cosmeticItemRepository.wearItem(itemId, isWorn)) {
                    is Result.Success -> {
                        Timber.d("코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 성공: itemId=$itemId")
                        // UI 상태 업데이트
                        updateWearState(itemId, isWorn, item.position)
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 실패: itemId=$itemId")
                    }
                    Result.Loading -> { }
                }
            } finally {
                _isWearLoading.value = false
            }
        }
    }
    /**
     * 선택된 아이템들 저장
     *
     * 카트에 구매할 아이템이 있으면 구매 다이얼로그 표시
     * 카트가 비어있으면 착용 상태를 저장
     */
    fun saveItems() {
        viewModelScope.launch {
            // 이미 작업 중이면 무시
            if (_isWearLoading.value) {
                Timber.d("저장 작업 진행 중 - 무시")
                return@launch
            }

            val currentCartItems = cartItems.value

            if (currentCartItems.isNotEmpty()) {
                // 카트에 아이템이 있으면 구매 다이얼로그 표시
                Timber.d("카트에 아이템 존재 - 구매 다이얼로그 표시: ${currentCartItems.size}개")
                _showCartDialog.value = true
            } else {
                // 카트가 비어있으면 착용 상태 저장
                Timber.d("카트가 비어있음 - 착용 상태 저장 시작")
                saveWornItems()
            }
        }
    }

    /**
     * 착용된 아이템들 저장
     *
     * 각 슬롯(HEAD, BODY, FEET)에 착용된 아이템들을 wearItem으로 저장
     */
    private suspend fun saveWornItems() {
        try {
            _isWearLoading.value = true
            Timber.d("착용 아이템 저장 시작")

            val wornItems = _wornItemsByPosition.value

            // 각 슬롯별 착용 아이템 저장
            val saveTasks = mutableListOf<suspend () -> Unit>()

            wornItems[EquipSlot.HEAD]?.let { itemId ->
                saveTasks.add {
                    Timber.d("HEAD 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            wornItems[EquipSlot.BODY]?.let { itemId ->
                saveTasks.add {
                    Timber.d("BODY 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            wornItems[EquipSlot.FEET]?.let { itemId ->
                saveTasks.add {
                    Timber.d("FEET 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            // 모든 저장 작업 실행
            saveTasks.forEach { task ->
                task()
            }

            Timber.d("착용 아이템 저장 완료: ${saveTasks.size}개 슬롯")
        } catch (e: Exception) {
            Timber.e(e, "착용 아이템 저장 실패")
            // TODO: 사용자에게 에러 표시
        } finally {
            _isWearLoading.value = false
        }
    }

    /**
     * 내부용 wearItem 함수 (UI 상태 업데이트 없이 API 호출만)
     */
    private suspend fun wearItemInternal(itemId: Int, isWorn: Boolean) {
        when (val result = cosmeticItemRepository.wearItem(itemId, isWorn)) {
            is Result.Success -> {
                Timber.d("아이템 저장 성공: itemId=$itemId, isWorn=$isWorn")
            }
            is Result.Error -> {
                Timber.e(result.exception, "아이템 저장 실패: itemId=$itemId")
                throw result.exception
            }
            Result.Loading -> { /* 무시 */ }
        }
    }

    /**
     * 장바구니 다이얼로그 닫기
     */
    fun dismissCartDialog() {
        _showCartDialog.value = false
        Timber.d("장바구니 다이얼로그 닫기")
    }

    /**
     * 착용 상태 업데이트 헬퍼 메서드
     *
     * @param itemId 대상 아이템 ID
     * @param isWorn 착용 여부
     * @param position 아이템 부위
     */
    private fun updateWearState(itemId: Int, isWorn: Boolean, position: EquipSlot) {
        if (_uiState.value is DressingRoomUiState.Success) {
            val currentState = _uiState.value as DressingRoomUiState.Success

            // _wornItemsByPosition을 기반으로 착용 상태 업데이트
            val updatedWornItems = _wornItemsByPosition.value.toMutableMap()

            if (isWorn) {
                // 착용: 해당 부위에 아이템 ID 설정
                updatedWornItems[position] = itemId
            } else {
                // 해제: 해당 부위에서 제거 (해당 아이템이 맞는 경우만)
                if (updatedWornItems[position] == itemId) {
                    updatedWornItems.remove(position)
                }
            }

            _wornItemsByPosition.value = updatedWornItems

            currentState.items.map { item ->
                updatedWornItems[item.position] = itemId
            }
            Timber.d("착용 상태 업데이트 완료: 부위별 착용 아이템 = $updatedWornItems")
        }
    }
}
