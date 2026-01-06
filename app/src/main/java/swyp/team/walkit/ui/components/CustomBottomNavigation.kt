package swyp.team.walkit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults.contentColor
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.shadow
import swyp.team.walkit.ui.theme.SemanticColor

/**
 * 커스텀 바텀 네비게이션 아이템 데이터 클래스
 */
data class BottomBarItem(
    val route: String,
    val icon: @Composable () -> Unit,
    val label: String
)

/**
 * 개별 바텀 네비게이션 아이템 뷰
 */
@Composable
private fun BottomBarItemView(
    item: BottomBarItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val durationMillis = 150

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            SemanticColor.iconGreen
        } else {
            SemanticColor.iconDisabled
        },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "contentColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            SemanticColor.stateGreenTertiary
        } else {
            SemanticColor.backgroundWhitePrimary
        },
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "backgroundColor"
    )

    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Column(
            modifier = modifier
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true)
                )
                .background(backgroundColor)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                item.icon() // 👈 Icon이 LocalContentColor 자동 사용
            }

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}


/**
 * 커스텀 바텀 네비게이션 바
 * Scaffold의 bottomBar 파라미터에 직접 사용 가능
 */
@Composable
fun CustomBottomNavigation(
    items: List<BottomBarItem>,
    selectedRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()), // 시스템 네비게이션 바 위에 표시
    ) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = Color(0x1A000000))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    BottomBarItemView(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onItemClick(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
        }

        }
    }
}

/**
 * BottomBarItemView 선택 상태 미리보기
 */
@Preview(showBackground = true, name = "선택됨")
@Composable
private fun BottomBarItemViewSelectedPreview() {
    val item = BottomBarItem(
        route = "home",
        icon = {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "홈"
            )
        },
        label = "홈"
    )

    BottomBarItemView(
        item = item,
        isSelected = true,
        onClick = {}
    )
}

/**
 * BottomBarItemView 비선택 상태 미리보기
 */
@Preview(showBackground = true, name = "비선택")
@Composable
private fun BottomBarItemViewUnselectedPreview() {
    val item = BottomBarItem(
        route = "home",
        icon = {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "홈"
            )
        },
        label = "홈"
    )

    BottomBarItemView(
        item = item,
        isSelected = false,
        onClick = {}
    )
}
