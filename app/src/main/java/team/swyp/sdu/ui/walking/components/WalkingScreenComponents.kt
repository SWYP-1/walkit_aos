package team.swyp.sdu.ui.walking.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.domain.service.ActivityType

/**
 * 감정 그리드 컴포넌트
 */
@Composable
fun EmotionGrid(
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
        EmotionType.IRRITATED to "짜증남",
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

/**
 * 감정 행 컴포넌트
 */
@Composable
fun EmotionRow(
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

/**
 * 감정 버튼 컴포넌트
 */
@Composable
fun EmotionButton(
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

/**
 * 활동 상태 카드 컴포넌트
 */
@Composable
fun ActivityStatusCard(activity: ActivityType) {
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

/**
 * 에러 뷰 컴포넌트
 */
@Composable
fun ErrorView(
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

/**
 * 활동 타입에 따른 아이콘 반환
 */
fun getActivityIcon(activity: ActivityType): ImageVector =
    when (activity) {
        ActivityType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
        ActivityType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
        ActivityType.IN_VEHICLE -> Icons.Filled.DirectionsCar
        ActivityType.ON_BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        ActivityType.STILL -> Icons.Filled.Pause
        ActivityType.ON_FOOT -> Icons.AutoMirrored.Filled.DirectionsWalk
        ActivityType.UNKNOWN -> Icons.Filled.Pause
    }

/**
 * 활동 타입에 따른 이름 반환
 */
fun getActivityName(activity: ActivityType): String =
    when (activity) {
        ActivityType.WALKING -> "걷기"
        ActivityType.RUNNING -> "달리기"
        ActivityType.IN_VEHICLE -> "차량"
        ActivityType.ON_BICYCLE -> "자전거"
        ActivityType.STILL -> "정지"
        ActivityType.ON_FOOT -> "도보"
        ActivityType.UNKNOWN -> "알 수 없음"
    }

/**
 * 활동 타입에 따른 색상 반환
 */
fun getActivityColor(activity: ActivityType): Color =
    when (activity) {
        ActivityType.WALKING -> Color(0xFF4CAF50)
        ActivityType.RUNNING -> Color(0xFFF44336)
        ActivityType.IN_VEHICLE -> Color(0xFF2196F3)
        ActivityType.ON_BICYCLE -> Color(0xFF9C27B0)
        ActivityType.STILL -> Color(0xFF9E9E9E)
        ActivityType.ON_FOOT -> Color(0xFFFF9800)
        ActivityType.UNKNOWN -> Color(0xFF9E9E9E)
    }

