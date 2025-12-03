package team.swyp.sdu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import team.swyp.sdu.navigation.Screen

/**
 * 메인 화면 - 상단 탭으로 기록 측정과 기록 리스트를 구분
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("기록 측정") },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("달린 기록") },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // 기록 측정 탭
                    // 탭이 다시 선택될 때마다 WalkingScreen이 재구성됨
                    // WalkingScreen 내부의 LaunchedEffect에서 Completed 상태 감지 시 자동 초기화됨
                    WalkingScreen(
                        onNavigateToRouteDetail = { locations ->
                            navController.navigate(Screen.RouteDetail.createRoute(locations))
                        },
                        onNavigateToResult = {
                            navController.navigate(Screen.WalkingResult.route)
                        },
                    )
                }

                1 -> {
                    // 달린 기록 리스트 탭
                    WalkingSessionListScreen(
                        onNavigateToRouteDetail = { locations ->
                            navController.navigate(Screen.RouteDetail.createRoute(locations))
                        },
                    )
                }
            }
        }
    }
}
