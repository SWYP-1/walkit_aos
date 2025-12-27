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
import team.swyp.sdu.domain.repository.UserRepository
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
    private val userRepository: UserRepository,
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

            // 캐릭터 & 아이템 병렬 로딩
            val characterDeferred = async { characterRepository.getCharacter(nickname) }
            val itemsDeferred = async { cosmeticItemRepository.getCosmeticItems(position) }

            var character: Character? = null
            var items: List<CosmeticItem> = emptyList()

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

            // UI 업데이트
            _uiState.value = DressingRoomUiState.Success(
                items = items,
                selectedItemId = null,
                selectedItemIdSet = LinkedHashSet(),
                currentPosition = position,
                availablePositions = listOf("HEAD", "BODY", "FEET"),
                character = character
            )
        }
    }

    /**
     * 드레싱룸 선택 UI (ID Set) 업데이트
     */
    fun selectItem(itemId: Int) {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val newSelectedSet = currentState.selectedItemIdSet
            if (!newSelectedSet.add(itemId)) {
                newSelectedSet.remove(itemId) // 이미 선택돼 있으면 제거
            }
            _uiState.value = currentState.copy(
                selectedItemId = newSelectedSet.lastOrNull(),
                selectedItemIdSet = newSelectedSet
            )
            Timber.d("아이템 선택: $itemId, 현재 선택 Set: $newSelectedSet")
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
