package team.swyp.sdu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import team.swyp.sdu.navigation.Screen
import team.swyp.sdu.presentation.viewmodel.UserViewModel
import team.swyp.sdu.core.Result
import team.swyp.sdu.ui.home.HomeScreen
import team.swyp.sdu.ui.record.RecordScreen
import team.swyp.sdu.ui.walking.WalkingScreen
import team.swyp.sdu.ui.settings.SettingsScreen
import timber.log.Timber

/**
 * 메인 탭 화면: 각 피처 화면 호출만 담당
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val userState by userViewModel.userState.collectAsStateWithLifecycle()
    val userLabel =
        when (val state = userState) {
            is Result.Success -> state.data.nickname.ifBlank { "" }
            is Result.Error -> "불러오기 실패"
            Result.Loading -> "불러오는 중"
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.weight(1f),
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("홈") },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("기록 측정") },
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("산책 기록") },
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = { Text("설정") },
                    )
                }

                if (userLabel.isNotBlank()) {
                    Text(
                        text = userLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }

                IconButton(
                    onClick = { navController.navigate(Screen.Shop.route) },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "상점",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(
                    onClick = { navController.navigate(Screen.Friends.route) },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "친구 목록",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    onClickWalk = { selectedTabIndex = 1 },
                    onClickGoal = { /* TODO: 목표 설정 네비게이션 */ },
                )

                1 -> WalkingScreen(
                    onNavigateToRouteDetail = { locations ->
                        navController.navigate(Screen.RouteDetail.createRoute(locations))
                    },
                    onNavigateToResult = {
                        navController.navigate(Screen.WalkingResult.route)
                    },
                )

                2 -> RecordScreen(
                    onStartOnboarding = {
                        navController.navigate(Screen.Onboarding.route)
                    },
                )

                3 -> SettingsScreen(
                    navController = navController,
                )
            }
        }
    }
}

