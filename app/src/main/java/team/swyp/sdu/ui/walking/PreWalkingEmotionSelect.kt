package team.swyp.sdu.ui.walking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.EmotionSlider
import team.swyp.sdu.ui.record.components.SectionCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.utils.createDefaultEmotionOptions
import team.swyp.sdu.ui.walking.utils.findSelectedEmotionIndex
import team.swyp.sdu.ui.walking.utils.valueToEmotionType

/**
 * 산책 전 감정 선택 화면
 *
 * UI 컴포넌트로 상태와 콜백을 파라미터로 받습니다.
 */
@Composable
fun PreWalkingEmotionSelectScreen(
    selectedEmotion: EmotionType?,
    permissionsGranted: Boolean,
    onEmotionSelected: (EmotionType) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 감정 옵션 리스트 생성
    val emotionOptions = remember {
        createDefaultEmotionOptions()
    }

    // 선택된 감정의 인덱스 찾기
    val selectedIndex = findSelectedEmotionIndex(selectedEmotion, emotionOptions)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {

        AppHeader(title = "", showBackButton = true, onNavigateBack = onPrev)
        Spacer(Modifier.height(22.dp))

        SectionCard {
            Text(
                text = "산책 전 나의 마음은 어떤가요?",
                style = MaterialTheme.walkItTypography.headingS.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "산책하기 전 지금 어떤 감정을 느끼는지 선택해주세요",
                style = MaterialTheme.walkItTypography.bodyS,
                color = SemanticColor.textBorderSecondary,
                textAlign = TextAlign.Center,
            )
        }

        // EmotionSlider를 사용한 감정 선택
        EmotionSlider(
            modifier = Modifier.fillMaxWidth(),
            emotions = emotionOptions,
            selectedIndex = selectedIndex,
            onEmotionSelected = { index ->
                if (index in emotionOptions.indices) {
                    val emotionType = valueToEmotionType(emotionOptions[index].value)
                    onEmotionSelected(emotionType)
                }
            }
        )

        // 권한 안내 메시지
        if (!permissionsGranted) {
            Text(
                text = "걸음 수 측정과 위치 추적을 위해 권한이 필요합니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CtaButton(
                text = "이전으로",
                textColor = SemanticColor.buttonPrimaryDefault,
                buttonColor = SemanticColor.backgroundWhitePrimary,
                onClick = onPrev,
                modifier = Modifier.width(96.dp)
            )

            CtaButton(
                text = "다음으로",
                textColor = SemanticColor.textBorderPrimaryInverse,
                onClick = onNext,
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = "arrow forward",
                        tint = SemanticColor.iconWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun PreWalkingEmotionSelectScreenPreview_NoSelection() {
    WalkItTheme {
        PreWalkingEmotionSelectScreen(
            selectedEmotion = null,
            permissionsGranted = true,
            onEmotionSelected = {},
            onPrev = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreWalkingEmotionSelectScreenPreview_WithSelection() {
    WalkItTheme {
        PreWalkingEmotionSelectScreen(
            selectedEmotion = EmotionType.HAPPY,
            permissionsGranted = true,
            onEmotionSelected = {},
            onPrev = {},
            onNext = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreWalkingEmotionSelectScreenPreview_NoPermissions() {
    WalkItTheme {
        PreWalkingEmotionSelectScreen(
            selectedEmotion = EmotionType.CONTENT,
            permissionsGranted = false,
            onEmotionSelected = {},
            onPrev = {},
            onNext = {},
        )
    }
}
