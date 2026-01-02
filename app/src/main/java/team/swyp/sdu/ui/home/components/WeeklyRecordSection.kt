package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 주간 기록 섹션 컴포넌트
 */
@Composable
fun WeeklyRecordSection(
    records: List<WalkingSession>,
    onClickMore: () -> Unit,
    onClickWalk: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 헤더
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "나의 산책 기록",
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            MoreButton(onClick = onClickMore)
        }

        Spacer(Modifier.height(12.dp))

        // 기록 카드 리스트
        if (records.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                records.forEach { session ->
                    WeeklyRecordCard(
                        session = session,
                        modifier = Modifier.width(260.dp),
                    )
                }
            }
        } else {
            HomeEmptySession(onClick = onClickWalk)
        }
    }
}
