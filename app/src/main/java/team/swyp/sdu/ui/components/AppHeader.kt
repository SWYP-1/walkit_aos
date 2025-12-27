package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import team.swyp.sdu.ui.theme.Pretendard
import team.swyp.sdu.ui.theme.TypeScale
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 앱 헤더 컴포넌트
 *
 * 여러 화면에서 공통으로 사용하는 헤더
 * - 왼쪽: 뒤로가기 버튼
 * - 중앙: 제목 (변경 가능)
 * - 오른쪽: 선택적 액션 버튼 (프로필 이미지, 아이콘 등)
 *
 * @param title 헤더 제목
 * @param showBackButton 뒤로가기 버튼 표시 여부
 * @param onNavigateBack 뒤로가기 클릭 핸들러
 * @param modifier Modifier
 * @param rightAction 오른쪽 액션 (선택사항)
 */
@Composable
fun AppHeader(
    title: String,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    rightAction: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // Material Design 표준 헤더 높이
            .background(Color(0xFFFFFFFF)) // color/background/whtie-primary
            .border(
                width = 1.dp,
                color = Color(0xFFF3F3F5), // color/text-border/disabled
                shape = RectangleShape,
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 뒤로가기 버튼
            if (showBackButton) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF191919), // color/icon/black
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Box(modifier = Modifier.size(24.dp))
            }
            // 중앙: 제목 (가운데 정렬)
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.walkItTypography.bodyXL
            )

            // 오른쪽: 액션 버튼 또는 프로필 이미지
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (rightAction != null) {
                    rightAction()
                } else {
                    // 기본: 빈 공간 (레이아웃 균형 유지)
                }
            }
        }
    }
}

/**
 * 프로필 이미지 액션 (오른쪽 버튼용)
 */
@Composable
fun ProfileImageAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(24.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFF9F9FA)), // color/background/white-secondary
        )
    }
}

/**
 * 설정 아이콘 액션 (오른쪽 버튼용)
 */
@Composable
fun SettingsIconAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(24.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "설정",
            tint = Color(0xFF191919), // color/icon/black
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview
@Composable
fun AppHeaderPreview() {
    WalkItTheme {
        AppHeader(
            title = "마이 페이지",
            onNavigateBack = {},
        )
    }
}

