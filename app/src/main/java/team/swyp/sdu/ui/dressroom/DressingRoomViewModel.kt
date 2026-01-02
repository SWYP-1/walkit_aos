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

    private val _showOwnedOnly = MutableStateFlow(false)
    val showOwnedOnly: StateFlow<Boolean> = _showOwnedOnly.asStateFlow()

    // 서버에 반영된 실제 착용 상태
    private val _serverWornItems = MutableStateFlow<Map<EquipSlot, Int>>(emptyMap())
    val serverWornItems: StateFlow<Map<EquipSlot, Int>> = _serverWornItems.asStateFlow()

    // UI 미리보기 착용 상태 (실제 API 반영 전) - 핵심 관리 변수
    private val _wornItemsByPosition = MutableStateFlow<Map<EquipSlot, Int>>(emptyMap())
    val wornItemsByPosition: StateFlow<Map<EquipSlot, Int>> = _wornItemsByPosition.asStateFlow()

    // 착용 요청 중 상태 (연속 클릭 방지)
    private val _isWearLoading = MutableStateFlow(false)
    val isWearLoading: StateFlow<Boolean> = _isWearLoading.asStateFlow()

    // 장바구니 다이얼로그 표시 상태
    private val _showCartDialog = MutableStateFlow(false)
    val showCartDialog: StateFlow<Boolean> = _showCartDialog.asStateFlow()

    // 이전 착용 상태 (diff 계산용)
    private var previousWornItems = mapOf<EquipSlot, Int>()

    // 캐릭터 파트별 Lottie 상태 (캐릭터 기본 파트 표시용)
    private val _characterLottieState = MutableStateFlow<LottieCharacterState?>(null)
    val characterLottieState: StateFlow<LottieCharacterState?> = _characterLottieState.asStateFlow()

    // 투명 PNG가 적용된 깨끗한 baseJson (재사용을 위해 저장)
    private var cleanBaseJson: JSONObject? = null

    // 착용 상태 로컬 저장용 SharedPreferences
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("dressing_room_prefs", android.content.Context.MODE_PRIVATE)
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
                var nickname: String? = null
                val userResult = userRepository.getUser()
                Timber.d("사용자 정보 API 호출 결과: $userResult")

                userResult
                    .onSuccess {
                        nickname = it.nickname
                        Timber.d("사용자 정보 로드 성공: $nickname")
                    }
                    .onError { exception, message ->
                        Timber.e(exception, "사용자 정보 로드 실패: $message")
                        Timber.e("UI 상태를 Error로 설정: 사용자 정보 로드 실패")
                        _uiState.value = DressingRoomUiState.Error(message ?: "사용자 정보 로드 실패")
                        return@launch
                    }

                if (nickname == null) {
                    Timber.e("사용자 정보가 null입니다")
                    Timber.e("UI 상태를 Error로 설정: 사용자 정보 null")
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
                val characterResult = characterDeferred.await()
                Timber.d("캐릭터 API 호출 결과: $characterResult")

                characterResult
                    .onSuccess {
                        character = it
                        Timber.d("캐릭터 로드 성공: ${it.nickName}")
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
                    availablePositions = listOf("HEAD", "BODY", "FEET"),
                    character = character,
                    myPoint = userPoint,
                    processedLottieJson = initialLottieJson
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
        previous: Map<EquipSlot, Int>,
        current: Map<EquipSlot, Int>
    ): Set<EquipSlot> {
        val changedSlots = mutableSetOf<EquipSlot>()

        // 모든 슬롯에 대해 비교
        EquipSlot.entries.forEach { slot ->
            val previousItemId = previous[slot]
            val currentItemId = current[slot]

            if (previousItemId != currentItemId) {
                changedSlots.add(slot)
                Timber.d("🔄 슬롯 변경 감지: $slot (이전: $previousItemId → 현재: $currentItemId)")
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
        val isCurrentlyWorn = currentPreview[position] == itemId

        Timber.d("🔍 현재 부위 $position 상태: ${currentPreview[position]}, 착용 여부: $isCurrentlyWorn")

        if (isCurrentlyWorn) {
            Timber.d("👕 착용 해제: $position 부위에서 $itemId 제거")
            // 착용 해제: 해당 부위에서 제거
            currentPreview.remove(position)
        } else {
            Timber.d("👗 착용: $position 부위에 $itemId 설정")
            // 착용: 해당 부위에 설정 (다른 아이템은 자동 해제)
            currentPreview[position] = itemId
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

                    // ⭐ CharacterPart 레벨에서 모든 Lottie asset들을 투명 PNG로 교체
                    Timber.d("🔄 CharacterPart 레벨 투명 PNG 교체 시작")
                    Timber.d("👤 캐릭터 레벨: ${character.level}")

                    var replacedCount = 0
                    var skippedCount = 0

                    // CharacterPart의 모든 asset ID들을 투명화
                    for (part in CharacterPart.entries) {
                        Timber.d("🔍 파트 투명 교체 시도: $part")

                        // 각 파트의 모든 lottieAssetIds를 투명화
                        for (assetId in part.lottieAssetIds) {
                            Timber.d("🎯 Asset 투명 교체 시도: $assetId")

                            try {
                                // 투명 PNG 생성 및 교체
                                val transparentPng = createTransparentPng(256, 256)
                                val dataUrl = transparentPng.toBase64DataUrl()
                                jsonObject.replaceAssetP(assetId, dataUrl)
                                Timber.d("✅ 투명 PNG 교체 성공: $assetId")
                                replacedCount++
                            } catch (e: IllegalArgumentException) {
                                Timber.w("⚠️ Asset을 찾을 수 없음, 투명 교체 건너뜀: $assetId")
                                skippedCount++
                            } catch (e: Exception) {
                                Timber.e(e, "❌ 투명 교체 중 예외 발생: $assetId")
                                skippedCount++
                            }
                        }
                    }

                    Timber.d("🎉 CharacterPart 투명 PNG 교체 완료: 성공=$replacedCount, 건너뜀=$skippedCount")

                    if (replacedCount == 0) {
                        Timber.e("❌ 모든 슬롯 투명 교체 실패! baseJson이 깨끗하지 않음")
                    } else if (replacedCount < CharacterPart.entries.size) {
                        Timber.w("⚠️ 일부 슬롯만 투명 교체 성공 (${replacedCount}/${CharacterPart.entries.size})")
                    } else {
                        Timber.d("✅ 모든 슬롯 투명 교체 성공 - 깨끗한 baseJson 생성됨")
                    }
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

            // 소유한 아이템만 미리보기 착용 토글 (미소유 아이템은 장바구니 선택만)
            togglePreviewWearState(itemId, item.position)

            // 선택 상태 관리
            val currentSelectedSet = _selectedItemIds.value
            val newSelectedSet = LinkedHashSet(currentSelectedSet)
            Timber.d("🔄 선택 상태 관리 시작 - 현재 selectedItemIds: $currentSelectedSet")

            if (item.owned) {
                Timber.d("✅ 소유한 아이템 - 착용 상태에 따른 선택 상태 자동 업데이트")
                // 소유한 아이템: 착용 상태에 따라 선택 상태가 자동으로 결정됨
                // UI에서 착용된 아이템만 선택 상태로 표시
                val isCurrentlyWorn = _wornItemsByPosition.value[item.position] == itemId

                if (isCurrentlyWorn) {
                    newSelectedSet.add(itemId)
                    Timber.d("착용된 아이템 선택 상태 추가: $itemId")
                } else {
                    newSelectedSet.remove(itemId)
                    Timber.d("착용 해제된 아이템 선택 상태 제거: $itemId")
                }
            } else {
                Timber.d("🛒 미소유 아이템 - 장바구니 선택 토글")
                // 미소유 아이템: 장바구니 선택 토글
                val wasSelected = newSelectedSet.contains(itemId)

                if (wasSelected) {
                    // 선택 해제: 장바구니에서 제거
                    Timber.d("🛒 미소유 아이템 선택 해제 - 장바구니에서 제거")
                    newSelectedSet.remove(itemId)
                    removeFromCart(itemId)
                    Timber.i("\"${item.name}\"이(가) 장바구니에서 제거되었습니다")
                } else {
                    // 선택: 장바구니에 담기
                    Timber.d("🛒 미소유 아이템 선택 - 장바구니에 담기")
                    newSelectedSet.add(itemId)
                    addToCartIfNotOwned(itemId, currentState.items)
                    Timber.i("\"${item.name}\"이(가) 장바구니에 추가되었습니다")
                }
                Timber.d("장바구니 아이템 선택: $itemId, 선택됨: ${!wasSelected}, 현재 선택 Set: $newSelectedSet")
            }

            Timber.d("🔄 선택 상태 업데이트 - 이전: $currentSelectedSet, 새로: $newSelectedSet")
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
        _showOwnedOnly.value = !_showOwnedOnly.value
        // UI 갱신
        refreshFilteredItems()
    }

    private fun refreshFilteredItems() {
        val currentState = _uiState.value
        if (currentState is DressingRoomUiState.Success) {
            val filtered = if (_showOwnedOnly.value) {
                allItems.filter { it.owned } // 전체 아이템에서 보유 아이템만 필터링
            } else {
                allItems // 전체 아이템 표시
            }
            _uiState.value = currentState.copy(
                items = filtered
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

                    // 구매 성공 후 착용 상태 저장
                    Timber.d("구매 성공 - 착용 상태 저장 시작")
                    saveWornItemsToServer()

                    // ✅ 장바구니 다이얼로그 닫기
                    dismissCartDialog()


                    // ✅ 저장하기 완료 시 캐릭터 정보 동기화 (백그라운드)
                    viewModelScope.launch {
                        refreshCharacterInfo()
                    }

                    Timber.d("코스메틱 아이템 구매 완료 및 UI 업데이트")
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
            Timber.d("착용 아이템 서버 저장 시작")

            val previewItems = _wornItemsByPosition.value

            // 각 슬롯별 착용 아이템 저장
            val saveTasks = mutableListOf<suspend () -> Unit>()

            previewItems[EquipSlot.HEAD]?.let { itemId ->
                saveTasks.add {
                    Timber.d("HEAD 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            previewItems[EquipSlot.BODY]?.let { itemId ->
                saveTasks.add {
                    Timber.d("BODY 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            previewItems[EquipSlot.FEET]?.let { itemId ->
                saveTasks.add {
                    Timber.d("FEET 슬롯 아이템 저장: $itemId")
                    wearItemInternal(itemId, true)
                }
            }

            // 모든 저장 작업 실행
            saveTasks.forEach { task ->
                task()
            }

            // 서버 저장 성공 시 서버 상태 업데이트
            _serverWornItems.value = previewItems.toMap()

            // ✅ 로컬 상태도 서버 상태와 동기화 (동기화 강화)
            saveWornItemsToLocal(previewItems)

            Timber.d("착용 아이템 서버 저장 완료: ${saveTasks.size}개 슬롯")
        } catch (e: Exception) {
            Timber.e(e, "착용 아이템 서버 저장 실패")
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
                    Timber.d("캐릭터 정보 refresh 성공: ${updatedCharacter.nickName}")

                    // UI 상태 업데이트 (캐릭터 정보만 교체)
                    if (_uiState.value is DressingRoomUiState.Success) {
                        val currentState = _uiState.value as DressingRoomUiState.Success
                        _uiState.value = currentState.copy(character = updatedCharacter)
                        Timber.d("캐릭터 정보 UI 업데이트 완료")
                    }

                    // DB에도 최신 정보 저장 (향후 빠른 로드를 위해)
                    characterRepository.saveCharacter(updatedCharacter.nickName, updatedCharacter)
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
    private fun saveWornItemsToLocal(wornItems: Map<EquipSlot, Int>) {
        try {
            val editor = prefs.edit()
            // 각 슬롯별로 저장
            EquipSlot.entries.forEach { slot ->
                val itemId = wornItems[slot]
                val key = "worn_item_${slot.name.lowercase()}"
                if (itemId != null) {
                    editor.putInt(key, itemId)
                } else {
                    editor.remove(key) // null이면 키 제거
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
    private fun loadWornItemsFromLocal(): Map<EquipSlot, Int> {
        val wornItems = mutableMapOf<EquipSlot, Int>()
        try {
            EquipSlot.entries.forEach { slot ->
                val key = "worn_item_${slot.name.lowercase()}"
                val itemId = prefs.getInt(key, -1)
                if (itemId != -1) {
                    wornItems[slot] = itemId
                }
            }
            Timber.d("착용 상태 로컬 복원 완료: $wornItems")
        } catch (e: Exception) {
            Timber.e(e, "착용 상태 로컬 복원 실패")
        }
        return wornItems
    }
}
