package swyp.team.walkit.ui.record.friendrecord

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

private const val MAX_CACHE_SIZE = 5
private const val LIKE_DEBOUNCE_MS = 500L

// 한 팔로워의 산책 기록 하나만 캐시
data class FriendRecordState(
    val record: FollowerWalkRecord,
    val processedLottieJson: String? = null // 캐시에 Lottie JSON도 포함
)

@HiltViewModel
class FriendRecordViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val locationManager: LocationManager,
    val lottieImageProcessor: LottieImageProcessor, // Lottie 캐릭터 처리를 위해 추가
    private val application: android.app.Application, // 애플리케이션 컨텍스트 추가
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
     * 캐시 키 생성 (nickname, level, grade를 포함)
     */
    private fun createCacheKey(nickname: String, level: Int, grade: Grade): String {
        return "${nickname}_${level}_${grade.name}"
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

                    // 1️⃣ 캐시 키 생성 (level과 grade 포함)
                    val cacheKey = createCacheKey(nickname, character.level, character.grade)

                    // 2️⃣ 캐시 확인 (레벨/등급이 포함된 키로 확인)
                    friendStateCache[cacheKey]?.let { cachedState ->
                        // 캐시된 Lottie JSON 사용 (없으면 생성)
                        val lottieJson = cachedState.processedLottieJson
                            ?: generateFriendCharacterLottie(cachedState.record.character)

                        Timber.d("🎭 FriendRecord 캐시 사용: cacheKey=$cacheKey, lottieJson=${lottieJson?.length} characters")

                        _uiState.value = FriendRecordUiState.Success(
                            data = cachedState.record,
                            like = LikeUiState(
                                count = cachedState.record.likeCount,
                                isLiked = cachedState.record.liked
                            ),
                            processedLottieJson = lottieJson
                        )
                        return@launch
                    }

                    // 3️⃣ 캐시가 없으면 Lottie JSON 생성 및 캐시 저장
                    Timber.d("🎭 FriendRecord Character 데이터: head=${character.headImageName}, body=${character.bodyImageName}, feet=${character.feetImageName}, tag=${character.headImageTag}, level=${character.level}, grade=${character.grade}")
                    val lottieJson = generateFriendCharacterLottie(character)
                    Timber.d("🎭 FriendRecord Lottie JSON 생성 완료: ${lottieJson?.length} characters")

                    // 4️⃣ 캐시에 저장 (레벨/등급이 포함된 키로 저장, Lottie JSON 포함)
                    friendStateCache[cacheKey] = FriendRecordState(
                        record = record,
                        processedLottieJson = lottieJson
                    )
                    Timber.d("🎭 FriendRecord 캐시 저장: cacheKey=$cacheKey")

                    // 5️⃣ UI 업데이트 (Lottie JSON 포함)
                    _uiState.value = FriendRecordUiState.Success(
                        data = record,
                        like = LikeUiState(
                            count = record.likeCount,
                            isLiked = record.liked
                        ),
                        processedLottieJson = lottieJson
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
     * 좋아요 토글 (Optimistic UI + debounce)
     */
    fun toggleLike() {
        val currentState = _uiState.value as? FriendRecordUiState.Success ?: return
        val walkId = currentState.data.walkId
        val isCurrentlyLiked = currentState.like.isLiked

        // 1️⃣ Optimistic UI 업데이트
        _uiState.value = currentState.copy(
            like = currentState.like.copy(
                isLiked = !isCurrentlyLiked,
                count = if (isCurrentlyLiked) (currentState.like.count - 1).coerceAtLeast(0)
                else currentState.like.count + 1
            )
        )

        // 2️⃣ debounce
        likeToggleJob?.cancel()
        likeToggleJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE_MS)
            val result = withContext(Dispatchers.IO) {
                if (isCurrentlyLiked) walkRepository.unlikeWalk(walkId)
                else walkRepository.likeWalk(walkId)
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
    private suspend fun generateFriendCharacterLottie(character: Character): String? {
        return try {
            withContext(Dispatchers.IO) {
                // 캐릭터 등급에 따른 base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)

                // 캐릭터 파트들을 적용하여 최종 JSON 생성
                val modifiedJson = lottieImageProcessor.updateCharacterPartsInLottie(
                    baseLottieJson = baseJson,
                    character = character
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
