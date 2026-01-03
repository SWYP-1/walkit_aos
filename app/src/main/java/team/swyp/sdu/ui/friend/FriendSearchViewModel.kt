package team.swyp.sdu.ui.friend

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.friend.FollowRemoteDataSource
import team.swyp.sdu.data.remote.user.AlreadyFollowingException
import team.swyp.sdu.data.remote.user.FollowRequestAlreadyExistsException
import team.swyp.sdu.data.remote.user.FollowSelfException
import team.swyp.sdu.data.remote.user.FollowUserNotFoundException
import team.swyp.sdu.data.remote.walking.mapper.FollowerWalkRecordMapper
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.WalkRepository
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.utils.LocationConstants
import timber.log.Timber
import android.content.SharedPreferences
import javax.inject.Inject
import team.swyp.sdu.domain.model.Grade
import org.json.JSONObject

/**
 * 친구 검색 상세 화면 ViewModel
 */
@HiltViewModel
class FriendSearchViewModel
@Inject
constructor(
    private val application: Application,
    private val walkRepository: WalkRepository,
    private val userRepository: UserRepository,
    private val followRemoteDataSource: FollowRemoteDataSource,
    private val locationManager: LocationManager,
    val lottieImageProcessor: LottieImageProcessor, // Lottie 캐릭터 처리를 위해 추가
) : ViewModel() {
    private val _uiState = MutableStateFlow<FriendSearchUiState>(FriendSearchUiState.Loading)
    val uiState: StateFlow<FriendSearchUiState> = _uiState.asStateFlow()

    // 팔로우 요청 중 상태
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    // 현재 조회 중인 사용자의 팔로우 상태
    private val _followStatus = MutableStateFlow<FollowStatus>(FollowStatus.EMPTY)
    val followStatus: StateFlow<FollowStatus> = _followStatus.asStateFlow()

    // 팔로우 상태 로컬 저장용 SharedPreferences
    private val followPrefs: SharedPreferences by lazy {
        application.getSharedPreferences("follow_status_prefs", android.content.Context.MODE_PRIVATE)
    }

    /**
     * 팔로워 산책 기록 조회
     *
     * @param nickname 팔로워 닉네임 (null이면 내 최근 정보 조회)
     * @param lat 위도 (선택사항)
     * @param lon 경도 (선택사항)
     */
    fun loadFollowerWalkRecord(
        nickname: String,
    ) {
        viewModelScope.launch {
            _uiState.value = FriendSearchUiState.Loading

            // 3️⃣ 현재 위치 가져오기
            val currentLocation = try {
                locationManager.getCurrentLocationOrLast()
            } catch (e: Exception) {
                Timber.w(e, "현재 위치를 가져올 수 없음 - 서울 시청 좌표 사용")
                null
            }

            withContext(Dispatchers.IO) {
                val result = userRepository.getUserSummaryByNickname(
                    nickname = nickname,
                    lat = currentLocation?.latitude ?: LocationConstants.DEFAULT_LATITUDE,
                    lon = currentLocation?.longitude ?: LocationConstants.DEFAULT_LONGITUDE
                )

                _uiState.value = when (result) {
                    is Result.Success -> {
                        val data = result.data

                        // Lottie 캐릭터 JSON 생성
                        val lottieJson = generateFriendSearchCharacterLottie(data.character)

                        // 팔로우 상태는 네비게이션 파라미터에서 이미 설정됨
                        Timber.d("FriendSearchViewModel.loadFollowerWalkRecord: $nickname 팔로우 상태 이미 설정됨 - ${_followStatus.value}")
                        FriendSearchUiState.Success(
                            data = data,
                            processedLottieJson = lottieJson
                        )
                    }

                    is Result.Error -> FriendSearchUiState.Error(result.message)
                    Result.Loading -> FriendSearchUiState.Loading
                }
            }
        }
    }

    /**
     * 팔로우 상태 설정 (네비게이션 파라미터에서 전달받은 값 사용)
     */
    fun setFollowStatus(followStatus: FollowStatus) {
        _followStatus.value = followStatus
        Timber.d("FriendSearchViewModel.setFollowStatus: $followStatus")
    }

    /**
     * 닉네임으로 사용자 팔로우 (Optimistic UI 패턴)
     *
     * 버튼 클릭 시 즉시 UI를 업데이트하고, 서버 요청 실패 시에만 롤백합니다.
     * FriendViewModel과의 상태 동기화를 위해 로컬에 저장합니다.
     *
     * @param nickname 팔로우할 사용자의 닉네임
     */
    fun followUser(nickname: String) {
        val trimmedNickname = nickname.trim()
        Timber.d("FriendSearchViewModel.followUser called: nickname=$trimmedNickname, currentStatus=${_followStatus.value}")

        if (trimmedNickname.isBlank() || _isFollowing.value) {
            Timber.d("FriendSearchViewModel.followUser: early return - blank or already following")
            return
        }

        // 이미 팔로우 중이거나 팔로잉 상태면 처리하지 않음
        if (_followStatus.value == FollowStatus.ACCEPTED ||
            _followStatus.value == FollowStatus.PENDING
        ) {
            Timber.d("FriendSearchViewModel.followUser: already in follow status, skipping")
            return
        }

        // 현재 상태 저장 (롤백용)
        val previousFollowStatus = _followStatus.value
        Timber.d("FriendSearchViewModel.followUser: previous status = $previousFollowStatus")

        // Optimistic UI: 즉시 팔로우 상태 업데이트 (PENDING 상태로 변경)
        _followStatus.value = FollowStatus.PENDING
        Timber.d("FriendSearchViewModel.followUser: status updated to PENDING")
        // 로컬에 팔로우 상태 저장
        saveFollowStatusToLocal(trimmedNickname, FollowStatus.PENDING)

        // 버튼 비활성화
        _isFollowing.value = true

        // 서버 요청 (백그라운드)
        viewModelScope.launch {
            try {
                followRemoteDataSource.followUserByNickname(trimmedNickname)
                Timber.d("팔로우 성공: $trimmedNickname")
                // 성공 시 이미 UI가 업데이트되어 있으므로 추가 작업 없음
            } catch (e: FollowUserNotFoundException) {
                Timber.e(e, "존재하지 않는 유저: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousFollowStatus)
            } catch (e: FollowSelfException) {
                Timber.e(e, "자기 자신에게 팔로우: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousFollowStatus)
            } catch (e: FollowRequestAlreadyExistsException) {
                Timber.e(e, "이미 보낸 팔로우 신청: $trimmedNickname")
                // 이미 PENDING 상태이므로 롤백 불필요 (상태 유지)
            } catch (e: AlreadyFollowingException) {
                Timber.e(e, "이미 팔로우 중: $trimmedNickname")
                // 이미 팔로우 중이므로 ACCEPTED 상태로 변경
                _followStatus.value = FollowStatus.ACCEPTED
                saveFollowStatusToLocal(trimmedNickname, FollowStatus.ACCEPTED)
            } catch (e: Exception) {
                Timber.e(e, "팔로우 실패: $trimmedNickname")
                // 롤백: 이전 상태로 복원
                rollbackFollowStatus(previousFollowStatus)
            } finally {
                _isFollowing.value = false
            }
        }
    }

    /**
     * 팔로우 상태 롤백
     */
    private fun rollbackFollowStatus(previousFollowStatus: FollowStatus?) {
        if (previousFollowStatus != null) {
            _followStatus.value = previousFollowStatus
            // 롤백 시에도 로컬에 저장
            val currentNickname = (uiState.value as? FriendSearchUiState.Success)?.data?.character?.nickName
            if (currentNickname != null) {
                saveFollowStatusToLocal(currentNickname, previousFollowStatus)
            }
        }
    }

    /**
     * 팔로우 상태를 로컬 SharedPreferences에 저장
     */
    private fun saveFollowStatusToLocal(nickname: String, status: FollowStatus) {
        try {
            followPrefs.edit()
                .putString("follow_status_$nickname", status.name)
                .apply()
            Timber.d("팔로우 상태 로컬 저장: $nickname -> $status")
        } catch (e: Exception) {
            Timber.e(e, "팔로우 상태 로컬 저장 실패: $nickname")
        }
    }

    /**
     * 로컬 SharedPreferences에서 팔로우 상태 복원
     */
    private fun loadFollowStatusFromLocal(nickname: String): FollowStatus {
        return try {
            val key = "follow_status_$nickname"
            val statusString = followPrefs.getString(key, null)
            Timber.d("FriendSearchViewModel: loading follow status, key=$key, statusString=$statusString")

            val status = statusString?.let { FollowStatus.valueOf(it) } ?: FollowStatus.EMPTY
            Timber.d("팔로우 상태 로컬 로드: $nickname -> $status")
            status
        } catch (e: Exception) {
            Timber.e(e, "팔로우 상태 로컬 로드 실패: $nickname")
            FollowStatus.EMPTY
        }
    }

    /**
     * 친구 검색 캐릭터 Lottie JSON 생성
     */
    private suspend fun generateFriendSearchCharacterLottie(character: team.swyp.sdu.domain.model.Character): String? {
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
        } catch (e: Exception) {
            Timber.e(e, "친구 검색 캐릭터 Lottie JSON 생성 실패")
            null
        }
    }

    /**
     * 캐릭터 등급에 따른 base Lottie JSON 로드
     */
    private suspend fun loadBaseLottieJson(character: team.swyp.sdu.domain.model.Character): JSONObject =
        withContext(Dispatchers.IO) {
            val resourceId = when (character.grade) {
                Grade.SEED -> team.swyp.sdu.R.raw.seed
                Grade.SPROUT -> team.swyp.sdu.R.raw.sprout
                Grade.TREE -> team.swyp.sdu.R.raw.tree
            }

            Timber.d("🎭 FriendSearch loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

            try {
                val inputStream = application.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                if (jsonString.isEmpty()) {
                    Timber.e("❌ JSON 문자열이 비어있음!")
                    return@withContext JSONObject() // 빈 JSON 반환
                }

                val jsonObject = JSONObject(jsonString)
                Timber.d("✅ FriendSearch JSONObject 생성 성공")

                jsonObject
            } catch (e: Exception) {
                Timber.e(e, "❌ FriendSearch base Lottie JSON 로드 실패")
                JSONObject() // 실패 시 빈 JSON 반환
            }
        }
}


