package team.swyp.sdu.ui.walking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.domain.contract.WalkingRawEvent
import team.swyp.sdu.domain.contract.WalkingTrackingContract
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.data.repository.WalkingSessionRepository
import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.domain.service.LocationManager
import team.swyp.sdu.domain.service.MovementState
import team.swyp.sdu.utils.DateUtils
import android.location.Location
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WalkingViewModel @Inject constructor(
    private val tracking: WalkingTrackingContract,
    private val walkingSessionRepository: WalkingSessionRepository,
    private val locationManager: LocationManager,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<WalkingUiState>(WalkingUiState.PreWalkingEmotionSelection())
    val uiState = _uiState.asStateFlow()

    private var durationJob: Job? = null

    private var startTimeMillis = 0L
    private var elapsedBeforePause = 0L
    private var lastStepCount = 0
    private var lastRawStepCount = 0
    
    // 현재 세션의 로컬 ID 저장
    private val _currentSessionLocalId = MutableStateFlow<String?>(null)

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
    private val _preWalkingEmotion = MutableStateFlow<EmotionType?>(null)
    val preWalkingEmotion: StateFlow<EmotionType?> = _preWalkingEmotion.asStateFlow()
    
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

    // 스냅샷 생성 및 서버 동기화 상태
    private val _snapshotState = MutableStateFlow<SnapshotState>(SnapshotState.Idle)
    val snapshotState: StateFlow<SnapshotState> = _snapshotState.asStateFlow()

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
            _preWalkingEmotion.value = emotionType
            _uiState.value = currentState.copy(preWalkingEmotion = emotionType)
        }
    }
    
    fun selectPostWalkingEmotion(emotionType: EmotionType) {
        _postWalkingEmotion.value = emotionType
        Timber.i("Post Emotion : $emotionType")
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
        // 산책 전 감정이 선택되었는지 확인 (UI에서 이미 체크하지만, 안전장치)
        val preEmotion = _preWalkingEmotion.value
        require(preEmotion != null) { "산책 전 감정을 선택해야 합니다" }
        
        startTimeMillis = System.currentTimeMillis()
        elapsedBeforePause = 0L
        lastStepCount = 0
        lastRawStepCount = 0
        
        // 위치 리스트 초기화 및 현재 위치를 첫 번째에 추가
        viewModelScope.launch {
            val initialLocations = mutableListOf<LocationPoint>()
            
            // 현재 위치 가져오기 (권한 체크 포함)
            // 실패하면 빈 배열로 시작하고, LocationTrackingService가 위치를 추적하여 추가함
            val currentLocation = locationManager.getCurrentLocation()
            if (currentLocation != null) {
                // 현재 위치를 첫 번째에 추가
                initialLocations.add(currentLocation)
                _currentLocation.value = currentLocation
                Timber.d("산책 시작: 현재 위치를 locations 배열 첫 번째에 추가 - ${currentLocation.latitude}, ${currentLocation.longitude}")
            } else {
                Timber.d("산책 시작: 현재 위치를 가져올 수 없음. LocationTrackingService가 위치를 추적하여 추가할 예정")
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

    /**
     * 산책 종료 및 세션 저장
     * 
     * 세션 저장이 완료될 때까지 기다린 후 Completed 상태로 변경합니다.
     */
    suspend fun stopWalking() {
        tracking.stopTracking()
        durationJob?.cancel()
        
        // 센서 상태 업데이트
        updateSensorStatus()
        
        // 완료된 세션 생성 (현재 메모리 데이터로 즉시 생성)
        val completedSession = createCompletedSession()
        
        // SavingSession 상태로 변경 (로딩 화면 표시)
        _uiState.value = WalkingUiState.SavingSession(completedSession)
        
        // DB에 저장하고 localId를 받아옴 (완료될 때까지 동기적으로 대기)
        try {
            Timber.d("🚶 WalkingViewModel.stopWalking - 저장 전: viewModel.hashCode=${this.hashCode()}, currentSessionLocalId=${_currentSessionLocalId.value}")
            val sessionId = walkingSessionRepository.createSessionPartial(completedSession)
            Timber.d("🚶 WalkingViewModel.stopWalking - 저장 후: viewModel.hashCode=${this.hashCode()}, currentSessionLocalId=$sessionId, postEmotion=${completedSession.postWalkEmotion}")
            Timber.d("부분 세션 저장 완료: localId=$sessionId, postEmotion=${completedSession.postWalkEmotion}")

            // 세션 저장 완료 후 SessionSaved 상태로 변경 (세션 데이터는 Flow로 관찰)
            _currentSessionLocalId.value = sessionId
            _uiState.value = WalkingUiState.SessionSaved
        } catch (e: Exception) {
            Timber.e(e, "부분 세션 저장 실패")
            // 에러 발생 시 Error 상태로 변경 (사용자에게 에러 표시)
            _uiState.value = WalkingUiState.Error(
                message = e.message ?: "세션 저장에 실패했습니다. 다시 시도해주세요."
            )
            throw e // 에러를 다시 던져서 호출자가 처리할 수 있도록 함
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
     * 완료된 세션 생성 (현재 메모리 데이터로 즉시 생성)
     * 
     * 하이브리드 접근: 메모리에서 즉시 세션 객체를 생성하여 Completed 상태로 사용
     */
    private fun createCompletedSession(): WalkingSession {
        val preEmotion = _preWalkingEmotion.value
            ?: throw IllegalStateException("산책 전 감정이 선택되지 않았습니다")
        
        // postWalkEmotion이 선택되지 않았으면 preWalkEmotion과 동일하게 설정
        val postEmotion = _postWalkingEmotion.value ?: preEmotion
        
        val endTime = System.currentTimeMillis()
        val collectedLocations = _locations.value
        val totalDistance = calculateTotalDistance(collectedLocations)
        
        // 완료된 세션 생성 (note, localImagePath, serverImageUrl은 null, 나중에 업데이트됨)
        return WalkingSession(
            startTime = startTimeMillis,
            endTime = endTime,
            stepCount = lastStepCount,
            locations = collectedLocations,
            totalDistance = totalDistance,
            preWalkEmotion = preEmotion,
            postWalkEmotion = postEmotion, // 기본값은 preWalkEmotion과 동일
            note = null, // 나중에 업데이트
            localImagePath = null, // 나중에 업데이트
            serverImageUrl = null, // 서버 동기화 후 업데이트
            createdDate = DateUtils.formatToIsoDateTime(startTimeMillis)
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
                    postWalkEmotion = postWalkEmotion
                )

                Timber.d("산책 후 감정 업데이트 완료: localId=$localId, emotion=$postWalkEmotion")
            } catch (e: Exception) {
                Timber.e(e, "산책 후 감정 업데이트 실패")
                throw e
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
         viewModelScope.launch {
             val localId = _currentSessionLocalId.value
                 ?: throw IllegalStateException("저장된 세션이 없습니다. 산책을 먼저 완료해주세요.")

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
            } catch (e: Exception) {
                Timber.e(e, "세션 노트 업데이트 실패: localId=$localId")
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
            } catch (e: Exception) {
                Timber.e(e, "세션 노트 삭제 실패: localId=$localId")
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
            } catch (e: Exception) {
                // 실제 서버 에러인 경우에만 로깅 및 사용자 알림
                _snapshotState.value = SnapshotState.Error(e.message ?: "서버 동기화 실패")
                Timber.e(e, "서버 동기화 실패: ${e.message}")
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
        } catch (e: Exception) {
            Timber.e(e, "스냅샷 생성 중 오류 발생")
            _snapshotState.value = SnapshotState.Error(e.message ?: "스냅샷 생성 실패")
            false
        }
    }
    
    /**
     * 현재 세션의 로컬 ID 노출 (WalkingResultScreen에서 사용)
     */
    val currentSessionLocalIdValue: String?
        get() = _currentSessionLocalId.value
    
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
     * 세션 저장 중 (로딩 화면 표시)
     */
    data object SavingSession : WalkingUiState

    /**
     * 세션 저장 완료 (DB에 저장됨, Flow로 데이터 관찰)
     */
    data object SessionSaved : WalkingUiState

    /**
     * 오류 상태
     */
    data class Error(
        val message: String,
    ) : WalkingUiState
}