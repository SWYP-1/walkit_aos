package swyp.team.walkit.ui.record.friendrecord

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.remote.walking.mapper.FollowerWalkRecordMapper
import swyp.team.walkit.domain.model.FollowerWalkRecord
import swyp.team.walkit.domain.repository.WalkRepository
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.utils.LocationConstants
import timber.log.Timber
import java.util.LinkedHashMap
import javax.inject.Inject
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.Grade
import org.json.JSONObject
import swyp.team.walkit.domain.model.CharacterPart
import swyp.team.walkit.domain.service.LottieProgressCallback

private const val MAX_CACHE_SIZE = 5
private const val LIKE_DEBOUNCE_MS = 500L
private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30분

// 한 팔로워의 산책 기록 하나만 캐시
data class FriendRecordState(
    val record: FollowerWalkRecord,
    val processedLottieJson: String? = null, // 캐시에 Lottie JSON도 포함
    val timestamp: Long = System.currentTimeMillis() // 캐시 생성 시간
)

@HiltViewModel
class FriendRecordViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val locationManager: LocationManager,
    val lottieImageProcessor: LottieImageProcessor, // Lottie 캐릭터 처리를 위해 추가
    private val application: android.app.Application, // 애플리케이션 컨텍스트 추가
    private val savedStateHandle: SavedStateHandle, // 좋아요 상태 유지
) : ViewModel() {

    private val _uiState = MutableStateFlow<FriendRecordUiState>(FriendRecordUiState.Loading)
    val uiState: StateFlow<FriendRecordUiState> = _uiState.asStateFlow()

    private var likeToggleJob: Job? = null

    // LRU 캐시: 최근 MAX_CACHE_SIZE명 저장
    // 키: "${nickname}_${level}_${grade}" (레벨/등급 변경 시 캐시 무효화를 위해 포함)
    private val friendStateCache: LinkedHashMap<String, FriendRecordState> =
        object : LinkedHashMap<String, FriendRecordState>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FriendRecordState>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }

    /**
     * 캐시 키 생성 (nickname, grade, 아이템 정보를 포함)
     */
    private fun createCacheKey(nickname: String, character: Character): String {
        // 아이템 정보를 해시로 포함하여 캐시 무효화
        val itemHash = character.run {
            // 착용 중인 아이템들의 ID를 정렬하여 해시 생성
            val wornItems = listOfNotNull(headImageName, bodyImageName, feetImageName)
                .sorted()
                .joinToString(",")
            wornItems.hashCode().toString()
        }
        return "${nickname}_${character.grade.name}_${itemHash}"
    }

    /**
     * 팔로워 산책 기록 로드
     */
    fun loadFollowerWalkRecord(nickname: String) {
        viewModelScope.launch {
            // 1️⃣ 로딩 상태
            _uiState.value = FriendRecordUiState.Loading

            // 2️⃣ 현재 위치 가져오기
            val currentLocation = try {
                locationManager.getCurrentLocationOrLast()
            } catch (t: Throwable) {
                Timber.w(t, "현재 위치를 가져올 수 없음 - 서울 시청 좌표 사용")
                null
            }

            // 3️⃣ 서버 요청 (위치 정보 포함)
            val result = withContext(Dispatchers.IO) {
                walkRepository.getFollowerWalkRecord(
                    nickname = nickname,
                    lat = currentLocation?.latitude ?: LocationConstants.DEFAULT_LATITUDE,
                    lon = currentLocation?.longitude ?: LocationConstants.DEFAULT_LONGITUDE
                )
            }

            when (result) {
                is Result.Success -> {
                    val record = result.data
                    val character = record.character

                    // 1️⃣ 캐시 키 생성 (아이템 정보 포함)
                    val cacheKey = createCacheKey(nickname, character)

                    // 2️⃣ 캐시 확인 (아이템 정보가 포함된 키로 확인)
                    friendStateCache[cacheKey]?.let { cachedState ->
                        // 캐시 만료 시간 체크
                        val currentTime = System.currentTimeMillis()
                        val isExpired = (currentTime - cachedState.timestamp) > CACHE_EXPIRY_MS

                        if (isExpired) {
                            // 캐시 만료됨: 캐시 삭제 후 서버 데이터 사용
                            friendStateCache.remove(cacheKey)
                            Timber.d("🎭 FriendRecord 캐시 만료: cacheKey=$cacheKey, age=${(currentTime - cachedState.timestamp) / 1000}s")
                        } else {
                            // 캐시 유효: 캐시된 Lottie JSON 사용 (없으면 생성)
                            val lottieJson = cachedState.processedLottieJson
                                ?: generateFriendCharacterLottie(cachedState.record.character)

                            Timber.d("🎭 FriendRecord 캐시 사용: cacheKey=$cacheKey, lottieJson=${lottieJson?.length} characters, age=${(currentTime - cachedState.timestamp) / 1000}s")

                            // SavedStateHandle에서 저장된 상태 우선 사용 (탭 이동 시 유지)
                            val savedIsLiked = savedStateHandle.get<Boolean>("like_state_${nickname}")
                            val savedCount = savedStateHandle.get<Int>("like_count_${nickname}")

                            val finalIsLiked = savedIsLiked ?: cachedState.record.liked
                            val finalCount = savedCount ?: cachedState.record.likeCount

                            Timber.d("🎭 캐시 SavedStateHandle 상태: savedIsLiked=$savedIsLiked, savedCount=$savedCount, 캐시: liked=${cachedState.record.liked}, count=${cachedState.record.likeCount} → 최종: isLiked=$finalIsLiked, count=$finalCount")

                            _uiState.value = FriendRecordUiState.Success(
                                data = cachedState.record,
                                like = LikeUiState(
                                    count = finalCount,
                                    isLiked = finalIsLiked
                                ),
                                processedLottieJson = lottieJson,
                                lottieLoadingProgress = 100 // 캐시에서 불러왔으므로 이미 완료됨
                            )
                            return@launch
                        }
                    }

                    // 3️⃣ 캐시가 없으면 Lottie JSON 생성 및 캐시 저장
                    Timber.d("🎭 FriendRecord Character 데이터: head=${character.headImageName}, body=${character.bodyImageName}, feet=${character.feetImageName}, tag=${character.headImageTag}, level=${character.level}, grade=${character.grade}")

                    // 진행률 콜백을 사용한 Lottie JSON 생성
                    Timber.d("🎯 Lottie 생성 시작 - progressCallback 등록")

                    // 초기 진행률 0%로 설정 (기존 상태 복사)
                    val progressState = _uiState.value as? FriendRecordUiState.Success
                    if (progressState is FriendRecordUiState.Success) {
                        val initialProgressState = progressState.copy(
                            processedLottieJson = null, // 아직 생성 중
                            lottieLoadingProgress = 0
                        )
                        _uiState.value = initialProgressState
                        Timber.d("📊 초기 진행률 설정: 0% (기존 상태 복사)")

                    }

                    var currentProgress = 0
                    val lottieJson = generateFriendCharacterLottie(character, object :
                        LottieProgressCallback {
                        override fun onItemProgress(part: CharacterPart, assetId: String, completed: Boolean) {
                            Timber.d("🎯 onItemProgress 호출됨: part=${part}, assetId=${assetId}, completed=${completed}")
                            if (completed) {
                                currentProgress++
                                // 진행률을 고정된 단계로 설정 (콜백 순서와 무관하게)
                                val progressPercent = when (currentProgress) {
                                    1 -> 25
                                    2 -> 50
                                    3 -> 75
                                    4 -> 100
                                    else -> 100
                                }
                                Timber.d("📊 Lottie 생성 진행률 계산: $progressPercent% ($currentProgress/4)")

                                // 진행률을 UI에 반영 (메인 스레드에서 실행)
                                viewModelScope.launch(Dispatchers.Main) {
                                    // 현재 상태를 가져와서 복사 후 업데이트
                                    val currentState = _uiState.value
                                    if (currentState is FriendRecordUiState.Success) {
                                        val updatedState = currentState.copy(
                                            processedLottieJson = null, // 교체 중이므로 null 유지
                                            lottieLoadingProgress = progressPercent
                                        )
                                        _uiState.value = updatedState
                                        Timber.d("📊 UI 상태 업데이트 완료: lottieLoadingProgress=$progressPercent (기존 상태 복사)")
                                    }
                                }
                            }
                        }

                        override fun onAllItemsCompleted(processedJson: String) {
                            Timber.d("🎉 onAllItemsCompleted 호출됨!")
                            // 최종 상태 업데이트 (기존 상태 복사)
                            val currentState = _uiState.value
                            if (currentState is FriendRecordUiState.Success) {
                                val finalState = currentState.copy(
                                    processedLottieJson = processedJson,
                                    lottieLoadingProgress = 100
                                )
                                _uiState.value = finalState
                                Timber.d("🎉 최종 UI 상태 업데이트 완료 (기존 상태 복사)")

                            }
                        }
                    })
                    Timber.d("🎯 Lottie 생성 완료 - 결과: ${lottieJson?.length} 글자")

                    Timber.d("🎭 FriendRecord Lottie JSON 생성 완료: ${lottieJson?.length} characters")

                    // 4️⃣ 캐시에 저장 (아이템 정보가 포함된 키로 저장, Lottie JSON 포함)
                    friendStateCache[cacheKey] = FriendRecordState(
                        record = record,
                        processedLottieJson = lottieJson
                    )
                    Timber.d("🎭 FriendRecord 캐시 저장: cacheKey=$cacheKey")

                    // 5️⃣ UI 업데이트 (Lottie JSON 포함)
                    // SavedStateHandle에서 저장된 상태 우선 사용 (탭 이동 시 유지)
                    val savedIsLiked = savedStateHandle.get<Boolean>("like_state_${nickname}")
                    val savedCount = savedStateHandle.get<Int>("like_count_${nickname}")

                    val finalIsLiked = savedIsLiked ?: record.liked
                    val finalCount = savedCount ?: record.likeCount

                    Timber.d("🎭 SavedStateHandle 상태: savedIsLiked=$savedIsLiked, savedCount=$savedCount, 서버: liked=${record.liked}, count=${record.likeCount} → 최종: isLiked=$finalIsLiked, count=$finalCount")

                    _uiState.value = FriendRecordUiState.Success(
                        data = record,
                        like = LikeUiState(
                            count = finalCount,
                            isLiked = finalIsLiked
                        ),
                        processedLottieJson = lottieJson,
                        lottieLoadingProgress = 100 // 생성 완료
                    )
                }
                is Result.Error -> {
                    // 서버 에러 코드에 따른 구체적인 UI 처리
                    when (result.exception?.message) {
                        "NOT_FOLLOWING" -> {
                            // 팔로워가 아닌 경우
                            _uiState.value = FriendRecordUiState.NotFollowing(
                                message = result.message ?: "팔로우하고 있지 않습니다"
                            )
                        }
                        "NO_WALK_RECORDS" -> {
                            // 산책 기록이 없는 경우
                            _uiState.value = FriendRecordUiState.NoRecords(
                                message = result.message ?: "산책 기록이 아직 없습니다"
                            )
                        }
                        else -> {
                            // 기타 에러
                            _uiState.value = FriendRecordUiState.Error(
                                result.message ?: "데이터를 불러올 수 없습니다"
                            )
                        }
                    }
                }
                Result.Loading -> {} // 이미 Loading 상태
            }
        }
    }

    /**
     * 좋아요 토글 (Optimistic UI + 서버 응답 처리)
     */
    fun toggleLike() {
        val currentState = _uiState.value as? FriendRecordUiState.Success ?: return
        val walkId = currentState.data.walkId
        val nickname = currentState.data.character.nickName
        val character = currentState.data.character
        val isCurrentlyLiked = currentState.like.isLiked
        val newLikedState = !isCurrentlyLiked

        // 1️⃣ Optimistic UI 업데이트
        _uiState.value = currentState.copy(
            like = currentState.like.copy(
                isLiked = newLikedState,
                count = if (isCurrentlyLiked) (currentState.like.count - 1).coerceAtLeast(0)
                else currentState.like.count + 1
            )
        )

        // 1.5️⃣ SavedStateHandle에 좋아요 상태 저장 (탭 이동 시 유지)
        savedStateHandle["like_state_${nickname}"] = newLikedState
        savedStateHandle["like_count_${nickname}"] = currentState.like.count + (if (isCurrentlyLiked) -1 else 1)

        // 2️⃣ 캐시 업데이트 (Optimistic 상태 반영)
        val cacheKey = createCacheKey(nickname, character)
        friendStateCache[cacheKey]?.let { cachedState ->
            // 캐시에 저장된 record의 liked 상태 업데이트
            val updatedRecord = cachedState.record.copy(liked = newLikedState)
            friendStateCache[cacheKey] = cachedState.copy(record = updatedRecord)
            Timber.d("🎭 캐시 좋아요 상태 업데이트: key=$cacheKey, liked=$newLikedState")
        }

        // 2️⃣ debounce + 서버 호출 + 응답 처리
        likeToggleJob?.cancel()
        likeToggleJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE_MS)
            try {
                val result = withContext(Dispatchers.IO) {
                    if (isCurrentlyLiked) walkRepository.unlikeWalk(walkId)
                    else walkRepository.likeWalk(walkId)
                }

                when (result) {
                    is Result.Success -> {
                        // 성공: Optimistic UI 유지 ✅
                        Timber.d("좋아요 토글 성공: walkId=$walkId")
                    }
                    is Result.Error -> {
                        // 실패: UI 롤백 + 캐시 롤백 + 서버 최신 상태 재로드
                        Timber.e(result.exception, "좋아요 토글 실패, 롤백 및 재로드: walkId=$walkId")

                        // UI 롤백
                        _uiState.value = currentState

                        // SavedStateHandle 롤백
                        savedStateHandle["like_state_${nickname}"] = isCurrentlyLiked
                        savedStateHandle["like_count_${nickname}"] = currentState.like.count

                        // 캐시 롤백
                        val cacheKey = createCacheKey(nickname, character)
                        friendStateCache[cacheKey]?.let { cachedState ->
                            val rolledBackRecord = cachedState.record.copy(
                                liked = isCurrentlyLiked,
                                likeCount = currentState.like.count // count도 롤백
                            )
                            friendStateCache[cacheKey] = cachedState.copy(record = rolledBackRecord)
                            Timber.d("🎭 SavedStateHandle + 캐시 롤백: liked=$isCurrentlyLiked, count=${currentState.like.count}")
                        }

                        // 서버 최신 상태 재로드
                        loadFollowerWalkRecord(nickname)
                    }
                    Result.Loading -> {} // 무시
                }
            } catch (t: Throwable) {
                // 예외: UI 롤백 + 캐시 롤백 + 서버 최신 상태 재로드
                Timber.e(t, "좋아요 토글 예외, 롤백 및 재로드: walkId=$walkId")

                // UI 롤백
                _uiState.value = currentState

                // SavedStateHandle 롤백
                savedStateHandle["like_state_${nickname}"] = isCurrentlyLiked
                savedStateHandle["like_count_${nickname}"] = currentState.like.count

                // 캐시 롤백
                val cacheKey = createCacheKey(nickname, character)
                friendStateCache[cacheKey]?.let { cachedState ->
                    val rolledBackRecord = cachedState.record.copy(
                        liked = isCurrentlyLiked,
                        likeCount = currentState.like.count // count도 롤백
                    )
                    friendStateCache[cacheKey] = cachedState.copy(record = rolledBackRecord)
                    Timber.d("🎭 SavedStateHandle + 캐시 롤백: liked=$isCurrentlyLiked, count=${currentState.like.count}")
                }

                // 서버 최신 상태 재로드
                loadFollowerWalkRecord(nickname)
            }
        }
    }

    /**
     * 친구 캐시 삭제 (nickname으로 시작하는 모든 캐시 키 삭제)
     * 레벨/등급이 변경되어도 이전 캐시가 남아있을 수 있으므로 모두 삭제
     */
    fun deleteFriend(nickname: String) {
        val keysToRemove = friendStateCache.keys.filter { it.startsWith("${nickname}_") }
        keysToRemove.forEach { key ->
            friendStateCache.remove(key)
            Timber.d("🎭 FriendRecord 캐시 삭제: key=$key")
        }
        Timber.d("🎭 FriendRecord 캐시 삭제 완료: nickname=$nickname, 삭제된 키 개수=${keysToRemove.size}")
    }

    /**
     * 친구 캐릭터 Lottie JSON 생성
     */
    private suspend fun generateFriendCharacterLottie(
        character: Character,
        progressCallback: LottieProgressCallback? = null
    ): String? {
        return try {
            withContext(Dispatchers.IO) {
                // 캐릭터 등급에 따른 base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)

                // 캐릭터 파트들을 적용하여 최종 JSON 생성
                val modifiedJson = lottieImageProcessor.updateCharacterPartsInLottie(
                    baseLottieJson = baseJson,
                    character = character,
                    progressCallback = progressCallback
                )

                modifiedJson.toString()
            }
        } catch (t: Throwable) {
            Timber.e(t, "친구 캐릭터 Lottie JSON 생성 실패")
            null
        }
    }

    /**
     * 캐릭터 등급에 따른 base Lottie JSON 로드
     */
    private suspend fun loadBaseLottieJson(character: Character): JSONObject =
        withContext(Dispatchers.IO) {
            val resourceId = when (character.grade) {
                Grade.SEED -> swyp.team.walkit.R.raw.seed
                Grade.SPROUT -> swyp.team.walkit.R.raw.sprout
                Grade.TREE -> swyp.team.walkit.R.raw.tree
            }

            Timber.d("🎭 FriendRecord loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

            try {
                val inputStream = application.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                if (jsonString.isEmpty()) {
                    Timber.e("❌ JSON 문자열이 비어있음!")
                    return@withContext JSONObject() // 빈 JSON 반환
                }

                val jsonObject = JSONObject(jsonString)
                Timber.d("✅ FriendRecord JSONObject 생성 성공")

                jsonObject
            } catch (t: Throwable) {
                Timber.e(t, "❌ FriendRecord base Lottie JSON 로드 실패")
                JSONObject() // 실패 시 빈 JSON 반환
            }
        }
}
