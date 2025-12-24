package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 미션 카드 컴포넌트
 *
 * 홈 화면에서 주간 미션을 표시하는 카드 컴포넌트입니다.
 * Figma 디자인에 맞춰 구현되었습니다.
 *
 * @param mission 미션 정보 (제목, 보상, 카테고리)
 * @param modifier Modifier
 * @param onClick 미션 카드 클릭 시 호출되는 콜백
 */
@Composable
fun MissionCard(
    mission: HomeMission,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SemanticColor.backgroundWhitePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 카테고리 태그 (12px, semibold, 파란색)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SemanticColor.stateBlueTertiary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                // caption M/semibold
                Text(
                    text = mission.category,
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = SemanticColor.stateBluePrimary
                )
            }

            // 미션 제목과 보상, 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // 미션 제목 (18px, semibold)
                    // body L/semibold
                    Text(
                        text = mission.title,
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary,
                    )
                    // 보상 (14px, medium, 초록색)
                    // body S/medium
                    Text(
                        text = mission.reward.replace(" Exp", " Exp").replace(" p", " Exp"),
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = SemanticColor.textBorderGreenSecondary
                    )
                }
                // 도전하기 버튼 (14px, semibold, 흰색, 높이 40px)
                // body S/semibold
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF191919))
                        .clickableNoRipple(onClick)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "도전하기",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SemanticColor.textBorderPrimaryInverse
                    )
                }
            }
        }
    }
}


@Composable
@Preview(showBackground = true)
fun MissionCardPreview() {
    WalkItTheme {
        MissionCard(
            mission = HomeMission(
                title = "5,000보 이상 걷기",
                reward = "30 Exp",
                category = "챌린지",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}