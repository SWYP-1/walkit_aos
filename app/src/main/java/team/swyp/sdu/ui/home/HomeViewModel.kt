package team.swyp.sdu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.core.DataState
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Goal
import timber.log.Timber
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.repository.UserRepository
import team.swyp.sdu.domain.repository.CharacterRepository
import team.swyp.sdu.domain.repository.GoalRepository
import team.swyp.sdu.domain.repository.MissionRepository
import team.swyp.sdu.domain.repository.HomeRepository
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Weather
import team.swyp.sdu.domain.model.WeeklyMission
import team.swyp.sdu.domain.model.WalkRecord
import team.swyp.sdu.data.remote.walking.dto.Grade
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
        val weather: Weather? = null,
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
        val weather: Weather?,
        val todaySteps: Int = 0
    ) : ProfileUiState

    data class Error(val message: String) : ProfileUiState
}

// Mission Section UiState
sealed interface MissionUiState {
    data object Loading : MissionUiState
    data class Success(
        val missions: List<WeeklyMission>
    ) : MissionUiState

    data object Empty : MissionUiState
    data class Error(val message: String) : MissionUiState
}

// Walking Session 데이터 모델 (API 독립적)
data class WalkingSessionData(
    val sessionsThisWeek: List<WalkingSession>,
    val dominantEmotion: EmotionType?,
    val recentEmotions: List<EmotionType?>
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walkingSessionRepository: WalkingSessionRepository,
    private val userRepository: UserRepository,
    private val characterRepository: CharacterRepository,
    private val goalRepository: GoalRepository,
    private val missionRepository: MissionRepository,
    private val homeRepository: HomeRepository,
    private val locationManager: LocationManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Section별 UiState 관리 (토스/배민 스타일)
    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

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
        loadWalkingSessionsFromRoom()  // API 독립적 로드
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
                walkingSessionRepository.getAllSessions().catch { e ->
                    _walkingSessionDataState.value =
                        DataState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }.collect { sessions ->
                    val thisWeekSessions = sessions.filterThisWeek()
                    val recentEmotions = sessions.sortedByDescending { it.startTime }.take(7)
                        .map { it.postWalkEmotion }
                    val dominantEmotion = findDominantEmotion(thisWeekSessions)

                    val walkingSessionData = WalkingSessionData(
                        sessionsThisWeek = thisWeekSessions,
                        dominantEmotion = dominantEmotion,
                        recentEmotions = recentEmotions
                    )

                    _walkingSessionDataState.value = DataState.Success(walkingSessionData)
                }
            } catch (e: Exception) {
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
            todaySteps = todayStepsFlow.value
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
            _missionUiState.value = MissionUiState.Success(missions = missions)
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
                val recentEmotions = sessions.sortedByDescending { it.startTime }.take(7)
                    .map { it.postWalkEmotion }
                val dominantEmotion = findDominantEmotion(thisWeekSessions)

                // 주간 미션
                val missions = homeData.weeklyMission?.let {
                    listOf(it)
                } ?: emptyList()

                _uiState.value = HomeUiState.Success(
                    character = homeData.character,
                    walkProgressPercentage = homeData.walkProgressPercentage,
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

    private fun loadUser() {
        viewModelScope.launch {
            userRepository.refreshUser().onSuccess { user ->
                // 현재 UI 상태에 사용자 정보 업데이트
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    // 사용자 정보 업데이트 - 필요한 경우 필드 업데이트
                    // 현재 HomeUiState.Success에는 nickname 등의 필드가 없으므로
                    // 별도 상태 관리로 분리됨
                }
            }.onError { throwable, message ->
                // 사용자 정보 로드 실패 - 기본값 유지
                Timber.e(throwable, "사용자 정보 로드 실패: $message")
            }
        }
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
                val recentEmotions = sessions.sortedByDescending { it.startTime }.take(7)
                    .map { it.postWalkEmotion }
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
     */
    private fun findDominantEmotion(sessions: List<WalkingSession>): EmotionType? {
        val emotionCounts = sessions.map { it.postWalkEmotion }.groupingBy { it }.eachCount()

        return emotionCounts.maxByOrNull { it.value }?.key
    }

}

