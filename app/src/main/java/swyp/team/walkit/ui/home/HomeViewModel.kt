package swyp.team.walkit.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.CharacterDisplayUtils
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import swyp.team.walkit.core.DataState
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.onError
import swyp.team.walkit.core.onSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import swyp.team.walkit.domain.model.Goal
import timber.log.Timber
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.data.local.dao.RecentSessionEmotion
import swyp.team.walkit.data.local.dao.EmotionCount
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.domain.repository.MissionRepository
import swyp.team.walkit.domain.repository.HomeRepository
import swyp.team.walkit.domain.repository.UserRepository
import swyp.team.walkit.worker.SessionSyncWorker
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.WeeklyMission
import swyp.team.walkit.domain.model.WalkRecord
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.ui.home.utils.WeatherType
import swyp.team.walkit.data.mapper.MissionCardStateMapper
import swyp.team.walkit.domain.model.LottieCharacterState
import swyp.team.walkit.domain.model.User
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel.WalkAggregate
import swyp.team.walkit.ui.home.MissionUiState.*
import swyp.team.walkit.ui.home.ProfileUiState.*
import swyp.team.walkit.ui.mypage.model.UserInfoData
import swyp.team.walkit.utils.CalenderUtils.weekRange
import swyp.team.walkit.utils.LocationConstants
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
    val cardState: swyp.team.walkit.ui.mission.model.MissionCardState
)

// Walking Session 데이터 모델 (API 독립적)
data class WalkingSessionData(
    val sessionsThisWeek: List<WalkingSession>,
    val dominantEmotion: String?,  // String으로 변경 (EmotionType.name)
    val dominantEmotionCount: Int?,  // dominant emotion의 등장 횟수
    val recentEmotions: List<String?>  // String으로 변경 (EmotionType.name)
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
    private val lottieImageProcessor: swyp.team.walkit.domain.service.LottieImageProcessor, // ✅ Lottie 이미지 프로세서 추가
    private val application: android.app.Application, // ✅ Application 추가
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Section별 UiState 관리 (토스/배민 스타일)
    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    // 캐릭터 Lottie 상태 관리
    private val _characterLottieState =
        MutableStateFlow<swyp.team.walkit.domain.model.LottieCharacterState?>(null)
    val characterLottieState: StateFlow<swyp.team.walkit.domain.model.LottieCharacterState?> =
        _characterLottieState.asStateFlow()

    // 캐릭터 Lottie 상태 캐시 (레벨/등급 변경 시 캐시 무효화를 위해 포함)
    // 본인 캐릭터만 관리하므로 단순 변수로 저장
    private var cachedCharacterLottieState: LottieCharacterState? = null
    private var cachedCharacterKey: String? = null // "${userId}_${level}_${grade}"

    // 테스트용 레벨/등급 순환 카운터
    private var testLevelCycleCount = 0

    /**
     * 캐시 키 생성 (userId, level, grade를 포함)
     */
    private fun createCharacterCacheKey(userId: Long, level: Int, grade: Grade): String {
        return "${userId}_${level}_${grade.name}"
    }

    /**
     * 캐릭터 Lottie 표시 상태 로드
     */
    fun loadCharacterDisplay() {
        viewModelScope.launch {
            try {
                Timber.d("🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 시작")

                // 현재 사용자 ID 가져오기
                val userId = currentUser.value?.userId
                if (userId == null) {
                    Timber.w("🏠 HomeViewModel: 사용자 정보를 가져올 수 없음")
                    _characterLottieState.value = null
                    return@launch
                }

                // 캐릭터 정보 가져오기 (테스트용 UI 상태 우선 사용)
                val character = when (val currentProfileState = _profileUiState.value) {
                    is ProfileUiState.Success -> {
                        // 테스트용: UI 상태의 캐릭터 정보 우선 사용
                        Timber.d("🏠 HomeViewModel: UI 상태의 캐릭터 정보 사용 - level=${currentProfileState.character.level}, grade=${currentProfileState.character.grade}")
                        currentProfileState.character
                    }
                    else -> {
                        // 서버에서 캐릭터 정보 가져오기
                        Timber.d("🏠 HomeViewModel: 서버에서 캐릭터 정보 가져오기")
                        val characterResult = characterRepository.getCharacter(userId)
                        when (characterResult) {
                            is Result.Success -> characterResult.data
                            is Result.Error -> {
                                Timber.w("🏠 HomeViewModel: 캐릭터 정보를 찾을 수 없음: ${characterResult.message}")
                                null
                            }
                            Result.Loading -> null
                        }
                    }
                }

                if (character == null) {
                    Timber.w("🏠 HomeViewModel: 캐릭터 정보가 없음")
                    _characterLottieState.value = null
                    return@launch
                }

                // 1️⃣ 캐시 키 생성 (level과 grade 포함)
                val cacheKey = createCharacterCacheKey(userId, character.level, character.grade)

                // 2️⃣ 캐시 확인 (레벨/등급이 포함된 키로 확인)
                if (cachedCharacterKey == cacheKey && cachedCharacterLottieState != null) {
                    Timber.d("🏠 HomeViewModel: 캐시 사용: cacheKey=$cacheKey")
                    _characterLottieState.value = cachedCharacterLottieState
                    return@launch
                }

                // 3️⃣ 캐시가 없거나 키가 변경되었으면 Lottie 상태 생성 및 캐시 저장
                Timber.d("🏠 HomeViewModel: 캐시 없음 또는 키 변경, 새로 생성: cacheKey=$cacheKey, level=${character.level}, grade=${character.grade}")

                // 캐릭터 등급에 따른 base Lottie JSON 로드
                val baseJson = loadBaseLottieJson(character)

                // Lottie 캐릭터 상태 생성
                val lottieState = CharacterDisplayUtils.createLottieCharacterState(
                    character = character,
                    lottieImageProcessor = lottieImageProcessor,
                    baseLottieJson = baseJson.toString()
                )

                // 4️⃣ 캐시에 저장 (레벨/등급이 포함된 키로 저장)
                cachedCharacterKey = cacheKey
                cachedCharacterLottieState = lottieState
                Timber.d("🏠 HomeViewModel: 캐시 저장: cacheKey=$cacheKey")

                _characterLottieState.value = lottieState
                Timber.d("🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 완료")

            } catch (t: Throwable) {
                Timber.e(t, "🏠 HomeViewModel: 캐릭터 Lottie 상태 로드 실패")
                _characterLottieState.value = swyp.team.walkit.domain.model.LottieCharacterState(
                    baseJson = "{}",
                    modifiedJson = null,
                    assets = emptyMap(),
                    isLoading = false,
                    error = t.message ?: "캐릭터 표시 준비 실패"
                )
            }
        }
    }

    /**
     * 캐릭터 Lottie 캐시 초기화 (레벨업 시 호출)
     */
    fun clearCharacterLottieCache() {
        cachedCharacterKey = null
        cachedCharacterLottieState = null
        Timber.d("🏠 HomeViewModel: 캐릭터 Lottie 캐시 초기화 완료")
    }

    /**
     * 테스트용: ProfileUiState의 level과 grade를 순환시키는 함수
     * 첫 번째 클릭: level 1, grade SEED
     * 두 번째 클릭: level 4, grade SPROUT
     * 세 번째 클릭: level 9, grade TREE
     */
    fun cycleCharacterLevelAndGradeForTest() {
        viewModelScope.launch {
            val currentProfileState = _profileUiState.value
            if (currentProfileState !is ProfileUiState.Success) {
                Timber.w("🏠 HomeViewModel: ProfileUiState가 Success 상태가 아님")
                return@launch
            }

            // 테스트 순환 카운터 증가 (0, 1, 2 순환)
            testLevelCycleCount = (testLevelCycleCount + 1) % 3

            // 새로운 level과 grade 설정
            val (newLevel, newGrade) = when (testLevelCycleCount) {
                0 -> 1 to Grade.SEED
                1 -> 4 to Grade.SPROUT
                2 -> 9 to Grade.TREE
                else -> 1 to Grade.SEED
            }

            // 캐릭터 업데이트 (level과 grade만 변경)
            val updatedCharacter = currentProfileState.character.copy(
                level = newLevel,
                grade = newGrade
            )

            // ProfileUiState 업데이트
            _profileUiState.value = currentProfileState.copy(character = updatedCharacter)

            // 캐릭터 Lottie 재로드 (캐시 키 변경으로 자동 캐시 무효화)
            loadCharacterDisplay()

            Timber.d("🏠 HomeViewModel: 테스트용 레벨/등급 순환 - count=$testLevelCycleCount, level=$newLevel, grade=$newGrade")
        }
    }

    /**
     * 캐릭터 등급에 따른 Base Lottie JSON 로드
     */
    private suspend fun loadBaseLottieJson(character: swyp.team.walkit.domain.model.Character): JSONObject =
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

            } catch (t: Throwable) {
                Timber.e(t, "❌ HomeViewModel: Lottie 파일 로드 실패")
                JSONObject() // 실패 시 빈 JSON 반환
            }
        }

    private val _missionUiState = MutableStateFlow<MissionUiState>(MissionUiState.Loading)
    val missionUiState: StateFlow<MissionUiState> = _missionUiState.asStateFlow()

    /**
     * 현재 사용자 정보를 전역으로 관리
     *
     * 사용법:
     * - `currentUser.value?.userId`로 ID 접근
     * - `currentUser.collect()`로 Flow 구독
     * - null이면 로그인하지 않은 상태
     */
    val currentUser: StateFlow<User?> = userRepository.userFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

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

        // 사용자 정보 변경 감지 및 UI 업데이트
        viewModelScope.launch {
            userRepository.userFlow.collect { user ->
                Timber.d("🏠 userRepository.userFlow 수신: user=${user?.nickname ?: "null"}")
                if (user != null) {
                    Timber.d("🏠 사용자 정보 업데이트 감지: nickname=${user.nickname}")

                    // 로그인 상태: 세션 데이터 로드
                    loadWalkingSessionsFromRoom()

                    // 프로필 상태 실시간 업데이트 (닉네임 변경 등)
                    _profileUiState.update { currentState ->
                        when (currentState) {
                            is ProfileUiState.Success -> {
                                // 기존 데이터 유지하면서 닉네임만 실시간 업데이트
                                Timber.d("🏠 프로필 닉네임 실시간 업데이트: ${currentState.nickname} -> ${user.nickname}")
                                currentState.copy(nickname = user.nickname)
                            }
                            else -> currentState // Loading/Error 상태는 유지
                        }
                    }
                } else {
                    Timber.d("🏠 로그아웃 상태 감지")
                    // 로그아웃 시 세션 데이터 초기화
                    _walkingSessionDataState.value = DataState.Success(WalkingSessionData(emptyList(), null, null, emptyList()))
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
                    val userId = currentUser.value?.userId
                    homeData.character.nickName?.let { nickname ->
                        if (userId != null) {
                            characterRepository.saveCharacter(userId, homeData.character)
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
                // 이번 주 범위 계산 (월요일~일요일)
                val currentDate = today.value
                val weekStart = currentDate.minusDays(currentDate.dayOfWeek.value - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val weekEnd = currentDate.plusDays(8L - currentDate.dayOfWeek.value).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
                Timber.d("🏠 이번 주 범위 (월~일): ${weekStart.formatTimestamp()} ~ ${weekEnd.formatTimestamp()}")
                Timber.d("🏠 이번 주 범위 (raw): start=$weekStart, end=$weekEnd")

                // 🚀 최적화: DB 쿼리로 이번 주 우세 감정 계산 (suspend 함수)
                val dominantEmotionData =
                    walkingSessionRepository.getDominantEmotionInPeriod(weekStart, weekEnd)

                val dominantEmotion = dominantEmotionData?.emotion // String으로 직접 사용

                val dominantEmotionCount = dominantEmotionData?.count ?: 0

                Timber.d("🏠 [dominantEmotion] DB 쿼리로 계산된 우세 감정: $dominantEmotion (카운트: $dominantEmotionCount)")

                // 🚀 최적화: 여러 Flow를 combine으로 결합
                combine(
                    walkingSessionRepository.getRecentSessionsForEmotions(),
                    walkingSessionRepository.getSessionsBetween(weekStart, weekEnd)
                ) { recentSessionEmotions, thisWeekSessions ->
                    // 이번 주 세션 수 로깅 추가
                    Timber.d("🏠 [thisWeekSessions] 이번 주 세션 수: ${thisWeekSessions.size}")
                    thisWeekSessions.forEachIndexed { index, session ->
                        Timber.d("🏠 [thisWeekSessions] 세션 ${index + 1}: 시작시간=${session.startTime.formatTimestamp()}, 걸음=${session.stepCount}")
                    }

                    // recentEmotions 추출 과정 로깅 (최적화된 데이터 사용)
                    Timber.d("🏠 [recentEmotions] 최적화된 쿼리로 조회된 최근 세션 수: ${recentSessionEmotions.size}")
                    Timber.d("🏠 [recentEmotions] 최근 감정 데이터:")
                    recentSessionEmotions.forEachIndexed { index, emotionData ->
                        Timber.d("🏠 [recentEmotions] 세션 ${index + 1}: 시작시간=${emotionData.startTime.formatTimestamp()}, 산책후감정=${emotionData.postWalkEmotion}")
                    }

                    // String으로 직접 사용 (변환 불필요)
                    val recentEmotions = recentSessionEmotions.map { emotionData ->
                        emotionData.postWalkEmotion
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
            } catch (t: Throwable) {
                Timber.e(t, "세션 로드 중 오류")
                _walkingSessionDataState.value = DataState.Error(t.message ?: "세션 로드 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 홈 API 데이터와 함께 세션 정보 로드
     */
    /**
     * 프로필 섹션 UiState 업데이트
     */
    private fun updateProfileSection(homeData: swyp.team.walkit.domain.model.HomeData) {
        Timber.d("프로필 섹션 업데이트 - character: ${homeData.character}, nickname: ${homeData.character.nickName}")

        // 닉네임은 로직상 항상 존재하므로 Success로 처리
        val goal = _goalState.value

        _profileUiState.value = ProfileUiState.Success(
            nickname = currentUser.value?.nickname ?: "게스트",
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
    private fun updateMissionSection(homeData: swyp.team.walkit.domain.model.HomeData) {
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
                } catch (t: Throwable) {
                    Timber.e(t, "미션 카드 상태 매핑 실패")
                    // 매핑 실패 시 기존 로직으로 fallback
                    _missionUiState.value = MissionUiState.Success(
                        missions = missions,
                        missionCardStates = missions.map {
                            MissionWithState(
                                it,
                                swyp.team.walkit.ui.mission.model.MissionCardState.INACTIVE
                            )
                        }
                    )
                }
            }
        }
    }
    private fun loadSessionsWithHomeData(homeData: swyp.team.walkit.domain.model.HomeData) {
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
    private fun findDominantEmotion(sessions: List<WalkingSession>): String? {
        val emotionCounts = sessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        if (emotionCounts.isEmpty()) return null

        // 1. 최대 등장 횟수 찾기
        val maxCount = emotionCounts.values.max()

        // 2. 최대 등장 횟수를 가진 감정들 필터링
        val candidates = emotionCounts.filter { it.value == maxCount }.keys

        // 3. 우선순위가 가장 높은 감정 선택 (String을 EmotionType으로 변환하여 value 비교)
        // value가 높을수록 우선순위가 높음 (HAPPY=5가 가장 높음)
        return candidates.maxByOrNull { emotionString ->
            stringToEmotionType(emotionString).value
        }
    }

    /**
     * 우세 감정과 그 등장 횟수를 반환
     *
     * 동일한 등장 횟수를 가진 감정이 여러 개일 경우 우선순위에 따라 결정
     */
    private fun findDominantEmotionWithCount(sessions: List<WalkingSession>): Pair<String?, Int?> {
        val emotionCounts = sessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        if (emotionCounts.isEmpty()) return Pair(null, null)

        // 1. 최대 등장 횟수 찾기
        val maxCount = emotionCounts.values.max()

        // 2. 최대 등장 횟수를 가진 감정들 필터링
        val candidates = emotionCounts.filter { it.value == maxCount }.keys

        // 3. 우선순위가 가장 높은 감정 선택 (String을 EmotionType으로 변환하여 value 비교)
        // value가 높을수록 우선순위가 높음 (HAPPY=5가 가장 높음)
        val dominantEmotion = candidates.maxByOrNull { emotionString ->
            stringToEmotionType(emotionString).value
        }

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
            } catch (t: Throwable) {
                Timber.e(t, "수동 세션 동기화 예약 실패")
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

            when (val result =
                missionRepository.verifyWeeklyMissionReward(userWeeklyMissionId)) {
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
                } catch (t: Throwable) {
                    Timber.e(t, "미션 카드 상태 업데이트 실패")
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

