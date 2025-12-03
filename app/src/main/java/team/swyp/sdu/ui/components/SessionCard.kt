package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import team.swyp.sdu.ui.theme.TtTheme

@Composable
fun SessionCard(
    modifier: Modifier = Modifier,
    category: String = "카테고리",
    language: String = "Kotlin",
    title: String = "Coroutine Deep Dive - Android 실전편",
    track: String = "Track 01",
    time: String = "17:00 발표",
    speakerName: String = "김준비",
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top labels
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 카테고리 라벨 (어두운 회색)
                    PillLabel(
                        text = category,
                        backgroundColor = Color(0xFF4A4A4A),
                        textColor = Color.White,
                    )
                    // Kotlin 라벨 (연한 회색)
                    PillLabel(
                        text = language,
                        backgroundColor = Color(0xFFE0E0E0),
                        textColor = Color(0xFF4A4A4A),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title (큰 굵은 초록색)
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    lineHeight = 32.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Track and Time labels
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Track 01 (밝은 초록색)
                    PillLabel(
                        text = track,
                        backgroundColor = Color(0xFF4CAF50),
                        textColor = Color.White,
                    )
                    // 17:00 발표 (연한 초록색)
                    PillLabel(
                        text = time,
                        backgroundColor = Color(0xFFC8E6C9),
                        textColor = Color(0xFF2E7D32),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Speaker name (왼쪽 하단, 큰 굵은 초록색)
                Text(
                    text = speakerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right side - Placeholder for profile image (원형)
            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center,
            ) {
                // 프로필 이미지 자리 - 실제 이미지는 여기에 로드하면 됨
                Text(
                    text = "👤",
                    fontSize = 40.sp,
                )
            }
        }
    }
}

@Composable
fun PillLabel(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp),
                ).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SessionCardPreview() {
    TtTheme {
        SessionCard(
            modifier = Modifier.padding(16.dp),
        )
    }
}
