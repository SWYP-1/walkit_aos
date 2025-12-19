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
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel


@Composable
fun PreWalkingEmotionSelectScreen(
    viewModel: WalkingViewModel,
    onNextClick: () -> Unit,
    permissionsGranted: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEmotions = when (uiState) {
        is WalkingUiState.PreWalkingEmotionSelection -> (uiState as WalkingUiState.PreWalkingEmotionSelection).preWalkingEmotion
        else -> null
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
                val isSelected = selectedEmotions == emotionType
                OutlinedButton(
                    onClick = { viewModel.selectPreWalkingEmotion(emotionType) },
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

            if (!permissionsGranted) {
                Text(
                    text = "걸음 수 측정과 위치 추적을 위해 권한이 필요합니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = onNextClick,
                enabled = permissionsGranted && selectedEmotions != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedEmotions != null) {
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
}