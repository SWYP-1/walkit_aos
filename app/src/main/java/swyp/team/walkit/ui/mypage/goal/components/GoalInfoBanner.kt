package swyp.team.walkit.ui.mypage.goal.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.InfoBanner
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 목표 관리 화면용 일반 InfoBanner
 * 고정 높이 72dp 사용
 */
@Composable
fun GoalInfoBanner(
    modifier: Modifier = Modifier
) {
    InfoBanner(
        title = "목표는 설정일부터 1주일 기준으로 설정가능합니다",
        description = "목표는 한 달에 한 번만 변경 가능합니다 \n변경된 목표는 목표 달성율과 캐릭터 레벨업에 영향을 미칩니다",
        backgroundColor = SemanticColor.backgroundWhiteTertiary,
        borderColor = SemanticColor.textBorderTertiary,
        textColor = SemanticColor.textBorderPrimary,
        descriptionTextColor = SemanticColor.textBorderSecondary,
        iconTint = SemanticColor.textBorderPrimary,
        icon = { iconTint ->
            Icon(
                painter = painterResource(R.drawable.ic_info_exclamation),
                contentDescription = "info",
                tint = iconTint
            )
        },
    )
}

/**
 * 목표 관리 화면용 에러 InfoBanner
 * 고정 높이 72dp 사용
 *
 * @param title 배너 제목 (기본값: "이번 달 목표 수정이 불가능합니다")
 * @param description 배너 설명 (기본값: "목표는 한 달에 한 번만 변경 가능합니다")
 */
@Composable
fun GoalErrorBanner(
    modifier: Modifier = Modifier,
    title: String = "이번 달 목표 수정이 불가능합니다",
    description: String = "목표는 한 달에 한 번만 변경 가능합니다"
) {
    InfoBanner(
        title = title,
        description = description,
        backgroundColor = SemanticColor.stateRedTertiary,
        borderColor = SemanticColor.stateRedSecondary,
        textColor = SemanticColor.stateRedPrimary,
        iconTint = SemanticColor.stateRedPrimary,
        icon = { iconTint ->
            Icon(
                painter = painterResource(R.drawable.ic_action_clear),
                contentDescription = "error",
                tint = iconTint
            )
        },
    )
}
