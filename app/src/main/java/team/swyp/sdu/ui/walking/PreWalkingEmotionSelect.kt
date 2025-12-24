package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.EmotionSlider
import team.swyp.sdu.ui.walking.utils.createDefaultEmotionOptions
import team.swyp.sdu.ui.walking.utils.findSelectedEmotionIndex
import team.swyp.sdu.ui.walking.utils.valueToEmotionType
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel


@Composable
fun PreWalkingEmotionSelectScreen(
    viewModel: WalkingViewModel,
    onNextClick: () -> Unit,
    permissionsGranted: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedEmotion = when (uiState) {
        is WalkingUiState.PreWalkingEmotionSelection -> (uiState as WalkingUiState.PreWalkingEmotionSelection).preWalkingEmotion
        else -> null
    }

    // 감정 옵션 리스트 생성
    val emotionOptions = remember {
        createDefaultEmotionOptions()
    }

    // 선택된 감정의 인덱스 찾기
    val selectedIndex = findSelectedEmotionIndex(selectedEmotion, emotionOptions)

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        // EmotionSlider를 사용한 감정 선택
        EmotionSlider(
            modifier = Modifier.fillMaxWidth(),
            emotions = emotionOptions,
            selectedIndex = selectedIndex,
            onEmotionSelected = { index ->
                if (index in emotionOptions.indices) {
                    val emotionType = valueToEmotionType(emotionOptions[index].value)
                    viewModel.selectPreWalkingEmotion(emotionType)
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

        // 다음 버튼
        Button(
            onClick = onNextClick,
            enabled = permissionsGranted && selectedEmotion != null,
            modifier = Modifier.fillMaxWidth(),
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
