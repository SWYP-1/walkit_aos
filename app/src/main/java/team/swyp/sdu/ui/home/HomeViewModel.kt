package team.swyp.sdu.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import team.swyp.sdu.core.Result
import team.swyp.sdu.core.onError
import team.swyp.sdu.core.onSuccess
import team.swyp.sdu.domain.model.Goal
import timber.log.Timber
import team.swyp.sdu.data.model.Emotion
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
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.utils.LocationConstants
import java.time.LocalDate
import java.time.ZoneId

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val nickname: String = "",
        val levelLabel: String = "",
        val todaySteps: Int = 0,
        val sessionsThisWeek: List<WalkingSession>,
        val dominantEmotion : EmotionType? = null,
        val recentEmotions: List<EmotionType?> = emptyList(), // 최근 7개의 postWalkEmotion
        val missions: List<WeeklyMission> = emptyList(), // 주간 미션 목록
        // Domain 모델 사용 (클린 아키텍처 준수)
        val character: Character,
        val walkProgressPercentage: String = "0",
        val weather: Weather? = null,
        val weeklyMission: WeeklyMission? = null,
        val walkRecords: List<WalkRecord> = emptyList(),
        val goal: Goal? = null, // 목표 정보
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}


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

    init {
        loadHomeData()
    }

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

            // 위치 획득 시도
            val locationStartTime = System.currentTimeMillis()
            val location = getLocationForApi()
            val locationElapsedTime = System.currentTimeMillis() - locationStartTime
            Timber.tag(TAG_PERFORMANCE).d("위치 획득 완료 (전체): ${locationElapsedTime}ms, lat=${location.latitude}, lon=${location.longitude}")

            // 홈 API 호출
            val apiStartTime = System.currentTimeMillis()
            val homeResult = homeRepository.getHomeData(
                lat = location.latitude,
                lon = location.longitude
            )
            val apiElapsedTime = System.currentTimeMillis() - apiStartTime

            when (homeResult) {
                is Result.Success -> {
                    val homeData = homeResult.data
                    val totalElapsedTime = System.currentTimeMillis() - totalStartTime
                    Timber.tag(TAG_PERFORMANCE).d("Home 데이터 로드 완료 (전체): ${totalElapsedTime}ms (위치: ${locationElapsedTime}ms, API: ${apiElapsedTime}ms)")
                    
                    // Home API에서 받은 Character 정보를 Room에 저장
                    homeData.character.nickName?.let { nickname ->
                        characterRepository.saveCharacter(nickname, homeData.character)
                            .onError { exception, message ->
                                Timber.w(exception, "캐릭터 정보 저장 실패: $message")
                            }
                    }
                    
                    // 기존 로직 유지 (세션 정보 등)
                    loadSessionsWithHomeData(homeData)
                }
                is Result.Error -> {
                    val totalElapsedTime = System.currentTimeMillis() - totalStartTime
                    Timber.tag(TAG_PERFORMANCE).w("Home 데이터 로드 실패 (전체): ${totalElapsedTime}ms (위치: ${locationElapsedTime}ms, API: ${apiElapsedTime}ms)")
                    // API 실패 시 기존 로직으로 Fallback
                    Timber.w("홈 API 호출 실패, 기존 로직으로 Fallback")
                    loadDataFallback()
                }
                Result.Loading -> {
                    // 이미 Loading 상태
                }
            }
        }
    }

    /**
     * 홈 API 데이터와 함께 세션 정보 로드
     */
    private fun loadSessionsWithHomeData(homeData: team.swyp.sdu.domain.model.HomeData) {
        viewModelScope.launch {
            // 목표 정보 가져오기
            val goalResult = goalRepository.getGoal()
            val goal = when (goalResult) {
                is Result.Success -> goalResult.data
                else -> null
            }
            
            walkingSessionRepository
                .getAllSessions()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }
                .collect { sessions ->
                    val thisWeekSessions = sessions.filterThisWeek()
                    val recentEmotions = sessions
                        .sortedByDescending { it.startTime }
                        .take(7)
                        .map { it.postWalkEmotion }
                    val dominantEmotion = findDominantEmotion(thisWeekSessions)
                    
                    // 주간 미션 (Domain 모델 사용)
                    val missions = homeData.weeklyMission?.let { 
                        listOf(it)
                    } ?: emptyList()
                    
                    _uiState.value = HomeUiState.Success(
                        nickname = homeData.character.nickName ?: "사용자",
                        levelLabel = "${homeData.character.grade} Lv.${homeData.character.level}",
                        todaySteps = homeData.todaySteps,
                        sessionsThisWeek = thisWeekSessions,
                        recentEmotions = recentEmotions,
                        dominantEmotion = dominantEmotion,
                        missions = missions,
                        character = homeData.character,
                        walkProgressPercentage = homeData.walkProgressPercentage,
                        weather = homeData.weather,
                        weeklyMission = homeData.weeklyMission,
                        walkRecords = homeData.walkRecords,
                        goal = goal,
                    )
                }
        }
    }

    /**
     * 기존 로직으로 Fallback (API 실패 시)
     */
    private fun loadDataFallback() {
        viewModelScope.launch {
            // 사용자 정보와 목표 정보, 미션 정보 동시에 로드
            val userResult = userRepository.refreshUser()
            val goalResult = goalRepository.getGoal()
            val missionResult = missionRepository.getWeeklyMissions()

            // refreshUser 실패 시 Room의 기존 데이터 사용
            val nickname = when (userResult) {
                is Result.Success -> {
                    Timber.d("사용자 정보 갱신 성공: ${userResult.data.nickname}")
                    userResult.data.nickname ?: "사용자"
                }
                is Result.Error -> {
                    Timber.w(userResult.exception, "사용자 정보 갱신 실패, Room의 기존 데이터 사용")
                    // Room에서 기존 사용자 정보 가져오기
                    when (val cachedUserResult = userRepository.getUser()) {
                        is Result.Success -> cachedUserResult.data.nickname ?: "사용자"
                        else -> {
                            Timber.w("Room에 사용자 정보 없음, 기본값 사용")
                            "사용자"
                        }
                    }
                }
                Result.Loading -> {
                    Timber.w("사용자 정보 갱신 중, Room의 기존 데이터 사용")
                    when (val cachedUserResult = userRepository.getUser()) {
                        is Result.Success -> cachedUserResult.data.nickname ?: "사용자"
                        else -> "사용자"
                    }
                }
            }

            val goal = when (goalResult) {
                is Result.Success -> goalResult.data
                else -> null
            }

            val missions = when (missionResult) {
                is Result.Success -> missionResult.data
                else -> {
                    Timber.w("주간 미션 조회 실패")
                    emptyList()
                }
            }

            // 레벨과 오늘 걸음 수 계산
            val levelLabel = calculateLevelLabel(goal)
            val todaySteps = calculateTodaySteps()

            // 세션 정보 로드
            loadSessions(nickname, levelLabel, todaySteps, missions, goal)
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
            locationManager.getCurrentLocationOrLast()
                ?: getDefaultLocation()
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
            userRepository.refreshUser()
                .onSuccess { user ->
                    // 현재 UI 상태에 사용자 정보 업데이트
                    val currentState = _uiState.value
                    if (currentState is HomeUiState.Success) {
                        _uiState.value = currentState.copy(
                            nickname = user.nickname ?: "사용자",
                            levelLabel = "새싹 Lv.1", // TODO: 레벨 계산 로직 구현
                            todaySteps = 0, // TODO: 오늘 걸음 수 계산 로직 구현
                        )
                    }
                }
                .onError { throwable, message ->
                    // 사용자 정보 로드 실패 - 기본값 유지
                    Timber.e(throwable, "사용자 정보 로드 실패: $message")
                }
        }
    }

    /**
     * 목표 정보를 기반으로 레벨 라벨 계산
     */
    private fun calculateLevelLabel(goal: Goal?): String {
        return when {
            goal == null -> "새싹 Lv.1"
            goal.targetStepCount >= 10000 -> "나무 Lv.3"
            goal.targetStepCount >= 5000 -> "나무 Lv.2"
            goal.targetStepCount > 0 -> "나무 Lv.1"
            else -> "새싹 Lv.1"
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
            walkingSessionRepository
                .getAllSessions()
                .catch { e ->
                    _uiState.value = HomeUiState.Error(e.message ?: "세션을 불러오지 못했습니다.")
                }
                .collect { sessions ->
                    val thisWeekSessions = sessions.filterThisWeek()
                    val recentEmotions = sessions
                        .sortedByDescending { it.startTime }
                        .take(7)
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
                        nickname = nickname,
                        levelLabel = levelLabel,
                        todaySteps = todaySteps,
                        sessionsThisWeek = thisWeekSessions,
                        recentEmotions = recentEmotions,
                        dominantEmotion = dominantEmotion,
                        missions = missions,
                        character = defaultCharacter,
                        walkProgressPercentage = "0",
                        weather = null,
                        weeklyMission = null,
                        walkRecords = emptyList(),
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
                java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
        }.sortedByDescending { it.startTime }
    }

    /**
     * 이번주 산책에서 가장 많이 경험된 감정 찾기
     */
    private fun findDominantEmotion(sessions: List<WalkingSession>): EmotionType? {
        val emotionCounts = sessions
            .map { it.postWalkEmotion }
            .groupingBy { it }
            .eachCount()
        
        return emotionCounts
            .maxByOrNull { it.value }
            ?.key
    }

}

