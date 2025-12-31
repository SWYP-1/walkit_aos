package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.home.utils.getEmotionDrawableRes
import team.swyp.sdu.ui.home.utils.getEmotionName
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.R
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 주요 감정을 표시하는 카드 (이번주/이번달)
 */
@Composable
fun DominantEmotionCard(
    emotionType: EmotionType?,
    emotionCnt: String = "4",
    periodText: String = "이번주",
    modifier: Modifier = Modifier,
) {
    val backgroundColor = getEmotionBackgroundColor(emotionType)
    val textColor = getEmotionTextColor(emotionType)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "${periodText} 나의 주요 감정은?",
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = textColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (emotionType != null) getEmotionName(emotionType) else "기록이 없어요",
                    style = MaterialTheme.walkItTypography.headingL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (emotionType == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "아직 산책 기록이 없어요!\n남은 일상을 워킷과 함께 보내볼까요?",
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = textColor,
                    )
                } else {
                    Text(
                        text = "${emotionType.ko} 감정을 ${if (periodText == "이번주") "7일 동안" else "이번 달에"} ${emotionCnt}회 경험했어요!\n남은 일상도 워킷과 함께 기쁘게 보내볼까요?",
                        // caption M/regular
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = textColor,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .aspectRatio(1f),
            ) {
                EmotionIcon(emotionType = emotionType)
            }
        }
    }
}

/**
 * 감정 타입에 따른 배경색 반환
 */
@Composable
private fun getEmotionBackgroundColor(emotionType: EmotionType?): androidx.compose.ui.graphics.Color {
    return when (emotionType) {
        EmotionType.HAPPY -> SemanticColor.stateYellowSecondary      // 기쁨 -> yellowSecondary
        EmotionType.JOYFUL -> SemanticColor.stateGreenSecondary      // 즐거움 -> greenSecondary
        EmotionType.CONTENT -> SemanticColor.statePinkSecondary      // 행복 -> pinkSecondary
        EmotionType.DEPRESSED -> SemanticColor.stateBlueSecondary   // 우울 -> blueSecondary
        EmotionType.TIRED -> SemanticColor.statePurpleSecondary      // 지침 -> purpleSecondary
        EmotionType.ANXIOUS -> SemanticColor.stateRedPrimary        // 짜증남 -> redPrimary
        null -> SemanticColor.backgroundWhiteTertiary                // 기록 없음 -> whiteTertiary
    }
}

/**
 * 감정 타입에 따른 텍스트 색상 반환
 * 배경이 Secondary 색상일 때 텍스트는 Primary 색상을 사용하여 대비를 확보
 */
@Composable
private fun getEmotionTextColor(emotionType: EmotionType?): androidx.compose.ui.graphics.Color {
    return when (emotionType) {
        EmotionType.HAPPY -> SemanticColor.stateYellowTertiaryInverse  // 기쁨 -> yellowTertiaryInverse
        EmotionType.JOYFUL -> SemanticColor.stateGreenTertiary          // 즐거움 -> greenPrimary (배경이 Secondary이므로 Primary 사용)
        EmotionType.CONTENT -> SemanticColor.statePinkPrimary           // 행복 -> pinkPrimary
        EmotionType.DEPRESSED -> SemanticColor.stateBluePrimary         // 우울 -> bluePrimary
        EmotionType.TIRED -> SemanticColor.statePurplePrimary            // 지침 -> purplePrimary
        EmotionType.ANXIOUS -> SemanticColor.stateRedTertiary           // 짜증남 -> redTertiary
        null -> SemanticColor.textBorderSecondary                       // 기록 없음 -> textBorderSecondary
    }
}

/**
 * 감정 아이콘 컴포넌트
 */
@Composable
fun EmotionIcon(emotionType: EmotionType?) {
    val drawableRes = if (emotionType != null) {
        getEmotionDrawableRes(emotionType)
    } else {
        R.drawable.ic_face_default
    }

    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

@Preview(showBackground = true, name = "기쁨 (이번달)")
@Composable
private fun DominantEmotionCardHappyPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.HAPPY,
            periodText = "이번달",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "즐거움 (JOYFUL)")
@Composable
private fun DominantEmotionCardJoyfulPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.JOYFUL,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "행복함 (CONTENT)")
@Composable
private fun DominantEmotionCardContentPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.CONTENT,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "우울함 (DEPRESSED)")
@Composable
private fun DominantEmotionCardDepressedPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.DEPRESSED,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "지침 (TIRED)")
@Composable
private fun DominantEmotionCardTiredPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.TIRED,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "짜증남 (ANXIOUS)")
@Composable
private fun DominantEmotionCardAnxiousPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = EmotionType.ANXIOUS,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "기록 없음 (null)")
@Composable
private fun DominantEmotionCardNullPreview() {
    WalkItTheme {
        DominantEmotionCard(
            emotionType = null,
            modifier = Modifier.padding(16.dp),
        )
    }
}


