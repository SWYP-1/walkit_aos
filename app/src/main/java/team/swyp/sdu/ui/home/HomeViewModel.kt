package team.swyp.sdu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.CharacterDisplayUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.DataState
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import team.swyp.sdu.domain.model.Goal
import timber.log.Timber
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.data.local.dao.RecentSessionEmotion
import team.swyp.sdu.data.local.dao.EmotionCount
import team.swyp.sdu.domain.repository.CharacterRepository
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.domain.repository.MissionRepository
import team.swyp.sdu.domain.repository.HomeRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.worker.SessionSyncWorker
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.domain.model.WalkRecord
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.home.utils.WeatherType
import team.swyp.sdu.data.mapper.MissionCardStateMapper
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel.WalkAggregate
import team.swyp.sdu.utils.CalenderUtils.weekRange
import team.swyp.sdu.utils.LocationConstants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        // 최소한의 필드만 유지 - 다른 상태들로 분리됨
        val character: Character,
        val walkProgressPercentage: String = "0",
        val temperature: Double? = null,
        val weather: WeatherType? = null,
        val goal: Goal? = null,
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}

// Profile Section UiState
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val nickname: String,
        val character: Character,
        val walkProgressPercentage: String,
        val goal: Goal?,
        val weather: WeatherType?,
        val temperature: Double?,
        val todaySteps: Int = 0
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}

// Mission Section UiState
sealed interface MissionUiState {
    data object Loading : MissionUiState
    data class Success(
        val missions: List<WeeklyMission>,
        val missionCardStates: List<MissionWithState>
    ) : MissionUiState

    data object Empty : MissionUiState
    data class Error(val message: String) : MissionUiState
}

/**
 * 미션과 그 상태를 함께 담는 데이터 클래스
 */
data class MissionWithState(
    val mission: WeeklyMission,
    val cardState: team.swyp.sdu.ui.mission.model.MissionCardState
)

// Walking Session 데이터 모델 (API 독립적)
data class WalkingSessionData(
    val sessionsThisWeek: List<WalkingSession>,
    val dominantEmotion: EmotionType?,
    val dominantEmotionCount: Int?,  // dominant emotion의 등장 횟수
    val recentEmotions: List<EmotionType?>
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val characterRepository: CharacterRepository,
    private val goalRepository: GoalRepository,
    private val missionRepository: MissionRepository,
    private val homeRepository: HomeRepository,
    private val userRepository: UserRepository,
    private val locationManager: LocationManager,
    private val missionCardStateMapper: MissionCardStateMapper,
    private val lottieImageProcessor: team.swyp.sdu.domain.service.LottieImageProcessor, // ✅ Lottie 이미지 프로세서 추가
    private val application: android.app.Application, // ✅ Application 추가
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Section별 UiState 관리 (토스/배민 스타일)
    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    // 캐릭터 Lottie 상태 관리
    private val _characterLottieState = MutableStateFlow<team.swyp.sdu.domain.model.LottieCharacterState?>(null)
    val characterLottieState: StateFlow<team.swyp.sdu.domain.model.LottieCharacterState?> = _characterLottieState.asStateFlow()

    /**
     * 캐릭터 Lottie 표시 상태 로드
     */
    fun loadCharacterDisplay() {
        viewModelScope.launch {
            try {
                Timber.d("🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 시작")

                // 현재 사용자 ID 가져오기
                val userResult = userRepository.getUser()
                val userId = when (userResult) {
                    is Result.Success -> userResult.data.userId.toString()
                    else -> {
                        Timber.w("🏠 HomeViewModel: 사용자 정보를 가져올 수 없음")
                        _characterLottieState.value = null
                        return@launch
                    }
                }

                // userId로 캐릭터 정보 가져오기
                val characterResult = characterRepository.getCharacter(userId)
                val character = when (characterResult) {
                    is Result.Success -> characterResult.data
                    is Result.Error -> {
                        Timber.w("🏠 HomeViewModel: 캐릭터 정보를 찾을 수 없음: ${characterResult.message}")
                        null
                    }
                    Result.Loading -> null
                }

                if (character == null) {
                    Timber.w("🏠 HomeViewModel: 캐릭터 정보가 없음")
                    _characterLottieState.value = null
                    return@launch
                }

                // 캐릭터 등급에 따른 base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)

                // Lottie 캐릭터 상태 생성
                val lottieState = CharacterDisplayUtils.createLottieCharacterState(
                    character = character,
                    lottieImageProcessor = lottieImageProcessor,
                    baseLottieJson = baseJson.toString()
                )

                _characterLottieState.value = lottieState
                Timber.d("🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 완료")

            } catch (e: Exception) {
                Timber.e(e, "🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 실패")
                _characterLottieState.value = team.swyp.sdu.domain.model.LottieCharacterState(
                    baseJson = "{}",
                    modifiedJson = null,
                    assets = emptyMap(),
                    isLoading = false,
                    error = e.message ?: "캐릭터 표시 준비 실패"
                )
            }
        }
    }

    /**
     * 캐릭터 등급에 따른 Base Lottie JSON 로드
     */
    private suspend fun loadBaseLottieJson(character: team.swyp.sdu.domain.model.Character): JSONObject =
        withContext(Dispatchers.IO) {
            val resourceId = when (character.grade) {
                Grade.SEED -> R.raw.seed
                Grade.SPROUT -> R.raw.sprout
                Grade.TREE -> R.raw.tree
            }

            Timber.d("🎭 HomeViewModel.loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

            try {
                Timber.d("📂 HomeViewModel: Lottie 파일 로드 시도")
                val inputStream = application.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                Timber.d("📄 HomeViewModel: JSON 문자열 길이: ${jsonString.length}")

                if (jsonString.isEmpty()) {
                    Timber.e("❌ HomeViewModel: JSON 문자열이 비어있음!")
                    return@withContext JSONObject()
                }

                val jsonObject = JSONObject(jsonString)
                Timber.d("✅ HomeViewModel: JSONObject 생성 성공, 키 개수: ${jsonObject.length()}")

                jsonObject

            } catch (e: Exception) {
                Timber.e(e, "❌ HomeViewModel: Lottie 파일 로드 실패")
                JSONObject() // 실패 시 빈 JSON 반환
            }
        }

    private val _missionUiState = MutableStateFlow<MissionUiState>(MissionUiState.Loading)
    val missionUiState: StateFlow<MissionUiState> = _missionUiState.asStateFlow()

    // Goal 정보를 별도 StateFlow로 관리
    private val _goalState = MutableStateFlow<Goal?>(null)

    // Walking Session 정보를 API 독립적으로 관리
    private val _walkingSessionDataState =
        MutableStateFlow<DataState<WalkingSessionData>>(DataState.Loading)
    val walkingSessionDataState: StateFlow<DataState<WalkingSessionData>> =
        _walkingSessionDataState.asStateFlow()

    // 오늘 걸음 수 계산 Flow
    private val todayStepsFlow = walkingSessionDataState
        .map { state ->
            when (state) {
                is DataState.Success -> {
                    val today = LocalDate.now()
                    val todaySessions = state.data.sessionsThisWeek
                        .filter { session ->
                            // startTime timestamp를 LocalDate로 변환해서 오늘인지 확인
                            val sessionDate = Instant.ofEpochMilli(session.startTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Timber.d("세션 날짜: $sessionDate, 오늘: $today, 걸음: ${session.stepCount}")
                            sessionDate == today
                        }

                    Timber.d("오늘 세션 개수: ${todaySessions.size}, 총 걸음: ${todaySessions.sumOf { it.stepCount }}")
                    todaySessions.sumOf { it.stepCount }
                }

                else -> {
                    Timber.d("walkingSessionDataState가 Success가 아님: $state")
                    0
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val today = MutableStateFlow(LocalDate.now())

    init {
        loadHomeData()

        // Goal 데이터를 자동으로 동기화
        viewModelScope.launch {
            goalRepository.goalFlow.collect { goal ->
                Timber.d("🏠 Goal 데이터 업데이트: $goal")
                _goalState.value = goal
            }
        }

        // 초기 Goal 데이터 로드
        viewModelScope.launch {
            goalRepository.getGoal()
                .onSuccess { goal ->
                    Timber.d("🏠 초기 Goal 데이터 로드 성공: $goal")
                }
                .onError { exception, message ->
                    Timber.w(exception, "🏠 초기 Goal 데이터 로드 실패: $message")
                }
        }

        // 사용자 로그인 상태에 따라 세션 데이터 로드
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                if (user != null) {
                    // 로그인 상태: 세션 데이터 로드
                    loadWalkingSessionsFromRoom()
                } else {
                    // 로그아웃 상태: 세션 데이터 초기화 도달해선안됨
                    Timber.d("🏠 로그아웃 상태: 세션 데이터 초기화")
                }
            }
        }
    }

    val goalUiState: StateFlow<DataState<Goal>> = goalRepository.goalFlow.map { goal ->
        if (goal != null) {
            DataState.Success(goal)
        } else {
            DataState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DataState.Loading
    )


    companion object {
        private const val TAG_PERFORMANCE = "HomePerformance"
    }

    /**
     * 홈 데이터 로드 (위치 기반)
     */
    fun loadHomeData() {
        viewModelScope.launch {
            val totalStartTime = System.currentTimeMillis()
            _uiState.value = HomeUiState.Loading

            // Section별 로딩 상태 초기화
            _profileUiState.value = ProfileUiState.Loading
            _missionUiState.value = MissionUiState.Loading

            // 위치 획득 시도
            val locationStartTime = System.currentTimeMillis()
            val location = getLocationForApi()
            val locationElapsedTime = System.currentTimeMillis() - locationStartTime
            Timber.tag(TAG_PERFORMANCE)
                .d("위치 획득 완료 (전체): ${locationElapsedTime}ms, lat=${location.latitude}, lon=${location.longitude}")

            // 홈 API 호출
            val apiStartTime = System.currentTimeMillis()
            val homeResult = homeRepository.getHomeData(
                lat = location.latitude, lon = location.longitude
            )
            val apiElapsedTime = System.currentTimeMillis() - apiStartTime

            when (homeResult) {
                is Result.Success -> {
                    val homeData = homeResult.data
                    val totalElapsedTime = System.currentTimeMillis() - totalStartTime
                    Timber.tag(TAG_PERFORMANCE)
                        .d("Home 데이터 로드 완료 (전체): ${totalElapsedTime}ms (위치: ${locationElapsedTime}ms, API: ${apiElapsedTime}ms)")

                    Timber.d("API 응답 데이터 확인 - weeklyMission: ${homeData.weeklyMission}")
                    Timber.d("API 응답 데이터 확인 - character: ${homeData.character}")

                    // Home API에서 받은 Character 정보를 Room에 저장
                    homeData.character.nickName?.let { nickname ->
                        characterRepository.saveCharacter(nickname, homeData.character)
                            .onError { exception, message ->
                                Timber.w(exception, "캐릭터 정보 저장 실패: $message")
                            }
                    }

                    // ✅ Home API 호출 후 User 정보를 Room에 저장 (마이페이지 닉네임 표시용)
                    userRepository.refreshUser()
                        .onError { exception, message ->
                            Timber.w(exception, "사용자 정보 저장 실패: $message")
                        }

                    // Section별 UiState 업데이트
                    updateProfileSection(homeData)
                    updateMissionSection(homeData)

                    // 기존 로직 유지 (세션 정보 등)
                    loadSessionsWithHomeData(homeData)
                }

                is Result.Error -> {
                    val totalElapsedTime = System.currentTimeMillis() - totalStartTime
                    Timber.tag(TAG_PERFORMANCE)
                        .w("Home 데이터 로드 실패 (전체): ${totalElapsedTime}ms (위치: ${locationElapsedTime}ms, API: ${apiElapsedTime}ms)")
                    Timber.w("홈 API 호출 실패 - 서버 문제로 판단하여 Error 상태 유지")

                    // Home API가 모든 데이터를 담당하므로 실패 시 서버 문제로 간주
                    // fallback 로직 제거 - 일관성 없는 데이터로 Success 표시하지 않음
                    _profileUiState.value =
                        ProfileUiState.Error("서버 연결에 문제가 있습니다.\n잠시 후 다시 시도해주세요.")
                    _missionUiState.value =
                        MissionUiState.Error("서버 연결에 문제가 있습니다.\n잠시 후 다시 시도해주세요.")

                    // 기존 세션 로드 로직도 호출하지 않음 (API 기반이므로)
                }

                Result.Loading -> {
                    // 이미 Loading 상태
                }
            }
        }
    }


    /**
     * Walking Session을 Room에서 API 독립적으로 로드
     */
    private fun loadWalkingSessionsFromRoom() {
        viewModelScope.launch {
            try {
                // 이번 주 범위 계산
                val (weekStart, weekEnd) = weekRange(today.value)
                Timber.d("🏠 이번 주 범위: ${weekStart.formatTimestamp()} ~ ${weekEnd.formatTimestamp()}")

                // 🚀 최적화: DB 쿼리로 이번 주 우세 감정 계산 (suspend 함수)
                val dominantEmotionData = walkingSessionRepository.getDominantEmotionInPeriod(weekStart, weekEnd)

                val dominantEmotion = dominantEmotionData?.let { data ->
                    try {
                        EmotionType.valueOf(data.emotion)
                    } catch (e: IllegalArgumentException) {
                        Timber.w("Unknown dominant emotion type: ${data.emotion}")
                        null
                    }
                }

                val dominantEmotionCount = dominantEmotionData?.count ?: 0

                Timber.d("🏠 [dominantEmotion] DB 쿼리로 계산된 우세 감정: $dominantEmotion (카운트: $dominantEmotionCount)")

                // 🚀 최적화: 여러 Flow를 combine으로 결합
                combine(
                    walkingSessionRepository.getRecentSessionsForEmotions(),
                    walkingSessionRepository.getSessionsBetween(weekStart, weekEnd)
                ) { recentSessionEmotions, thisWeekSessions ->
                    // recentEmotions 추출 과정 로깅 (최적화된 데이터 사용)
                    Timber.d("🏠 [recentEmotions] 최적화된 쿼리로 조회된 최근 세션 수: ${recentSessionEmotions.size}")
                    Timber.d("🏠 [recentEmotions] 최근 감정 데이터:")
                    recentSessionEmotions.forEachIndexed { index, emotionData ->
                        Timber.d("🏠 [recentEmotions] 세션 ${index + 1}: 시작시간=${emotionData.startTime.formatTimestamp()}, 산책후감정=${emotionData.postWalkEmotion}")
                    }

                    // EmotionType으로 변환 (String -> EmotionType)
                    val recentEmotions = recentSessionEmotions.mapNotNull { emotionData ->
                        try {
                            EmotionType.valueOf(emotionData.postWalkEmotion)
                        } catch (e: IllegalArgumentException) {
                            Timber.w("Unknown emotion type: ${emotionData.postWalkEmotion}")
                            null
                        }
                    }
                    Timber.d("🏠 [recentEmotions] 최종 추출된 감정들: $recentEmotions")

                    WalkingSessionData(
                        sessionsThisWeek = thisWeekSessions,
                        dominantEmotion = dominantEmotion,
                        dominantEmotionCount = dominantEmotionCount,
                        recentEmotions = recentEmotions
                    )
                }.catch { e ->
                    Timber.e(e, "세션 데이터 결합 중 오류")
                    _walkingSessionDataState.value = DataState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                    return@catch
                }.collect { walkingSessionData ->
                    _walkingSessionDataState.value = DataState.Success(walkingSessionData)
                }
            } catch (e: Exception) {
                Timber.e(e, "세션 로드 중 오류")
                _walkingSessionDataState.value = DataState.Error(e.message ?: "세션 로드 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 홈 API 데이터와 함께 세션 정보 로드
     */
    /**
     * 프로필 섹션 UiState 업데이트
     */
    private fun updateProfileSection(homeData: team.swyp.sdu.domain.model.HomeData) {
        Timber.d("프로필 섹션 업데이트 - character: ${homeData.character}, nickname: ${homeData.character.nickName}")

        // 닉네임은 로직상 항상 존재하므로 Success로 처리
        val goal = _goalState.value

        _profileUiState.value = ProfileUiState.Success(
            nickname = homeData.character.nickName ?: "사용자",
            character = homeData.character,
            walkProgressPercentage = homeData.walkProgressPercentage,
            goal = goal,
            weather = homeData.weather,
            todaySteps = todayStepsFlow.value,
            temperature = homeData.temperature
        )
        Timber.d("프로필 상태: Success")
    }

    /**
     * 미션 섹션 UiState 업데이트
     */
    private fun updateMissionSection(homeData: team.swyp.sdu.domain.model.HomeData) {
        Timber.d("미션 섹션 업데이트 - weeklyMission: ${homeData.weeklyMission}")

        val missions = homeData.weeklyMission?.let {
            Timber.d("미션 데이터 존재: $it")
            listOf(it)
        } ?: run {
            Timber.d("미션 데이터 없음 (null)")
            emptyList()
        }

        Timber.d("최종 missions 리스트 크기: ${missions.size}")

        if (missions.isEmpty()) {
            Timber.d("미션 상태: Empty")
            _missionUiState.value = MissionUiState.Empty
        } else {
            Timber.d("미션 상태: Success, 개수: ${missions.size}")
            // 미션 상태 매핑 (비동기로 처리)
            viewModelScope.launch {
                try {
                    val missionCardStates = missions.map { mission ->
                        Timber.d("미션 정보: title=${mission.title}, status=${mission.status}, assignedConfigJson=${mission.assignedConfigJson}")
                        val missionConfig = mission.getMissionConfig()
                        Timber.d("미션 설정 파싱 결과: $missionConfig")

                        // 현재 todaySteps 값도 로깅
                        val currentTodaySteps = todayStepsFlow.value
                        Timber.d("현재 HomeViewModel todaySteps: $currentTodaySteps")

                        val cardState =
                            missionCardStateMapper.mapToCardState(mission, isActive = true)
                        Timber.d("미션 카드 상태 계산 결과: $cardState")
                        MissionWithState(mission, cardState)
                    }
                    Timber.d("미션 카드 상태 매핑 완료: $missionCardStates")
                    _missionUiState.value = MissionUiState.Success(
                        missions = missions,
                        missionCardStates = missionCardStates
                    )
                } catch (e: Exception) {
                    Timber.e(e, "미션 카드 상태 매핑 실패")
                    // 매핑 실패 시 기존 로직으로 fallback
                    _missionUiState.value = MissionUiState.Success(
                        missions = missions,
                        missionCardStates = missions.map {
                            MissionWithState(
                                it,
                                team.swyp.sdu.ui.mission.model.MissionCardState.INACTIVE
                            )
                        }
                    )
                }
            }
        }
    }


    private fun loadSessionsWithHomeData(homeData: team.swyp.sdu.domain.model.HomeData) {
        viewModelScope.launch {
            // 목표 정보는 별도 StateFlow에서 가져옴 (flow로 관리)
            val goal = _goalState.value

            val (start, end) = weekRange(today.value)

            walkingSessionRepository.getSessionsBetween(start, end).catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
            }.collect { sessions ->
                val thisWeekSessions = sessions.filterThisWeek()

                // recentEmotions 추출 과정 로깅 (loadSessionsWithHomeData)
                Timber.d("🏠 [loadSessionsWithHomeData] 총 세션 수: ${sessions.size}")
                val sortedSessions = sessions.sortedByDescending { it.startTime }.take(7)
                Timber.d("🏠 [loadSessionsWithHomeData] 최근 7개 세션 추출:")
                sortedSessions.forEachIndexed { index, session ->
                    Timber.d("🏠 [loadSessionsWithHomeData] 세션 ${index + 1}: id=${session.id}, 시작시간=${session.startTime.formatTimestamp()}, 산책후감정=${session.postWalkEmotion}")
                }
                val recentEmotions = sortedSessions.map { it.postWalkEmotion }
                Timber.d("🏠 [loadSessionsWithHomeData] 최종 추출된 감정들: $recentEmotions")
                val dominantEmotion = findDominantEmotion(thisWeekSessions)

                // 주간 미션
                val missions = homeData.weeklyMission?.let {
                    listOf(it)
                } ?: emptyList()

                _uiState.value = HomeUiState.Success(
                    character = homeData.character,
                    walkProgressPercentage = homeData.walkProgressPercentage,
                    temperature = homeData.temperature,
                    weather = homeData.weather,
                    goal = goal,
                )
            }
        }
    }


    /**
     * API 호출을 위한 위치 획득
     *
     * 1. 위치 권한 확인
     * 2. 권한 있음 → 현재 위치 획득 시도
     * 3. 권한 없음 또는 위치 획득 실패 → 기본 위치(서울시청) 반환
     */
    private suspend fun getLocationForApi(): LocationPoint {
        return if (locationManager.hasLocationPermission()) {
            // 위치 권한 있음 → 현재 위치 획득 시도
            locationManager.getCurrentLocationOrLast() ?: getDefaultLocation()
        } else {
            // 위치 권한 없음 → 기본 위치 사용
            getDefaultLocation()
        }
    }

    /**
     * 기본 위치 반환 (서울시청)
     */
    private fun getDefaultLocation(): LocationPoint {
        return LocationPoint(
            latitude = LocationConstants.DEFAULT_LATITUDE,
            longitude = LocationConstants.DEFAULT_LONGITUDE,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * 위치 권한 요청 후 데이터 재로드
     */
    fun reloadAfterPermissionGranted() {
        loadHomeData()
    }


    /**
     * 오늘의 실제 걸음 수 계산
     * TODO: 실제 걸음 수 데이터에서 계산하도록 구현
     */
    private fun calculateTodaySteps(): Int {
        // 임시 구현: 실제로는 걸음 수 센서나 건강 데이터에서 가져와야 함
        return 0
    }

    private fun loadSessions(
        nickname: String,
        levelLabel: String,
        todaySteps: Int,
        missions: List<WeeklyMission>,
        goal: Goal? = null,
    ) {
        viewModelScope.launch {
            walkingSessionRepository.getAllSessions().catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
            }.collect { sessions ->
                val thisWeekSessions = sessions.filterThisWeek()

                // recentEmotions 추출 과정 로깅 (loadSessions)
                Timber.d("🏠 [loadSessions] 총 세션 수: ${sessions.size}")
                val sortedSessions = sessions.sortedByDescending { it.startTime }.take(7)
                Timber.d("🏠 [loadSessions] 최근 7개 세션 추출:")
                sortedSessions.forEachIndexed { index, session ->
                    Timber.d("🏠 [loadSessions] 세션 ${index + 1}: id=${session.id}, 시작시간=${session.startTime.formatTimestamp()}, 산책후감정=${session.postWalkEmotion}")
                }
                val recentEmotions = sortedSessions.map { it.postWalkEmotion }
                Timber.d("🏠 [loadSessions] 최종 추출된 감정들: $recentEmotions")
                val dominantEmotion = findDominantEmotion(thisWeekSessions)

                // 기본 Character Domain 모델 생성 (Fallback용)
                val defaultCharacter = Character(
                    headImageName = null,
                    bodyImageName = null,
                    feetImageName = null,
                    characterImageName = null,
                    backgroundImageName = null,
                    level = 1,
                    grade = Grade.SEED,
                    nickName = nickname,
                )

                _uiState.value = HomeUiState.Success(
                    character = defaultCharacter,
                    walkProgressPercentage = "0",
                    temperature = null,
                    weather = null,
                    goal = goal,
                )
            }
        }
    }

    private fun List<WalkingSession>.filterThisWeek(): List<WalkingSession> {
        val today = LocalDate.now()
        val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
        val endOfWeek = startOfWeek.plusDays(6)
        return filter { session ->
            val date =
                java.time.Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault())
                    .toLocalDate()
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }.sortedByDescending { it.startTime }
    }

    /**
     * 이번주 산책에서 가장 많이 경험된 감정 찾기
     *
     * 동일한 등장 횟수를 가진 감정이 여러 개일 경우 우선순위에 따라 결정:
     * 1. HAPPY (기쁨) > 2. JOYFUL (즐거움) > 3. CONTENT (행복함)
     * > 4. DEPRESSED (우울함) > 5. TIRED (지침) > 6. IRRITATED (짜증남)
     */
    private fun findDominantEmotion(sessions: List<WalkingSession>): EmotionType? {
        val emotionCounts = sessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        if (emotionCounts.isEmpty()) return null

        // 1. 최대 등장 횟수 찾기
        val maxCount = emotionCounts.values.max()

        // 2. 최대 등장 횟수를 가진 감정들 필터링
        val candidates = emotionCounts.filter { it.value == maxCount }.keys

        // 3. 우선순위가 가장 높은 감정 선택 (priority 값이 낮을수록 우선)
        return candidates.minByOrNull { it.priority }
    }

    /**
     * 우세 감정과 그 등장 횟수를 반환
     *
     * 동일한 등장 횟수를 가진 감정이 여러 개일 경우 우선순위에 따라 결정
     */
    private fun findDominantEmotionWithCount(sessions: List<WalkingSession>): Pair<EmotionType?, Int?> {
        val emotionCounts = sessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        if (emotionCounts.isEmpty()) return Pair(null, null)

        // 1. 최대 등장 횟수 찾기
        val maxCount = emotionCounts.values.max()

        // 2. 최대 등장 횟수를 가진 감정들 필터링
        val candidates = emotionCounts.filter { it.value == maxCount }.keys

        // 3. 우선순위가 가장 높은 감정 선택
        val dominantEmotion = candidates.minByOrNull { it.priority }

        return Pair(dominantEmotion, maxCount)
    }

    /**
     * 수동 세션 동기화 실행
     *
     * UI에서 즉시 동기화를 원할 때 호출 (예: 설정 화면의 동기화 버튼)
     */
    fun triggerManualSessionSync(context: android.content.Context) {
        viewModelScope.launch {
            try {
                Timber.d("수동 세션 동기화 시작")
                SessionSyncWorker.scheduleOneTimeSync(context)
                Timber.d("수동 세션 동기화 작업 예약됨")
            } catch (e: Exception) {
                Timber.e(e, "수동 세션 동기화 예약 실패")
            }
        }
    }

    /**
     * 주간 미션 보상 요청
     *
     * @param userWeeklyMissionId 보상을 요청할 미션 ID
     */
    fun requestWeeklyMissionReward(userWeeklyMissionId: Long) {
        viewModelScope.launch {
            Timber.d("주간 미션 보상 요청 시작: $userWeeklyMissionId")

            when (val result = missionRepository.verifyWeeklyMissionReward(userWeeklyMissionId)) {
                is Result.Success -> {
                    val verifiedMission = result.data
                    Timber.d("미션 보상 검증 성공: ${verifiedMission.title}, 상태: ${verifiedMission.status}")

                    // 현재 미션 상태를 업데이트
                    updateMissionAfterRewardVerification(verifiedMission)

                    // TODO: 보상 지급 성공 UI 피드백 추가
                }

                is Result.Error -> {
                    Timber.e(result.exception, "미션 보상 검증 실패: $userWeeklyMissionId")
                    // TODO: 에러 처리 UI 피드백 추가
                }

                Result.Loading -> {
                    // 로딩 상태 처리 (필요시)
                }
            }
        }
    }

    /**
     * 미션 클릭 처리 (도전하기)
     * READY_FOR_CLAIM 상태가 아닐 때 호출됨
     */
    fun onClickToWalk() {
        Timber.d("미션 클릭: 산책 화면으로 이동")
        // TODO: 산책 화면으로 네비게이션
    }

    /**
     * 보상 검증 후 미션 상태 업데이트
     *
     * @param verifiedMission 검증된 미션 데이터
     */
    private fun updateMissionAfterRewardVerification(verifiedMission: WeeklyMission) {
        Timber.d("미션 상태 업데이트 시작: ${verifiedMission.title}")

        // 현재 미션 UI 상태 가져오기
        val currentMissionUiState = _missionUiState.value

        if (currentMissionUiState is MissionUiState.Success) {
            // 기존 미션 목록에서 검증된 미션으로 교체
            val updatedMissions = currentMissionUiState.missions.map { existingMission ->
                if (existingMission.userWeeklyMissionId == verifiedMission.userWeeklyMissionId) {
                    Timber.d("미션 교체: ${existingMission.title} -> ${verifiedMission.title}")
                    verifiedMission
                } else {
                    existingMission
                }
            }

            // 미션 카드 상태도 함께 업데이트
            viewModelScope.launch {
                try {
                    val updatedMissionCardStates = updatedMissions.map { mission ->
                        Timber.d("업데이트된 미션 카드 상태 계산: ${mission.title}")
                        val missionConfig = mission.getMissionConfig()
                        val cardState =
                            missionCardStateMapper.mapToCardState(mission, isActive = true)
                        MissionWithState(mission, cardState)
                    }

                    Timber.d("미션 상태 업데이트 완료")
                    _missionUiState.value = MissionUiState.Success(
                        missions = updatedMissions,
                        missionCardStates = updatedMissionCardStates
                    )
                } catch (e: Exception) {
                    Timber.e(e, "미션 카드 상태 업데이트 실패")
                    // 실패 시 미션 정보만 업데이트
                    _missionUiState.value = MissionUiState.Success(
                        missions = updatedMissions,
                        missionCardStates = currentMissionUiState.missionCardStates
                    )
                }
            }
        } else {
            Timber.w("미션 UI 상태가 Success가 아니어서 업데이트할 수 없음: $currentMissionUiState")
        }
    }

}

/**
 * Long 타입 timestamp를 읽기 쉬운 날짜 형식으로 변환
 */
private fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

