package swyp.team.walkit.ui.dressroom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
import swyp.team.walkit.utils.replaceAssetP
import swyp.team.walkit.utils.toBase64DataUrl
import timber.log.Timber
import java.io.BufferedReader
import javax.inject.Inject

/**
 * DressingRoom ViewModel
 *
 * 코스메틱 아이템 관리 및 선택 상태를 담당합니다.
 */
@HiltViewModel
class DressingRoomViewModel @Inject constructor(
    private val application: Application,
    private val cosmeticItemRepository: CosmeticItemRepository,
    private val characterRepository: CharacterRepository,
    private val pointRepository: PointRepository,
    private val userRepository: UserRepository,
    val lottieImageProcessor: LottieImageProcessor,
    private val characterImageLoader: CharacterImageLoader,
) : ViewModel() {

    // UI 상태
    private val _uiState = MutableStateFlow<DressingRoomUiState>(DressingRoomUiState.Loading)
    val uiState: StateFlow<DressingRoomUiState> = _uiState.asStateFlow()

    // 전체 아이템 리스트 (필터링용)
    private var allItems: List<CosmeticItem> = emptyList()


    // 장바구니 아이템들 (직접 관리)
    private val _cartItems = MutableStateFlow<LinkedHashSet<CosmeticItem>>(LinkedHashSet())
    val cartItems: StateFlow<LinkedHashSet<CosmeticItem>> = _cartItems.asStateFlow()

    // UI 미리보기 착용 상태 (실제 API 반영 전) - 핵심 관리 변수
    private val _wornItemsByPosition = MutableStateFlow<Map<EquipSlot, WearState>>(emptyMap())
    val wornItemsByPosition: StateFlow<Map<EquipSlot, WearState>> = _wornItemsByPosition.asStateFlow()

    // 서버에 반영된 실제 착용 상태
    private val _serverWornItems = MutableStateFlow<Map<EquipSlot, WearState>>(emptyMap())
    val serverWornItems: StateFlow<Map<EquipSlot, WearState>> = _serverWornItems.asStateFlow()

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
            Timber.d("드레스룸 이미 로딩 중 - 중복 호출 무시: position=$position")
            return
        }

        viewModelScope.launch {
            try {
                _isDressingRoomLoading.value = true
                Timber.d("드레스룸 로딩 시작 - position: $position")

                // refresh 시 선택 상태 및 장바구니 초기화
                // UiState에서 관리되므로 별도 초기화 불필요
                _cartItems.value = LinkedHashSet()
                _showCartDialog.value = false
                Timber.d("✅ 선택 상태 및 장바구니 초기화 완료")

                _uiState.value = DressingRoomUiState.Loading

                // 사용자 정보 확보
                var userId: Long? = null
                val userResult = userRepository.getUser()
                Timber.d("사용자 정보 API 호출 결과: $userResult")

                userResult
                    .onSuccess {
                        userId = it.userId
                        Timber.d("사용자 정보 로드 성공: $userId")
                    }
                    .onError { exception, message ->
                        Timber.e(exception, "사용자 정보 로드 실패: $message")
                        Timber.e("UI 상태를 Error로 설정: 사용자 정보 로드 실패")
                        _uiState.value = DressingRoomUiState.Error(message ?: "사용자 정보 로드 실패")
                        return@launch
                    }

                if (userId == null) {
                    Timber.e("사용자 ID가 null입니다")
                    Timber.e("UI 상태를 Error로 설정: 사용자 ID null")
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
                Timber.d("캐릭터 API 호출 결과: $characterResult")

                characterResult
                    .onSuccess {
                        character = it
                        Timber.d("캐릭터 로드 성공: ${it.nickName}")

                        // 캐릭터 로드 시 착용 상태를 Default로 초기화 (아이템 로드 후 CosmeticItem worn 정보로 업데이트됨)
                        val defaultWearStates = mapOf(
                            EquipSlot.HEAD to WearState.Default,
                            EquipSlot.BODY to WearState.Default,
                            EquipSlot.FEET to WearState.Default
                        )
                        _wornItemsByPosition.value = defaultWearStates
                        previousWornItems = defaultWearStates
                        Timber.d("✅ 캐릭터 로드 시 착용 상태 임시 Default로 초기화 (아이템 로드 후 업데이트 예정)")
                    }
                    .onError { exception, message ->
                        Timber.e(exception, "캐릭터 로드 실패: $message")
                        Timber.e("캐릭터 로드 실패에도 계속 진행 (아이템은 표시 가능)")

                        // 캐릭터 로드 실패 시에도 아이템은 표시할 수 있으므로 계속 진행
                        // _uiState.value = DressingRoomUiState.Error(message ?: "캐릭터 로드 실패")
                        // return@launch // 제거 - 캐릭터 없이도 아이템 표시 가능
                    }

                // 아이템 처리
                val itemsResult = itemsDeferred.await()
                Timber.d("코스메틱 아이템 API 호출 결과: $itemsResult")

                when (itemsResult) {
                    is Result.Success -> {
                        items = itemsResult.data
                        Timber.d("코스메틱 아이템 로드 성공: ${items.size}개")
                    }

                    is Result.Error -> {
                        Timber.e(itemsResult.exception, "코스메틱 아이템 로드 실패: ${itemsResult.message}")
                        Timber.e("UI 상태를 Error로 설정: 코스메틱 아이템 로드 실패")
                        _uiState.value =
                            DressingRoomUiState.Error(itemsResult.message ?: "아이템 로드 실패")
                        return@launch
                    }

                    Result.Loading -> {
                        Timber.d("코스메틱 아이템 로딩 중")
                    }
                }

                // 포인트 처리
                val pointResult = pointDeferred.await()
                Timber.d("포인트 API 호출 결과: $pointResult")

                when (pointResult) {
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
                        Timber.d("포인트 정보 로딩 중 - 기본값 0 사용")
                        userPoint = 0
                    }
                }

                Timber.d("모든 API 호출 완료 - Success 상태로 전환")

                // 초기 Lottie JSON 설정 - 캐릭터 정보와 착용 아이템 정보 모두 활용
                val initialLottieJson = if (character != null) {
                    try {
                        Timber.d("🏠 초기 Lottie JSON 생성 (캐릭터 + 착용 아이템 정보 적용)")
                        cleanBaseJson = loadBaseLottieJson(character) // 깨끗한 baseJson 저장

                        if (cleanBaseJson != null && cleanBaseJson.toString().isNotEmpty()) {
                            Timber.d("✅ 깨끗한 baseJson 로드 및 저장 완료, 길이: ${cleanBaseJson.toString().length}")

                            // 캐릭터 기본 이미지와 착용 아이템 정보를 모두 적용
                            val processedJson = lottieImageProcessor.updateCharacterPartsInLottie(
                                baseLottieJson = cleanBaseJson!!,
                                character = character,
                                progressCallback = null
                            )

                            Timber.d("🎨 초기 Lottie에 캐릭터 정보 적용 완료, 길이: ${processedJson.toString().length}")
                            processedJson.toString()
                        } else {
                            Timber.e("❌ cleanBaseJson이 null이거나 비어있음")
                            null
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "초기 Lottie JSON 설정 실패")
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
                    selectedItemIdSet = LinkedHashSet(wornSet), // 초기에는 착용된 아이템들 선택
                    currentPosition = position,
                    character = character,
                    myPoint = userPoint,
                    processedLottieJson = initialLottieJson,
                    showOwnedOnly = false // 초기에는 전체 아이템 표시
                )
                Timber.d("Success 상태 설정: character=${character?.nickName}, items=${items.size}개, points=$userPoint")
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

                Timber.d("✅ CosmeticItem worn 정보로 착용 상태 초기화: $wornItemsByPosition")

                // 캐릭터 파트별 Lottie 상태 초기화
                if (character != null) {
                    initializeCharacterLottieState(character)
                }

                Timber.d("드레스룸 로딩 완료")
            } catch (t: Throwable) {
                Timber.e(t, "드레스룸 로딩 중 예외 발생")
                _uiState.value = DressingRoomUiState.Error("드레스룸 로딩 실패: ${t.message}")
            } finally {
                _isDressingRoomLoading.value = false
                Timber.d("드레스룸 로딩 상태 해제")
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
                Timber.d("🔄 슬롯 변경 감지: $slot (이전: $previousWearState → 현재: $currentWearState)")
            }
        }

        return changedSlots
    }

    /**
     * 미리보기 착용 상태 토글
     */
    private fun togglePreviewWearState(itemId: Int, position: EquipSlot) {
        Timber.d("🔄 togglePreviewWearState 시작: itemId=$itemId, position=$position")

        val beforeState = _wornItemsByPosition.value
        Timber.d("📊 변경 전 착용 상태: $beforeState")

        val currentPreview = _wornItemsByPosition.value.toMutableMap()
        val currentWearState = currentPreview[position]

        Timber.d("🔍 현재 부위 $position 상태: $currentWearState")

        if (currentWearState is WearState.Worn && currentWearState.itemId == itemId) {
            Timber.d("👕 착용 해제: $position 부위에서 $itemId 제거 → Unworn 상태로")
            // 착용중인 아이템 클릭: 미착용 상태로 변경 (투명 PNG)
            currentPreview[position] = WearState.Unworn
        } else {
            Timber.d("👗 착용: $position 부위에 $itemId 설정")

            // 다른 아이템 착용: Worn 상태로 설정
            currentPreview[position] = WearState.Worn(itemId)
        }

        _wornItemsByPosition.value = currentPreview

        val afterState = _wornItemsByPosition.value
        Timber.d("📊 변경 후 착용 상태: $afterState")

        // Lottie 업데이트는 selectItem에서 호출하도록 함 (중복 방지)
    }

    /**
     * Lottie 미리보기 업데이트 (착용 상태 변경 시 호출)
     */
    private fun updateLottiePreview() {
        Timber.d("🎭 updateLottiePreview 시작")

        val currentState = _uiState.value
        Timber.d("📋 현재 UI 상태: ${currentState::class.simpleName}")

        if (currentState !is DressingRoomUiState.Success || currentState.character == null) {
            Timber.w("❌ Lottie 미리보기 업데이트 건너뜀: Success 상태 아님 또는 캐릭터 없음")
            return
        }

        val currentWornItems = _wornItemsByPosition.value
        Timber.d("✅ UI 상태 확인됨 - 캐릭터: ${currentState.character.nickName}")
        Timber.d("🧷 현재 착용 상태: $currentWornItems")
        Timber.d("🧷 이전 착용 상태: $previousWornItems")

        // 변경된 슬롯만 계산 (diff)
        val changedSlots = calculateChangedSlots(previousWornItems, currentWornItems)
        Timber.d("🔄 변경된 슬롯들: $changedSlots")

        // 변경사항이 없으면 업데이트 스킵
        if (changedSlots.isEmpty()) {
            Timber.d("⚡ 변경사항 없음 - Lottie 업데이트 스킵")
            return
        }

        viewModelScope.launch {
            try {
                Timber.d("🔄 저장된 cleanBaseJson 사용")
                val baseJson =
                    cleanBaseJson ?: loadBaseLottieJson(character = currentState.character)
                Timber.d("📂 Base Lottie JSON 준비 완료 (길이: ${baseJson.toString().length})")

                Timber.d("🔄 Lottie asset 교체 시작")
                // 변경된 슬롯만 선택적으로 교체 (아이템 선택 시에는 정확한 tags 정보 활용)
                val processedJson = lottieImageProcessor.updateAssetsForChangedSlots(
                    baseLottieJson = baseJson,
                    wornItemsByPosition = currentWornItems,
                    cosmeticItems = currentState.items,
                    character = currentState.character,
                    changedSlots = changedSlots
                )
                Timber.d("🔄 Lottie asset 교체 완료 (길이: ${processedJson.toString().length})")

                Timber.d("💾 UI State processedLottieJson 업데이트")
                val processedJsonString = processedJson.toString()
                val newState = currentState.copy(
                    processedLottieJson = processedJsonString
                )
                Timber.d("📊 새 UI State processedLottieJson 길이: ${newState.processedLottieJson?.length}")
                Timber.d("✅ Lottie JSON 업데이트 완료 - UI State에 반영됨")

                // UI State 업데이트 (Lottie JSON만)
                _uiState.value = newState

                // 이전 상태 업데이트
                previousWornItems = currentWornItems.toMap()

                Timber.d("✅ Lottie 미리보기 업데이트 완료 - UI 리컴포지션 대기")
            } catch (t: Throwable) {
                Timber.e(t, "❌ Lottie 미리보기 업데이트 실패")
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
                Timber.d("🎭 캐릭터 Lottie 상태 초기화 시작")
                _characterLottieState.value = LottieCharacterState(baseJson = "", isLoading = true)

                // Base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)
                Timber.d("📂 Base Lottie JSON 로드 완료")

                // 캐릭터 파트별 Lottie JSON 수정
                val modifiedJson =
                    lottieImageProcessor.updateCharacterPartsInLottie(baseJson, character)
                Timber.d("🔄 캐릭터 파트 Lottie JSON 수정 완료")

                // ✅ UI 상태도 업데이트 (캐릭터 기본 이미지 적용)
                if (_uiState.value is DressingRoomUiState.Success) {
                    val currentState = _uiState.value as DressingRoomUiState.Success
                    _uiState.value = currentState.copy(
                        processedLottieJson = modifiedJson.toString()
                    )
                    Timber.d("✅ UI 캐릭터 기본 이미지 적용 완료 - processedLottieJson 길이: ${modifiedJson.toString().length}")
                }

                // 최종 상태 설정
                _characterLottieState.value = LottieCharacterState(
                    baseJson = baseJson.toString(),
                    modifiedJson = modifiedJson.toString(),
                    assets = createCharacterAssetMap(character),
                    isLoading = false
                )

                Timber.d("✅ 캐릭터 Lottie 상태 초기화 완료")
            } catch (t: Throwable) {
                Timber.e(t, "❌ 캐릭터 Lottie 상태 초기화 실패")
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

            Timber.d("🎨 캐릭터 파트 asset 생성: $part -> $assetId (imageName: $imageName)")
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

            Timber.d("🎭 loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

            try {
                Timber.d("📂 Lottie 파일 로드 시도: grade=${character.grade}, resourceId=$resourceId")
                val inputStream = application.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                Timber.d("📄 JSON 문자열 길이: ${jsonString.length}")
                if (jsonString.isEmpty()) {
                    Timber.e("❌ JSON 문자열이 비어있음!")
                    return@withContext JSONObject() // 빈 JSON 반환
                }

                val jsonObject = JSONObject(jsonString)
                Timber.d("✅ JSONObject 생성 성공, 키 개수: ${jsonObject.length()}")

                // 디버깅: 로드된 JSON의 assets 구조 확인
                val assets = jsonObject.optJSONArray("assets")
                if (assets != null) {
                    Timber.d("📋 로드된 Lottie 파일 assets 개수: ${assets.length()}")
//                    for (i in 0 until minOf(assets.length(), 10)) {
//                        val asset = assets.optJSONObject(i)
//                        val id = asset?.optString("id", "unknown")
//                        val w = asset?.optInt("w", 0)
//                        val h = asset?.optInt("h", 0)
//                        Timber.d("📋 Asset[$i]: id=$id, size=${w}x${h}")
//                    }

                    // ⭐ 캐릭터의 기본 이미지를 설정하여 깨끗한 baseJson 생성
                    Timber.d("🔄 캐릭터 기본 이미지 설정 시작")
                    Timber.d("👤 캐릭터 레벨: ${character.level}")

                    // 캐릭터의 기본 이미지로 asset들을 교체
                    val characterBaseJson =
                        lottieImageProcessor.updateCharacterPartsInLottie(jsonObject, character)

                    Timber.d("✅ 캐릭터 기본 이미지 설정 완료")

                    // cleanBaseJson으로 저장
                    cleanBaseJson = characterBaseJson
                } else {
                    Timber.e("❌ 로드된 Lottie 파일에 assets 배열이 없음 - 파일 손상 가능성")
                    // 다른 필드들 확인
                    val keys = jsonObject.keys()
                    Timber.d("📋 JSON에 있는 키들:")
                    while (keys.hasNext()) {
                        Timber.d("  - ${keys.next()}")
                    }
                }

                jsonObject
            } catch (t: Throwable) {
                Timber.e(
                    t,
                    "❌ Base Lottie JSON 로드 실패: grade=${character.grade}, resourceId=$resourceId"
                )
                Timber.e(t, "스택트레이스: ${t.stackTraceToString()}")
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
        Timber.d("🎯 selectItem 호출: itemId=$itemId")

        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val item = currentState.items.find { it.itemId == itemId }
            if (item == null) {
                Timber.w("❌ 아이템을 찾을 수 없음: $itemId")
                return
            }

            Timber.d("📦 아이템 정보: id=$itemId, name=${item.name}, owned=${item.owned}, position=${item.position}")

            // 선택 상태 토글 및 장바구니 동기화 (UiState에서 관리)
            _uiState.update { currentState ->
                if (currentState is DressingRoomUiState.Success) {
                    val currentSelected = currentState.selectedItemIdSet
                    val newSelected = LinkedHashSet(currentSelected)
                    val wasSelected = newSelected.contains(itemId)

                    if (wasSelected) {
                        newSelected.remove(itemId)
                        Timber.d("❌ 선택 해제: $itemId")
                    } else {
                        newSelected.add(itemId)
                        Timber.d("✅ 선택 추가: $itemId")
                    }

                    // 선택 상태 맵 로깅
                    Timber.d("🗺️ selectedItemIdSet 상태: [${newSelected.joinToString(", ")}] (${newSelected.size}개)")

                    // 장바구니 업데이트 (selectedItemIdSet 기반으로 자동 동기화)
                    val updatedCart = LinkedHashSet<CosmeticItem>()
                    newSelected.forEach { selectedId ->
                        val selectedItem = currentState.items.find { it.itemId == selectedId && !it.owned }
                        if (selectedItem != null) {
                            updatedCart.add(selectedItem)
                        }
                    }
                    _cartItems.value = updatedCart

                    currentState.copy(selectedItemIdSet = newSelected)
                } else {
                    currentState
                }
            }

            // 착용 상태 토글 (항상 수행)
            val currentWearState = _wornItemsByPosition.value[item.position]
            val isCurrentlyWorn = currentWearState is WearState.Worn && currentWearState.itemId == itemId

            if (isCurrentlyWorn) {
                // 착용 해제
                Timber.d("👕 착용 해제: $itemId")
                val updatedWornItems = _wornItemsByPosition.value.toMutableMap()
                updatedWornItems[item.position] = WearState.Unworn
                _wornItemsByPosition.value = updatedWornItems
            } else {
                // 착용 수행
                Timber.d("👕 착용 수행: $itemId")
                togglePreviewWearState(itemId, item.position)
            }

            // Lottie 업데이트
            updateLottiePreview()
        } else {
            Timber.w("❌ UI 상태가 Success가 아님: ${currentState::class.simpleName}")
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

        // selectedItemIdSet도 동기화
        _uiState.update { currentState ->
            if (currentState is DressingRoomUiState.Success) {
                val newSelected = LinkedHashSet(newCart.map { it.itemId })
                currentState.copy(selectedItemIdSet = newSelected)
            } else {
                currentState
            }
        }

        Timber.d("장바구니 토글 - itemId: ${item.itemId}, 장바구니: ${newCart.size}개")
    }

    /**
     * 장바구니 비우기 (cartItems 직접 조작)
     */
    fun clearCart() {
        _cartItems.value = LinkedHashSet()
        // UiState에서 관리되므로 별도 초기화 불필요
        Timber.d("장바구니 비움")
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
            val filteredItems = if (positionFilter != null) {
                allItems.filter { it.position == positionFilter } // 특정 position만 필터링
            } else {
                allItems // ALL 선택 시 전체 아이템 표시
            }

            _uiState.value = currentState.copy(
                items = filteredItems,
                currentPosition = position
            )

            Timber.d("포지션 필터 변경: $position → 필터링된 아이템 ${filteredItems.size}개")
        } else {
            Timber.w("UI 상태가 Success가 아님 - 포지션 필터 변경 무시")
        }
    }

    fun toggleShowOwnedOnly() {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val newShowOwnedOnly = !currentState.showOwnedOnly
            val filteredItems = if (newShowOwnedOnly) {
                allItems.filter { it.owned } // 전체 아이템에서 보유 아이템만 필터링
            } else {
                allItems // 전체 아이템 표시
            }
            _uiState.value = currentState.copy(
                items = filteredItems,
                showOwnedOnly = newShowOwnedOnly
            )
        }
    }

    /**
     * 코스메틱 아이템 실제 구매 수행
     *
     * 다이얼로그에서 확인 버튼을 눌렀을 때 호출됨
     */
    fun performPurchase() {
        viewModelScope.launch {
            val items = cartItems.value.toList()
            Timber.d("코스메틱 아이템 실제 구매 시작: ${items.size}개")

            val totalPrice = items.sumOf { it.point }

            when (val result = cosmeticItemRepository.purchaseItems(items, totalPrice)) {
                is Result.Success -> {
                    Timber.d("코스메틱 아이템 구매 성공")

                    // 구매 성공 시 장바구니에서 아이템 제거 및 UI 업데이트
                    val currentCart = _cartItems.value
                    val updatedCart = LinkedHashSet(currentCart.filterNot { cartItem ->
                        items.any { purchasedItem -> purchasedItem.itemId == cartItem.itemId }
                    })

                    _cartItems.value = updatedCart

                    // 구매 성공 시 selectedItemIdSet에서도 제거 (UI 선택 상태 정리)
                    _uiState.update { currentState ->
                        if (currentState is DressingRoomUiState.Success) {
                            val updatedSelected = currentState.selectedItemIdSet.filterNot { selectedId ->
                                items.any { purchased -> purchased.itemId == selectedId }
                            }.toCollection(LinkedHashSet())

                            currentState.copy(selectedItemIdSet = updatedSelected)
                        } else {
                            currentState
                        }
                    }

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

                    // 구매 성공 후 착용 상태 저장
                    Timber.d("구매 성공 - 착용 상태 저장 시작")
                    saveWornItemsToServer()

                    // ✅ 장바구니 다이얼로그 닫기
                    dismissCartDialog()

                    Timber.d("코스메틱 아이템 구매 완료 및 로컬 상태 업데이트")
                }

                is Result.Error -> {
                    Timber.e(result.exception, "코스메틱 아이템 구매 실패")

                    // 실패 시에도 다이얼로그 닫기
                    dismissCartDialog()

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
                    val currentWearState = _wornItemsByPosition.value[item.position]

                    if (currentWearState is WearState.Worn && currentWearState.itemId != itemId) {
                        // 같은 부위에 다른 아이템이 착용되어 있으면 해제
                        Timber.d("같은 부위 아이템 자동 해제: ${currentWearState.itemId}")
                        cosmeticItemRepository.wearItem(currentWearState.itemId, false)
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
                        Timber.e(
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
                saveWornItemsToServer()
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
            Timber.d("🎯 착용 아이템 서버 저장 시작 - 로딩 상태: ${_isWearLoading.value}")

            val previewItems = _wornItemsByPosition.value
            val currentServerWornItems = _serverWornItems.value

            // 변경된 아이템들만 저장 (착용/해제 모두 처리)
            val saveTasks = mutableListOf<suspend () -> Unit>()

            // 각 슬롯별로 변경된 아이템 저장
            EquipSlot.values().forEach { slot ->
                val currentWearState = previewItems[slot]
                val previousWearState = currentServerWornItems[slot]

                // 착용 상태가 변경된 경우
                if (currentWearState != previousWearState) {
                    when (currentWearState) {
                        is WearState.Worn -> {
                            // 새로 착용된 아이템
                            saveTasks.add {
                                Timber.d("$slot 슬롯 아이템 착용: ${currentWearState.itemId}")
                                wearItemInternal(currentWearState.itemId, true)
                            }
                        }
                        WearState.Unworn -> {
                            // 명시적으로 미착용 상태로 설정
                            Timber.d("$slot 슬롯 명시적 미착용")
                        }
                        WearState.Default -> {
                            // 기본 상태로 복원
                            Timber.d("$slot 슬롯 기본 상태로 복원")
                        }
                        null -> {
                            // 상태 없음
                            Timber.d("$slot 슬롯 상태 없음")
                        }
                    }

                    // 이전 상태가 착용중이었다면 해제
                    if (previousWearState is WearState.Worn) {
                        saveTasks.add {
                            Timber.d("$slot 슬롯 아이템 해제: ${previousWearState.itemId}")
                            wearItemInternal(previousWearState.itemId, false)
                        }
                    }
                }
            }

            Timber.d("총 ${saveTasks.size}개 아이템 상태 변경 작업")

            // 모든 저장 작업 실행
            saveTasks.forEach { task ->
                task()
            }

            // 서버 저장 성공 시 서버 착용 상태 업데이트 (UI 반영)
            Timber.d("서버 저장 성공 - 서버 착용 상태 업데이트")
            _serverWornItems.value = previewItems.toMap()

            // 캐릭터 정보 새로고침으로 전체 상태 동기화
            Timber.d("서버 저장 성공 - 캐릭터 정보 새로고침으로 상태 동기화")
            refreshCharacterInfo()

            // 서버 상태와 UI 상태 동기화 완료

            Timber.d("착용 아이템 서버 저장 완료: ${saveTasks.size}개 슬롯")
        } catch (t: Throwable) {
            Timber.e(t, "착용 아이템 서버 저장 실패")
            // TODO: 사용자에게 에러 표시
        } finally {
            Timber.d("🎯 착용 아이템 서버 저장 종료 - 로딩 상태 해제")
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

            Result.Loading -> { /* 무시 */
            }
        }
    }

    /**
     * 캐릭터 정보 새로고침 (착용 상태 변경 후 최신 정보 반영)
     */
    suspend fun refreshCharacterInfo() {
        try {
            Timber.d("캐릭터 정보 refresh 시작")
            _isRefreshLoading.value = true

            // 최신 캐릭터 정보 로드 (항상 API 호출)
            when (val result = characterRepository.getCharacterFromApi()) {
                is Result.Success -> {
                    val updatedCharacter = result.data
                    Timber.d(
                        "캐릭터 정보 refresh 성공: ${updatedCharacter.nickName} : body ${updatedCharacter.bodyImageName},head ${updatedCharacter.headImageName}"
                    )

                    // ✅ 새로운 캐릭터로 Lottie JSON 재생성
                    val updatedLottieJson = loadBaseLottieJson(updatedCharacter)
                    Timber.d("캐릭터 Lottie JSON 재생성 완료: ${updatedLottieJson?.toString()?.length ?: 0} chars")

                    // UI 상태 업데이트 (캐릭터 정보와 Lottie JSON 모두 교체)
                    if (_uiState.value is DressingRoomUiState.Success) {
                        val currentState = _uiState.value as DressingRoomUiState.Success
                        _uiState.value = currentState.copy(
                            character = updatedCharacter,
                            processedLottieJson = updatedLottieJson?.toString()
                        )
                        Timber.d("캐릭터 정보 및 Lottie JSON UI 업데이트 완료")
                    }

                    // DB에도 최신 정보 저장 (향후 빠른 로드를 위해)
                    // userId를 얻어서 저장
                    val userResult = userRepository.getUser()
                    when (userResult) {
                        is Result.Success -> {
                            val userId = userResult.data.userId
                            characterRepository.saveCharacter(userId, updatedCharacter)
                                .onSuccess {
                                    Timber.d("캐릭터 정보 DB 저장 성공: userId=$userId")
                                }
                                .onError { exception, message ->
                                    Timber.e(exception, "캐릭터 정보 DB 저장 실패: userId=$userId, $message")
                                }
                        }
                        else -> {
                            Timber.e("사용자 정보 조회 실패 - 캐릭터 정보 DB 저장 건너뜀")
                        }
                    }
                }

                is Result.Error -> {
                    Timber.e(result.exception, "캐릭터 정보 refresh 실패")
                    // 에러 시에도 계속 진행 (기존 캐릭터 정보 유지)
                }

                Result.Loading -> {
                    // 로딩 상태 무시
                }
            }

        } catch (t: Throwable) {
            Timber.e(t, "캐릭터 정보 refresh 중 예외 발생")
        } finally {
            // 로딩 상태 해제
            _isRefreshLoading.value = false
            Timber.d("캐릭터 정보 refresh 완료")
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

            Timber.d("착용 상태 업데이트 완료: 부위별 착용 아이템 = $updatedWornItems")
        }
    }

}
