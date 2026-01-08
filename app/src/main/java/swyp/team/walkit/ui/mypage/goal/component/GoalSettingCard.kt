package swyp.team.walkit.ui.mypage.goal.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.goal.GoalRange
import swyp.team.walkit.ui.theme.Green4
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White
import swyp.team.walkit.ui.theme.walkItTypography
import java.text.NumberFormat
import java.util.Locale

/**
 * 목표 설정 카드 컴포넌트
 *
 * @param title 제목 텍스트
 * @param modifier Modifier
 * @param currentNumber 현재 선택된 숫자
 * @param onNumberChange 숫자 변경 콜백
 * @param range 유효 범위
 * @param unit 단위 (예: "회", "보")
 * @param onClickMinus 감소 버튼 클릭 콜백
 * @param onClickPlus 증가 버튼 클릭 콜백
 * @param accentColor 강조 색상 (테두리 및 아이콘 tint에 사용, 기본값: Green4)
 * @param backgroundColor 배경 색상 (기본값: White)
 * @param borderColor 테두리 색상 (기본값: accentColor와 동일)
 * @param iconTint 아이콘 tint 색상 (기본값: accentColor와 동일)
 * @param plusButtonBackgroundColor 증가 버튼 배경 색상 (기본값: accentColor)
 * @param plusButtonIconTint 증가 버튼 아이콘 tint (기본값: White)
 */
@Composable
fun GoalSettingCard(
    title: String,
    modifier: Modifier = Modifier,
    currentNumber: Int,
    onNumberChange: (Int) -> Unit,
    range: GoalRange,
    unit: String,
    onClickMinus: () -> Unit = {},
    onClickPlus: () -> Unit = {},
    accentColor: Color = Green4,
    backgroundColor: Color = White,
    borderColor: Color = accentColor,
    iconTint: Color = accentColor,
    plusButtonBackgroundColor: Color = accentColor,
    plusButtonIconTint: Color = White,
) {
    val ControlHeight = 40.dp
    val ButtonShape = RoundedCornerShape(8.dp)
    
    // 숫자 포맷팅 (천 단위 구분자 추가)
    val formattedNumber = remember(currentNumber) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(currentNumber)
    }

    // 범위 숫자 포맷팅 (최소/최대 값에 천 단위 구분자 추가)
    val formattedMin = remember(range.min) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(range.min)
    }
    val formattedMax = remember(range.max) {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(range.max)
    }

    Column(modifier = modifier) {

        Text(
            text = title,
            style = MaterialTheme.walkItTypography.bodyM,
            color = Grey10
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "최소 ${formattedMin}${unit} ~ 최대 ${formattedMax}${unit}",
            style = MaterialTheme.walkItTypography.captionM,
            color = Grey10
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 숫자 표시 (읽기 전용)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ControlHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = SemanticColor.textBorderPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.walkItTypography.bodyM,
                    color = SemanticColor.textBorderSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // - 버튼
            Box(
                modifier = Modifier
                    .size(ControlHeight)
                    .clip(ButtonShape)
                    .background(backgroundColor)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = ButtonShape
                    )
                    .clickable(onClick = onClickMinus),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_minus),
                    contentDescription = "감소",
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }

            Spacer(Modifier.width(4.dp))

            // + 버튼
            Box(
                modifier = Modifier
                    .size(ControlHeight) // ⭐ 핵심: 높이 통일
                    .clip(ButtonShape)
                    .background(plusButtonBackgroundColor)
                    .clickable(onClick = onClickPlus),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_plus),
                    contentDescription = "증가",
                    modifier = Modifier.size(20.dp),
                    tint = plusButtonIconTint
                )
            }
        }
    }
}


@Composable
@Preview(name = "기본 색상 (Green4)")
fun GoalSettingCardPreview() {
    WalkItTheme {
        Surface(color = White) {
            GoalSettingCard(
                title = "주간 산책 횟수",
                modifier = Modifier.padding(16.dp),
                currentNumber = 5,
                onNumberChange = {},
                range = GoalRange(1, 7),
                unit = "회",
                onClickMinus = {},
                onClickPlus = {}
            )
        }
    }
}

@Composable
@Preview(name = "커스텀 색상 (파란색) - 천 단위 구분자")
fun GoalSettingCardCustomColorPreview() {
    WalkItTheme {
        Surface(color = White) {
            GoalSettingCard(
                title = "걸음 수 목표",
                modifier = Modifier.padding(16.dp),
                currentNumber = 10000, // 10,000으로 표시됨
                onNumberChange = {},
                range = GoalRange(1000, 30000), // 1,000 ~ 30,000으로 표시됨
                unit = "보",
                onClickMinus = {},
                onClickPlus = {},
                accentColor = Color(0xFF2196F3), // 파란색
            )
        }
    }
}

@Composable
@Preview(name = "큰 숫자 - 100000보")
fun GoalSettingCardLargeNumberPreview() {
    WalkItTheme {
        Surface(color = White) {
            GoalSettingCard(
                title = "걸음 수 목표",
                modifier = Modifier.padding(16.dp),
                currentNumber = 100000, // 100,000으로 표시됨
                onNumberChange = {},
                range = GoalRange(1000, 500000), // 1,000 ~ 500,000으로 표시됨
                unit = "보",
                onClickMinus = {},
                onClickPlus = {},
                accentColor = Color(0xFF2196F3),
            )
        }
    }
}

@Composable
@Preview(name = "개별 색상 지정")
fun GoalSettingCardIndividualColorPreview() {
    WalkItTheme {
        Surface(color = White) {
            GoalSettingCard(
                title = "커스텀 목표",
                modifier = Modifier.padding(16.dp),
                currentNumber = 3,
                onNumberChange = {},
                range = GoalRange(1, 10),
                unit = "회",
                onClickMinus = {},
                onClickPlus = {},
                accentColor = Color(0xFF9C27B0), // 보라색
                borderColor = Color(0xFF7B1FA2), // 진한 보라색 (테두리)
                iconTint = Color(0xFF9C27B0), // 보라색 (아이콘)
                plusButtonBackgroundColor = Color(0xFF9C27B0), // 보라색 배경
                plusButtonIconTint = White, // 흰색 아이콘
            )
        }
    }
}

