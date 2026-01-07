package swyp.team.walkit.ui.mypage.userInfo.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import swyp.team.walkit.ui.theme.Grey7
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 사용자 정보 표시 섹션 컴포넌트
 * 유저 ID, 연동된 계정 등의 읽기 전용 정보 표시
 */
@Composable
fun UserInfoDisplaySection(
    modifier: Modifier = Modifier,
    provider: String? = null,
    email: String? = null,
) {
    val tertiaryText = SemanticColor.textBorderTertiary

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 이메일 표시 필드 (비활성화)
        email?.let {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "이메일",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey7,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = SemanticColor.backgroundWhiteSecondary,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = tertiaryText,
                        maxLines = 1,
                    )
                }

            }
        }
        // 연동된 계정 표시 필드 (비활성화)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "연동된 계정",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Grey7,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = SemanticColor.backgroundWhiteSecondary,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = provider ?: "알 수 없음",
                    style = MaterialTheme.walkItTypography.bodyM.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = tertiaryText,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // 위/아래 8dp
                )
            }

        }


    }
}
