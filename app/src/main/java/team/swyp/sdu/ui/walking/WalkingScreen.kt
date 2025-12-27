package team.swyp.sdu.ui.walking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.domain.service.MovementState
import team.swyp.sdu.ui.walking.viewmodel.SensorStatus
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.LottieAnimationView
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 산책 화면
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkingScreen(
    modifier: Modifier = Modifier,
    viewModel: WalkingViewModel = hiltViewModel(),
    onNavigateBack : () -> Unit = {},
    onNavigateToFinish: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val permissionsState =
        rememberMultiplePermissionsState(
            permissions =
                listOf(
                    android.Manifest.permission.ACTIVITY_RECOGNITION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ),
        )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        val currentState = viewModel.uiState.value
        if (currentState is WalkingUiState.SessionSaved) {
//            viewModel.reset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentState = viewModel.uiState.value
            if (currentState !is WalkingUiState.SessionSaved &&
                currentState !is WalkingUiState.Walking
            ) {
//                viewModel.reset()
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
    ) {
        when (val state = uiState) {
            is WalkingUiState.PreWalkingEmotionSelection -> {
                // 산책 전 감정 선택
                PreWalkingEmotionSelectRoute(
                    viewModel = viewModel,
                    onNext = {
                        if (permissionsState.allPermissionsGranted) {
                            viewModel.startWalking()
                        }
                    },
                    onPrev = {
                        onNavigateBack()
                    },
                    permissionsGranted = permissionsState.allPermissionsGranted,
                )
            }

            is WalkingUiState.Walking -> {
                val sensorStatus by viewModel.sensorStatus.collectAsStateWithLifecycle()
                val currentActivityType by viewModel.currentActivityType.collectAsStateWithLifecycle()
                val activityConfidence by viewModel.activityConfidence.collectAsStateWithLifecycle()
                val currentAcceleration by viewModel.currentAcceleration.collectAsStateWithLifecycle()
                val currentMovementState by viewModel.currentMovementState.collectAsStateWithLifecycle()
                val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
                WalkingView(
                    stepCount = state.stepCount,
                    duration = state.duration,
                    isPaused = state.isPaused,
                    sensorStatus = sensorStatus,
                    currentActivityType = currentActivityType,
                    activityConfidence = activityConfidence,
                    currentAcceleration = currentAcceleration,
                    currentMovementState = currentMovementState,
                    currentLocation = currentLocation,
                    onPauseToggle = {
                        if (state.isPaused) {
                            viewModel.resumeWalking()
                        } else {
                            viewModel.pauseWalking()
                        }
                    },
                    onStopClick = {
                        // 세션 저장이 완료될 때까지 기다림
                        coroutineScope.launch {
                            Timber.d("🚶 WalkingScreen - stopWalking 호출 전: viewModel.hashCode=${viewModel.hashCode()}, currentSessionLocalId=${viewModel.currentSessionLocalIdValue}")
                            viewModel.stopWalking()
                            Timber.d("🚶 WalkingScreen - stopWalking 호출 후: viewModel.hashCode=${viewModel.hashCode()}, currentSessionLocalId=${viewModel.currentSessionLocalIdValue}")
                        }
                        // 화면 전환은 LaunchedEffect에서 처리 (세션 저장 완료 후)
                    },
                )
            }

            is WalkingUiState.SavingSession -> {
                // 세션 저장 중 로딩 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                        Text(
                            text = "산책 기록을 저장하는 중...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            is WalkingUiState.SessionSaved -> {
                // 세션 저장 완료 후 자동으로 다음 화면으로 이동
                LaunchedEffect(Unit) {
                    onNavigateToFinish()
                }
            }

            is WalkingUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetryClick = {

                    },
                )
            }
        }
    }
}

@Composable
private fun EmotionGrid(
    selectedEmotions: Set<EmotionType>,
    onEmotionToggle: (EmotionType) -> Unit,
) {
    val positiveEmotions = listOf(
        EmotionType.HAPPY to "기쁨",
        EmotionType.JOYFUL to "즐거움",
        EmotionType.CONTENT to "행복함",
    )

    val negativeEmotions = listOf(
        EmotionType.DEPRESSED to "우울함",
        EmotionType.TIRED to "지침",
        EmotionType.ANXIOUS to "짜증남",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EmotionRow(
            emotions = positiveEmotions,
            selectedEmotions = selectedEmotions,
            onEmotionToggle = onEmotionToggle,
        )
        EmotionRow(
            emotions = negativeEmotions,
            selectedEmotions = selectedEmotions,
            onEmotionToggle = onEmotionToggle,
        )
    }
}

@Composable
private fun EmotionRow(
    emotions: List<Pair<EmotionType, String>>,
    selectedEmotions: Set<EmotionType>,
    onEmotionToggle: (EmotionType) -> Unit,
) {
    val rows = emotions.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { rowEmotions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowEmotions.forEach { (emotionType, label) ->
                    EmotionButton(
                        emotionType = emotionType,
                        label = label,
                        isSelected = selectedEmotions.contains(emotionType),
                        onClick = { onEmotionToggle(emotionType) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowEmotions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmotionButton(
    emotionType: EmotionType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        when (emotionType) {
            EmotionType.HAPPY,
            EmotionType.JOYFUL,
            EmotionType.CONTENT,
                -> MaterialTheme.colorScheme.surfaceVariant

            else -> Color(0xFFB3E5FC)
        }
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun WalkingView(
    stepCount: Int,
    duration: Long,
    isPaused: Boolean,
    sensorStatus: SensorStatus,
    currentActivityType: ActivityType?,
    activityConfidence: Int,
    currentAcceleration: Float,
    currentMovementState: MovementState?,
    currentLocation: LocationPoint?,
    onPauseToggle: () -> Unit,
    onStopClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        LottieAnimationView(isPlaying = !isPaused)

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "걸음 수",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$stepCount 걸음",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatDuration(duration),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "시간",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatDistance(0f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "거리",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        val pauseLabel = if (isPaused) "재개" else "일시정지"
                        val pauseIcon =
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause
                        Button(
                            onClick = onPauseToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) {
                            Icon(imageVector = pauseIcon, contentDescription = pauseLabel)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pauseLabel)
                        }

                        Button(onClick = onStopClick) {
                            Text("측정 종료")
                        }
                    }
                }
            }
        }

        // 활동 인식 상태 표시 카드
        currentActivityType?.let { activity ->
            ActivityStatusCard(
                activity = activity,
                confidence = activityConfidence,
            )
        }

        // 센서 상태 표시 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "센서 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                SensorStatusRow(
                    icon = Icons.Default.DirectionsWalk,
                    label = "걸음 수 센서",
                    isActive = sensorStatus.isStepCounterActive,
                    isAvailable = sensorStatus.isStepCounterAvailable,
                )

                SensorStatusRow(
                    icon = Icons.Default.Speed,
                    label = "가속도계",
                    isActive = sensorStatus.isAccelerometerActive,
                    isAvailable = sensorStatus.isAccelerometerAvailable,
                )

                SensorStatusRow(
                    icon = Icons.Default.Sensors,
                    label = "활동 인식",
                    isActive = sensorStatus.isActivityRecognitionActive,
                    isAvailable = sensorStatus.isActivityRecognitionAvailable,
                )

                SensorStatusRow(
                    icon = Icons.Default.LocationOn,
                    label = "위치 추적",
                    isActive = sensorStatus.isLocationTrackingActive,
                    isAvailable = true,
                )
            }
        }

        // 센서 수치 표시 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "센서 수치",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                HorizontalDivider()

                // 걸음 수 센서
                SensorValueRow(
                    label = "걸음 수",
                    value = "$stepCount 걸음",
                )

                // 가속도계
                SensorValueRow(
                    label = "가속도",
                    value = String.format("%.2f m/s²", currentAcceleration),
                    subValue = currentMovementState?.let { getMovementStateName(it) },
                )

                // 활동 인식
                if (currentActivityType != null) {
                    SensorValueRow(
                        label = "활동 인식",
                        value = getActivityName(currentActivityType!!),
                        subValue = "신뢰도: ${activityConfidence}%",
                    )
                } else {
                    SensorValueRow(
                        label = "활동 인식",
                        value = "측정 중...",
                    )
                }

                // 위치 정보
                if (currentLocation != null) {
                    SensorValueRow(
                        label = "위치",
                        value = String.format(
                            "%.6f, %.6f",
                            currentLocation!!.latitude,
                            currentLocation!!.longitude
                        ),
                        subValue = currentLocation!!.accuracy?.let {
                            "정확도: ${
                                String.format(
                                    "%.1f",
                                    it
                                )
                            }m"
                        } ?: "정확도: 측정 중",
                    )
                } else {
                    SensorValueRow(
                        label = "위치",
                        value = "측정 중...",
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
//                currentMovementState?.let { movementState ->
//                    val (statusText, statusColor) =
//                        when (movementState) {
//                            team.swyp.sdu.domain.service.MovementState.WALKING -> "걷는 중" to Color(0xFF4CAF50)
//                            team.swyp.sdu.domain.service.MovementState.RUNNING -> "뛰는 중" to Color(0xFFF44336)
//                            team.swyp.sdu.domain.service.MovementState.STILL -> "정지" to Color(0xFF9E9E9E)
//                            team.swyp.sdu.domain.service.MovementState.UNKNOWN -> "알 수 없음" to Color(0xFF9E9E9E)
//                        }
//
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(8.dp),
//                    ) {
//                        Row(
//                            horizontalArrangement = Arrangement.Center,
//                            verticalAlignment = Alignment.CenterVertically,
//                        ) {
//                            Box(
//                                modifier =
//                                    Modifier
//                                        .size(12.dp)
//                                        .background(statusColor, CircleShape),
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text(
//                                text = statusText,
//                                style = MaterialTheme.typography.titleLarge,
//                                fontWeight = FontWeight.Bold,
//                                color = statusColor,
//                            )
//                        }
//
//                        Text(
//                            text = "달리기 전환: 가속도 ≥ 4.5 m/s² 필요",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontSize = 11.sp,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            lineHeight = 14.sp,
//                        )
//                    }
//                }
//
//                if (currentSpeed >= 0.5f) {
//                    HorizontalDivider()
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(8.dp),
//                    ) {
//                        Text(
//                            text = "현재 속도",
//                            style = MaterialTheme.typography.labelMedium,
//                            color = MaterialTheme.colorScheme.primary,
//                        )
//                        Row(
//                            horizontalArrangement = Arrangement.spacedBy(24.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                        ) {
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Text(
//                                    text = String.format("%.1f", currentSpeed),
//                                    style = MaterialTheme.typography.headlineMedium,
//                                    fontWeight = FontWeight.Bold,
//                                )
//                                Text(
//                                    text = "m/s",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                )
//                                Text(
//                                    text = "(미터/초)",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    fontSize = 10.sp,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                )
//                            }
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                val speedKmh = currentSpeed * 3.6f
//                                Text(
//                                    text = String.format("%.1f", speedKmh),
//                                    style = MaterialTheme.typography.headlineMedium,
//                                    fontWeight = FontWeight.Bold,
//                                )
//                                Text(
//                                    text = "km/h",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                )
//                                Text(
//                                    text = "(킬로미터/시간)",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    fontSize = 10.sp,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                )
//                            }
//                        }
//                    }
//                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "오류",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onRetryClick) {
            Text("다시 시도")
        }
    }
}

@Composable
private fun ActivityStatusCard(
    activity: ActivityType,
    confidence: Int = 0,
) {
    val activityColor = getActivityColor(activity)
    val activityName = getActivityName(activity)
    val activityIcon = getActivityIcon(activity)

    var shouldAnimate by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        shouldAnimate = true
        kotlinx.coroutines.delay(300)
        shouldAnimate = false
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.05f else 1f,
        animationSpec =
            tween(
                durationMillis = 300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
        label = "pulse_scale",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(pulseScale),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = activityColor.copy(alpha = 0.15f),
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(
                            color = activityColor.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = activityIcon,
                    contentDescription = activityName,
                    modifier = Modifier.size(32.dp),
                    tint = activityColor,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "현재 활동",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activityName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = activityColor,
                )
                if (confidence > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "신뢰도: ${confidence}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun getActivityIcon(activity: ActivityType): ImageVector =
    when (activity) {
        ActivityType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        ActivityType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
        ActivityType.IN_VEHICLE -> Icons.Filled.DirectionsCar
        ActivityType.ON_BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        ActivityType.STILL -> Icons.Filled.Pause
        ActivityType.ON_FOOT -> Icons.AutoMirrored.Filled.DirectionsWalk
        ActivityType.UNKNOWN -> Icons.Filled.Pause
    }

private fun getActivityName(activity: ActivityType): String =
    when (activity) {
        ActivityType.WALKING -> "걷기"
        ActivityType.RUNNING -> "달리기"
        ActivityType.IN_VEHICLE -> "차량"
        ActivityType.ON_BICYCLE -> "자전거"
        ActivityType.STILL -> "정지"
        ActivityType.ON_FOOT -> "도보"
        ActivityType.UNKNOWN -> "알 수 없음"
    }

private fun getActivityColor(activity: ActivityType): Color =
    when (activity) {
        ActivityType.WALKING -> Color(0xFF4CAF50)
        ActivityType.RUNNING -> Color(0xFFF44336)
        ActivityType.IN_VEHICLE -> Color(0xFF2196F3)
        ActivityType.ON_BICYCLE -> Color(0xFF9C27B0)
        ActivityType.STILL -> Color(0xFF9E9E9E)
        ActivityType.ON_FOOT -> Color(0xFFFF9800)
        ActivityType.UNKNOWN -> Color(0xFF757575)
    }

private fun formatDistance(meters: Float): String =
    if (meters >= 1000) {
        String.format("%.2f km", meters / 1000f)
    } else {
        String.format("%.0f m", meters)
    }

private fun formatDuration(millis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun getMovementStateName(state: MovementState): String =
    when (state) {
        MovementState.STILL -> "정지"
        MovementState.WALKING -> "걷기"
        MovementState.RUNNING -> "달리기"
        MovementState.UNKNOWN -> "알 수 없음"
    }

@Composable
private fun SensorValueRow(
    label: String,
    value: String,
    subValue: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (subValue != null) {
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 0.dp),
            )
        }
    }
}

@Composable
private fun SensorStatusRow(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isAvailable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else if (isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else if (isAvailable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when {
                            isActive -> Color(0xFF4CAF50) // 초록색
                            isAvailable -> Color(0xFF9E9E9E) // 회색
                            else -> Color(0xFFF44336) // 빨간색
                        },
                        shape = CircleShape,
                    ),
            )
            Text(
                text = when {
                    isActive -> "활성"
                    isAvailable -> "대기"
                    else -> "사용 불가"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isActive -> Color(0xFF4CAF50)
                    isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
