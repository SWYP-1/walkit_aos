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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.domain.service.ActivityType
import team.swyp.sdu.presentation.viewmodel.WalkingUiState
import team.swyp.sdu.presentation.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.components.LottieAnimationView
import java.util.concurrent.TimeUnit

/**
 * 산책 화면
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkingScreen(
    viewModel: WalkingViewModel = hiltViewModel(),
    onNavigateToRouteDetail: (List<team.swyp.sdu.data.model.LocationPoint>) -> Unit = {},
    onNavigateToResult: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
        if (currentState is WalkingUiState.Completed) {
            viewModel.reset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val currentState = viewModel.uiState.value
            if (currentState !is WalkingUiState.Completed &&
                currentState !is WalkingUiState.Walking
            ) {
                viewModel.reset()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val state = uiState) {
            is WalkingUiState.Initial,
            is WalkingUiState.EmotionSelection,
            -> {
                EmotionSelectionView(
                    viewModel = viewModel,
                    onNextClick = {
                        if (permissionsState.allPermissionsGranted) {
                            viewModel.startWalking()
                        }
                    },
                    permissionsGranted = permissionsState.allPermissionsGranted,
                )
            }

            is WalkingUiState.Walking -> {
                WalkingView(
                    stepCount = state.stepCount,
                    duration = state.duration,
                    distance = state.distance,
                    currentActivity = state.currentActivity,
                    currentMovementState = state.currentMovementState,
                    currentSpeed = state.currentSpeed,
                    debugInfo = state.debugInfo,
                    isPaused = state.isPaused,
                    onPauseToggle = {
                        if (state.isPaused) {
                            viewModel.resumeWalking()
                        } else {
                            viewModel.pauseWalking()
                        }
                    },
                    onStopClick = {
                        viewModel.stopWalking()
                        onNavigateToResult()
                    },
                )
            }

            is WalkingUiState.Completed -> {}

            is WalkingUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetryClick = { viewModel.reset() },
                )
            }
        }
    }
}

@Composable
private fun EmotionSelectionView(
    viewModel: WalkingViewModel,
    onNextClick: () -> Unit,
    permissionsGranted: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEmotions = when (uiState) {
        is WalkingUiState.EmotionSelection -> (uiState as WalkingUiState.EmotionSelection).selectedEmotions
        else -> emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "산책 전 나의 마음은 어떤가요?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        if (!permissionsGranted) {
            Text(
                text = "걸음 수 측정과 위치 추적을 위해 권한이 필요합니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        EmotionGrid(
            selectedEmotions = selectedEmotions,
            onEmotionToggle = { emotionType ->
                viewModel.toggleEmotion(emotionType)
            },
        )

        Button(
            onClick = onNextClick,
            enabled = permissionsGranted && selectedEmotions.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedEmotions.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text(
                text = "다음",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmotionGrid(
    selectedEmotions: Set<EmotionType>,
    onEmotionToggle: (EmotionType) -> Unit,
) {
    val positiveEmotions = listOf(
        EmotionType.HAPPY to "기쁘다",
        EmotionType.JOYFUL to "행복하다",
        EmotionType.LIGHT_FOOTED to "발걸음이 가볍다",
        EmotionType.EXCITED to "신난다",
        EmotionType.THRILLED to "설레인다",
    )

    val negativeEmotions = listOf(
        EmotionType.TIRED to "지친다",
        EmotionType.SAD to "슬프다",
        EmotionType.DEPRESSED to "우울하다",
        EmotionType.SLUGGISH to "축축 처진다",
        EmotionType.MANY_THOUGHTS to "생각이 많다",
        EmotionType.COMPLEX_MIND to "머릿속이 복잡하다",
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
            EmotionType.LIGHT_FOOTED,
            EmotionType.EXCITED,
            EmotionType.THRILLED,
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
    distance: Float,
    currentActivity: ActivityType?,
    currentMovementState: team.swyp.sdu.domain.service.MovementState?,
    currentSpeed: Float,
    debugInfo: team.swyp.sdu.presentation.viewmodel.WalkingUiState.DebugInfo?,
    isPaused: Boolean,
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
                                    text = formatDistance(distance),
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
                currentMovementState?.let { movementState ->
                    val (statusText, statusColor) =
                        when (movementState) {
                            team.swyp.sdu.domain.service.MovementState.WALKING -> "걷는 중" to Color(0xFF4CAF50)
                            team.swyp.sdu.domain.service.MovementState.RUNNING -> "뛰는 중" to Color(0xFFF44336)
                            team.swyp.sdu.domain.service.MovementState.STILL -> "정지" to Color(0xFF9E9E9E)
                            team.swyp.sdu.domain.service.MovementState.UNKNOWN -> "알 수 없음" to Color(0xFF9E9E9E)
                        }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .background(statusColor, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                            )
                        }

                        debugInfo?.let { info ->
                            val currentAccel = info.acceleration
                            val thresholdDescription =
                                when (movementState) {
                                    team.swyp.sdu.domain.service.MovementState.STILL -> {
                                        "가속도 ≤ 1.0 m/s² (현재: ${String.format("%.2f", currentAccel)} m/s²)"
                                    }

                                    team.swyp.sdu.domain.service.MovementState.WALKING -> {
                                        "가속도 1.0 ~ 2.5 m/s² (현재: ${String.format("%.2f", currentAccel)} m/s²)"
                                    }

                                    team.swyp.sdu.domain.service.MovementState.RUNNING -> {
                                        "가속도 ≥ 4.5 m/s² (현재: ${String.format("%.2f", currentAccel)} m/s²)"
                                    }

                                    else -> {
                                        "가속도: ${String.format("%.2f", currentAccel)} m/s²"
                                    }
                                }

                            Text(
                                text = thresholdDescription,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp,
                            )
                        }

                        Text(
                            text = "달리기 전환: 가속도 ≥ 4.5 m/s² 필요",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp,
                        )
                    }
                }

                if (currentSpeed >= 0.5f) {
                    HorizontalDivider()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "현재 속도",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f", currentSpeed),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "m/s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "(미터/초)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val speedKmh = currentSpeed * 3.6f
                                Text(
                                    text = String.format("%.1f", speedKmh),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "km/h",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "(킬로미터/시간)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        currentActivity?.let { activity ->
            ActivityStatusCard(activity = activity)
        }

        debugInfo?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "검증 정보",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider()
                    Text(
                        text = "센서 데이터",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "가속도",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.2f", info.acceleration),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "m/s² (미터/초²)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column {
                            Text(
                                text = "걸음/초",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.2f", info.stepsPerSecond),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Column {
                            Text(
                                text = "평균 보폭",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = info.averageStepLength?.let { String.format("%.2f m", it) } ?: "N/A",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = "걸음 수 비교",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "실제 걸음 수",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${info.realStepCount} 걸음",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Column {
                            Text(
                                text = "보간 걸음 수",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${info.interpolatedStepCount} 걸음",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = "거리 비교",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "GPS 거리",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.1f m", info.gpsDistance),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Column {
                            Text(
                                text = "Step 거리",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.1f m", info.stepBasedDistance),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Column {
                            Text(
                                text = "하이브리드",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.1f m", distance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    HorizontalDivider()
                    Text(
                        text = "거리 계산 공식",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "하이브리드 거리 = GPS 거리와 Step Counter 거리 결합",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "GPS 거리 (Haversine 공식):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "d = 2R × arcsin(√(sin²(Δlat/2) + cos(lat1)×cos(lat2)×sin²(Δlon/2)))",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "R = 6,371,000m (지구 반지름)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Step Counter 거리:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "거리 = 걸음 수 × 평균 보폭",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "평균 보폭 = GPS 거리 / 걸음 수 (동적 계산)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "GPS 정확도 ≤ 20m: GPS 우선 사용",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "GPS 부정확: Step Counter 거리 우선 사용",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "차이 > 20%: 가중 평균 (GPS 70% + Step 30%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()
                    Text(
                        text = "위치 정보",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "위치 포인트",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${info.locationPointCount}개",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        info.lastLocation?.let { location ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "마지막 위치",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = String.format("%.6f, %.6f", location.latitude, location.longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                )
                                location.accuracy?.let { accuracy ->
                                    Text(
                                        text = String.format("정확도: %.1fm", accuracy),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
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
private fun ActivityStatusCard(activity: ActivityType) {
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

