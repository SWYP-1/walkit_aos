package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 감정 기록 섹션 컴포넌트
 */
@Composable
fun EmotionRecordSection(
    dominantEmotion: EmotionType?,
    dominantEmotionCount: Int?,
    recentEmotions: List<EmotionType?>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "나의 감정 기록",
            style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = SemanticColor.textBorderPrimary
        )

        Spacer(Modifier.height(12.dp))

        // 이번주 주요 감정 카드
        DominantEmotionCard(
            emotionType = dominantEmotion,
            emotionCount = dominantEmotionCount,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        // 최근 감정 아이콘 리스트
        RecentEmotionsList(
            emotions = recentEmotions,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 최근 감정 아이콘 리스트 컴포넌트
 */
@Composable
private fun RecentEmotionsList(
    emotions: List<EmotionType?>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val itemCount = emotions.size.coerceAtMost(7)

        repeat(7) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            ) {
                if (index < itemCount) {
                    emotions[index]?.let { emotion ->
                        EmotionIcon(emotionType = emotion)
                    }
                }
            }
        }
    }
}
