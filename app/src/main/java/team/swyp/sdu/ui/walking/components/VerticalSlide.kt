package team.swyp.sdu.ui.walking.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import kotlin.math.abs

/**
 * 감정 타입에 따른 아이콘 리소스 ID 반환
 */
private fun getEmotionIconResId(emotionType: EmotionType): Int {
    return when (emotionType) {
        EmotionType.HAPPY -> R.drawable.ic_circle_happy
        EmotionType.JOYFUL -> R.drawable.ic_circle_joyful
        EmotionType.CONTENT -> R.drawable.ic_circle_content
        EmotionType.DEPRESSED -> R.drawable.ic_circle_depressed
        EmotionType.TIRED -> R.drawable.ic_circle_tired
        EmotionType.ANXIOUS -> R.drawable.ic_circle_anxious
    }
}

/**
 * Material3 Expressive의 공식 VerticalSlider 구현
 * VerticalSlider가 사용 불가능한 경우 Slider를 회전시켜 사용
 * 회전 전에 충분한 가로 공간을 확보하여 실제 높이를 충분히 확보
 */
@Composable
fun TestMaterial3VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    // 드래그가 끝났을 때 onValueChangeFinished 호출
    LaunchedEffect(isDragged) {
        if (!isDragged) {
            onValueChangeFinished?.invoke()
        }
    }
    
    Box(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Slider를 회전시켜 수직 슬라이더로 사용
        // 회전 전에 충분한 가로 공간(fillMaxHeight)을 확보하여
        // 회전 후 실제 높이가 충분히 확보되도록 함
        Box(
            modifier = Modifier
                .fillMaxHeight() // 회전 전 가로 공간 (회전 후 높이가 됨)
                .width(48.dp) // 회전 전 높이 (회전 후 가로가 됨)
                .graphicsLayer {
                    rotationZ = -90f
                },
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                colors = colors,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth() // 회전 전 가로를 최대한 확보
            )
        }
    }
}


/**
 * 감정 선택용 VerticalSnapSlider
 * Box + pointerInput을 사용한 세로 Slider 직접 구현
 * 
 * @param modifier Modifier
 * @param emotions 감정 리스트 (위에서 아래 순서) - (EmotionType, 감정 라벨)
 * @param selectedIndex 현재 선택된 감정 인덱스
 * @param onEmotionSelected 감정 선택 시 호출되는 콜백
 */
@Composable
fun EmotionVerticalSnapSlider(
    modifier: Modifier = Modifier,
    emotions: List<Pair<EmotionType, String>>, // (EmotionType, 감정 라벨)
    selectedIndex: Int? = null,
    onEmotionSelected: (Int) -> Unit = {}
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    // 스냅 포인트 생성 (0f ~ 100f 범위)
    val snapPoints = if (emotions.size > 1) {
        emotions.indices.map { index ->
            index * (100f / (emotions.size - 1))
        }
    } else {
        listOf(50f)
    }
    
    // 초기값 설정
    val initialValue = if (selectedIndex != null && selectedIndex in emotions.indices) {
        snapPoints[selectedIndex]
    } else {
        snapPoints[snapPoints.size / 2]
    }
    
    var rawValue by remember(selectedIndex) {
        mutableFloatStateOf(
            if (selectedIndex != null && selectedIndex in emotions.indices) {
                snapPoints[selectedIndex]
            } else {
                initialValue
            }
        )
    }
    
    // 외부에서 selectedIndex가 변경되면 값 업데이트
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null && selectedIndex in emotions.indices) {
            rawValue = snapPoints[selectedIndex]
        }
    }
    
    // 애니메이션된 값
    val animatedValue by animateFloatAsState(
        targetValue = rawValue,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "snapSlider"
    )
    
    // 현재 선택된 인덱스 계산
    val currentIndex = findNearestSnapIndex(animatedValue, snapPoints)
    
    // 드래그 중인지 추적
    var isDragging by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 왼쪽: 세로 Slider
        Box(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            VerticalSnapSlider(
                values = snapPoints,
                value = animatedValue,
                onValueChange = { newValue ->
                    rawValue = newValue
                    isDragging = true
                },
                onValueChangeFinished = {
                    // 드래그가 끝났을 때 스냅 적용
                    val snappedValue = findNearestSnap(rawValue, snapPoints)
                    rawValue = snappedValue
                    val index = snapPoints.indexOf(snappedValue)
                    if (index in emotions.indices) {
                        onEmotionSelected(index)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    isDragging = false
                },
                modifier = Modifier.fillMaxHeight()
            )
        }
        
        // 오른쪽: 모든 감정 아이콘과 텍스트
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            emotions.forEachIndexed { index, (emotionType, label) ->
                val isSelected = currentIndex == index
                
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 아이콘
                    Image(
                        painter = painterResource(id = getEmotionIconResId(emotionType)),
                        contentDescription = label,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    // 텍스트
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 세로 Snap Slider 직접 구현
 * Box + pointerInput을 사용하여 Y 좌표를 value로 매핑
 */
@Composable
private fun VerticalSnapSlider(
    values: List<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val trackColor = Color(0xFFE0E0E0)
    val activeTrackColor = Color(0xFF2ABB42)
    val thumbColor = Color(0xFF2ABB42)
    val thumbRadius = 8.dp
    val trackWidth = 4.dp
    
    var containerHeight by remember { mutableStateOf(0f) }
    var thumbY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // thumb 위치 계산
    LaunchedEffect(value, containerHeight) {
        if (containerHeight > 0f) {
            thumbY = containerHeight - (value / 100f * containerHeight)
        }
    }
    
    Box(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .pointerInput(value, containerHeight) {
                val thumbRadiusPx = with(density) { thumbRadius.toPx() }
                val touchAreaRadius = thumbRadiusPx * 2.5f // 터치 영역 확대
                val trackCenterXPx = with(density) { 24.dp.toPx() } // width 48.dp의 절반
                val trackTouchWidth = with(density) { 24.dp.toPx() } // 트랙 터치 영역
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (containerHeight > 0f) {
                            // thumb 영역 내에서 터치했는지 확인
                            val currentThumbY = containerHeight - (value / 100f * containerHeight)
                            val distanceFromThumb = kotlin.math.abs(offset.y - currentThumbY)
                            
                            // thumb 영역 내에서 터치했거나, 트랙 근처에서 터치한 경우
                            val distanceFromTrack = kotlin.math.abs(offset.x - trackCenterXPx)
                            val isNearTrack = distanceFromTrack < trackTouchWidth
                            
                            if (distanceFromThumb <= touchAreaRadius || isNearTrack) {
                                isDragging = true
                                // 터치 시작 위치로 즉시 이동
                                val y = offset.y.coerceIn(0f, containerHeight)
                                val newValue = 100f - (y / containerHeight * 100f)
                                onValueChange(newValue.coerceIn(0f, 100f))
                            }
                        }
                    },
                    onDragEnd = {
                        // 드래그 종료 시 스냅 적용
                        isDragging = false
                        onValueChangeFinished()
                    }
                ) { change, dragAmount ->
                    if (isDragging && containerHeight > 0f) {
                        // 터치 위치를 부모 기준으로 변환
                        val y = change.position.y.coerceIn(0f, containerHeight)
                        
                        // Y 좌표를 value로 변환 (0f ~ 100f)
                        // 위쪽이 0f, 아래쪽이 100f
                        val newValue = 100f - (y / containerHeight * 100f)
                        val clampedValue = newValue.coerceIn(0f, 100f)
                        
                        onValueChange(clampedValue)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    containerHeight = size.height.toFloat()
                }
        ) {
            val trackStartY = 0f
            val trackEndY = size.height
            val trackCenterX = size.width / 2f
            
            // 비활성 트랙 (전체)
            drawLine(
                color = trackColor,
                start = Offset(trackCenterX, trackStartY),
                end = Offset(trackCenterX, trackEndY),
                strokeWidth = with(density) { trackWidth.toPx() },
                cap = StrokeCap.Round
            )
            
            // 활성 트랙 (선택된 값까지)
            val activeEndY = trackEndY - (value / 100f * (trackEndY - trackStartY))
            drawLine(
                color = activeTrackColor,
                start = Offset(trackCenterX, activeEndY),
                end = Offset(trackCenterX, trackEndY),
                strokeWidth = with(density) { trackWidth.toPx() },
                cap = StrokeCap.Round
            )
            
            // Thumb (선택된 위치) - 드래그 중일 때 약간 크게 표시
            val currentThumbY = trackEndY - (value / 100f * (trackEndY - trackStartY))
            val thumbRadiusPx = with(density) { 
                thumbRadius.toPx() * if (isDragging) 1.2f else 1f 
            }
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(trackCenterX, currentThumbY)
            )
        }
    }
}

/**
 * 기존 VerticalSnapSlider (하위 호환성 유지)
 */
@Composable
fun VerticalSnapSlider(
    modifier: Modifier = Modifier,
    snapPoints: List<Float> = listOf(0f, 25f, 50f, 75f, 100f),
    initialValue: Float = 50f,
    onSnapChanged: (Int) -> Unit = {}
) {
    var rawValue by remember { mutableFloatStateOf(initialValue) }

    // "탁" 붙는 애니메이션
    val animatedValue by animateFloatAsState(
        targetValue = rawValue,
        animationSpec = tween(
            durationMillis = 120,
            easing = FastOutSlowInEasing
        ),
        label = "snapSlider"
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 상단 아이콘 예시
        Text(
            text = "😆",
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = animatedValue,
            onValueChange = { rawValue = it },
            onValueChangeFinished = {
                val snappedValue = findNearestSnap(rawValue, snapPoints)
                rawValue = snappedValue

                val index = snapPoints.indexOf(snappedValue)
                onSnapChanged(index)

                // 촉각 피드백 (선택)
                haptic.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove
                )
            },
            valueRange = snapPoints.first()..snapPoints.last(),
            steps = snapPoints.size - 2,
            modifier = Modifier
                .height(280.dp)
                .graphicsLayer {
                    rotationZ = -90f
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 하단 아이콘 예시
        Text(
            text = "😭",
            fontSize = 32.sp
        )
    }
}

private fun findNearestSnap(
    value: Float,
    points: List<Float>
): Float {
    return points.minBy { abs(it - value) }
}

private fun findNearestSnapIndex(
    value: Float,
    points: List<Float>
): Int {
    return points.indexOf(points.minBy { abs(it - value) })
}
