package team.swyp.sdu.ui.walking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.domain.contract.WalkingRawEvent
import team.swyp.sdu.domain.contract.WalkingTrackingContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.domain.service.MovementState
import android.location.Location
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val tracking: WalkingTrackingContract,
    private val walkingSessionRepository: WalkingSessionRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<WalkingUiState>(WalkingUiState.PreWalkingEmotionSelection())
    val uiState = _uiState.asStateFlow()

    private var durationJob: Job? = null

    private var startTimeMillis = 0L
    private var elapsedBeforePause = 0L
    private var lastStepCount = 0
    private var lastRawStepCount = 0
    
    // 산책 전 감정을 저장 (산책 종료 후 감정 선택 시 사용)
    private var savedPreWalkingEmotion: EmotionType? = null
    
    // 산책 후 감정을 저장 (별도 화면에서 사용)
    private val _postWalkingEmotion = MutableStateFlow<EmotionType?>(null)
    val postWalkingEmotion: StateFlow<EmotionType?> = _postWalkingEmotion.asStateFlow()

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
            _uiState.value = currentState.copy(preWalkingEmotion = emotionType)
        }
    }
    
    fun selectPostWalkingEmotion(emotionType: EmotionType) {
        _postWalkingEmotion.value = emotionType
        Timber.i("Post Emotion : $emotionType")
    }
    
    /**
     * 산책 후 감정 초기화 (새 산책 시작 시)
     */
    fun resetPostWalkingEmotion() {
        _postWalkingEmotion.value = null
    }

    init {
        observeRawEvents()
        observeTrackingStatus()
        updateSensorAvailability()
    }

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
                handleStepCountUpdate(event.rawStepCount)
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

    fun startWalking() {
        startTimeMillis = System.currentTimeMillis()
        elapsedBeforePause = 0L
        lastStepCount = 0
        lastRawStepCount = 0
        
        // 위치 리스트 초기화
        _locations.value = emptyList()
        
        // 현재 상태에서 산책 전 감정을 저장
        val currentState = _uiState.value
        if (currentState is WalkingUiState.PreWalkingEmotionSelection) {
            savedPreWalkingEmotion = currentState.preWalkingEmotion
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
        _currentLocation.value = null

        startDurationUpdates()
        updateSensorStatus()
    }

    fun pauseWalking() {
        viewModelScope.launch {
            tracking.pauseTracking()
        }
    }

    fun resumeWalking() {
        viewModelScope.launch {
            tracking.resumeTracking()
        }
    }

    fun stopWalking() {
        viewModelScope.launch {
            tracking.stopTracking()
        }
        durationJob?.cancel()
        
        // 센서 상태 업데이트
        updateSensorStatus()
        
        // 세션 완성 및 저장
        completeAndSaveSession()
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

    private fun handleStepCountUpdate(raw: Int) {
        lastRawStepCount = raw
        val state = _uiState.value
        if (state is WalkingUiState.Walking && !state.isPaused) {
            lastStepCount = raw
            _uiState.value = state.copy(stepCount = raw)
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
        Timber.d("가속도계 업데이트: ${movementState.name}, 가속도: ${acceleration}m/s²")
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
     * 세션 완성 및 저장
     */
    private fun completeAndSaveSession() {
        viewModelScope.launch {
            try {
                val endTime = System.currentTimeMillis()
                val collectedLocations = _locations.value
                val totalDistance = calculateTotalDistance(collectedLocations)
                
                // WalkingSession 완성
                val completedSession = WalkingSession(
                    startTime = startTimeMillis,
                    endTime = endTime,
                    stepCount = lastStepCount,
                    locations = collectedLocations,
                    totalDistance = totalDistance,
                    preWalkEmotion = savedPreWalkingEmotion,
                    postWalkEmotion = _postWalkingEmotion.value,
                    note = _emotionText.value.ifEmpty { null },
                    imageUrl = null, // 서버 업로드 후 업데이트됨
                    createdDate = null // 서버 응답에서 받아옴
                )
                
                // 세션 저장 (로컬 + 서버 동기화)
                walkingSessionRepository.saveSession(
                    session = completedSession,
                    imageUri = _emotionPhotoUri.value
                )
                
                // UI State를 Completed로 변경
                _uiState.value = WalkingUiState.Completed(completedSession)
                
                Timber.d("산책 세션 저장 완료: 걸음수=${lastStepCount}, 거리=${totalDistance}m, 위치포인트=${collectedLocations.size}개")
            } catch (e: Exception) {
                Timber.e(e, "산책 세션 저장 실패")
                _uiState.value = WalkingUiState.Error("세션 저장 중 오류가 발생했습니다: ${e.message}")
            }
        }
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
 * Walking UI State
 */
sealed interface WalkingUiState {
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
     * 산책 완료
     */
    data class Completed(
        val session: WalkingSession, // TODO: Add proper session type
    ) : WalkingUiState

    /**
     * 오류 상태
     */
    data class Error(
        val message: String,
    ) : WalkingUiState
}