package team.swyp.sdu.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import team.swyp.sdu.navigation.Screen
import team.swyp.sdu.ui.home.HomeScreen
import team.swyp.sdu.ui.mypage.MyPageRoute
import team.swyp.sdu.ui.record.RecordRoute

/**
 * 메인 탭 화면: 각 피처 화면 호출만 담당
 */
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route



    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // navigation 상태에 따라 탭 결정
    val calculatedTabIndex = when (currentRoute) {
        Screen.GoalManagement.route,
        Screen.UserInfoManagement.route,
        Screen.NotificationSettings.route -> 2 // 마이페이지 탭
        else -> selectedTabIndex
    }

    // 계산된 탭 인덱스 사용 (navigation에서 돌아올 때)
    val currentTabIndex = if (calculatedTabIndex != selectedTabIndex && calculatedTabIndex in 0..2) {
        calculatedTabIndex
    } else {
        selectedTabIndex
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "홈",
                        )
                    },
                    label = { Text("홈") },
                )
                NavigationBarItem(
                    selected = currentTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "산책 기록",
                        )
                    },
                    label = { Text("산책 기록") },
                )
                NavigationBarItem(
                    selected = currentTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "마이 페이지",
                        )
                    },
                    label = { Text("마이 페이지") },
                )
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
            when (currentTabIndex) {
                0 -> HomeScreen(
                    onClickWalk = {
                        // WalkingScreen으로 네비게이션
                        navController.navigate(Screen.Walking.route)
                    },
                    onClickGoal = { /* TODO: 목표 설정 네비게이션 */ },
                    onClickMission = {
                        navController.navigate(Screen.Mission.route)
                    },
                )

                1 -> RecordRoute(
                    onStartOnboarding = {
                        navController.navigate(Screen.Onboarding.route)
                    },
                    onNavigateToAlarm = {
                        
                    },
                    onNavigateToFriend = {
                        navController.navigate(Screen.Friends.route)
                    }
                )

                2 -> {
                    MyPageRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateUserInfoEdit = {
                            navController.navigate(Screen.UserInfoManagement.route)
                        },
                        onNavigateGoalManagement = {
                            navController.navigate(Screen.GoalManagement.route)
                        },
                        onNavigateCharacterEdit = {},
                        onNavigateNotificationSetting = {
                            navController.navigate(Screen.NotificationSettings.route)
                        }
                    )
                }
            }
        }
    }
}

