산책 앱 어뷰징 방지 시스템 구현 요청 (수정본)
개요
WalkingViewModel에 규칙 기반 어뷰징 방지 시스템을 추가해주세요. 단일 지표가 아닌 복합 조건으로 판단하여 정상 사용자(느린 걷기, 어린이, 노인)를 오판하지 않도록 합니다. 서버 전송은 제외하고 클라이언트 측 검증만 구현합니다.

1. 새로운 데이터 클래스 추가
data/model/ValidationResult.kt 파일을 생성하고 다음 클래스들을 추가해주세요:
kotlinpackage swyp.team.walkit.data.model

/**
 * 세션 검증 결과
 */
data class ValidationResult(
    val isValid: Boolean,
    val flags: List<SuspicionFlag>,
    val action: ValidationAction,
    val message: String? = null
)

/**
 * 의심 플래그 (심각도별)
 * 
 * 원칙: 단일 지표로 판단하지 않고, 복합 조건으로 판단
 */
enum class SuspicionFlag(val severity: Severity, val description: String) {
    // Critical (즉시 거부) - 물리적으로 불가능하거나 명백한 어뷰징
    IMPOSSIBLE_STRIDE(
        Severity.CRITICAL, 
        "보폭이 물리적으로 불가능합니다 (20cm 미만 또는 2m 초과)"
    ),
    IMPOSSIBLE_SPEED(
        Severity.CRITICAL, 
        "이동 속도가 차량 수준입니다 (20km/h 초과)"
    ),
    VEHICLE_DETECTED(
        Severity.CRITICAL, 
        "차량 이동이 감지되었습니다"
    ),
    EXCESSIVE_STEPS(
        Severity.CRITICAL, 
        "비현실적으로 많은 걸음수입니다 (10만보 초과)"
    ),
    
    // Warning (저장하지만 플래그) - 의심스럽지만 확신할 수 없음
    STATIONARY_WALKING(
        Severity.WARNING, 
        "제자리 걸음이 의심됩니다 (작은 보폭 + GPS 이동 없음)"
    ),
    SHAKING_PATTERN(
        Severity.WARNING, 
        "흔들기 패턴이 의심됩니다 (작은 보폭 + 매우 느린 속도 + GPS 이동 없음)"
    ),
    HIGH_SPEED_RUNNING(
        Severity.WARNING, 
        "매우 빠른 속도입니다 (10~20km/h) - 조깅 또는 자전거 의심"
    ),
    SHORT_DURATION_HIGH_STEPS(
        Severity.WARNING, 
        "짧은 시간에 많은 걸음수입니다 (시간당 1.5만보 초과)"
    ),
    
    // Info (정보성) - 정상이지만 참고용
    INDOOR_SUSPECTED(
        Severity.INFO, 
        "실내 활동으로 추정됩니다 (GPS 정확도 낮음)"
    ),
    LONG_DURATION(
        Severity.INFO, 
        "장시간 활동입니다 (3시간 초과)"
    ),
    SLOW_WALKING(
        Severity.INFO, 
        "천천히 걷기로 감지되었습니다 (정상)"
    )
}

/**
 * 플래그 심각도
 */
enum class Severity {
    CRITICAL,  // 즉시 거부
    WARNING,   // 저장하지만 플래그
    INFO       // 정보성
}

/**
 * 검증 액션
 */
enum class ValidationAction {
    ACCEPT,          // 정상 저장
    ACCEPT_FLAGGED,  // 저장하지만 의심 플래그 표시
    REJECT           // 거부
}

2. WalkingSession 모델 수정
data/model/WalkingSession.kt에 검증 관련 필드를 추가해주세요:
kotlindata class WalkingSession(
    // ... 기존 필드들 ...
    
    // 새로 추가
    val suspicionFlags: List<String> = emptyList(),  // 의심 플래그 리스트
    val validationAction: String? = null,             // ACCEPT, ACCEPT_FLAGGED, REJECT
    val isValidated: Boolean = false                  // 검증 완료 여부
)

3. Validator 클래스 생성
domain/validator/WalkingSessionValidator.kt 파일을 생성해주세요:
kotlinpackage swyp.team.walkit.domain.validator

import swyp.team.walkit.data.model.*
import swyp.team.walkit.domain.service.ActivityType
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 산책 세션 검증 클래스
 * 
 * 핵심 원칙:
 * 1. 단일 지표로 판단하지 않음 (보폭만, 속도만 보지 않음)
 * 2. 복합 조건으로 판단 (보폭 + GPS 이동 + 속도 조합)
 * 3. 정상 사용자 오판 방지 (느린 걷기, 어린이, 노인 고려)
 */
class WalkingSessionValidator {
    
    companion object {
        // 보폭 임계값 (미터) - 물리적 가능성만 체크
        private const val MIN_POSSIBLE_STRIDE = 0.2f   // 20cm 미만은 불가능
        private const val MAX_POSSIBLE_STRIDE = 2.0f   // 2m 초과는 불가능
        
        // 제자리 걸음 감지용 임계값
        private const val STATIONARY_STRIDE_THRESHOLD = 0.3f   // 30cm 미만
        private const val STATIONARY_STEP_THRESHOLD = 500      // 500걸음 이상
        
        // 흔들기 패턴 감지용 임계값
        private const val SHAKING_STRIDE_THRESHOLD = 0.25f     // 25cm 미만
        private const val SHAKING_SPEED_THRESHOLD = 0.8f       // 0.8km/h 미만
        
        // 속도 임계값 (km/h)
        private const val MAX_POSSIBLE_SPEED = 20f     // 20km/h 초과는 차량
        private const val HIGH_SPEED_THRESHOLD = 10f   // 10km/h 초과는 빠른 조깅
        private const val MIN_WALKING_SPEED = 1f       // 1km/h 미만은 매우 느림
        
        // 걸음수 임계값
        private const val MAX_POSSIBLE_STEPS = 100000  // 하루 10만보는 불가능
        private const val MAX_STEPS_PER_HOUR = 15000   // 시간당 1.5만보 초과는 의심
        
        // 위치 변화 임계값
        private const val MIN_LOCATION_VARIANCE = 0.0001f  // 이것보다 작으면 제자리
        
        // GPS 정확도 임계값
        private const val GPS_ACCURACY_THRESHOLD = 50f  // 50m 초과는 부정확
        
        // 장시간 활동 임계값
        private const val LONG_DURATION_HOURS = 3f  // 3시간 초과
    }
    
    /**
     * 세션 검증
     */
    fun validate(session: WalkingSession): ValidationResult {
        val flags = mutableListOf<SuspicionFlag>()
        
        // 1. 물리적 불가능성 검증 (Critical)
        validatePhysicalImpossibility(session, flags)
        
        // 2. 복합 패턴 검증 (Warning) - 핵심!
        validateComplexPatterns(session, flags)
        
        // 3. 정보성 플래그 (Info)
        validateInformationalFlags(session, flags)
        
        // 액션 결정
        val action = determineAction(flags)
        
        // 메시지 생성
        val message = generateMessage(flags, action)
        
        Timber.d("세션 검증 완료: action=$action, flags=${flags.map { it.name }}")
        
        return ValidationResult(
            isValid = action != ValidationAction.REJECT,
            flags = flags,
            action = action,
            message = message
        )
    }
    
    /**
     * 1. 물리적 불가능성 검증 (Critical)
     * 
     * 이것만으로도 명백히 어뷰징인 경우
     */
    private fun validatePhysicalImpossibility(
        session: WalkingSession, 
        flags: MutableList<SuspicionFlag>
    ) {
        // 1-1. 불가능한 보폭
        if (session.stepCount > 0 && session.totalDistance > 0f) {
            val stride = session.totalDistance / session.stepCount
            
            if (stride < MIN_POSSIBLE_STRIDE) {
                flags.add(SuspicionFlag.IMPOSSIBLE_STRIDE)
                Timber.w("불가능한 보폭 (너무 짧음): ${stride}m")
            } else if (stride > MAX_POSSIBLE_STRIDE) {
                flags.add(SuspicionFlag.IMPOSSIBLE_STRIDE)
                Timber.w("불가능한 보폭 (너무 김): ${stride}m")
            }
        }
        
        // 1-2. 불가능한 속도
        val durationHours = session.getDurationHours()
        if (durationHours > 0f && session.totalDistance > 0f) {
            val speedKmh = session.totalDistance / 1000f / durationHours
            
            if (speedKmh > MAX_POSSIBLE_SPEED) {
                flags.add(SuspicionFlag.IMPOSSIBLE_SPEED)
                Timber.w("불가능한 속도: ${speedKmh}km/h")
            }
        }
        
        // 1-3. 과도한 걸음수
        if (session.stepCount > MAX_POSSIBLE_STEPS) {
            flags.add(SuspicionFlag.EXCESSIVE_STEPS)
            Timber.w("과도한 걸음수: ${session.stepCount}걸음")
        }
        
        // 1-4. 차량 이동 감지
        if (session.primaryActivity == ActivityType.IN_VEHICLE) {
            flags.add(SuspicionFlag.VEHICLE_DETECTED)
            Timber.w("차량 이동 감지")
        }
    }
    
    /**
     * 2. 복합 패턴 검증 (Warning)
     * 
     * 핵심: 여러 지표를 조합하여 판단
     * - 느린 걷기 (정상): 보폭 작음 + GPS 이동 있음
     * - 흔들기 (의심): 보폭 작음 + GPS 이동 없음
     */
    private fun validateComplexPatterns(
        session: WalkingSession, 
        flags: MutableList<SuspicionFlag>
    ) {
        if (session.locations.size < 5) return
        if (session.stepCount < STATIONARY_STEP_THRESHOLD) return
        
        val stride = if (session.stepCount > 0) {
            session.totalDistance / session.stepCount
        } else {
            0f
        }
        val locationVariance = calculateLocationVariance(session.locations)
        val speedKmh = if (session.getDurationHours() > 0f) {
            session.totalDistance / 1000f / session.getDurationHours()
        } else {
            0f
        }
        
        // 패턴 1: 제자리 걸음 감지
        // 조건: 보폭 작음 + GPS 이동 거의 없음 + 걸음수 많음
        if (stride > 0f && stride < STATIONARY_STRIDE_THRESHOLD && 
            locationVariance < MIN_LOCATION_VARIANCE &&
            session.stepCount >= STATIONARY_STEP_THRESHOLD) {
            
            flags.add(SuspicionFlag.STATIONARY_WALKING)
            Timber.w("제자리 걸음 의심: 보폭=${stride}m, GPS변화=${locationVariance}, 걸음=${session.stepCount}")
        }
        
        // 패턴 2: 흔들기 패턴 감지
        // 조건: 매우 작은 보폭 + GPS 이동 없음 + 매우 느린 속도
        if (stride > 0f && stride < SHAKING_STRIDE_THRESHOLD && 
            locationVariance < MIN_LOCATION_VARIANCE &&
            speedKmh < SHAKING_SPEED_THRESHOLD &&
            session.stepCount >= STATIONARY_STEP_THRESHOLD) {
            
            flags.add(SuspicionFlag.SHAKING_PATTERN)
            Timber.w("흔들기 패턴 의심: 보폭=${stride}m, 속도=${speedKmh}km/h, GPS변화=${locationVariance}")
        }
        
        // 패턴 3: 높은 속도 (차량이 아니지만 빠름)
        // 조건: 속도 빠름 (10~20km/h)
        // 참고: 빠른 조깅(8~12km/h), 자전거(15~20km/h)는 정상일 수 있음
        if (speedKmh in HIGH_SPEED_THRESHOLD..MAX_POSSIBLE_SPEED) {
            flags.add(SuspicionFlag.HIGH_SPEED_RUNNING)
            Timber.w("높은 속도: ${speedKmh}km/h (조깅 또는 자전거 의심)")
        }
        
        // 패턴 4: 짧은 시간에 많은 걸음수
        // 조건: 시간당 걸음수가 너무 많음
        val durationHours = session.getDurationHours()
        if (durationHours > 0f) {
            val stepsPerHour = session.stepCount / durationHours
            
            if (stepsPerHour > MAX_STEPS_PER_HOUR) {
                flags.add(SuspicionFlag.SHORT_DURATION_HIGH_STEPS)
                Timber.w("짧은 시간 과도한 걸음수: ${stepsPerHour}걸음/시간")
            }
        }
    }
    
    /**
     * 3. 정보성 플래그 (Info)
     * 
     * 정상이지만 참고용으로 표시
     */
    private fun validateInformationalFlags(
        session: WalkingSession, 
        flags: MutableList<SuspicionFlag>
    ) {
        // 3-1. 실내 활동 추정
        if (session.locations.isNotEmpty()) {
            val inaccurateCount = session.locations.count { 
                it.accuracy?.let { acc -> acc > GPS_ACCURACY_THRESHOLD } ?: false
            }
            val inaccurateRatio = inaccurateCount.toFloat() / session.locations.size
            
            // 70% 이상 부정확하면 실내 의심
            if (inaccurateRatio > 0.7f) {
                flags.add(SuspicionFlag.INDOOR_SUSPECTED)
                Timber.d("실내 활동 추정: GPS 부정확 비율=${inaccurateRatio * 100}%")
            }
        }
        
        // 3-2. 장시간 활동
        val durationHours = session.getDurationHours()
        if (durationHours > LONG_DURATION_HOURS) {
            flags.add(SuspicionFlag.LONG_DURATION)
            Timber.d("장시간 활동: ${durationHours}시간")
        }
        
        // 3-3. 천천히 걷기 (정상)
        // 조건: 느린 속도 + GPS 이동 있음 (정상적인 느린 걷기)
        if (session.locations.size >= 5 && session.stepCount > 0) {
            val speedKmh = if (durationHours > 0f) {
                session.totalDistance / 1000f / durationHours
            } else {
                0f
            }
            val locationVariance = calculateLocationVariance(session.locations)
            
            if (speedKmh in 0.5f..2f && locationVariance > MIN_LOCATION_VARIANCE) {
                flags.add(SuspicionFlag.SLOW_WALKING)
                Timber.d("천천히 걷기 (정상): 속도=${speedKmh}km/h")
            }
        }
    }
    
    /**
     * 위치 변화량(분산) 계산
     */
    private fun calculateLocationVariance(locations: List<LocationPoint>): Float {
        if (locations.size < 2) return 0f
        
        val latitudes = locations.map { it.latitude }
        val longitudes = locations.map { it.longitude }
        
        val latMean = latitudes.average()
        val lonMean = longitudes.average()
        
        val latVariance = latitudes.map { (it - latMean).pow(2) }.average()
        val lonVariance = longitudes.map { (it - lonMean).pow(2) }.average()
        
        return (latVariance + lonVariance).toFloat()
    }
    
    /**
     * 액션 결정
     */
    private fun determineAction(flags: List<SuspicionFlag>): ValidationAction {
        return when {
            flags.any { it.severity == Severity.CRITICAL } -> {
                ValidationAction.REJECT
            }
            flags.any { it.severity == Severity.WARNING } -> {
                ValidationAction.ACCEPT_FLAGGED
            }
            else -> {
                ValidationAction.ACCEPT
            }
        }
    }
    
    /**
     * 메시지 생성
     */
    private fun generateMessage(flags: List<SuspicionFlag>, action: ValidationAction): String? {
        if (flags.isEmpty()) return null
        
        return when (action) {
            ValidationAction.REJECT -> {
                val criticalFlag = flags.first { it.severity == Severity.CRITICAL }
                "기록을 저장할 수 없습니다: ${criticalFlag.description}"
            }
            ValidationAction.ACCEPT_FLAGGED -> {
                "기록이 저장되었지만 일부 의심스러운 활동이 감지되었습니다"
            }
            else -> null
        }
    }
}

/**
 * WalkingSession 확장 함수
 */
private fun WalkingSession.getDurationHours(): Float {
    val endTime = this.endTime ?: System.currentTimeMillis()
    val durationMs = endTime - startTime
    return durationMs / 1000f / 3600f
}

4. WalkingViewModel 수정
presentation/viewmodel/WalkingViewModel.kt의 stopWalking() 메서드를 다음과 같이 수정해주세요:
4-1. Validator 인스턴스 추가
kotlin@HiltViewModel
class WalkingViewModel @Inject constructor(
    application: Application,
    private val stepCounterManager: StepCounterManager,
    private val activityRecognitionManager: ActivityRecognitionManager,
    private val accelerometerManager: AccelerometerManager,
    private val walkingSessionRepository: WalkingSessionRepository,
) : AndroidViewModel(application) {
    
    // ... 기존 필드들 ...
    
    // 새로 추가
    private val sessionValidator = WalkingSessionValidator()
    
    // ... 나머지 코드 ...
}
4-2. stopWalking() 메서드 수정
기존 stopWalking() 메서드의 세션 저장 부분을 다음과 같이 수정해주세요:
kotlinfun stopWalking() {
    val session = currentSession ?: return

    stepCounterManager.stopTracking()
    stopLocationTracking()
    accelerometerManager.stopTracking()

    stepCountJob?.cancel()
    locationJob?.cancel()
    durationUpdateJob?.cancel()
    activityJob?.cancel()
    accelerometerJob?.cancel()

    val endTime = System.currentTimeMillis()
    updateActivityStatsForCurrentState(endTime)

    val locationPointsFromService = locationPoints.toList()
    val finalActivityStats = calculateFinalActivityStats(locationPointsFromService)
    val primaryActivity = findPrimaryActivity(finalActivityStats)

    val completedSession = session.copy(
        endTime = endTime,
        locations = locationPointsFromService,
        totalDistance = calculateHybridDistance(locationPointsFromService, session.stepCount),
        activityStats = finalActivityStats,
        primaryActivity = primaryActivity,
    )

    // ========== 검증 로직 시작 ==========
    
    val validation = sessionValidator.validate(completedSession)
    
    when (validation.action) {
        ValidationAction.ACCEPT -> {
            // 정상 저장
            val validatedSession = completedSession.copy(
                isValidated = true,
                validationAction = "ACCEPT"
            )
            
            _locations.value = locationPointsFromService
            activityRecognitionManager.stopTracking()
            
            viewModelScope.launch {
                try {
                    walkingSessionRepository.saveSession(validatedSession)
                    Timber.d("정상 세션 저장: ${validatedSession.stepCount}걸음, ${validatedSession.getFormattedDistance()}")
                } catch (e: Exception) {
                    Timber.e(e, "세션 저장 실패")
                }
            }
            
            currentSession = null
            locationPoints.clear()
            _uiState.value = WalkingUiState.Completed(validatedSession)
        }
        
        ValidationAction.ACCEPT_FLAGGED -> {
            // 의심 플래그와 함께 저장
            val flaggedSession = completedSession.copy(
                suspicionFlags = validation.flags.map { it.name },
                validationAction = "ACCEPT_FLAGGED",
                isValidated = true
            )
            
            _locations.value = locationPointsFromService
            activityRecognitionManager.stopTracking()
            
            viewModelScope.launch {
                try {
                    walkingSessionRepository.saveSession(flaggedSession)
                    Timber.w("의심 플래그와 함께 저장: ${validation.flags.map { it.description }}")
                } catch (e: Exception) {
                    Timber.e(e, "세션 저장 실패")
                }
            }
            
            currentSession = null
            locationPoints.clear()
            _uiState.value = WalkingUiState.Completed(flaggedSession)
        }
        
        ValidationAction.REJECT -> {
            // 거부 - 저장하지 않음
            activityRecognitionManager.stopTracking()
            currentSession = null
            locationPoints.clear()
            
            val criticalFlags = validation.flags.filter { it.severity == Severity.CRITICAL }
            Timber.e("세션 거부: ${criticalFlags.map { it.description }}")
            
            _uiState.value = WalkingUiState.Error(
                validation.message ?: "비정상적인 활동이 감지되었습니다"
            )
        }
    }
}

5. 실시간 필터링 추가 (선택 사항)
5-1. startWalking()의 걸음수 업데이트에 필터링 추가
stepCounterManager.getStepCountUpdates() Flow에서 차량 이동 시 걸음을 무시하도록 수정해주세요:
kotlinstepCountJob = stepCounterManager.getStepCountUpdates()
    .onEach { realStepCount ->
        lastRawStepCount = realStepCount
        val state = _uiState.value
        if (state is WalkingUiState.Walking) {
            if (state.isPaused) return@onEach
            
            // ========== 실시간 필터링 ==========
            // 차량 이동 중이면 걸음 무시
            if (state.currentActivity == ActivityType.IN_VEHICLE) {
                Timber.w("차량 이동 중 - 걸음 무시")
                return@onEach
            }
            
            // GPS 속도가 차량 수준이면 무시 (20 km/h = 5.56 m/s)
            if (state.currentSpeed > 5.6f) {
                Timber.w("차량 속도 감지 (${state.currentSpeed * 3.6f}km/h) - 걸음 무시")
                return@onEach
            }
            // ===================================
            
            val effectiveStepCount = (realStepCount - stepOffset).coerceAtLeast(0)
            
            // ... 나머지 기존 코드 ...
        }
    }
    .catch { e -> Timber.e(e, "걸음 수 업데이트 오류") }
    .launchIn(viewModelScope)

6. UI에 검증 결과 표시 (선택 사항)
6-1. WalkingResultScreen에 의심 플래그 표시
WalkingResultScreen.kt에 다음과 같은 UI 추가를 고려해주세요:
kotlin// 의심 플래그가 있으면 경고 배너 표시
if (session.suspicionFlags.isNotEmpty()) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "일부 의심스러운 활동이 감지되었습니다",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "기록은 저장되었지만 검토가 필요할 수 있습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

7. 테스트 시나리오
구현 후 다음 시나리오로 테스트해주세요:
7-1. 정상 케이스 - 일반 걷기
조건:
- 걸음수: 1000걸음
- 거리: 700m
- 보폭: 0.7m
- 시간: 15분
- 속도: 2.8 km/h
- GPS 변화: 있음

예상 결과: ACCEPT (플래그 없음)
7-2. 정상 케이스 - 느린 걷기 (노인/어린이)
조건:
- 걸음수: 500걸음
- 거리: 180m
- 보폭: 0.36m ← 작지만 정상!
- 시간: 20분
- 속도: 0.54 km/h
- GPS 변화: 있음

예상 결과: ACCEPT (SLOW_WALKING Info 플래그만)
7-3. 정상 케이스 - 산책 (느긋하게)
조건:
- 걸음수: 2000걸음
- 거리: 800m
- 보폭: 0.4m
- 시간: 40분
- 속도: 1.2 km/h
- GPS 변화: 있음

예상 결과: ACCEPT (SLOW_WALKING Info 플래그만)
7-4. 의심 케이스 - 제자리 걸음
조건:
- 걸음수: 1000걸음
- 거리: 250m
- 보폭: 0.25m ← 작음
- 시간: 10분
- 속도: 1.5 km/h (계산상)
- GPS 변화: 거의 없음 ← 의심!

예상 결과: ACCEPT_FLAGGED (STATIONARY_WALKING 플래그)