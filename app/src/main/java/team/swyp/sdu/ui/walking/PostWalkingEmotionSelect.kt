package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel

/**
 * 감정 선택 단계 화면 (단계 1)
 * 슬라이더로 감정을 선택하는 화면
 */
@Composable
fun PostWalkingEmotionSelectScreen(
    viewModel: WalkingViewModel,
    onNext: () -> Unit,
) {
    val selectedEmotion by viewModel.postWalkingEmotion.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "산책 후 나의 마음은 어떤가요?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // 감정 선택 버튼들
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val emotions = listOf(
                EmotionType.HAPPY to "기쁨 😊",
                EmotionType.JOYFUL to "즐거움 🎉",
                EmotionType.CONTENT to "행복함 😌",
                EmotionType.DEPRESSED to "우울함 😔",
                EmotionType.TIRED to "지침 😴",
                EmotionType.ANXIOUS to "짜증남 😠"
            )

            emotions.forEach { (emotionType, displayText) ->
                val isSelected = selectedEmotion == emotionType
                OutlinedButton(
                    onClick = { viewModel.selectPostWalkingEmotion(emotionType) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = selectedEmotion != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedEmotion != null) {
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