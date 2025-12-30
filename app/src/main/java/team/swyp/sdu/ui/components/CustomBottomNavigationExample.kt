package team.swyp.sdu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Custom Bottom Navigation 사용 예시
 */
@Preview(showBackground = true)
@Composable
fun CustomBottomNavigationExample() {
    // 현재 선택된 route 상태
    var selectedRoute by remember { mutableStateOf("home") }

    // 네비게이션 아이템 정의
    val bottomNavItems = listOf(
        BottomBarItem(
            route = "home",
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "홈"
                )
            },
            label = "홈"
        ),
        BottomBarItem(
            route = "profile",
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "프로필"
                )
            },
            label = "프로필"
        ),
        BottomBarItem(
            route = "settings",
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정"
                )
            },
            label = "설정"
        )
    )

    // Scaffold에 적용
    Scaffold(
        bottomBar = {
            CustomBottomNavigation(
                items = bottomNavItems,
                selectedRoute = selectedRoute,
                onItemClick = { route ->
                    selectedRoute = route
                    // 여기서 navigation 로직 추가
                    // navController.navigate(route)
                }
            )
        }
    ) { paddingValues ->
        // 메인 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "현재 선택: $selectedRoute")
        }
    }
}
