package swyp.team.walkit.ui.walking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.walking.utils.emotionTypeToString
import swyp.team.walkit.domain.contract.WalkingRawEvent
import swyp.team.walkit.domain.contract.WalkingTrackingContract
import swyp.team.walkit.domain.model.StepValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.utils.LocationConstants
import swyp.team.walkit.data.repository.WalkingSessionRepository
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.Goal
import swyp.team.walkit.domain.repository.CharacterRepository
import swyp.team.walkit.domain.repository.GoalRepository
import swyp.team.walkit.domain.service.ActivityType
import swyp.team.walkit.domain.service.LocationManager
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.domain.service.MovementState
import swyp.team.walkit.domain.service.filter.PathSmoother
import swyp.team.walkit.utils.DateUtils
import android.content.Context
import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import swyp.team.walkit.core.Result
import swyp.team.walkit.core.onError
import swyp.team.walkit.core.onSuccess
import swyp.team.walkit.utils.loadLocationsFromJson
import timber.log.Timber
import javax.inject.Inject

// DataStore 키 정의
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "walking_prefs")

private object PreferencesKeys {
    val IS_WALKING_ACTIVE = booleanPreferencesKey("is_walking_active")
    val WALKING_START_TIME = longPreferencesKey("walking_start_time")
    val WALKING_STEP_COUNT = intPreferencesKey("walking_step_count")
    val WALKING_DURATION = longPreferencesKey("walking_duration")
    val WALKING_IS_PAUSED = booleanPreferencesKey("walking_is_paused")
    val PRE_WALKING_EMOTION = stringPreferencesKey("pre_walking_emotion")
    val POST_WALKING_EMOTION = stringPreferencesKey("post_walking_emotion")
}

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val tracking: WalkingTrackingContract,
    private val walkingSessionRepository: WalkingSessionRepository,
    private val locationManager: LocationManager,
    private val characterRepository: CharacterRepository,
    private val goalRepository: GoalRepository,
    private val lottieImageProcessor: LottieImageProcessor,
    private val pathSmoother: PathSmoother, // ✅ 경로 스무딩을 위한 PathSmoother 추가
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<WalkingUiState>(WalkingUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var durationJob: Job? = null

    private var startTimeMillis = 0L
    private var elapsedBeforePause = 0L
    private var lastStepCount = 0
    private var lastRawStepCount = 0

    // 현재 세션의 로컬 ID 저장
    private val _currentSessionLocalId = MutableStateFlow<String?>(null)

    // 세션 저장 중인지 여부
    private val _isSavingSession = MutableStateFlow(false)
    val isSavingSession = _isSavingSession.asStateFlow()

    // 현재 세션을 Flow로 관찰 (DB 변경 시 자동 업데이트)
    val currentSession: StateFlow<WalkingSession?> = _currentSessionLocalId
        .flatMapLatest { id ->
            if (id != null) {
                walkingSessionRepository.observeSessionById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 산책 전 감정을 저장 (StateFlow로 통일하여 일관성 유지)
    // 초기값을 HAPPY로 설정하여 에러 방지
    private val _preWalkingEmotion = MutableStateFlow<String?>("HAPPY")
    val preWalkingEmotion: StateFlow<String?> = _preWalkingEmotion.asStateFlow()

    // 산책 후 감정을 저장 (별도 화면에서 사용)
    private val _postWalkingEmotion = MutableStateFlow<String?>(null)

    // 산책 중 사용할 캐릭터 정보 (위치 기반)
    private val _walkingCharacter = MutableStateFlow<Character?>(null)
    val walkingCharacter: StateFlow<Character?> = _walkingCharacter.asStateFlow()

    // 산책 중 사용할 캐릭터 Lottie JSON
    private val _walkingCharacterLottieJson = MutableStateFlow<String?>(null)
    val walkingCharacterLottieJson: StateFlow<String?> = _walkingCharacterLottieJson.asStateFlow()

    // 이번 주 목표 초과 세션 개수
    private val _currentWeekGoalChallengeCount = MutableStateFlow(0)
    val currentWeekGoalChallengeCount: StateFlow<Int> = _currentWeekGoalChallengeCount.asStateFlow()

    // WalkingScreen 통합 상태 (UI에서 하나의 StateFlow로 사용)
    val walkingScreenState: StateFlow<WalkingScreenState> = combine(
        _uiState,
        _walkingCharacter,
        _walkingCharacterLottieJson,
        _currentWeekGoalChallengeCount
    ) { uiState, character, lottieJson, goalChallengeCount ->
        WalkingScreenState(uiState, character, lottieJson, goalChallengeCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalkingScreenState(WalkingUiState.Loading, null, null, 0)
    )

    // 현재 목표 정보를 저장 (targetStepCount 추출용)
    private var currentGoal: Goal? = null

    /**
     * 현재 목표 정보를 설정 (산책 시작 시 호출)
     */
    fun setCurrentGoal(goal: Goal?) {
        currentGoal = goal
        Timber.d("WalkingViewModel: 현재 목표 설정 - ${goal?.targetStepCount ?: 0} 걸음")
    }

    val postWalkingEmotion: StateFlow<String?> = _postWalkingEmotion.asStateFlow()

    private val _emotionPhotoUri = MutableStateFlow<android.net.Uri?>(null)
    val emotionPhotoUri: StateFlow<android.net.Uri?> = _emotionPhotoUri.asStateFlow()

    private val _emotionText = MutableStateFlow<String>("")
    val emotionText: StateFlow<String> = _emotionText.asStateFlow()

    // Location 리스트를 StateFlow로 노출 (Shared ViewModel을 위한)
    private val _locations = MutableStateFlow<List<LocationPoint>>(emptyList())
    val locations: StateFlow<List<LocationPoint>> = _locations.asStateFlow()

    // 센서 상태 관리
    private val _sensorStatus = MutableStateFlow<SensorStatus>(
        SensorStatus(
            isStepCounterActive = false,
            isAccelerometerActive = false,
            isActivityRecognitionActive = false,
            isLocationTrackingActive = false,
        )
    )
    val sensorStatus: StateFlow<SensorStatus> = _sensorStatus.asStateFlow()

    // 현재 활동 인식 상태
    private val _currentActivityType = MutableStateFlow<ActivityType?>(null)
    val currentActivityType: StateFlow<ActivityType?> = _currentActivityType.asStateFlow()

    // 활동 인식 신뢰도
    private val _activityConfidence = MutableStateFlow<Int>(0)
    val activityConfidence: StateFlow<Int> = _activityConfidence.asStateFlow()

    // 가속도계 데이터
    private val _currentAcceleration = MutableStateFlow<Float>(0f)
    val currentAcceleration: StateFlow<Float> = _currentAcceleration.asStateFlow()

    private val _currentMovementState = MutableStateFlow<MovementState?>(null)
    val currentMovementState: StateFlow<MovementState?> = _currentMovementState.asStateFlow()

    // 현재 위치 정보
    private val _currentLocation = MutableStateFlow<LocationPoint?>(null)
    val currentLocation: StateFlow<LocationPoint?> = _currentLocation.asStateFlow()

    // 최신 걸음 수 검증 결과
    private val _latestValidationResult = MutableStateFlow<StepValidationResult?>(null)
    val latestValidationResult: StateFlow<StepValidationResult?> =
        _latestValidationResult.asStateFlow()

    // 스냅샷 생성 및 서버 동기화 상태
    private val _snapshotState = MutableStateFlow<SnapshotState>(SnapshotState.Idle)
    val snapshotState: StateFlow<SnapshotState> = _snapshotState.asStateFlow()

    // 세션 저장 완료 상태 추적
    private val _isSessionSaved = MutableStateFlow(false)
    val isSessionSaved: StateFlow<Boolean> = _isSessionSaved.asStateFlow()

    /**
     * 감정 기록 사진 URI 설정
     */
    fun setEmotionPhotoUri(uri: android.net.Uri?) {
        _emotionPhotoUri.value = uri
    }

    /**
     * 감정 기록 텍스트 설정
     */
    fun setEmotionText(text: String) {
        _emotionText.value = text
    }

    fun selectPreWalkingEmotion(emotionType: EmotionType) {
        val currentState = _uiState.value
        if (currentState is WalkingUiState.PreWalkingEmotionSelection) {
            val emotionString = emotionTypeToString(emotionType)
            _preWalkingEmotion.value = emotionString
            _postWalkingEmotion.value = emotionString
            _uiState.value = currentState.copy(
                preWalkingEmotion = emotionType,
            )
        }
    }

    fun selectPostWalkingEmotion(emotionType: EmotionType) {
        val emotionString = emotionTypeToString(emotionType)
        _postWalkingEmotion.value = emotionString
        Timber.i("Post Emotion : $emotionString")
    }

    /**
     * PostWalkingEmotion이 설정되지 않았다면 PreWalkingEmotion으로 초기화
     */
    fun initializePostWalkingEmotionIfNeeded() {
        if (_postWalkingEmotion.value == null) {
            val preEmotion = _preWalkingEmotion.value
            if (preEmotion != null) {
                _postWalkingEmotion.value = preEmotion
                Timber.d("🚶 initializePostWalkingEmotionIfNeeded - 초기화됨: $preEmotion")
            }
        }
    }

    /**
     * 산책 전 감정 초기화 (새 산책 시작 시)
     */
    fun resetPreWalkingEmotion() {
        _preWalkingEmotion.value = null
    }

    /**
     * 산책 후 감정 초기화 (새 산책 시작 시)
     */
    fun resetPostWalkingEmotion() {
        _postWalkingEmotion.value = null
    }

    init {
        Timber.d("🚶 WalkingViewModel init 시작 - hashCode: ${this.hashCode()}, Thread: ${Thread.currentThread().name}")
        observeRawEvents()
        observeTrackingStatus()
        updateSensorAvailability()
        restoreWalkingStateFromDataStore() // DataStore에서 산책 상태 복원
        // 캐릭터 정보 로드는 WalkingScreen에서 명시적으로 호출하도록 변경

        // 세션 저장 상태 초기화
        _isSessionSaved.value = false
        Timber.d("WalkingViewModel init 완료 - hashCode: ${this.hashCode()}")
    }


    /**
     * 산책 상태를 DataStore에 저장
     */
    private fun saveWalkingStateToDataStore() {
        viewModelScope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val currentState = _uiState.value
                    if (currentState is WalkingUiState.Walking) {
                        preferences[PreferencesKeys.IS_WALKING_ACTIVE] = true
                        preferences[PreferencesKeys.WALKING_START_TIME] = startTimeMillis
                        preferences[PreferencesKeys.WALKING_STEP_COUNT] = currentState.stepCount
                        preferences[PreferencesKeys.WALKING_DURATION] = currentState.duration
                        preferences[PreferencesKeys.WALKING_IS_PAUSED] = currentState.isPaused
                        preferences[PreferencesKeys.PRE_WALKING_EMOTION] =
                            _preWalkingEmotion.value ?: ""
                        preferences[PreferencesKeys.POST_WALKING_EMOTION] =
                            _postWalkingEmotion.value ?: ""
                    } else {
                        preferences[PreferencesKeys.IS_WALKING_ACTIVE] = false
                        // 다른 키들은 유지 (다음 복원을 위해)
                    }
                }
                Timber.d("산책 상태 DataStore에 저장됨: ${_uiState.value}")
            } catch (t: Throwable) {
                Timber.e(t, "DataStore 저장 실패")
            }
        }
    }

    /**
     * 산책용 캐릭터 정보 로드 (현재 위치 기반)
     * 화면에서 필요할 때 호출하기 위해 public으로 변경
     */
    fun loadWalkingCharacterIfNeeded() {
        Timber.d("🔍 loadWalkingCharacterIfNeeded 호출됨 - character: ${_walkingCharacter.value?.nickName ?: "null"}, hash: ${this.hashCode()}")
        if (_walkingCharacter.value == null) {
            Timber.d("🔄 캐릭터 정보가 없어서 loadWalkingCharacter 호출")
            loadWalkingCharacter()
        } else {
            Timber.d("✅ 캐릭터 정보가 이미 있어서 스킵")
        }
    }

    /**
     * 산책용 캐릭터 정보 로드 (현재 위치 기반)
     */
    private fun loadWalkingCharacter() {
        viewModelScope.launch {
            try {
                Timber.d("산책용 캐릭터 정보 로드 시작")

                // 현재 위치 가져오기 (캐시된 마지막 위치 우선 사용)
                val currentLocation = locationManager.getCurrentLocationOrLast()

                // 위치 정보를 가져올 수 없으면 서울 시청 좌표를 기본값으로 사용
                val (lat, lon) = if (currentLocation != null) {
                    val latitude = currentLocation.latitude
                    val longitude = currentLocation.longitude
                    Timber.d("현재 위치로 캐릭터 정보 조회: lat=$latitude, lon=$longitude")
                    latitude to longitude
                } else {
                    // 서울 시청 좌표 (기본값)
                    val defaultLat = LocationConstants.DEFAULT_LATITUDE
                    val defaultLon = LocationConstants.DEFAULT_LONGITUDE
                    Timber.d("현재 위치를 가져올 수 없어 서울 시청 좌표로 캐릭터 정보 조회: lat=$defaultLat, lon=$defaultLon")
                    defaultLat to defaultLon
                }

                // 위치 기반 캐릭터 정보 API 호출 (항상 실행)
                characterRepository.getCharacterByLocation(lat, lon)
                    .onSuccess { character ->
                        _walkingCharacter.value = character
                        Timber.d("산책용 캐릭터 정보 로드 성공: ${character.nickName}")

                        // 캐릭터 정보가 있으면 Lottie JSON 생성
                        generateWalkingCharacterLottie(character)

                        // 이번 주 목표 초과 세션 개수 계산
                        calculateCurrentWeekGoalChallengeCount()
                    }
                    .onError { exception, message ->
                        Timber.e(exception, "산책용 캐릭터 정보 로드 실패: $message")
                        // 실패 시 기본 캐릭터 정보는 null로 유지
                    }
            } catch (t: Throwable) {
                Timber.e(t, "산책용 캐릭터 정보 로드 중 예외 발생")
            }
        }
    }

    /**
     * 산책용 캐릭터 Lottie JSON 생성
     * DressingRoom과 동일한 방식으로 캐릭터 기본 이미지가 적용된 깨끗한 baseJson 사용
     */
    private fun generateWalkingCharacterLottie(character: Character) {
        viewModelScope.launch {
            try {
                Timber.d("산책용 캐릭터 Lottie JSON 생성 시작")

                // DressingRoom과 동일하게 캐릭터 기본 이미지가 적용된 깨끗한 baseJson 생성
                val cleanBaseJson = createCleanBaseJson(character)

                // 생성된 JSON을 문자열로 변환해서 저장
                val lottieJsonString = cleanBaseJson.toString()
                _walkingCharacterLottieJson.value = lottieJsonString

                Timber.d("산책용 캐릭터 Lottie JSON 생성 완료: ${lottieJsonString.length} chars")
            } catch (t: Throwable) {
                Timber.e(t, "산책용 캐릭터 Lottie JSON 생성 실패")
                _walkingCharacterLottieJson.value = null
            }
        }
    }

    /**
     * DressingRoom과 동일한 방식으로 캐릭터 기본 이미지가 적용된 깨끗한 baseJson 생성
     */
    private suspend fun createCleanBaseJson(character: Character): JSONObject =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("🧹 Walking createCleanBaseJson 시작")

                // 기본 Lottie JSON 로드
                val jsonObject = loadBaseLottieJson(character)
                Timber.d("📂 기본 Lottie JSON 로드 완료")

                // 캐릭터의 기본 이미지로 asset들을 교체 (투명 PNG 적용)
                val characterBaseJson =
                    lottieImageProcessor.updateCharacterPartsInLottie(jsonObject, character)
                Timber.d("✅ 캐릭터 기본 이미지 설정 완료")

                characterBaseJson
            } catch (t: Throwable) {
                Timber.e(t, "❌ cleanBaseJson 생성 실패")
                JSONObject("{}") // 빈 JSON 반환
            }
        }

    /**
     * 이번 주 목표 초과 세션 개수 계산
     * 이번 주(월요일부터)에 시작한 세션 중 목표 걸음 수를 초과한 세션의 개수
     */
    private fun calculateCurrentWeekGoalChallengeCount() {
        viewModelScope.launch {
            try {
                // 이번 주 범위 계산 (월요일부터)
                val weekRange = DateUtils.getCurrentWeekRange()
                Timber.d("이번 주 범위: ${weekRange.first} ~ ${weekRange.second}")

                // 이번 주 세션들 가져오기
                val thisWeekSessions =
                    walkingSessionRepository.getSessionsBetween(weekRange.first, weekRange.second)
                        .firstOrNull() ?: emptyList()

                Timber.d("이번 주 세션 수: ${thisWeekSessions.size}")

                // 현재 목표 가져오기
                val goalResult = goalRepository.getGoal()
                val currentGoal = when (goalResult) {
                    is Result.Success -> goalResult.data
                    else -> null
                }
                if (currentGoal == null) {
                    Timber.d("목표가 설정되지 않음")
                    _currentWeekGoalChallengeCount.value = 0
                    return@launch
                }

                // 목표 걸음 수 초과한 세션 개수 계산
                val goalExceededCount = thisWeekSessions.count { session ->
                    session.stepCount > currentGoal.targetStepCount
                }

                Timber.d("목표 걸음 수(${currentGoal.targetStepCount}) 초과 세션 수: $goalExceededCount")
                _currentWeekGoalChallengeCount.value = goalExceededCount

            } catch (t: Throwable) {
                Timber.e(t, "이번 주 목표 초과 세션 개수 계산 실패")
                _currentWeekGoalChallengeCount.value = 0
            }
        }
    }

    /**
     * 기본 Lottie JSON 로드
     */
    private suspend fun loadBaseLottieJson(character: Character): JSONObject =
        withContext(Dispatchers.IO) {
            try {
                // 캐릭터 grade에 따라 적절한 Lottie 리소스 선택
                val resourceId = when (character.grade) {
                    Grade.SEED -> R.raw.seed
                    Grade.SPROUT -> R.raw.sprout
                    Grade.TREE -> R.raw.tree
                }

                Timber.d("🎭 Walking loadBaseLottieJson: grade=${character.grade}, resourceId=$resourceId")

                // res/raw에서 기본 캐릭터 Lottie JSON 로드
                val inputStream = context.resources.openRawResource(resourceId)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                JSONObject(jsonString)
            } catch (t: Throwable) {
                Timber.e(t, "기본 Lottie JSON 로드 실패, 빈 JSON 사용")
                JSONObject("{}")
            }
        }


    /**
     * DataStore에서 산책 상태 복원
     */
    private fun restoreWalkingStateFromDataStore() {
        viewModelScope.launch {
            try {
                val preferences = context.dataStore.data.firstOrNull()
                val isWalkingActive = preferences?.get(PreferencesKeys.IS_WALKING_ACTIVE) ?: false

                if (isWalkingActive) {
                    val startTime = preferences.get(PreferencesKeys.WALKING_START_TIME) ?: 0L

                    // 앱 강제종료 대응: 산책 시작 후 2시간 이상 지났으면 무효화
                    val currentTime = System.currentTimeMillis()
                    val hoursSinceStart = (currentTime - startTime) / (1000 * 60 * 60)

                    if (hoursSinceStart >= 2) {
                        Timber.w("산책 시작 후 24시간 이상 경과하여 DataStore 상태를 무효화합니다")
                        clearWalkingStateFromDataStore()
                        return@launch
                    }

                    val stepCount = preferences.get(PreferencesKeys.WALKING_STEP_COUNT) ?: 0
                    val savedDuration = preferences.get(PreferencesKeys.WALKING_DURATION) ?: 0L
                    val isPaused = preferences.get(PreferencesKeys.WALKING_IS_PAUSED) ?: false
                    val preEmotionName = preferences.get(PreferencesKeys.PRE_WALKING_EMOTION) ?: ""
                    val postEmotionName =
                        preferences.get(PreferencesKeys.POST_WALKING_EMOTION) ?: ""

                    // 시간 경과 계산 (앱 종료 후 재시작까지의 시간)
                    val elapsedSinceSave = currentTime - startTime - savedDuration
                    val currentDuration = savedDuration + (if (!isPaused) elapsedSinceSave else 0L)

                    // Walking 상태로 복원
                    _uiState.value = WalkingUiState.Walking(
                        stepCount = stepCount,
                        duration = currentDuration,
                        isPaused = true // 재시작 시 일시정지 상태로 시작
                    )

                    // 감정 상태 복원 (String으로 직접 저장)
                    if (preEmotionName.isNotEmpty()) {
                        _preWalkingEmotion.value = preEmotionName
                    } else {
                        _preWalkingEmotion.value = "HAPPY" // 기본값
                    }
                    if (postEmotionName.isNotEmpty()) {
                        _postWalkingEmotion.value = postEmotionName
                    }

                    // 시간 변수 복원
                    startTimeMillis = startTime
                    elapsedBeforePause = if (isPaused) currentDuration else 0L

                    Timber.d("DataStore에서 산책 상태 복원됨: stepCount=$stepCount, duration=$currentDuration")
                } else {
                    // 산책 상태가 없거나 무효화된 경우 기본 감정 선택 상태로 설정
                    _uiState.value = WalkingUiState.PreWalkingEmotionSelection()
                    Timber.d("DataStore에 유효한 산책 상태가 없어 기본 상태로 초기화")
                }
            } catch (t: Throwable) {
                Timber.e(t, "DataStore 복원 실패")
                // 에러 발생 시에도 기본 상태로 설정
                _uiState.value = WalkingUiState.PreWalkingEmotionSelection()
            }
        }
    }

    /**
     * DataStore에서 산책 상태 초기화 (산책 완료/취소 시)
     */
    private suspend fun clearWalkingStateFromDataStore() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(PreferencesKeys.IS_WALKING_ACTIVE)
                preferences.remove(PreferencesKeys.WALKING_START_TIME)
                preferences.remove(PreferencesKeys.WALKING_STEP_COUNT)
                preferences.remove(PreferencesKeys.WALKING_DURATION)
                preferences.remove(PreferencesKeys.WALKING_IS_PAUSED)
                preferences.remove(PreferencesKeys.PRE_WALKING_EMOTION)
                preferences.remove(PreferencesKeys.POST_WALKING_EMOTION)
            }
            Timber.d("DataStore에서 산책 상태 초기화됨")
        } catch (t: Throwable) {
            Timber.e(t, "DataStore 초기화 실패")
        }
    }

    /**
     * 앱 재시작 시 저장된 세션 상태 복원 (DB 기반)
     */
//    private fun restoreSessionState() {
//        viewModelScope.launch {
//            try {
//                // 가장 최근의 미완료 세션 조회 (endTime이 null인 세션)
//                val latestIncompleteSession = walkingSessionRepository.getAllSessions()
//                    .firstOrNull()
//                    ?.firstOrNull { it.endTime == null }
//
//                if (latestIncompleteSession != null) {
//                    Timber.d("미완료 세션 발견, Walking 상태로 복원: ${latestIncompleteSession.id}")
//
//                    // 세션 ID 설정 (Flow가 자동으로 세션 데이터를 로드)
//                    _currentSessionLocalId.value = latestIncompleteSession.id
//
//                    // Walking 상태로 복원
//                    _uiState.value = WalkingUiState.Walking(
//                        stepCount = latestIncompleteSession.stepCount,
//                        duration = System.currentTimeMillis() - latestIncompleteSession.startTime,
//                        isPaused = false // 재시작 시 일시정지 해제
//                    )
//
//                    // 기존 감정 상태 복원
//                    _preWalkingEmotion.value = latestIncompleteSession.preWalkEmotion
//                    _postWalkingEmotion.value = latestIncompleteSession.postWalkEmotion
//
//                    // 트래킹 재시작 (포그라운드 서비스 재개)
//                    tracking.startTracking()
//                }
//            } catch (t: Throwable) {
//                Timber.e(t, "세션 상태 복원 실패")
//                // 복원 실패 시 기본 상태 유지
//            }
//        }
//    }

    /**
     * 추적 상태 관찰 및 센서 상태 업데이트
     */
    private fun observeTrackingStatus() {
        tracking.isTracking
            .onEach { isTracking ->
                _sensorStatus.value = _sensorStatus.value.copy(
                    isStepCounterActive = isTracking && tracking.isStepCounterAvailable(),
                    isAccelerometerActive = isTracking && tracking.isAccelerometerAvailable(),
                    isActivityRecognitionActive = isTracking && tracking.isActivityRecognitionAvailable(),
                    isLocationTrackingActive = isTracking,
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * 센서 사용 가능 여부 업데이트
     */
    private fun updateSensorAvailability() {
        _sensorStatus.value = SensorStatus(
            isStepCounterActive = false,
            isAccelerometerActive = false,
            isActivityRecognitionActive = false,
            isLocationTrackingActive = false,
            isStepCounterAvailable = tracking.isStepCounterAvailable(),
            isAccelerometerAvailable = tracking.isAccelerometerAvailable(),
            isActivityRecognitionAvailable = tracking.isActivityRecognitionAvailable(),
        )
    }

    /* ---------------- Raw Event ---------------- */

    private fun observeRawEvents() {
        tracking.rawEvents
            .onEach { reduce(it) }
            .launchIn(viewModelScope)
    }

    private fun reduce(event: WalkingRawEvent) {
        when (event) {
            is WalkingRawEvent.TrackingPaused -> handleTrackingPaused()
            is WalkingRawEvent.TrackingResumed -> handleTrackingResumed()
            is WalkingRawEvent.StepCountUpdate ->
                handleStepCountUpdate(event.rawStepCount, event.validationResult)

            is WalkingRawEvent.LocationUpdate ->
                handleLocationUpdate(event.locations)

            is WalkingRawEvent.ActivityStateChange ->
                handleActivityStateChange(event.activityType, event.confidence)

            is WalkingRawEvent.AccelerometerUpdate ->
                handleAccelerometerUpdate(event.acceleration, event.movementState)

            else -> Unit
        }
    }

    /* ---------------- Actions ---------------- */

    suspend fun startWalking() {
        // 산책 전 감정이 선택되었는지 확인 (UI에서 이미 체크하지만, 안전장치)
        val preEmotion = _preWalkingEmotion.value
        require(preEmotion != null) { "산책 전 감정을 선택해야 합니다" }

        // 로그인된 사용자인지 확인 (임시 세션 방지)
        Timber.d("산책 시작 전 사용자 ID 확인")
        val currentUserId = walkingSessionRepository.getCurrentUserId()
        Timber.d("산책 시작: currentUserId=$currentUserId")
        require(currentUserId != 0L) { "로그인이 필요합니다. 산책을 시작할 수 없습니다." }

        // 산책 시작 시 캐릭터 정보 로드 (최초 1회만)
        if (_walkingCharacter.value == null) {
            loadWalkingCharacter()
        }

        // 새로운 산책 시작 전 DataStore 초기화 (이전 잔여 데이터 제거)
        clearWalkingStateFromDataStore()

        // 세션 저장 상태 초기화
        _isSessionSaved.value = false
        _currentSessionLocalId.value = null

        startTimeMillis = System.currentTimeMillis()
        // elapsedBeforePause는 유지 (강제 종료 후 재시작 시 이전 시간 보존)
        lastStepCount = 0
        lastRawStepCount = 0

        // 위치 리스트 초기화 및 현재 위치를 첫 번째에 추가
        viewModelScope.launch {
            val initialLocations = mutableListOf<LocationPoint>()

            // 현재 위치 가져오기 (getCurrentLocation 실패 시 getLastLocation 시도)
            // fusedLocation의 최신 위치를 가져와서 크기가 0이 아닌 location 리스트 생성
            val currentLocation = locationManager.getCurrentLocationOrLast()
            if (currentLocation != null) {
                // 현재 위치를 첫 번째에 추가
                initialLocations.add(currentLocation)
                _currentLocation.value = currentLocation
                Timber.d("산책 시작: 현재 위치를 locations 배열 첫 번째에 추가 - ${currentLocation.latitude}, ${currentLocation.longitude}, accuracy=${currentLocation.accuracy}m")
            } else {
                Timber.w("산책 시작: 현재 위치와 마지막 위치 모두 가져올 수 없음. LocationTrackingService가 위치를 추적하여 추가할 예정")
            }

            _locations.value = initialLocations
        }

        viewModelScope.launch {
            tracking.startTracking()
        }
        _uiState.value =
            WalkingUiState.Walking(
                stepCount = 0,
                duration = 0L,
            )

        // 활동 상태 초기화
        _currentActivityType.value = null
        _activityConfidence.value = 0
        _currentAcceleration.value = 0f
        _currentMovementState.value = null

        startDurationUpdates()
        updateSensorStatus()

        // DataStore에 산책 상태 저장
        saveWalkingStateToDataStore()
    }

    fun pauseWalking() {
        viewModelScope.launch {
            tracking.pauseTracking()
        }
        // 타이머 업데이트 일시정지 (일시정지 시 불필요한 타이머 업데이트 중단)
        durationJob?.cancel()
        durationJob = null
        // DataStore에 일시정지 상태 저장
        saveWalkingStateToDataStore()
    }

    fun resumeWalking() {
        viewModelScope.launch {
            tracking.resumeTracking()
        }
        // 타이머 업데이트 재개 (일시정지 해제 시 타이머를 다시 시작)
        startDurationUpdates()
        // DataStore에 재개 상태 저장
        saveWalkingStateToDataStore()
    }

    /**
     * 산책 취소 (세션 저장 없이 추적만 중단)
     */
    fun cancelWalking() {
        viewModelScope.launch {
            tracking.stopTracking() // suspend
            durationJob?.cancel()
            updateSensorStatus()
            clearWalkingStateFromDataStore() // suspend
            Timber.d("산책 취소됨 - 추적 중단 및 DataStore 초기화")
        }
    }


    /**
     * 산책 종료 및 세션 저장
     *
     * 세션 저장이 완료될 때까지 기다린 후 Completed 상태로 변경합니다.
     */
    /**
     * 산책이 진행 중인 경우에만 중단 (안전한 중단)
     */
    suspend fun stopWalkingIfNeeded() {
        val currentState = _uiState.value
        if (currentState is WalkingUiState.Walking) {
            Timber.d("🚶 WalkingViewModel.stopWalkingIfNeeded - 산책 진행 중이므로 중단")
            stopWalking()
        } else if(currentState is WalkingUiState.PreWalkingEmotionSelection){
            stopWalking()
            Timber.d("🚶 WalkingViewModel.stopWalkingIfNeeded - pre감정 선택 진행 중이므로 중단")
        } else {
            Timber.d("🚶 WalkingViewModel.stopWalkingIfNeeded - 산책 진행 중이 아님, 중단 불필요")
        }
    }

    suspend fun stopWalking() {
        tracking.stopTracking()
        durationJob?.cancel()

        // 센서 상태 업데이트
        updateSensorStatus()

        // 완료된 세션 생성 (현재 메모리 데이터로 즉시 생성)
        val targetStepCount = currentGoal?.targetStepCount ?: 0
        val targetWalkCount = currentGoal?.targetWalkCount ?: 0
        val completedSession = createCompletedSession(targetStepCount = targetStepCount, targetWalkCount = targetWalkCount)

        // 세션 저장 중 상태로 변경
        _isSavingSession.value = true

        // DB에 저장하고 localId를 받아옴 (완료될 때까지 동기적으로 대기)
        try {
            Timber.d("🚶 WalkingViewModel.stopWalking - 저장 전: viewModel.hashCode=${this.hashCode()}, currentSessionLocalId=${_currentSessionLocalId.value}")
            Timber.d("🚶 WalkingViewModel.stopWalking - 저장 전 세션 정보: userId=${completedSession.userId}, localId=${completedSession.id}")
            val sessionId = walkingSessionRepository.createSessionPartial(completedSession)
            Timber.d("🚶 WalkingViewModel.stopWalking - 저장 후: viewModel.hashCode=${this.hashCode()}, currentSessionLocalId=$sessionId, postEmotion=${completedSession.postWalkEmotion}")
            Timber.d("부분 세션 저장 완료: localId=$sessionId, userId=${completedSession.userId}, postEmotion=${completedSession.postWalkEmotion}")

            // ⭐ DB 저장이 완료된 후 세션 ID만 설정 (UI 상태는 이미 변경됨)
            _currentSessionLocalId.value = sessionId
            Timber.d("🚶 WalkingViewModel.stopWalking - _currentSessionLocalId 설정 완료: ${_currentSessionLocalId.value}, ViewModel hashCode: ${this.hashCode()}")
            _isSessionSaved.value = true  // 세션 저장 완료 플래그 설정
            _isSavingSession.value = false  // 세션 저장 완료
            // _uiState.value는 이미 버튼 클릭 시 finishWalking()에서 변경됨

            // DataStore에서 산책 상태 초기화 (산책이 완료되었으므로)
            clearWalkingStateFromDataStore()

            Timber.d("🚶 WalkingViewModel.stopWalking - 모든 작업 완료: sessionId=$sessionId")
        } catch (t: Throwable) {
            Timber.e(t, "부분 세션 저장 실패")
            // 에러 발생 시 Error 상태로 변경 (사용자에게 에러 표시)
            _isSessionSaved.value = false  // 세션 저장 실패 플래그
            _isSavingSession.value = false  // 세션 저장 실패
            Timber.e(message = t.message)
            // 에러를 다시 던지지 않고 로그만 남김 (UI에서 에러 상태 표시)
        }
    }

    /**
     * 센서 상태 업데이트
     */
    private fun updateSensorStatus() {
        val isTracking = tracking.isTracking.value
        _sensorStatus.value = _sensorStatus.value.copy(
            isStepCounterActive = isTracking && tracking.isStepCounterAvailable(),
            isAccelerometerActive = isTracking && tracking.isAccelerometerAvailable(),
            isActivityRecognitionActive = isTracking && tracking.isActivityRecognitionAvailable(),
            isLocationTrackingActive = isTracking,
        )
    }

    /* ---------------- Reducers ---------------- */

    private fun handleTrackingPaused() {
        val state = _uiState.value
        if (state is WalkingUiState.Walking && !state.isPaused) {
            elapsedBeforePause = state.duration
            _uiState.value = state.copy(isPaused = true)
        }
    }

    private fun handleTrackingResumed() {
        val state = _uiState.value
        if (state is WalkingUiState.Walking && state.isPaused) {
            startTimeMillis = System.currentTimeMillis()
            _uiState.value = state.copy(isPaused = false)
        }
    }

    private fun handleStepCountUpdate(
        validatedStepCount: Int,
        validationResult: StepValidationResult? = null
    ) {
        lastRawStepCount = validatedStepCount

        // 검증 결과를 저장 (UI 표시용)
        _latestValidationResult.value = validationResult

        val state = _uiState.value
        if (state is WalkingUiState.Walking && !state.isPaused) {
            // 검증된 걸음 수만 사용 (검증 실패 시 증가하지 않음)
            lastStepCount = validatedStepCount
            _uiState.value = state.copy(stepCount = validatedStepCount)
        }
    }

    /**
     * 위치 업데이트 처리
     */
    private fun handleLocationUpdate(newLocations: List<LocationPoint>) {
        val currentLocations = _locations.value.toMutableList()

        // 새로운 위치 포인트 추가 (중복 제거)
        newLocations.forEach { newPoint ->
            val exists = currentLocations.any { existing ->
                existing.timestamp == newPoint.timestamp ||
                        (kotlin.math.abs(existing.latitude - newPoint.latitude) < 0.000001 &&
                                kotlin.math.abs(existing.longitude - newPoint.longitude) < 0.000001)
            }

            if (!exists) {
                currentLocations.add(newPoint)
            }
        }

        _locations.value = currentLocations

        // 최신 위치를 현재 위치로 설정
        if (newLocations.isNotEmpty()) {
            _currentLocation.value = newLocations.last()
        }

        Timber.d("위치 업데이트: ${newLocations.size}개 포인트 추가, 총 ${currentLocations.size}개 포인트")
    }

    /**
     * 활동 상태 변경 처리
     */
    private fun handleActivityStateChange(activityType: ActivityType, confidence: Int) {
        _currentActivityType.value = activityType
        _activityConfidence.value = confidence
        Timber.d("활동 상태 변경: ${activityType.name}, 신뢰도: ${confidence}%")
    }

    /**
     * 가속도계 업데이트 처리
     */
    private fun handleAccelerometerUpdate(acceleration: Float, movementState: MovementState) {
        _currentAcceleration.value = acceleration
        _currentMovementState.value = movementState
    }

    /* ---------------- Duration ---------------- */

    private fun startDurationUpdates() {
        durationJob?.cancel()
        durationJob =
            viewModelScope.launch {
                while (true) {
                    delay(1_000)
                    val state = _uiState.value
                    if (state is WalkingUiState.Walking && !state.isPaused) {
                        val duration =
                            elapsedBeforePause +
                                    (System.currentTimeMillis() - startTimeMillis)

                        _uiState.value = state.copy(duration = duration)
                    }
                }
            }
    }

    /* ---------------- Session Completion ---------------- */

    /**
     * 완료된 세션 생성 (현재 메모리 데이터로 즉시 생성)
     *
     * 하이브리드 접근: 메모리에서 즉시 세션 객체를 생성하여 Completed 상태로 사용
     */
    private suspend fun createCompletedSession(targetStepCount: Int = 0, targetWalkCount: Int = 0): WalkingSession {
        val preEmotion = _preWalkingEmotion.value
            ?: throw IllegalStateException("산책 전 감정이 선택되지 않았습니다")

        // postWalkEmotion이 선택되지 않았으면 preWalkEmotion과 동일하게 설정
        val postEmotion = _postWalkingEmotion.value ?: preEmotion

        val endTime = System.currentTimeMillis()
//        val collectedLocations = _locations.value
        //TODO : 삭제
        val collectedLocations = loadLocationsFromJson(context)
        val totalDistance = calculateTotalDistance(collectedLocations)

        // ✅ 경로 스무딩 적용
        val smoothedLocations = if (collectedLocations.size >= 3) {
            try {
                val latitudes = collectedLocations.map { it.latitude }
                val longitudes = collectedLocations.map { it.longitude }

                val (smoothedLats, smoothedLngs) = pathSmoother.smoothPath(latitudes, longitudes)

                // 스무딩된 데이터를 LocationPoint로 변환
                smoothedLats.zip(smoothedLngs).map { (lat, lng) ->
                    LocationPoint(
                        latitude = lat,
                        longitude = lng,
                        timestamp = endTime, // 스무딩된 포인트는 종료 시간으로 설정
                        accuracy = null // 스무딩된 데이터는 정확도 정보 없음
                    )
                }.also { smoothedPoints ->
                    Timber.d("✅ 경로 스무딩 적용: ${collectedLocations.size} → ${smoothedPoints.size} 포인트")
                }
            } catch (e: Exception) {
                Timber.w(e, "❌ 경로 스무딩 실패, 원본 데이터 사용")
                null
            }
        } else {
            Timber.d("ℹ️ 위치 데이터가 부족하여 스무딩 생략 (${collectedLocations.size} 포인트)")
            null
        }

        // 현재 사용자 ID 가져오기
        val currentUserId = walkingSessionRepository.getCurrentUserId()
        Timber.d("createCompletedSession: currentUserId=$currentUserId, smoothedLocations=${smoothedLocations?.size ?: 0}")

        // 완료된 세션 생성 (note, localImagePath, serverImageUrl은 null, 나중에 업데이트됨)
        return WalkingSession(
            startTime = startTimeMillis,
            endTime = endTime,
            stepCount = lastStepCount,
            locations = smoothedLocations ?: collectedLocations, // 원본 위치 데이터
            totalDistance = totalDistance,
            preWalkEmotion = preEmotion,
            postWalkEmotion = postEmotion, // 기본값은 preWalkEmotion과 동일
            note = null, // 나중에 업데이트
            localImagePath = null, // 나중에 업데이트
            serverImageUrl = null, // 서버 동기화 후 업데이트
            createdDate = DateUtils.formatToIsoDateTime(startTimeMillis),
            targetStepCount = targetStepCount,
            targetWalkCount = targetWalkCount,
            userId = currentUserId // ✅ 현재 사용자 ID 설정
        )
    }


    /**
     * 산책 후 감정 업데이트 (PostWalkingEmotionScreen에서 선택 시 호출)
     *
     * @param postWalkEmotion 선택된 산책 후 감정
     */
    /**
     * 산책 후 감정 업데이트 (PostWalkingEmotionScreen에서 선택 시 호출)
     *
     * @param postWalkEmotion 선택된 산책 후 감정
     */
    fun updatePostWalkEmotion(postWalkEmotion: EmotionType) {
        viewModelScope.launch {
            try {
                val localId = _currentSessionLocalId.value
                    ?: throw IllegalStateException("저장된 세션이 없습니다")

                // DB만 업데이트 (Flow가 자동으로 UI 갱신)
                walkingSessionRepository.updatePostWalkEmotion(
                    localId = localId,
                    postWalkEmotion = postWalkEmotion.name
                )

                Timber.d("산책 후 감정 업데이트 완료: localId=$localId, emotion=$postWalkEmotion")
            } catch (t: Throwable) {
                Timber.e(t, "산책 후 감정 업데이트 실패")
                throw t
            }
        }
    }

    /**
     * 세션의 이미지와 노트 업데이트 (사진/텍스트 단계에서 호출)
     *
     * URI를 파일로 복사하고 경로를 저장합니다.
     *
     * stopWalking()에서 이미 세션 저장이 완료되었으므로 currentSessionLocalId는 항상 설정되어 있어야 합니다.
     *
     * @return 업데이트 성공 여부
     */
    fun updateSessionImageAndNote() {
        Timber.d("updateSessionImageAndNote 호출 - ViewModel hashCode: ${this.hashCode()}, currentSessionLocalId: ${_currentSessionLocalId.value}")
        viewModelScope.launch {
            val localId = _currentSessionLocalId.value
                ?: throw IllegalStateException("저장된 세션이 없습니다. 산책을 먼저 완료해주세요.")

            // ViewModel 공유가 제대로 된다면 여기에 도달하지 않아야 함
            Timber.d("✅ ViewModel 공유 성공 - localId: $localId")

            val imageUri = _emotionPhotoUri.value // URI 그대로 전달
            val note = _emotionText.value.ifEmpty { null }

            walkingSessionRepository.updateSessionImageAndNote(
                localId = localId,
                imageUri = imageUri, // URI를 전달하면 Repository에서 파일로 복사
                note = note
            )

            Timber.d("세션 이미지/노트 업데이트 완료: localId=$localId, imageUri=$imageUri, note=$note")
        }
    }

    /**
     * 세션의 노트 업데이트
     *
     * @param localId 업데이트할 세션의 로컬 ID
     * @param note 업데이트할 노트 텍스트
     */
    fun updateSessionNote(localId: String, note: String) {
        viewModelScope.launch {
            try {
                walkingSessionRepository.updateSessionImageAndNote(
                    localId = localId,
                    imageUri = null, // 이미지는 변경하지 않음
                    note = note
                )
                Timber.d("세션 노트 업데이트 완료: localId=$localId, note=$note")
            } catch (t: Throwable) {
                Timber.e(t, "세션 노트 업데이트 실패: localId=$localId")
            }
        }
    }

    /**
     * 세션의 노트 삭제 (null로 설정)
     *
     * @param localId 삭제할 세션의 로컬 ID
     */
    fun deleteSessionNote(localId: String) {
        viewModelScope.launch {
            try {
                walkingSessionRepository.updateSessionImageAndNote(
                    localId = localId,
                    imageUri = null, // 이미지는 변경하지 않음
                    note = null // 노트를 null로 설정하여 삭제
                )
                Timber.d("세션 노트 삭제 완료: localId=$localId")
                // TODO: 삭제 성공 시 관련 UI 상태 업데이트 (필요시 구현)
            } catch (t: Throwable) {
                Timber.e(t, "세션 노트 삭제 실패: localId=$localId")
                // Repository에서 이미 에러 처리를 하므로 여기서는 추가 처리 불필요
            }
        }
    }

    /**
     * 현재 진행 중인 세션 삭제 (임시 저장된 산책 기록 삭제)
     * PostWalkingEmotionSelectRoute에서 취소할 때 호출됨
     */
    fun deleteCurrentSession() {
        viewModelScope.launch {
            val localId = _currentSessionLocalId.value
            if (localId != null) {
                try {
                    walkingSessionRepository.deleteSession(localId)
                    _currentSessionLocalId.value = null
                    Timber.d("임시 산책 세션 삭제 완료: localId=$localId")
                } catch (t: Throwable) {
                    Timber.e(t, "임시 산책 세션 삭제 실패: localId=$localId")
                }
            } else {
                Timber.w("삭제할 임시 세션이 없습니다")
            }
        }
    }

    /**
     * 세션을 서버와 동기화 (WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 호출)
     *
     * 화면을 벗어나도 네트워크 요청이 계속 진행되도록 nonCancellable 컨텍스트 사용
     */
    fun syncSessionToServer() {
        viewModelScope.launch {
            try {
                _snapshotState.value = SnapshotState.Syncing

                val localId = _currentSessionLocalId.value
                    ?: throw IllegalStateException("저장된 세션이 없습니다")

                // nonCancellable 컨텍스트를 사용하여 화면을 벗어나도 네트워크 요청이 계속 진행되도록 함
                // 큰 이미지 파일(3MB+) 업로드 중에 화면을 벗어나도 취소되지 않음
                withContext(NonCancellable) {
                    walkingSessionRepository.syncSessionToServer(localId)
                }

                _snapshotState.value = SnapshotState.Complete
                Timber.d("서버 동기화 완료: localId=$localId")
            } catch (e: CancellationException) {
                // nonCancellable을 사용했으므로 이 경우는 발생하지 않아야 하지만, 안전을 위해 처리
                _snapshotState.value = SnapshotState.Error("서버 동기화 취소됨")
                Timber.w("서버 동기화 취소됨 (예상치 못한 취소): localId=${_currentSessionLocalId.value}")
            } catch (t: Throwable) {
                // 실제 서버 에러인 경우에만 로깅 및 사용자 알림
                _snapshotState.value = SnapshotState.Error(t.message ?: "서버 동기화 실패")
                Timber.e(t, "서버 동기화 실패: ${t.message}")
                // TODO: 에러 처리 (사용자에게 알림)
            }
        }
    }

    /**
     * 스냅샷 생성 및 저장 프로세스 시작
     *
     * @param captureSnapshot 스냅샷 생성 suspend 함수
     * @return 저장 성공 여부
     */
    suspend fun captureAndSaveSnapshot(captureSnapshot: suspend () -> String?): Boolean {
        return try {
            _snapshotState.value = SnapshotState.Capturing

            val imagePath = captureSnapshot()

            if (imagePath != null) {
                _snapshotState.value = SnapshotState.Saving
                saveSnapshotToSession(imagePath)
                _snapshotState.value = SnapshotState.Idle // 저장 완료 후 Idle로 변경
                Timber.d("스냅샷 생성 및 저장 완료: imagePath=$imagePath")
                true
            } else {
                Timber.w("스냅샷 생성 실패")
                _snapshotState.value = SnapshotState.Error("스냅샷 생성 실패")
                false
            }
        } catch (t: Throwable) {
            Timber.e(t, "스냅샷 생성 중 오류 발생")
            _snapshotState.value = SnapshotState.Error(t.message ?: "스냅샷 생성 실패")
            false
        }
    }

    /**
     * 현재 세션의 로컬 ID 노출 (WalkingResultScreen에서 사용)
     */
    val currentSessionLocalIdValue: String?
        get() = _currentSessionLocalId.value

    /**
     * 현재 사용자 ID 확인 (로그인 상태 체크용)
     */
    suspend fun getCurrentUserId(): Long {
        return walkingSessionRepository.getCurrentUserId()
    }

    /**
     * ID로 세션 조회 (WalkingResultScreen에서 사용)
     */
    suspend fun getSessionById(id: String): WalkingSession? {
        return walkingSessionRepository.getSessionById(id)
    }

    /**
     * 스냅샷 이미지를 세션에 저장 (WalkingResultScreen에서 "기록 완료" 버튼 클릭 시 호출)
     *
     * @param imagePath 스냅샷 파일 경로
     */
    suspend fun saveSnapshotToSession(imagePath: String?) {
        val localId = _currentSessionLocalId.value
            ?: throw IllegalStateException("저장된 세션이 없습니다")

        if (imagePath != null) {
            val imageUri = android.net.Uri.fromFile(java.io.File(imagePath))
            walkingSessionRepository.updateSessionImageAndNote(
                localId = localId,
                imageUri = imageUri,
                note = null
            )
            Timber.d("스냅샷 저장 완료: localId=$localId, imagePath=$imagePath")
        } else {
            Timber.w("스냅샷 이미지 경로가 null입니다 - 이미지 없이 저장됨")
        }
    }

    /**
     * 산책 시작 시간 가져오기 (외부에서 접근용)
     */
    fun getStartTimeMillis(): Long {
        return startTimeMillis
    }

    /**
     * 총 이동 거리 계산 (미터)
     * LocationPoint 리스트를 기반으로 GPS 거리를 계산합니다.
     */
    private fun calculateTotalDistance(locations: List<LocationPoint>): Float {
        if (locations.size < 2) {
            return 0f
        }

        var totalDistance = 0f
        val results = FloatArray(1)

        for (i in 0 until locations.size - 1) {
            val start = locations[i]
            val end = locations[i + 1]

            Location.distanceBetween(
                start.latitude,
                start.longitude,
                end.latitude,
                end.longitude,
                results
            )

            totalDistance += results[0]
        }

        return totalDistance
    }

    /**
     * UI 상태를 SessionSaved로 즉시 변경 (버튼 클릭 시 사용)
     */
    fun finishWalking() {
        _uiState.value = WalkingUiState.SessionSaved
    }

    /**
     * 커스텀 데이터: 더미 WalkingSession 추가 (개발용)
     * 오늘 날짜 이전으로만 추가 가능
     */
    fun addDummyWalkingSessionForDate(
        dateMillis: Long,
        stepCount: Int = 5000,
        durationMillis: Long = 1800000L, // 30분
        note: String = "더미 데이터"
    ) {
        viewModelScope.launch {
            try {
                val today = DateUtils.getStartOfDay(System.currentTimeMillis())
                val targetDate = DateUtils.getStartOfDay(dateMillis)

                if (targetDate >= today) {
                    Timber.w("🚫 더미 세션은 오늘 날짜 이전으로만 추가 가능합니다")
                    return@launch
                }
                val currentUserId = walkingSessionRepository.getCurrentUserId()

                val dummySession = WalkingSession(
                    id = "", // ID는 repository에서 자동 생성 (빈 문자열이면 UUID 생성)
                    userId = currentUserId,
                    startTime = targetDate + 9 * 60 * 60 * 1000L, // 오전 9시 시작
                    endTime = targetDate + 9 * 60 * 60 * 1000L + durationMillis, // 시작시간 + 산책 시간
                    stepCount = stepCount,
                    locations = emptyList(), // 더미 데이터이므로 GPS 좌표 없음
                    filteredLocations = null, // 필터링 데이터 없음
                    smoothedLocations = null, // 스무딩 데이터 없음
                    totalDistance = stepCount * 0.7f, // 걸음 수 기반 대략적 거리 계산 (70cm 보폭 가정)
                    preWalkEmotion = emotionTypeToString(EmotionType.HAPPY), // 산책 전 감정 (String)
                    postWalkEmotion = emotionTypeToString(EmotionType.CONTENT), // 산책 후 감정 (String)
                    note = note,
                    localImagePath = null, // 로컬 이미지 없음
                    serverImageUrl = null, // 서버 이미지 없음
                    createdDate = DateUtils.millisToIsoUtc(targetDate), // ISO 8601 형식의 날짜 (String)
                    targetStepCount = 0, // 목표 걸음 수 (더미에서는 0)
                    targetWalkCount = 0, // 목표 산책 횟수 (더미에서는 0)
                )

                walkingSessionRepository.saveSessionLocalOnly(dummySession)
                Timber.d("✅ 더미 WalkingSession 추가 완료: ${DateUtils.formatDate(targetDate)} - $stepCount 걸음")
            } catch (e: Exception) {
                Timber.e(e, "❌ 더미 WalkingSession 추가 실패")
            }
        }
    }

    /**
     * 커스텀 데이터: 여러 날짜에 걸쳐 더미 세션 일괄 추가 (개발용)
     */
    fun addDummyWalkingSessionsForWeek(
        startDateMillis: Long,
        daysCount: Int = 7,
        baseStepCount: Int = 5000
    ) {
        viewModelScope.launch {
            try {
                val today = DateUtils.getStartOfDay(System.currentTimeMillis())
                val startDate = DateUtils.getStartOfDay(startDateMillis)

                if (startDate >= today) {
                    Timber.w("🚫 더미 세션은 오늘 날짜 이전으로만 추가 가능합니다")
                    return@launch
                }

                repeat(daysCount) { dayIndex ->
                    val targetDate = startDate + (dayIndex * 24 * 60 * 60 * 1000L)
                    if (targetDate >= today) return@repeat

                    val stepCount = baseStepCount + (dayIndex * 500) // 날짜별로 걸음 수 증가
                    val durationMillis = 1500000L + (dayIndex * 300000L) // 날짜별로 시간 증가

                    addDummyWalkingSessionForDate(
                        dateMillis = targetDate,
                        stepCount = stepCount,
                        durationMillis = durationMillis,
                        note = "주간 더미 데이터 ${dayIndex + 1}일차"
                    )

                    delay(100) // 약간의 딜레이로 순차적 처리
                }

                Timber.d("✅ 주간 더미 WalkingSession 일괄 추가 완료: ${daysCount}일")
            } catch (e: Exception) {
                Timber.e(e, "❌ 주간 더미 WalkingSession 추가 실패")
            }
        }
    }
}

/**
 * 센서 상태 정보
 */
data class SensorStatus(
    val isStepCounterActive: Boolean = false,
    val isAccelerometerActive: Boolean = false,
    val isActivityRecognitionActive: Boolean = false,
    val isLocationTrackingActive: Boolean = false,
    val isStepCounterAvailable: Boolean = true,
    val isAccelerometerAvailable: Boolean = true,
    val isActivityRecognitionAvailable: Boolean = true,
)

/**
 * 스냅샷 생성 및 서버 동기화 상태
 */
sealed class SnapshotState {
    data object Idle : SnapshotState()
    data object Capturing : SnapshotState()
    data object Saving : SnapshotState()
    data object Syncing : SnapshotState()
    data object Complete : SnapshotState()
    data class Error(val message: String) : SnapshotState()
}

/**
 * Walking UI State
 */
/**
 * WalkingScreen 통합 상태 (UI 상태 + 캐릭터 정보)
 */
data class WalkingScreenState(
    val uiState: WalkingUiState,
    val character: Character?,
    val characterLottieJson: String? = null,
    val currentWeekGoalChallengeCount: Int = 0
)

sealed interface WalkingUiState {
    /**
     * 초기 로딩 상태 (DataStore 복원 중)
     */
    data object Loading : WalkingUiState

    /**
     * 산책 전 감정 선택 상태
     */
    data class PreWalkingEmotionSelection(
        val preWalkingEmotion: EmotionType? = null,
    ) : WalkingUiState

    /**
     * 산책 중
     */
    data class Walking(
        val stepCount: Int,
        val duration: Long,
        val isPaused: Boolean = false,
    ) : WalkingUiState

    /**
     * 세션 저장 완료 (CTA 버튼 표시)
     */
    data object SessionSaved : WalkingUiState

}