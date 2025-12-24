package team.swyp.sdu.ui.mission.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import team.swyp.sdu.domain.model.MissionCategory
import team.swyp.sdu.domain.model.MissionConfig
import team.swyp.sdu.domain.model.WeeklyMission

/**
 * 미션 카드 컴포넌트
 *
 * 주간 미션 정보를 표시하는 카드
 */
@Composable
fun MissionCard(
    mission: WeeklyMission,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 카테고리 태그
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    val category = MissionCategory.fromApiValue(mission.category)
                    Text(
                        text = category?.displayName ?: mission.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black,
                    )
                }

                // 미션 제목
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                // 목표 정보 (MissionConfig 기반)
                val configText = when (val config = mission.getMissionConfig()) {
                    is MissionConfig.ChallengeStepsConfig -> {
                        "주간 ${config.weeklyGoalSteps.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")}보 달성"
                    }
                    is MissionConfig.PhotoColorConfig -> {
                        val colorName = when (config.color.uppercase()) {
                            "PURPLE" -> "보라색"
                            "RED" -> "빨간색"
                            "BLUE" -> "파란색"
                            "GREEN" -> "초록색"
                            "YELLOW" -> "노란색"
                            "ORANGE" -> "주황색"
                            "PINK" -> "분홍색"
                            "WHITE" -> "흰색"
                            "BLACK" -> "검은색"
                            else -> config.color
                        }
                        "${colorName} 사진 찍기"
                    }
                    null -> {
                        // 파싱 실패 또는 빈 설정인 경우 description 사용
                        // 서버에서 항상 값을 제공하므로, 파싱 실패는 예외 상황
                        mission.description
                    }
                }

                Text(
                    text = configText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575), // 회색으로 표시
                )

                // 보상
                Text(
                    text = "${mission.rewardPoints} p",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
            }

            // ArrowRight 아이콘 버튼
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "미션 상세",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
