package team.swyp.sdu.ui.dressroom

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
import org.json.JSONObject
import team.swyp.sdu.R
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CharacterPart
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.domain.model.LottieAsset
import team.swyp.sdu.domain.model.LottieCharacterState
import team.swyp.sdu.domain.model.WearState
import team.swyp.sdu.domain.repository.CharacterRepository
import team.swyp.sdu.domain.repository.CosmeticItemRepository
import team.swyp.sdu.domain.repository.PointRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.service.CharacterImageLoader
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.utils.replaceAssetP
import team.swyp.sdu.utils.toBase64DataUrl
import timber.log.Timber
import android.content.SharedPreferences
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

    // 장바구니 상태 (실제 아이템 객체)
    private val _cartItems = MutableStateFlow<LinkedHashSet<CosmeticItem>>(LinkedHashSet())
    val cartItems: StateFlow<LinkedHashSet<CosmeticItem>> = _cartItems.asStateFlow()

    // 선택된 아이템 ID들 (UI 상태와 분리)
    private val _selectedItemIds = MutableStateFlow<LinkedHashSet<Int>>(LinkedHashSet())
    val selectedItemIds: StateFlow<LinkedHashSet<Int>> = _selectedItemIds.asStateFlow()


    // 서버에 반영된 실제 착용 상태
    private val _serverWornItems = MutableStateFlow<Map<EquipSlot, Int>>(emptyMap())
    val serverWornItems: StateFlow<Map<EquipSlot, Int>> = _serverWornItems.asStateFlow()

    // UI 미리보기 착용 상태 (실제 API 반영 전) - 핵심 관리 변수
    private val _wornItemsByPosition = MutableStateFlow<Map<EquipSlot, WearState>>(emptyMap())
    val wornItemsByPosition: StateFlow<Map<EquipSlot, WearState>> = _wornItemsByPosition.asStateFlow()

    // 착용 요청 중 상태 (연속 클릭 방지)
    private val _isWearLoading = MutableStateFlow(false)
    val isWearLoading: StateFlow<Boolean> = _isWearLoading.asStateFlow()

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

    // 착용 상태 로컬 저장용 SharedPreferences
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences(
            "dressing_room_prefs",
            android.content.Context.MODE_PRIVATE
        )
    }


    init {
        loadDressingRoom()
    }

    /**
     * 캐릭터 + 코스메틱 아이템 병렬 로딩
     */
    fun loadDressingRoom(position: String? = null) {
        viewModelScope.launch {
            try {
                Timber.d("드레스룸 로딩 시작 - position: $position")
                _uiState.value = DressingRoomUiState.Loading

                // 사용자 정보 확보
                var userId: String? = null
                val userResult = userRepository.getUser()
                Timber.d("사용자 정보 API 호출 결과: $userResult")

                userResult
                    .onSuccess {
                        userId = it.userId.toString()
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

                        // 캐릭터 로드 시 착용 상태를 Default로 초기화
                        val defaultWearStates = mapOf(
                            EquipSlot.HEAD to WearState.Default,
                            EquipSlot.BODY to WearState.Default,
                            EquipSlot.FEET to WearState.Default
                        )
                        _wornItemsByPosition.value = defaultWearStates
                        previousWornItems = defaultWearStates
                        Timber.d("✅ 캐릭터 로드 시 착용 상태 Default로 초기화")
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

                // 초기 Lottie JSON 설정 (이미 투명 PNG로 교체된 깨끗한 baseJson 사용)
                val initialLottieJson = if (character != null) {
                    try {
                        Timber.d("🏠 초기 Lottie JSON 로드 (투명 PNG 적용됨)")
                        cleanBaseJson = loadBaseLottieJson(character) // 깨끗한 baseJson 저장

                        if (cleanBaseJson != null && cleanBaseJson.toString().isNotEmpty()) {
                            Timber.d("✅ 깨끗한 baseJson 로드 및 저장 완료, 길이: ${cleanBaseJson.toString().length}")

                            // baseJson이 정말 깨끗한지 검증
                            val assets = cleanBaseJson!!.optJSONArray("assets")
                            if (assets != null) {
                                Timber.d("🔍 초기 baseJson assets 검증:")
                                for (i in 0 until minOf(assets.length(), 3)) {
                                    val asset = assets.optJSONObject(i)
                                    val id = asset?.optString("id", "unknown")
                                    val p = asset?.optString("p", "")?.take(50) // data URL 앞부분만
                                    Timber.d("  Asset[$i]: id=$id, p=${p}...")
                                }
                            }

                            cleanBaseJson.toString() // 이미 투명 PNG가 적용된 상태
                        } else {
                            Timber.e("❌ cleanBaseJson이 null이거나 비어있음")
                            null
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "초기 Lottie JSON 설정 실패")
                        null
                    }
                } else {
                    null
                }

                // 전체 아이템 저장 (필터링용)
                allItems = items

                // UI 업데이트 (초기에는 전체 아이템 표시)
                val newSuccessState = DressingRoomUiState.Success(
                    items = items,
                    selectedItemId = null,
                    selectedItemIdSet = LinkedHashSet(),
                    currentPosition = position,
                    character = character,
                    myPoint = userPoint,
                    processedLottieJson = initialLottieJson,
                    showOwnedOnly = false // 초기에는 전체 아이템 표시
                )
                Timber.d("Success 상태 설정: character=${character?.nickName}, items=${items.size}개, points=$userPoint")
                _uiState.value = newSuccessState

                // ✅ 착용 상태 초기화: 로컬 저장된 상태 복원 (동기화 강화)
                val restoredWornItems = loadWornItemsFromLocal()
                _wornItemsByPosition.value = restoredWornItems
                _serverWornItems.value = restoredWornItems // 일단 로컬 상태로 초기화

                // 초기 previousWornItems 설정
                previousWornItems = restoredWornItems.toMap()

                Timber.d("착용 상태 초기화 완료 - 복원된 상태: $restoredWornItems")

                // 캐릭터 파트별 Lottie 상태 초기화
                if (character != null) {
                    initializeCharacterLottieState(character)
                }

                Timber.d("드레스룸 로딩 완료")
            } catch (e: Exception) {
                Timber.e(e, "드레스룸 로딩 중 예외 발생")
                _uiState.value = DressingRoomUiState.Error("드레스룸 로딩 실패: ${e.message}")
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
        EquipSlot.entries.forEach { slot ->
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

        // Lottie 미리보기 업데이트
        Timber.d("🎨 Lottie 미리보기 업데이트 호출")
        updateLottiePreview()
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
                // 변경된 슬롯만 선택적으로 교체
                val processedJson = lottieImageProcessor.updateAssetsForChangedSlots(
                    baseLottieJson = baseJson,
                    wornItemsByPosition = currentWornItems,
                    cosmeticItems = currentState.items,
                    character = currentState.character,
                    changedSlots = changedSlots
                )
                Timber.d("🔄 Lottie asset 교체 완료 (길이: ${processedJson.toString().length})")

                Timber.d("💾 UI State processedLottieJson 업데이트")
                val newState = currentState.copy(
                    processedLottieJson = processedJson.toString()
                )
                Timber.d("📊 새 UI State processedLottieJson 길이: ${newState.processedLottieJson?.length}")

                // UI State 업데이트 (Lottie JSON만)
                _uiState.value = newState

                // 이전 상태 업데이트
                previousWornItems = currentWornItems.toMap()

                Timber.d("✅ Lottie 미리보기 업데이트 완료 - UI 리컴포지션 대기")
            } catch (e: Exception) {
                Timber.e(e, "❌ Lottie 미리보기 업데이트 실패")
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
            } catch (e: Exception) {
                Timber.e(e, "❌ 캐릭터 Lottie 상태 초기화 실패")
                _characterLottieState.value = LottieCharacterState(
                    baseJson = "",
                    modifiedJson = null,
                    assets = emptyMap(),
                    isLoading = false,
                    error = e.message ?: "캐릭터 Lottie 초기화 실패"
                )
            }
        }
    }

    /**
     * 캐릭터 파트별 asset 맵 생성
     */
    private suspend fun createCharacterAssetMap(character: Character): Map<String, LottieAsset> {
        val assetMap = mutableMapOf<String, LottieAsset>()

        CharacterPart.entries.forEach { part ->
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
                    for (i in 0 until minOf(assets.length(), 10)) {
                        val asset = assets.optJSONObject(i)
                        val id = asset?.optString("id", "unknown")
                        val w = asset?.optInt("w", 0)
                        val h = asset?.optInt("h", 0)
                        Timber.d("📋 Asset[$i]: id=$id, size=${w}x${h}")
                    }

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
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "❌ Base Lottie JSON 로드 실패: grade=${character.grade}, resourceId=$resourceId"
                )
                Timber.e(e, "스택트레이스: ${e.stackTraceToString()}")
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

    /**
     * 투명 PNG 생성 함수
     */
    private fun createTransparentPng(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 모든 픽셀을 완전 투명으로 설정
        bitmap.eraseColor(Color.TRANSPARENT)

        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()

        return output.toByteArray()
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

            // 착용중인 아이템인지 확인
            val wearState = _wornItemsByPosition.value[item.position]
            val isCurrentlyWorn = wearState is WearState.Worn && wearState.itemId == itemId

            if (isCurrentlyWorn) {
                // 착용중인 아이템 클릭 → 미리보기 착용 해제
                Timber.d("👕 착용중인 아이템 클릭 - 미리보기 착용 해제: $itemId")
                togglePreviewWearState(itemId, item.position)

                // 착용 해제된 아이템은 선택 상태에서도 제거
                val currentSelectedSet = _selectedItemIds.value
                val newSelectedSet = LinkedHashSet(currentSelectedSet)
                newSelectedSet.remove(itemId)
                _selectedItemIds.value = newSelectedSet

                Timber.d("✅ 착용 해제 완료 - 선택 상태에서도 제거됨: $itemId")
                return
            }

            // 일반적인 선택 토글 로직
            val currentSelectedSet = _selectedItemIds.value
            val newSelectedSet = LinkedHashSet(currentSelectedSet)

            if (newSelectedSet.contains(itemId)) {
                // 선택 해제
                Timber.d("🔄 아이템 선택 해제: $itemId")
                newSelectedSet.remove(itemId)

                // 미소유 아이템이면 장바구니에서도 제거
                if (!item.owned) {
                    removeFromCart(itemId)
                    Timber.i("\"${item.name}\"이(가) 장바구니에서 제거되었습니다")
                }
            } else {
                // 선택 추가
                Timber.d("🔄 아이템 선택 추가: $itemId")
                newSelectedSet.add(itemId)

                // 미소유 아이템이면 장바구니에 추가
                if (!item.owned) {
                    addToCartIfNotOwned(itemId, currentState.items)
                    Timber.i("\"${item.name}\"이(가) 장바구니에 추가되었습니다")
                }
            }

            // 소유한 아이템만 미리보기 착용 토글
            if (item.owned) {
                togglePreviewWearState(itemId, item.position)
            }

            _selectedItemIds.value = newSelectedSet
            Timber.d("✅ 선택 상태 업데이트 완료 - 최종 selectedItemIds: ${_selectedItemIds.value}")
        } else {
            Timber.w("❌ UI 상태가 Success가 아님: ${currentState::class.simpleName}")
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
        Timber.d("🧹 clearSelection 호출됨")
        Timber.d("🧹 clearSelection - 이전 selectedItemIds: ${_selectedItemIds.value}")
        _selectedItemIds.value = LinkedHashSet()
        Timber.d("🧹 clearSelection - 모든 아이템 선택 해제 완료")
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
     * 코스메틱 아이템 구매 요청
     *
     * 카트에 아이템이 있으면 구매 다이얼로그 표시
     */
    fun purchaseItems() {
        Timber.d("🛒 purchaseItems() 호출됨")

        viewModelScope.launch {
            // 이미 작업 중이면 무시
            if (_isWearLoading.value) {
                Timber.d("구매 작업 진행 중 - 무시")
                return@launch
            }

            val currentCartItems = cartItems.value
            Timber.d("🛒 현재 장바구니 상태: ${currentCartItems.size}개 아이템")
            currentCartItems.forEach { item ->
                Timber.d("  - ${item.name} (ID: ${item.itemId})")
            }

            if (currentCartItems.isNotEmpty()) {
                // 카트에 아이템이 있으면 구매 다이얼로그 표시
                Timber.d("카트에 아이템 존재 - 구매 다이얼로그 표시: ${currentCartItems.size}개")
                _showCartDialog.value = true
                Timber.d("다이얼로그 상태 설정: true")
            } else {
                // 카트가 비어있으면 아무 작업도 하지 않음
                Timber.d("카트가 비어있음 - 구매할 아이템 없음")
            }
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
                    val updatedCart = currentCart.filterNot { cartItem ->
                        items.any { purchasedItem -> purchasedItem.itemId == cartItem.itemId }
                    }.toCollection(LinkedHashSet())

                    _cartItems.value = updatedCart

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

                    // 캐릭터 정보 백그라운드 동기화 (선택사항)
                    viewModelScope.launch {
                        refreshCharacterInfo()
                    }

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
            EquipSlot.entries.forEach { slot ->
                val currentItemId = previewItems[slot]
                val previousItemId = currentServerWornItems[slot]

                // 아이템이 변경되었거나 해제된 경우
                if (currentItemId != previousItemId) {
                    if (currentItemId != null) {
                        // 새로 착용된 아이템
                        saveTasks.add {
                            Timber.d("$slot 슬롯 아이템 착용: $currentItemId")
                            wearItemInternal(currentItemId, true)
                        }
                    }

                    if (previousItemId != null) {
                        // 착용 해제된 아이템
                        saveTasks.add {
                            Timber.d("$slot 슬롯 아이템 해제: $previousItemId")
                            wearItemInternal(previousItemId, false)
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

            // ✅ 로컬 상태도 서버 상태와 동기화 (동기화 강화)
            saveWornItemsToLocal(previewItems)

            Timber.d("착용 아이템 서버 저장 완료: ${saveTasks.size}개 슬롯")
        } catch (e: Exception) {
            Timber.e(e, "착용 아이템 서버 저장 실패")
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
                            val userId = userResult.data.userId.toString()
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
        } catch (e: Exception) {
            Timber.e(e, "캐릭터 정보 refresh 중 예외 발생")
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

            // ✅ 로컬에 착용 상태 저장 (동기화 강화)
            saveWornItemsToLocal(updatedWornItems)

            Timber.d("착용 상태 업데이트 완료: 부위별 착용 아이템 = $updatedWornItems")
        }
    }

    /**
     * 착용 상태를 로컬 SharedPreferences에 저장
     */
    private fun saveWornItemsToLocal(wornItems: Map<EquipSlot, WearState>) {
        try {
            val editor = prefs.edit()
            // 각 슬롯별로 저장
            EquipSlot.entries.forEach { slot ->
                val wearState = wornItems[slot]
                val key = "worn_item_${slot.name.lowercase()}"
                if (wearState is WearState.Worn) {
                    editor.putInt(key, wearState.itemId)
                } else {
                    editor.remove(key) // 착용중이 아니면 키 제거
                }
            }
            editor.apply()
            Timber.d("착용 상태 로컬 저장 완료: $wornItems")
        } catch (e: Exception) {
            Timber.e(e, "착용 상태 로컬 저장 실패")
        }
    }

    /**
     * 로컬 SharedPreferences에서 착용 상태 복원
     */
    private fun loadWornItemsFromLocal(): Map<EquipSlot, WearState> {
        val wornItems = mutableMapOf<EquipSlot, WearState>()
        try {
            EquipSlot.entries.forEach { slot ->
                val key = "worn_item_${slot.name.lowercase()}"
                val itemId = prefs.getInt(key, -1)
                if (itemId != -1) {
                    wornItems[slot] = WearState.Worn(itemId)
                } else {
                    // 저장된 아이템이 없으면 Default 상태로 설정
                    wornItems[slot] = WearState.Default
                }
            }
            Timber.d("착용 상태 로컬 복원 완료: $wornItems")
        } catch (e: Exception) {
            Timber.e(e, "착용 상태 로컬 복원 실패")
            // 오류 시 모든 슬롯을 Default로 설정
            EquipSlot.entries.forEach { slot ->
                wornItems[slot] = WearState.Default
            }
        }
        return wornItems
    }
}
