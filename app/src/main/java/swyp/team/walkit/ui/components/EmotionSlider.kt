package swyp.team.walkit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.WalkItTypography
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.utils.createDefaultEmotionOptions
import kotlin.math.roundToInt

/**
 * 감정 옵션 데이터 클래스
 *
 * @param imageResId 감정을 나타내는 이미지 리소스 ID
 * @param label 감정 라벨 (예: "기쁘다", "슬프다")
 * @param boxColor 박스 배경 색상
 * @param textColor 텍스트 색상
 * @param value 감정 값 (정렬 및 비교용)
 */
data class EmotionOption(
    val imageResId: Int,
    val label: String,
    val boxColor: Color,
    val textColor: Color,
    val value: Int
)

/**
 * 감정 선택 슬라이더
 *
 * Material3 Expressive의 VerticalSlider를 사용하여 감정을 선택할 수 있습니다.
 * 슬라이더는 0.0~1.0 비율 기반으로 동작하며, 감정 리스트와 독립적으로 높이만 맞춥니다.
 *
 * @param emotions 감정 옵션 리스트 (위에서 아래 순서)
 * @param selectedIndex 현재 선택된 감정의 인덱스
 * @param onEmotionSelected 감정이 선택될 때 호출되는 콜백
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmotionSlider(
    emotions: List<EmotionOption>,
    selectedIndex: Int?,
    onEmotionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    // 현재 선택된 인덱스
    val currentSelectedIndex = selectedIndex ?: (emotions.size / 2).coerceIn(0, emotions.size - 1)

    // 슬라이더 상태 (0.0 ~ 1.0 범위 사용 - 절대 비율 기반)
    val sliderState = rememberSliderState(
        value = if (emotions.size > 1) {
            currentSelectedIndex.toFloat() / (emotions.size - 1).toFloat()
        } else {
            0f
        },
        valueRange = 0f..1f,
        steps = emotions.size - 2 // 중간 스텝 수
    )

    // selectedIndex가 변경되면 슬라이더 상태 업데이트
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null && selectedIndex in emotions.indices) {
            val normalizedValue = if (emotions.size > 1) {
                selectedIndex.toFloat() / (emotions.size - 1).toFloat()
            } else {
                0f
            }
            sliderState.value = normalizedValue
        }
    }

    // 슬라이더 값 변경 감지 (0.0~1.0 비율 → 인덱스 변환)
    LaunchedEffect(sliderState.value) {
        val normalizedValue = sliderState.value.coerceIn(0f, 1f)
        val newIndex = (normalizedValue * (emotions.size - 1)).roundToInt()
            .coerceIn(0, emotions.size - 1)

        if (newIndex != currentSelectedIndex) {
            onEmotionSelected(newIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(432.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Material3 Expressive VerticalSlider
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                VerticalSlider(
                    state = sliderState,
                    modifier = Modifier.fillMaxHeight(),
                    reverseDirection = false,
                    colors = SliderDefaults.colors(
                        activeTrackColor = SemanticColor.iconDisabled,
                        inactiveTrackColor = SemanticColor.iconDisabled,
                        thumbColor = SemanticColor.buttonPrimaryActive,
                        activeTickColor = SemanticColor.iconDisabled,
                        inactiveTickColor = SemanticColor.iconDisabled
                    ),
                    thumb = { state ->
                        // 커스텀 thumb 디자인
                        Box(
                            modifier = Modifier
                                .height(48.dp).width(35.dp)
                                .clip(CircleShape)
                                .background(SemanticColor.buttonPrimaryActive),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_info_equal),
                                contentDescription = "info equal",
                                tint = SemanticColor.iconWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(32.dp))

            // 감정 옵션들 (독립적으로 높이만 맞춤)
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                emotions.forEachIndexed { index, emotion ->
                    val isSelected = index == currentSelectedIndex
                    val alpha = if (isSelected) 1.0f else 0.5f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.alpha(alpha)
                    ) {
                        // 이미지 원형 배경
                        Image(
                            painter = painterResource(id = emotion.imageResId),
                            contentDescription = emotion.label,
                            alpha = alpha,
                            modifier = Modifier.graphicsLayer {
                                scaleX = if (isSelected) 1.2f else 1f
                                scaleY = if (isSelected) 1.2f else 1f
                                transformOrigin = TransformOrigin(1f, 0.5f) // 👈 오른쪽 기준
                            }
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        // 라벨
                        Box(
                            modifier = Modifier
                                .widthIn(min = 72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(emotion.boxColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emotion.label,
                                style = MaterialTheme.walkItTypography.bodyS,
                                fontWeight = FontWeight.SemiBold,
                                color = emotion.textColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmotionSliderPreview() {
    WalkItTheme {
        val emotions = createDefaultEmotionOptions()

        var selectedIndex by remember { mutableStateOf(3) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "산책 후 나의 마음은 어떤가요?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "산책 후 감정이 어떻게 변했는지 기록해주세요.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            EmotionSlider(
                emotions = emotions,
                selectedIndex = selectedIndex,
                onEmotionSelected = { index ->
                    selectedIndex = index
                }
            )
        }
    }
}