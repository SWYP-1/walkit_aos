package swyp.team.walkit.ui.character.charactershop

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import swyp.team.walkit.R
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.onError
import swyp.team.walkit.core.onSuccess
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterPart
import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.LottieAsset
import swyp.team.walkit.domain.model.LottieCharacterState
import swyp.team.walkit.domain.model.WearState
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.CosmeticItemRepository
import swyp.team.walkit.domain.repository.PointRepository
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.domain.service.CharacterImageLoader
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.ui.dressroom.DressingRoomUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * DressingRoom ViewModel
 *
 * 코스메틱 아이템 관리 및 선택 상태를 담당합니다.
 */
@HiltViewModel
class CharacterShopViewModel @Inject constructor(
    private val application: Application,
    private val cosmeticItemRepository: CosmeticItemRepository,
    private val characterRepository: CharacterRepository,
    private val pointRepository: PointRepository,
    private val userRepository: UserRepository,
    val lottieImageProcessor: LottieImageProcessor,
    private val characterImageLoader: CharacterImageLoader,
    private val characterEventBus: swyp.team.walkit.core.CharacterEventBus, // ✅ 이벤트 버스 추가
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<DressingRoomUiState>(DressingRoomUiState.Loading)
    val uiState: StateFlow<DressingRoomUiState> = _uiState.asStateFlow()

    // 전체 아이템 리스트 (필터링용)
    private var allItems: List<CosmeticItem> = emptyList()

    // 선택된 카테고리 필터 (null = ALL, HEAD/BODY/FEET = 해당 카테고리)
    private val _selectedCategory = MutableStateFlow<EquipSlot?>(null)
    val selectedCategory: StateFlow<EquipSlot?> = _selectedCategory.asStateFlow()


    // 장바구니 아이템들 (직접 관리)
    private val _cartItems = MutableStateFlow<LinkedHashSet<CosmeticItem>>(LinkedHashSet())
    val cartItems: StateFlow<LinkedHashSet<CosmeticItem>> = _cartItems.asStateFlow()

    // UI 미리보기 착용 상태 (실제 API 반영 전) - 핵심 관리 변수
    private val _wornItemsByPosition = MutableStateFlow<Map<EquipSlot, WearState>>(emptyMap())
    val wornItemsByPosition: StateFlow<Map<EquipSlot, WearState>> =
        _wornItemsByPosition.asStateFlow()

    // 서버에 반영된 실제 착용 상태
    private val _serverWornItems = MutableStateFlow<Map<EquipSlot, WearState>>(emptyMap())
    val serverWornItems: StateFlow<Map<EquipSlot, WearState>> = _serverWornItems.asStateFlow()

    // selectedItemIdSet은 _wornItemsByPosition에서 파생됨 (단일 진실 공급원)
    val selectedItemIdSet = _wornItemsByPosition.map { wornItems ->
        wornItems.entries.mapNotNull { (_, wearState) ->
            when (wearState) {
                is WearState.Worn -> wearState.itemId
                else -> null
            }
        }.toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Companion.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    // 착용 요청 중 상태 (연속 클릭 방지)
    private val _isWearLoading = MutableStateFlow(false)
    val isWearLoading: StateFlow<Boolean> = _isWearLoading.asStateFlow()

    // 새로고침 중 상태
    private val _isRefreshLoading = MutableStateFlow(false)
    val isRefreshLoading: StateFlow<Boolean> = _isRefreshLoading.asStateFlow()

    // 드레스룸 로딩 중 상태 (중복 로딩 방지)
    private val _isDressingRoomLoading = MutableStateFlow(false)

    // 장바구니 다이얼로그 표시 상태
    private val _showCartDialog = MutableStateFlow(false)
    val showCartDialog: StateFlow<Boolean> = _showCartDialog.asStateFlow()

    // InfoBanner 메시지 상태
    data class InfoBannerMessage(
        val title: String,
        val description: String? = null
    )

    private val _infoBannerMessage = MutableStateFlow<InfoBannerMessage?>(null)
    val infoBannerMessage: StateFlow<InfoBannerMessage?> = _infoBannerMessage.asStateFlow()

    /**
     * InfoBanner 메시지 표시
     */
    private fun showInfoBanner(title: String, description: String? = null) {
        _infoBannerMessage.value = InfoBannerMessage(title, description)
        // 다음 프레임에서 자동으로 null로 리셋 (한 번만 표시)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _infoBannerMessage.value = null
        }
    }

    // 이전 착용 상태 (diff 계산용)
    private var previousWornItems = mapOf<EquipSlot, WearState>()

    // 캐릭터 파트별 Lottie 상태 (캐릭터 기본 파트 표시용)
    private val _characterLottieState = MutableStateFlow<LottieCharacterState?>(null)
    val characterLottieState: StateFlow<LottieCharacterState?> = _characterLottieState.asStateFlow()

    // 투명 PNG가 적용된 깨끗한 baseJson (재사용을 위해 저장)
    private var cleanBaseJson: JSONObject? = null

    init {
        // selectedItemIds 초기화 보장
        // UiState에서 관리되므로 별도 초기화 불필요
        loadDressingRoom()
    }

    /**
     * 캐릭터 + 코스메틱 아이템 병렬 로딩
     */
    fun loadDressingRoom(position: String? = null) {
        // 🚫 중복 로딩 방지
        if (_isDressingRoomLoading.value) {
            Timber.Forest.d("드레스룸 이미 로딩 중 - 중복 호출 무시: position=$position")
            return
        }

        viewModelScope.launch {
            _isDressingRoomLoading.value = true
            try {
                Timber.Forest.d("드레스룸 로딩 시작 - position: $position")

                // refresh 시 선택 상태 및 장바구니 초기화
                // UiState에서 관리되므로 별도 초기화 불필요
                _cartItems.value = LinkedHashSet()
                _showCartDialog.value = false
                Timber.Forest.d("✅ 선택 상태 및 장바구니 초기화 완료")

                _uiState.value = DressingRoomUiState.Loading

                // 사용자 정보 확보
                var userId: Long? = null
                val userResult = userRepository.getUser()
                Timber.Forest.d("사용자 정보 API 호출 결과: $userResult")

                userResult
                    .onSuccess {
                        userId = it.userId
                        Timber.Forest.d("사용자 정보 로드 성공: $userId")
                    }
                    .onError { exception, message ->
                        Timber.Forest.e(exception, "사용자 정보 로드 실패: $message")
                        Timber.Forest.e("UI 상태를 Error로 설정: 사용자 정보 로드 실패")
                        _uiState.value = DressingRoomUiState.Error(message ?: "사용자 정보 로드 실패")
                        return@launch
                    }

                if (userId == null) {
                    Timber.Forest.e("사용자 ID가 null입니다")
                    Timber.Forest.e("UI 상태를 Error로 설정: 사용자 ID null")
                    _uiState.value = DressingRoomUiState.Error("사용자 ID가 없습니다.")
                    return@launch
                }

                // 캐릭터 & 아이템 & 포인트 병렬 로딩
                val characterDeferred = async { characterRepository.getCharacter(userId) }
                val itemsDeferred = async { cosmeticItemRepository.getCosmeticItems(position) }
                val pointDeferred = async { pointRepository.getUserPoint() }

                var character: Character? = null
                var items: List<CosmeticItem> = emptyList()
                var userPoint: Int = 0

                // 캐릭터 처리
                val characterResult = characterDeferred.await()
                Timber.Forest.d("캐릭터 API 호출 결과: $characterResult")

                characterResult
                    .onSuccess {
                        character = it
                        Timber.Forest.d("캐릭터 로드 성공: ${it.nickName}")

                        // 캐릭터 로드 시 착용 상태를 Default로 초기화 (아이템 로드 후 CosmeticItem worn 정보로 업데이트됨)
                        val defaultWearStates = mapOf(
                            EquipSlot.HEAD to WearState.Default,
                            EquipSlot.BODY to WearState.Default,
                            EquipSlot.FEET to WearState.Default
                        )
                        _wornItemsByPosition.value = defaultWearStates
                        previousWornItems = defaultWearStates
                        Timber.Forest.d("✅ 캐릭터 로드 시 착용 상태 임시 Default로 초기화 (아이템 로드 후 업데이트 예정)")
                    }
                    .onError { exception, message ->
                        Timber.Forest.e(exception, "캐릭터 로드 실패: $message")
                        Timber.Forest.e("캐릭터 로드 실패에도 계속 진행 (아이템은 표시 가능)")

                        // 캐릭터 로드 실패 시에도 아이템은 표시할 수 있으므로 계속 진행
                        // _uiState.value = DressingRoomUiState.Error(message ?: "캐릭터 로드 실패")
                        // return@launch // 제거 - 캐릭터 없이도 아이템 표시 가능
                    }

                // 아이템 처리
                val itemsResult = itemsDeferred.await()
                Timber.Forest.d("코스메틱 아이템 API 호출 결과: $itemsResult")

                when (itemsResult) {
                    is Result.Success -> {
                        items = itemsResult.data
                        Timber.Forest.d("코스메틱 아이템 로드 성공: ${items.size}개")
                    }

                    is Result.Error -> {
                        Timber.Forest.e(
                            itemsResult.exception,
                            "코스메틱 아이템 로드 실패: ${itemsResult.message}"
                        )
                        Timber.Forest.e("UI 상태를 Error로 설정: 코스메틱 아이템 로드 실패")
                        _uiState.value =
                            DressingRoomUiState.Error(itemsResult.message ?: "아이템 로드 실패")
                        return@launch
                    }

                    Result.Loading -> {
                        Timber.Forest.d("코스메틱 아이템 로딩 중")
                    }
                }

                // 포인트 처리
                val pointResult = pointDeferred.await()
                Timber.Forest.d("포인트 API 호출 결과: $pointResult")

                when (pointResult) {
                    is Result.Success -> {
                        userPoint = pointResult.data
                        Timber.Forest.d("포인트 정보 로드 성공: $userPoint")
                    }

                    is Result.Error -> {
                        Timber.Forest.w(
                            pointResult.exception,
                            "포인트 정보 로드 실패: ${pointResult.message} - 기본값 0 사용"
                        )
                        userPoint = 0 // 실패 시 기본값 사용
                    }

                    Result.Loading -> {
                        Timber.Forest.d("포인트 정보 로딩 중 - 기본값 0 사용")
                        userPoint = 0
                    }
                }

                Timber.Forest.d("모든 API 호출 완료 - Success 상태로 전환")

                // 초기 Lottie JSON 설정 (이미 투명 PNG로 교체된 깨끗한 baseJson 사용)
                val initialLottieJson = if (character != null) {
                    try {
                        Timber.Forest.d("🏠 초기 Lottie JSON 로드 (투명 PNG 적용됨)")
                        cleanBaseJson = loadBaseLottieJson(character) // 깨끗한 baseJson 저장

                        if (cleanBaseJson != null && cleanBaseJson.toString().isNotEmpty()) {
                            Timber.Forest.d("✅ 깨끗한 baseJson 로드 및 저장 완료, 길이: ${cleanBaseJson.toString().length}")

                            // baseJson이 정말 깨끗한지 검증
                            val assets = cleanBaseJson!!.optJSONArray("assets")
                            if (assets != null) {
                                Timber.Forest.d("🔍 초기 baseJson assets 검증:")
                                for (i in 0 until minOf(assets.length(), 3)) {
                                    val asset = assets.optJSONObject(i)
                                    val id = asset?.optString("id", "unknown")
                                    val p = asset?.optString("p", "")?.take(50) // data URL 앞부분만
                                    Timber.Forest.d("  Asset[$i]: id=$id, p=${p}...")
                                }
                            }

                            cleanBaseJson.toString() // 이미 투명 PNG가 적용된 상태
                        } else {
                            Timber.Forest.e("❌ cleanBaseJson이 null이거나 비어있음")
                            null
                        }
                    } catch (t: Throwable) {
                        Timber.Forest.e(t, "초기 Lottie JSON 설정 실패")
                        null
                    }
                } else {
                    null
                }

                // 전체 아이템 저장 (필터링용)
                allItems = items
                val wornSet = allItems.filter { it.worn }.map { item -> item.itemId }.toSet()

                // UI 업데이트 (초기에는 전체 아이템 표시)
                val newSuccessState = DressingRoomUiState.Success(
                    items = items,
                    selectedItemId = null,
                    currentPosition = position,
                    character = character,
                    myPoint = userPoint,
                    processedLottieJson = initialLottieJson,
                    showOwnedOnly = false // 초기에는 전체 아이템 표시
                )
                Timber.Forest.d("Success 상태 설정: character=${character?.nickName}, items=${items.size}개, points=$userPoint")
                _uiState.value = newSuccessState

                // ✅ 착용 상태 초기화: CosmeticItem의 worn 정보를 기반으로 설정
                val wornItemsByPosition = mutableMapOf<EquipSlot, WearState>()
                val serverWornItems = mutableMapOf<EquipSlot, WearState>()

                // 아이템에서 worn=true인 것들을 찾아서 착용 상태로 설정
                items.filter { it.worn }.forEach { item ->
                    wornItemsByPosition[item.position] = WearState.Worn(item.itemId)
                    serverWornItems[item.position] = WearState.Worn(item.itemId)
                }

                // 설정되지 않은 슬롯들은 Default로 설정
                EquipSlot.values().forEach { slot ->
                    if (!wornItemsByPosition.containsKey(slot)) {
                        wornItemsByPosition[slot] = WearState.Default
                        serverWornItems[slot] = WearState.Default
                    }
                }

                _wornItemsByPosition.value = wornItemsByPosition
                _serverWornItems.value = serverWornItems

                // 초기 previousWornItems 설정
                previousWornItems = wornItemsByPosition.toMap()

                Timber.Forest.d("✅ CosmeticItem worn 정보로 착용 상태 초기화: $wornItemsByPosition")

                // 캐릭터 파트별 Lottie 상태 초기화
                if (character != null) {
                    initializeCharacterLottieState(character)
                }

                Timber.Forest.d("드레스룸 로딩 완료")
            } catch (t: Throwable) {
                Timber.Forest.e(t, "드레스룸 로딩 중 예외 발생")
                _uiState.value = DressingRoomUiState.Error("드레스룸 로딩 실패: ${t.message}")
            } finally {
                _isDressingRoomLoading.value = false
                Timber.Forest.d("드레스룸 로딩 상태 해제")
            }
        }
    }

    /**
     * 드레싱룸 선택 UI (ID Set) 업데이트 + 장바구니 자동 담기
     * 선택하는 즉시 장바구니에 담김 (이미 소유한 아이템 제외)
     */
    /**
     * 변경된 슬롯 계산 (diff)
     */
    private fun calculateChangedSlots(
        previous: Map<EquipSlot, WearState>,
        current: Map<EquipSlot, WearState>
    ): Set<EquipSlot> {
        val changedSlots = mutableSetOf<EquipSlot>()

        // 모든 슬롯에 대해 비교
        EquipSlot.values().forEach { slot ->
            val previousWearState = previous[slot]
            val currentWearState = current[slot]

            if (previousWearState != currentWearState) {
                changedSlots.add(slot)
                Timber.Forest.d("🔄 슬롯 변경 감지: $slot (이전: $previousWearState → 현재: $currentWearState)")
            }
        }

        return changedSlots
    }

    /**
     * 미리보기 착용 상태 토글
     */
    private fun togglePreviewWearState(itemId: Int, position: EquipSlot) {
        Timber.Forest.d("🔄 togglePreviewWearState 시작: itemId=$itemId, position=$position")

        val beforeState = _wornItemsByPosition.value
        Timber.Forest.d("📊 변경 전 착용 상태: $beforeState")

        val currentPreview = _wornItemsByPosition.value.toMutableMap()
        val currentWearState = currentPreview[position]

        Timber.Forest.d("🔍 현재 부위 $position 상태: $currentWearState")

        if (currentWearState is WearState.Worn && currentWearState.itemId == itemId) {
            Timber.Forest.d("👕 착용 해제: $position 부위에서 $itemId 제거 → Unworn 상태로")
            // 착용중인 아이템 클릭: 미착용 상태로 변경 (투명 PNG)
            currentPreview[position] = WearState.Unworn
        } else {
            Timber.Forest.d("👗 착용: $position 부위에 $itemId 설정")

            // 다른 아이템 착용: Worn 상태로 설정
            currentPreview[position] = WearState.Worn(itemId)
        }

        _wornItemsByPosition.value = currentPreview

        val afterState = _wornItemsByPosition.value
        Timber.Forest.d("📊 변경 후 착용 상태: $afterState")

        // Lottie 업데이트는 selectItem에서 호출하도록 함 (중복 방지)
    }

    /**
     * Lottie 미리보기 업데이트 (착용 상태 변경 시 호출)
     */
    private fun updateLottiePreview() {
        Timber.Forest.d("🎭 updateLottiePreview 시작")

        val currentState = _uiState.value
        Timber.Forest.d("📋 현재 UI 상태: ${currentState::class.simpleName}")

        if (currentState !is DressingRoomUiState.Success || currentState.character == null) {
            Timber.Forest.w("❌ Lottie 미리보기 업데이트 건너뜀: Success 상태 아님 또는 캐릭터 없음")
            return
        }

        val currentWornItems = _wornItemsByPosition.value
        Timber.Forest.d("✅ UI 상태 확인됨 - 캐릭터: ${currentState.character.nickName}")
        Timber.Forest.d("🧷 현재 착용 상태: $currentWornItems")
        Timber.Forest.d("🧷 이전 착용 상태: $previousWornItems")

        // 변경된 슬롯만 계산 (diff)
        val changedSlots = calculateChangedSlots(previousWornItems, currentWornItems)
        Timber.Forest.d("🔄 변경된 슬롯들: $changedSlots")

        // 변경사항이 없으면 업데이트 스킵
        if (changedSlots.isEmpty()) {
            Timber.Forest.d("⚡ 변경사항 없음 - Lottie 업데이트 스킵")
            return
        }

        viewModelScope.launch {
            try {
                Timber.Forest.d("🔄 저장된 cleanBaseJson 사용")
                val baseJson =
                    cleanBaseJson ?: loadBaseLottieJson(character = currentState.character)
                Timber.Forest.d("📂 Base Lottie JSON 준비 완료 (길이: ${baseJson.toString().length})")

                Timber.Forest.d("🔄 Lottie asset 교체 시작")
                // 변경된 슬롯만 선택적으로 교체
                val processedJson = lottieImageProcessor.updateAssetsForChangedSlots(
                    baseLottieJson = baseJson,
                    wornItemsByPosition = currentWornItems,
                    cosmeticItems = currentState.items,
                    character = currentState.character,
                    changedSlots = changedSlots
                )
                Timber.Forest.d("🔄 Lottie asset 교체 완료 (길이: ${processedJson.toString().length})")

                Timber.Forest.d("💾 UI State processedLottieJson 업데이트")
                val processedJsonString = processedJson.toString()
                val newState = currentState.copy(
                    processedLottieJson = processedJsonString
                )
                Timber.Forest.d("📊 새 UI State processedLottieJson 길이: ${newState.processedLottieJson?.length}")
                Timber.Forest.d("✅ Lottie JSON 업데이트 완료 - UI State에 반영됨")

                // UI State 업데이트 (Lottie JSON만)
                _uiState.value = newState

                // 이전 상태 업데이트
                previousWornItems = currentWornItems.toMap()

                Timber.Forest.d("✅ Lottie 미리보기 업데이트 완료 - UI 리컴포지션 대기")
            } catch (t: Throwable) {
                Timber.Forest.e(t, "❌ Lottie 미리보기 업데이트 실패")
                // 실패 시에도 계속 진행 (기본 Lottie 사용)
            }
        }
    }

    /**
     * 캐릭터 파트별 Lottie 상태 초기화
     * 캐릭터 데이터를 받아서 각 파트의 imageName에 따라 Lottie JSON을 수정
     */
    private fun initializeCharacterLottieState(character: Character) {
        viewModelScope.launch {
            try {
                Timber.Forest.d("🎭 캐릭터 Lottie 상태 초기화 시작")
                _characterLottieState.value = LottieCharacterState(baseJson = "", isLoading = true)

                // Base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)
                Timber.Forest.d("📂 Base Lottie JSON 로드 완료")

                // 캐릭터 파트별 Lottie JSON 수정
                val modifiedJson =
                    lottieImageProcessor.updateCharacterPartsInLottie(baseJson, character)
                Timber.Forest.d("🔄 캐릭터 파트 Lottie JSON 수정 완료")

                // ✅ UI 상태도 업데이트 (캐릭터 기본 이미지 적용)
                if (_uiState.value is DressingRoomUiState.Success) {
                    val currentState = _uiState.value as DressingRoomUiState.Success
                    _uiState.value = currentState.copy(
                        processedLottieJson = modifiedJson.toString()
                    )
                    Timber.Forest.d("✅ UI 캐릭터 기본 이미지 적용 완료 - processedLottieJson 길이: ${modifiedJson.toString().length}")
                }

                // 최종 상태 설정
                _characterLottieState.value = LottieCharacterState(
                    baseJson = baseJson.toString(),
                    modifiedJson = modifiedJson.toString(),
                    assets = createCharacterAssetMap(character),
                    isLoading = false
                )

                Timber.Forest.d("✅ 캐릭터 Lottie 상태 초기화 완료")
            } catch (t: Throwable) {
                Timber.Forest.e(t, "❌ 캐릭터 Lottie 상태 초기화 실패")
                _characterLottieState.value = LottieCharacterState(
                    baseJson = "",
                    modifiedJson = null,
                    assets = emptyMap(),
                    isLoading = false,
                    error = t.message ?: "캐릭터 Lottie 초기화 실패"
                )
            }
        }
    }

    /**
     * 캐릭터 파트별 asset 맵 생성
     */
    private suspend fun createCharacterAssetMap(character: Character): Map<String, LottieAsset> {
        val assetMap = mutableMapOf<String, LottieAsset>()

        CharacterPart.values().forEach { part ->
            val imageName = when (part) {
                CharacterPart.HEAD -> character.headImageName
                CharacterPart.BODY -> character.bodyImageName
                CharacterPart.FEET -> character.feetImageName
            }

            val assetId = part.getLottieAssetId()
            val imageData = characterImageLoader.loadCharacterPartImage(imageName, part)

            assetMap[assetId] = LottieAsset(
                id = assetId,
                currentImageData = imageData
            )

            Timber.Forest.d("🎨 캐릭터 파트 asset 생성: $part -> $assetId (imageName: $imageName)")
        }

        return assetMap
    }

    /**
     * Base Lottie JSON 로드 (raw resource)
     */
    private suspend fun loadBaseLottieJson(character: Character): JSONObject =
        withContext(Dispatchers.IO) {
            val resourceId = when (character.grade) {
                Grade.SEED -> R.raw.seed
                Grade.SPROUT -> R.raw.sprout
                Grade.TREE -> R.raw.tree
            }

            Timber.Forest.d("🎭 loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

            try {
                Timber.Forest.d("📂 Lottie 파일 로드 시도: grade=${character.grade}, resourceId=$resourceId")
                val inputStream = application.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                Timber.Forest.d("📄 JSON 문자열 길이: ${jsonString.length}")
                if (jsonString.isEmpty()) {
                    Timber.Forest.e("❌ JSON 문자열이 비어있음!")
                    return@withContext JSONObject() // 빈 JSON 반환
                }

                val jsonObject = JSONObject(jsonString)
                Timber.Forest.d("✅ JSONObject 생성 성공, 키 개수: ${jsonObject.length()}")

                // 디버깅: 로드된 JSON의 assets 구조 확인
                val assets = jsonObject.optJSONArray("assets")
                if (assets != null) {
                    Timber.Forest.d("📋 로드된 Lottie 파일 assets 개수: ${assets.length()}")
//                    for (i in 0 until minOf(assets.length(), 10)) {
//                        val asset = assets.optJSONObject(i)
//                        val id = asset?.optString("id", "unknown")
//                        val w = asset?.optInt("w", 0)
//                        val h = asset?.optInt("h", 0)
//                        Timber.d("📋 Asset[$i]: id=$id, size=${w}x${h}")
//                    }

                    // ⭐ 캐릭터의 기본 이미지를 설정하여 깨끗한 baseJson 생성
                    Timber.Forest.d("🔄 캐릭터 기본 이미지 설정 시작")
                    Timber.Forest.d("👤 캐릭터 레벨: ${character.level}")

                    // 캐릭터의 기본 이미지로 asset들을 교체
                    val characterBaseJson =
                        lottieImageProcessor.updateCharacterPartsInLottie(jsonObject, character)

                    Timber.Forest.d("✅ 캐릭터 기본 이미지 설정 완료")

                    // cleanBaseJson으로 저장
                    cleanBaseJson = characterBaseJson
                } else {
                    Timber.Forest.e("❌ 로드된 Lottie 파일에 assets 배열이 없음 - 파일 손상 가능성")
                    // 다른 필드들 확인
                    val keys = jsonObject.keys()
                    Timber.Forest.d("📋 JSON에 있는 키들:")
                    while (keys.hasNext()) {
                        Timber.Forest.d("  - ${keys.next()}")
                    }
                }

                jsonObject
            } catch (t: Throwable) {
                Timber.Forest.e(
                    t,
                    "❌ Base Lottie JSON 로드 실패: grade=${character.grade}, resourceId=$resourceId"
                )
                Timber.Forest.e(t, "스택트레이스: ${t.stackTraceToString()}")
                JSONObject() // 빈 JSON 반환
            }
        }

    /**
     * 슬롯별 asset ID 매핑 (level 기반)
     */
    private fun getAssetIdForSlot(slot: EquipSlot): String {
        return when (slot) {
            EquipSlot.HEAD -> "head"
            EquipSlot.BODY -> "body"
            EquipSlot.FEET -> "foot"
        }
    }

    fun selectItem(itemId: Int) {
        Timber.Forest.d("🎯 selectItem 호출: itemId=$itemId")

        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val item = currentState.items.find { it.itemId == itemId }
            if (item == null) {
                Timber.Forest.w("❌ 아이템을 찾을 수 없음: $itemId")
                return
            }

            Timber.Forest.d("📦 아이템 정보: id=$itemId, name=${item.name}, owned=${item.owned}, position=${item.position}")

            // 선택 상태 토글 - _wornItemsByPosition에서 관리 (단일 진실 공급원)
            if (currentState is DressingRoomUiState.Success) {
                val item = currentState.items.find { it.itemId == itemId } ?: return

                _wornItemsByPosition.update { wornItems ->
                    val updatedWornItems = wornItems.toMutableMap()
                    val currentWearState = wornItems[item.position]

                    if (currentWearState is WearState.Worn && currentWearState.itemId == itemId) {
                        // 이미 선택된 아이템 클릭: 선택 해제
                        updatedWornItems[item.position] = WearState.Unworn
                        Timber.Forest.d("❌ 선택 해제: $itemId")
                    } else {
                        // 새로운 아이템 선택: 같은 슬롯의 다른 아이템들은 모두 해제
                        updatedWornItems[item.position] = WearState.Worn(itemId)
                        Timber.Forest.d("✅ 선택 추가: $itemId (${item.position})")
                    }

                    updatedWornItems
                }

                // 선택 상태 맵 로깅 (selectedItemIdSet에서 파생)
                viewModelScope.launch {
                    val currentSelectedIds = selectedItemIdSet.value
                    Timber.Forest.d("🗺️ selectedItemIdSet 상태: [${currentSelectedIds.joinToString(", ")}] (${currentSelectedIds.size}개)")

                    // 장바구니 업데이트 (selectedItemIdSet 기반으로 자동 동기화)
                    val updatedCart = LinkedHashSet<CosmeticItem>()
                    currentSelectedIds.forEach { selectedId ->
                        val selectedItem =
                            currentState.items.find { it.itemId == selectedId && !it.owned }
                        if (selectedItem != null) {
                            updatedCart.add(selectedItem)
                        }
                    }
                    _cartItems.value = updatedCart
                }
            }

            // 선택 상태에 따른 착용 상태 업데이트 (selectedItemIdSet에서 파생)
            val currentState = _uiState.value as DressingRoomUiState.Success
            val selectedItemsInSameSlot = selectedItemIdSet.value
                .mapNotNull { selectedId -> currentState.items.find { it.itemId == selectedId } }
                .filter { it.position == item.position }

            // 같은 슬롯의 선택된 아이템들로 착용 상태 업데이트
            val updatedWornItems = _wornItemsByPosition.value.toMutableMap()
            if (selectedItemsInSameSlot.isNotEmpty()) {
                // 선택된 아이템들 중 마지막 선택된 아이템을 대표로 착용 상태 설정
                // (UI에서는 여러 개 선택 가능하지만, Lottie 미리보기는 마지막 선택된 것만 표시)
                updatedWornItems[item.position] =
                    WearState.Worn(selectedItemsInSameSlot.last().itemId)
            } else {
                // 선택된 아이템이 없으면 미착용 상태
                updatedWornItems[item.position] = WearState.Unworn
            }
            _wornItemsByPosition.value = updatedWornItems

            // Lottie 업데이트 (선택된 아이템 적용)
            updateLottiePreview()
        } else {
            Timber.Forest.w("❌ UI 상태가 Success가 아님: ${currentState::class.simpleName}")
        }
    }


    /**
     * 장바구니 추가/제거 (cartItems 직접 조작)
     */
    fun toggleCartItem(item: CosmeticItem) {
        val currentCart = _cartItems.value
        val newCart = LinkedHashSet(currentCart)
        if (!newCart.add(item)) {
            newCart.remove(item)
        }
        _cartItems.value = newCart

        Timber.Forest.d("장바구니 토글 - itemId: ${item.itemId}, 장바구니: ${newCart.size}개")
    }

    /**
     * 장바구니 비우기 (cartItems 직접 조작)
     */
    fun clearCart() {
        _cartItems.value = LinkedHashSet()
        // UiState에서 관리되므로 별도 초기화 불필요
        Timber.Forest.d("장바구니 비움")
    }

    /**
     * 포지션 필터 변경 (로컬 필터링)
     */
    fun changePositionFilter(position: String?) {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            // position 파라미터를 EquipSlot으로 변환
            val positionFilter = position?.let { pos ->
                try {
                    EquipSlot.valueOf(pos.uppercase())
                } catch (e: IllegalArgumentException) {
                    null // 유효하지 않은 position이면 null (ALL)
                }
            }

            // 로컬에서 필터링 적용
            val tempState = currentState.copy(currentPosition = position)
            val filteredItems = applyFilters(tempState)

            _uiState.value = currentState.copy(
                items = filteredItems,
                currentPosition = position
            )

            Timber.Forest.d("포지션 필터 변경: $position → 필터링된 아이템 ${filteredItems.size}개")
        } else {
            Timber.Forest.w("UI 상태가 Success가 아님 - 포지션 필터 변경 무시")
        }
    }

    /**
     * 아이템 필터링 적용 (보유 아이템 + 카테고리 필터)
     */
    private fun applyFilters(stateForFiltering: DressingRoomUiState.Success? = null): List<CosmeticItem> {
        val uiState = stateForFiltering ?: (_uiState.value as? DressingRoomUiState.Success)

        // 선택된 아이템 ID들 (항상 표시되어야 함)
        val selectedItemIds = selectedItemIdSet.value

        return allItems.filter { item ->
            // ✅ 선택된 아이템은 필터링에서 제외 (항상 표시)
            if (selectedItemIds.contains(item.itemId)) {
                return@filter true
            }

            // 보유 아이템 필터 적용
            val ownedFilter = if (uiState != null) {
                !uiState.showOwnedOnly || item.owned
            } else {
                true
            }

            // 카테고리 필터 적용
            val categoryFilter = _selectedCategory.value?.let { selected ->
                item.position == selected
            } ?: true // null이면 ALL (모든 카테고리 표시)

            ownedFilter && categoryFilter
        }
    }

    fun toggleShowOwnedOnly() {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val newShowOwnedOnly = !currentState.showOwnedOnly

            // 새로운 showOwnedOnly 값으로 필터링하기 위해 임시 상태 생성
            val tempState = currentState.copy(showOwnedOnly = newShowOwnedOnly)
            val filteredItems = applyFilters(tempState)

            _uiState.value = tempState.copy(items = filteredItems)
        }
    }

    /**
     * 카테고리 필터 변경
     */
    fun changeCategoryFilter(category: EquipSlot?) {
        _selectedCategory.value = category

        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val filteredItems = applyFilters()

            _uiState.value = currentState.copy(
                items = filteredItems
            )
        }
    }

    /**
     * 코스메틱 아이템 실제 구매 수행
     *
     * 다이얼로그에서 확인 버튼을 눌렀을 때 호출됨
     */
    fun performPurchase() {
        // ✅ 구매 시작: 버튼 disabled
        _isWearLoading.value = true

        viewModelScope.launch {
            try {
                val items = cartItems.value.toList()
                Timber.Forest.d("코스메틱 아이템 실제 구매 시작: ${items.size}개")

                val totalPrice = items.sumOf { it.point }

                when (val result = cosmeticItemRepository.purchaseItems(items, totalPrice)) {
                    is Result.Success -> {
                        Timber.Forest.d("코스메틱 아이템 구매 성공")

                        // 구매 성공 시 장바구니에서 아이템 제거 및 UI 업데이트
                        val currentCart = _cartItems.value
                        val updatedCart = LinkedHashSet(currentCart.filterNot { cartItem ->
                            items.any { purchasedItem -> purchasedItem.itemId == cartItem.itemId }
                        })

                        _cartItems.value = updatedCart

                        // ❌ 구매 성공 시 착용 상태 유지 (제거하지 않음)
                        // 사용자가 이미 착용하고 있던 아이템을 구매하더라도 착용 상태를 유지

                        // UI 상태 업데이트 (아이템 소유 상태 변경)
                        if (_uiState.value is DressingRoomUiState.Success) {
                            val currentState = _uiState.value as DressingRoomUiState.Success

                            // 구매된 아이템들의 owned 상태 업데이트 (서버와 동일하게)
                            val updatedItems = allItems.map { item ->
                                if (items.any { purchased -> purchased.itemId == item.itemId }) {
                                    item.copy(owned = true)
                                } else {
                                    item
                                }
                            }

                            // allItems 업데이트 (필터링용)
                            allItems = updatedItems

                            // 포인트 정보 업데이트 및 필터링 재적용
                            val currentPoints = currentState.myPoint - totalPrice
                            val filteredItems = if (currentState.showOwnedOnly) {
                                updatedItems.filter { it.owned }
                            } else {
                                updatedItems
                            }

                            _uiState.value = currentState.copy(
                                items = filteredItems,
                                myPoint = currentPoints
                            )
                        }

                        // 구매 성공 후 착용 상태 저장 (동기로 대기)
                        Timber.Forest.d("구매 성공 - 착용 상태 저장 시작")
                        saveWornItemsToServer()

                        // ✅ 구매 완료 InfoBanner 표시
                        showInfoBanner(
                            title = "아이템 구매가 완료되었습니다",
                            description = "보유한 아이템만 보기에서 확인하실 수 있습니다"
                        )

                        // ✅ 장바구니 다이얼로그 닫기
                        dismissCartDialog()

                        // ❌ 캐릭터 정보 백그라운드 동기화 제거
                        // 구매 완료 후 refreshCharacterInfo() 호출 시 이미 착용하고 있던 아이템 상태가 사라짐
                        // 구매 작업에서는 로컬 상태만 업데이트하고 서버 동기화는 불필요

                        Timber.Forest.d("코스메틱 아이템 구매 완료 및 로컬 상태 업데이트")
                    }

                    is Result.Error -> {
                        Timber.Forest.e(result.exception, "코스메틱 아이템 구매 실패")

                        // 실패 시에도 다이얼로그 닫기
                        dismissCartDialog()

                        // TODO: 에러 처리 UI 표시 (Snackbar 등)
                    }

                    is Result.Loading -> {
                        // Loading 상태 유지
                    }
                }
            } finally {
                // ✅ 모든 작업 완료: 버튼 enabled
                _isWearLoading.value = false
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
            Timber.Forest.d("착용 요청 진행 중 - 무시: itemId=$itemId")
            return
        }

        val currentState = _uiState.value
        if (currentState !is DressingRoomUiState.Success) return

        val item = currentState.items.find { it.itemId == itemId } ?: return

        viewModelScope.launch {
            try {
                _isWearLoading.value = true
                Timber.Forest.d("코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 시작: itemId=$itemId")

                if (isWorn) {
                    // 착용: 같은 부위 다른 아이템들 해제
                    val currentWearState = _wornItemsByPosition.value[item.position]

                    if (currentWearState is WearState.Worn && currentWearState.itemId != itemId) {
                        // 같은 부위에 다른 아이템이 착용되어 있으면 해제
                        Timber.Forest.d("같은 부위 아이템 자동 해제: ${currentWearState.itemId}")
                        cosmeticItemRepository.wearItem(currentWearState.itemId, false)
                    }
                }

                // 현재 아이템 착용/해제 API 호출
                when (val result = cosmeticItemRepository.wearItem(itemId, isWorn)) {
                    is Result.Success -> {
                        Timber.Forest.d("코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 성공: itemId=$itemId")
                        // UI 상태 업데이트
                        updateWearState(itemId, isWorn, item.position)
                    }

                    is Result.Error -> {
                        Timber.Forest.e(
                            result.exception,
                            "코스메틱 아이템 ${if (isWorn) "착용" else "해제"} 실패: itemId=$itemId"
                        )
                    }

                    Result.Loading -> {}
                }
            } finally {
                _isWearLoading.value = false
            }
        }
    }

    fun openCartDialogState() {
        _showCartDialog.value = true
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
                Timber.Forest.d("저장 작업 진행 중 - 무시")
                return@launch
            }

            val currentCartItems = cartItems.value

            if (currentCartItems.isNotEmpty()) {
                // 카트에 아이템이 있으면 구매 다이얼로그 표시
                Timber.Forest.d("카트에 아이템 존재 - 구매 다이얼로그 표시: ${currentCartItems.size}개")
                _showCartDialog.value = true
            } else {
                // 카트가 비어있으면 착용 상태 저장
                Timber.Forest.d("카트가 비어있음 - 착용 상태 저장 시작")
                saveWornItemsToServer()
//                saveWornItemFalse()
            }
        }
    }

    /**
     * 착용된 아이템들 저장
     *
     * 각 슬롯(HEAD, BODY, FEET)에 착용된 아이템들을 wearItem으로 저장
     */
    /**
     * 미리보기 착용 상태를 서버에 저장
     */

    private suspend fun saveWornItemsToServer() {
        try {
            _isWearLoading.value = true
            Timber.Forest.d("🎯 착용 아이템 서버 저장 시작 - 로딩 상태: ${_isWearLoading.value}")

            val previewItems = _wornItemsByPosition.value

            // ✅ 옵션 3: 캐시 비교 제거 - 현재 UI 상태를 기준으로 무조건 저장
            val saveTasks = mutableListOf<suspend () -> Unit>()

            // 모든 슬롯을 순회하며 현재 착용 상태를 서버에 반영
            EquipSlot.values().forEach { slot ->
                val currentWearState = previewItems[slot]

                when (currentWearState) {
                    is WearState.Worn -> {
                        // 착용된 아이템: 서버에 착용 상태로 저장
                        saveTasks.add {
                            Timber.Forest.d("$slot 슬롯 아이템 착용: ${currentWearState.itemId}")
                            wearItemInternal(currentWearState.itemId, true)
                        }
                    }

                    WearState.Unworn, WearState.Default, null -> {
                        // 미착용 상태: 해당 슬롯의 이전 착용 아이템 해제
                        // _serverWornItems에 기록된 이전 착용 아이템이 있다면 해제
                        val previousServerState = _serverWornItems.value[slot]
                        if (previousServerState is WearState.Worn) {
                            saveTasks.add {
                                Timber.Forest.d("$slot 슬롯 이전 아이템 해제: ${previousServerState.itemId}")
                                wearItemInternal(previousServerState.itemId, false)
                            }
                        }
                        Timber.Forest.d("$slot 슬롯 미착용 상태")
                    }
                }
            }

            Timber.Forest.d("총 ${saveTasks.size}개 아이템 착용 작업")

            // 모든 저장 작업 실행
            saveTasks.forEach { task ->
                task()
            }

            // 서버 저장 성공 시 서버 착용 상태 업데이트 (UI 반영)
            Timber.Forest.d("서버 저장 성공 - 서버 착용 상태 업데이트")
            _serverWornItems.value = previewItems.toMap()

            // ✅ 캐릭터 정보 새로고침 제거 - 착용 상태만 변경되므로 캐릭터 기본 정보는 유지
            Timber.Forest.d("서버 저장 성공 - 캐릭터 정보 유지 (새로고침 불필요)")

            // HomeViewModel에 캐릭터 캐시 무효화 알림
            viewModelScope.launch {
                characterEventBus.notifyCharacterUpdated()
                Timber.Forest.d("🏠 HomeViewModel에 캐릭터 캐시 무효화 알림 전송")
            }

            // 서버 상태와 UI 상태 동기화 완료
            Timber.Forest.d("착용 아이템 서버 저장 완료: ${saveTasks.size}개 슬롯")

            // ✅ 저장 완료 InfoBanner 표시
            showInfoBanner(
                title = "저장되었습니다",
                description = null
            )
        } catch (t: Throwable) {
            Timber.Forest.e(t, "착용 아이템 서버 저장 실패")
            // TODO: 사용자에게 에러 표시
        } finally {
            Timber.Forest.d("🎯 착용 아이템 서버 저장 종료 - 로딩 상태 해제")
            _isWearLoading.value = false
        }
    }

    /**
     * 내부용 wearItem 함수 (UI 상태 업데이트 없이 API 호출만)
     */
    private suspend fun wearItemInternal(itemId: Int, isWorn: Boolean) {
        when (val result = cosmeticItemRepository.wearItem(itemId, isWorn)) {
            is Result.Success -> {
                Timber.Forest.d("아이템 저장 성공: itemId=$itemId, isWorn=$isWorn")
            }

            is Result.Error -> {
                Timber.Forest.e(result.exception, "아이템 저장 실패: itemId=$itemId")
                throw result.exception
            }

            Result.Loading -> { /* 무시 */
            }
        }
    }

    /**
     * 모든 착용 아이템 해제
     */
    suspend fun saveWornItemFalse() {
        try {
            _isWearLoading.value = true
            Timber.Forest.d("🎯 모든 착용 아이템 해제 시작 - 로딩 상태: ${_isWearLoading.value}")

            val currentWornItems = _wornItemsByPosition.value
            val currentServerWornItems = _serverWornItems.value

            // 현재 착용된 아이템들만 해제
            val unwearTasks = mutableListOf<suspend () -> Unit>()

            // 각 슬롯별로 착용된 아이템 해제
            EquipSlot.values().forEach { slot ->
                val currentWearState = currentWornItems[slot]
                val serverWearState = currentServerWornItems[slot]

                // 현재 UI에서 착용된 상태라면 해제
                if (currentWearState is WearState.Worn) {
                    unwearTasks.add {
                        Timber.Forest.d("$slot 슬롯 아이템 해제: ${currentWearState.itemId}")
                        wearItemInternal(currentWearState.itemId, false)
                    }
                }

                // 서버 상태와 UI 상태가 다르다면 서버 상태도 정리
                if (serverWearState is WearState.Worn && currentWearState !is WearState.Worn) {
                    unwearTasks.add {
                        Timber.Forest.d("$slot 슬롯 서버 상태 정리 해제: ${serverWearState.itemId}")
                        wearItemInternal(serverWearState.itemId, false)
                    }
                }
            }

            Timber.Forest.d("총 ${unwearTasks.size}개 아이템 해제 작업")

            // 모든 해제 작업 실행
            unwearTasks.forEach { task ->
                task()
            }

            // 로컬 상태 초기화 (모두 Unworn으로)
            val initialWornItems = EquipSlot.values().associateWith { WearState.Unworn }
            _wornItemsByPosition.value = initialWornItems
            _serverWornItems.value = initialWornItems

            // 선택 상태도 초기화
            _cartItems.value = LinkedHashSet()

            // 캐릭터 정보 새로고침으로 전체 상태 동기화
            Timber.Forest.d("모든 아이템 해제 완료 - 캐릭터 정보 새로고침")
            refreshCharacterInfo()

            Timber.Forest.d("모든 착용 아이템 해제 완료: ${unwearTasks.size}개 슬롯")
        } catch (t: Throwable) {
            Timber.Forest.e(t, "모든 착용 아이템 해제 실패")
            // TODO: 사용자에게 에러 표시
        } finally {
            Timber.Forest.d("🎯 모든 착용 아이템 해제 종료 - 로딩 상태 해제")
            _isWearLoading.value = false
        }
    }

    /**
     * 캐릭터 정보 새로고침 (착용 상태 변경 후 최신 정보 반영)
     * 선택 상태 및 장바구니 초기화 후 서버의 최신 worn 정보로 재설정
     */
    /**
     * 포인트만 갱신 (보상 받기 등으로 포인트가 변경되었을 때 호출)
     */
    fun refreshPoint() {
        viewModelScope.launch {
            try {
                val pointResult = pointRepository.getUserPoint()
                when (pointResult) {
                    is Result.Success -> {
                        val updatedPoint = pointResult.data
                        val currentState = _uiState.value
                        if (currentState is DressingRoomUiState.Success) {
                            _uiState.value = currentState.copy(myPoint = updatedPoint)
                            Timber.Forest.d("💎 포인트 갱신 완료: $updatedPoint")
                        }
                    }
                    is Result.Error -> {
                        Timber.Forest.w(pointResult.exception, "포인트 갱신 실패: ${pointResult.message}")
                    }
                    Result.Loading -> {
                        Timber.Forest.d("포인트 갱신 중...")
                    }
                }
            } catch (t: Throwable) {
                Timber.Forest.e(t, "포인트 갱신 중 예외 발생")
            }
        }
    }
    
    suspend fun refreshCharacterInfo() {
        try {
            Timber.Forest.d("캐릭터 정보 refresh 시작")
            _isRefreshLoading.value = true

            // ✅ refresh 시 장바구니 상태도 초기화 (loadDressingRoom과 동일)
            _cartItems.value = LinkedHashSet()
            _showCartDialog.value = false
            Timber.Forest.d("장바구니 상태 초기화 완료")

            // 최신 캐릭터 정보 로드 (항상 API 호출)
            when (val result = characterRepository.getCharacterFromApi()) {
                is Result.Success -> {
                    val updatedCharacter = result.data
                    Timber.Forest.d(
                        "캐릭터 정보 refresh 성공: ${updatedCharacter.nickName} : body ${updatedCharacter.bodyImageName},head ${updatedCharacter.headImageName},feet ${updatedCharacter.feetImageName}"
                    )

                    // ✅ 캐릭터샵 처음 들어갔을 때처럼 worn 상태 기반으로 착용 상태 설정
                    // UI 상태에서 아이템 리스트 가져와서 worn=true인 아이템들로 착용 상태 설정
                    val currentItems = if (_uiState.value is DressingRoomUiState.Success) {
                        (_uiState.value as DressingRoomUiState.Success).items
                    } else emptyList()

                    val wornItemsMap = mutableMapOf<EquipSlot, WearState>()

                    // 아이템에서 worn=true인 것들을 찾아서 착용 상태로 설정 (loadDressingRoom과 동일한 로직)
                    currentItems.filter { it.worn }.forEach { item ->
                        wornItemsMap[item.position] = WearState.Worn(item.itemId)
                    }

                    // 설정되지 않은 슬롯들은 Default로 설정
                    EquipSlot.values().forEach { slot ->
                        if (!wornItemsMap.containsKey(slot)) {
                            wornItemsMap[slot] = WearState.Default
                        }
                    }

                    // 서버 착용 상태 업데이트
                    _serverWornItems.value = wornItemsMap
                    Timber.Forest.d("장착 아이템 상태 동기화 완료: ${wornItemsMap.size}개 슬롯")

                    // ✅ refresh 시 선택 상태 초기화 (캐릭터샵 처음 들어갔을 때처럼)
                    // 미리보기 착용 상태를 서버 착용 상태로 설정하되, 선택된 아이템들은 모두 해제
                    val initialWornItems = wornItemsMap.toMutableMap()
                    _wornItemsByPosition.value = initialWornItems
                    Timber.Forest.d("미리보기 착용 상태 초기화 완료: 선택 상태 모두 해제됨")

                    // ✅ 로띠 캐릭터 상태도 업데이트 (시각적 상태 동기화)
                    updateLottiePreview()
                    Timber.Forest.d("로띠 캐릭터 상태 업데이트 완료")

                    // UI 상태의 캐릭터 정보도 업데이트
                    if (_uiState.value is DressingRoomUiState.Success) {
                        val currentState = _uiState.value as DressingRoomUiState.Success
                        _uiState.value = currentState.copy(character = updatedCharacter)
                        Timber.Forest.d("UI 상태 캐릭터 정보 업데이트 완료")
                    }
                }
                is Result.Error -> {
                    Timber.Forest.e(result.exception, "캐릭터 정보 refresh 실패: ${result.message}")
                    // 에러 발생 시 사용자에게 알림
                    showInfoBanner("캐릭터 정보 갱신 실패", "잠시 후 다시 시도해주세요")
                }
                Result.Loading -> {
                    Timber.Forest.d("캐릭터 정보 refresh 로딩 중")
                }
            }

            _isRefreshLoading.value = false
            Timber.Forest.d("캐릭터 정보 refresh 완료")

        } catch (t: Throwable) {
            Timber.Forest.e(t, "캐릭터 정보 refresh 중 예외 발생")
            _isRefreshLoading.value = false
            showInfoBanner("캐릭터 정보 갱신 실패", "잠시 후 다시 시도해주세요")
        }
    }

    /**
     * 장바구니 다이얼로그 닫기
     */
    fun dismissCartDialog() {
        _showCartDialog.value = false
        Timber.Forest.d("장바구니 다이얼로그 닫기")
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
                // 착용: 해당 부위에 Worn 상태 설정
                updatedWornItems[position] = WearState.Worn(itemId)
            } else {
                // 해제: 해당 부위에서 Unworn 상태로 설정 (해당 아이템이 맞는 경우만)
                val currentWearState = updatedWornItems[position]
                if (currentWearState is WearState.Worn && currentWearState.itemId == itemId) {
                    updatedWornItems[position] = WearState.Unworn
                }
            }

            _wornItemsByPosition.value = updatedWornItems

            Timber.Forest.d("착용 상태 업데이트 완료: 부위별 착용 아이템 = $updatedWornItems")
        }
    }

}