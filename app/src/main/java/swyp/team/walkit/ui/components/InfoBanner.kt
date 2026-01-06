package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.theme.BluePrimary
import swyp.team.walkit.ui.theme.BlueSecondary
import swyp.team.walkit.ui.theme.BlueTertiary
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 정보 알림 배너 컴포넌트
 *
 * 여러 화면에서 정보를 알리기 위한 재사용 가능한 배너입니다.
 * 예: "기기 알림을 켜주세요", "권한 설정이 필요합니다" 등
 *
 * @param title 배너 제목 (필수)
 * @param description 배너 설명 (선택)
 * @param backgroundColor 배경 색상 (기본값: BlueTertiary)
 * @param borderColor 테두리 색상 (기본값: BlueSecondary)
 * @param iconTint 아이콘 틴트 색상 (기본값: SemanticColor.stateBluePrimary)
 * @param textColor 텍스트 색상 (기본값: BluePrimary)
 * @param modifier Modifier
 */
@Composable
fun InfoBanner(
    title: String,
    description: String? = null,
    backgroundColor: Color = BlueTertiary,
    borderColor: Color = BlueSecondary,
    iconTint: Color = SemanticColor.stateBluePrimary,
    textColor: Color = BluePrimary,
    icon: @Composable (iconTint: Color) -> Unit = { tint -> InfoIcon(iconTint = tint) },
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(
                start = 12.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = if (description != null) Alignment.Top else Alignment.CenterVertically,
    ) {
        // 왼쪽: 정보 아이콘
        icon(iconTint)

        // 오른쪽: 텍스트 영역
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 제목
            Text(
                text = title,
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = textColor,
            )

            // 설명 (있는 경우)
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.walkItTypography.captionM,
                    color = textColor,
                )
            }
        }
    }
}

/**
 * 정보 아이콘
 *
 * 파란색 원 배경에 흰색 Info 아이콘이 있는 커스텀 아이콘입니다.
 *
 * @param iconTint 아이콘 틴트 색상
 * @param modifier Modifier
 */
@Composable
private fun InfoIcon(
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info_exclamation),
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Preview
@Composable
private fun InfoBannerPreview() {
    WalkItTheme {
        InfoBanner(
            title = "기기 알림을 켜주세요",
            description = "정보 알림을 받기 위해 기기 알림을 켜주세요",
        )
    }
}

@Preview
@Composable
private fun InfoBannerTitleOnlyPreview() {
    WalkItTheme {
        InfoBanner(
            title = "기기 알림을 켜주세요",
        )
    }
}

