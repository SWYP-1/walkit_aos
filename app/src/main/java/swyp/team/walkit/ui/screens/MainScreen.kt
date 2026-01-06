package swyp.team.walkit.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import swyp.team.walkit.ui.components.BottomBarItem
import swyp.team.walkit.ui.components.CustomBottomNavigation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.navigation.Screen
import swyp.team.walkit.ui.home.HomeRoute
import swyp.team.walkit.ui.home.HomeScreen
import swyp.team.walkit.ui.home.LocationAgreementUiState
import swyp.team.walkit.ui.home.LocationAgreementViewModel
import swyp.team.walkit.ui.home.components.WalkingFloatingActionButton
import swyp.team.walkit.ui.home.components.LocationAgreementDialog
import swyp.team.walkit.ui.mypage.MyPageRoute
import swyp.team.walkit.ui.record.RecordRoute

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 위치 동의 ViewModel
    val locationViewModel: LocationAgreementViewModel = hiltViewModel()
    val locationUiState by locationViewModel.uiState.collectAsStateWithLifecycle()

    // 위치 권한 요청 Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationViewModel.handlePermissionResult(isGranted)
        if (isGranted) {
            // 권한 승인 시 Walking 화면으로 이동
            navController.navigate(Screen.WalkingGraph.route)
        } else {
            // 권한 거부 시 사용자에게 안내 및 재요청 옵션 제공
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "산책 기능을 사용하려면 위치 권한이 필요합니다",
                    actionLabel = "설정에서 허용"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // 설정 앱으로 이동하여 권한 설정
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // navigation 상태에 따라 탭 결정
    val calculatedTabIndex = when (currentRoute) {
        Screen.GoalManagement.route,
        Screen.UserInfoManagement.route,
        Screen.NotificationSettings.route -> 2 // 마이페이지 탭
        else -> selectedTabIndex
    }

    // 계산된 탭 인덱스 사용 (navigation에서 돌아올 때)
    val currentTabIndex =
        if (calculatedTabIndex != selectedTabIndex && calculatedTabIndex in 0..2) {
            calculatedTabIndex
        } else {
            selectedTabIndex
        }

    // Route 기반 selected route 결정
    val selectedRoute = when (currentTabIndex) {
        0 -> "home"
        1 -> "record"
        2 -> "mypage"
        else -> "home"
    }

    // Bottom Navigation 아이템 정의
    val bottomNavItems = listOf(
        BottomBarItem(
            route = "record",
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_record),
                    contentDescription = "산책 기록"
                )
            },
            label = "산책 기록"
        ),
        BottomBarItem(
            route = "home",
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_home),
                    contentDescription = "홈"
                )
            },
            label = "홈"
        ),

        BottomBarItem(
            route = "mypage",
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_mypage),
                    contentDescription = "마이 페이지"
                )
            },
            label = "마이 페이지"
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars, // Status bar만 고려, 시스템 네비게이션 바는 제외
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentTabIndex == 0) {
                // 홈 화면에서만 FloatingActionButton 표시
                WalkingFloatingActionButton(
                    onClick = {
                        // 위치 권한 확인 후 LocationService 시작 및 WalkingScreen 이동
                        if (locationViewModel.hasLocationPermission()) {
                            val intent = android.content.Intent(context, swyp.team.walkit.domain.service.LocationTrackingService::class.java).apply {
                                action = swyp.team.walkit.domain.service.LocationTrackingService.ACTION_START_TRACKING
                            }
                            context.startService(intent)
                            // 버그로 인해 서비스가 이미 실행중이더라도 강제로 WalkingScreen으로 이동
                            navController.navigate(Screen.WalkingGraph.route) {
                                popUpTo(Screen.Main.route) { saveState = true }
                                launchSingleTop = true
                            }
                        } else {
                            locationViewModel.checkShouldShowDialog()
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp), // Custom Bottom Navigation 위에 표시 (시스템 네비게이션 바는 이미 고려됨)
                )
            }
        },
        bottomBar = {
            CustomBottomNavigation(
                items = bottomNavItems,
                selectedRoute = selectedRoute,
                onItemClick = { route ->
                    val newIndex = when (route) {
                        "home" -> 0
                        "record" -> 1
                        "mypage" -> 2
                        else -> 0
                    }
                    selectedTabIndex = newIndex
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
        ) {
            when (currentTabIndex) {
                0 -> HomeRoute(
                    onClickWalk = { navController.navigate(Screen.WalkingGraph.route) },
                    onClickAlarm = {
                        navController.navigate(Screen.Alarm.route)
                    },
                    onClickMissionMore = {
                        navController.navigate(Screen.Mission.route)
                    },
                    onNavigateToRecord = {
                        selectedTabIndex = 1
                    }
                )

                1 -> RecordRoute(
                    onStartOnboarding = {
                        navController.navigate(Screen.Onboarding.route)
                    },
                    onNavigateToAlarm = {
                        navController.navigate(Screen.Alarm.route)
                    },
                    onNavigateToFriend = {
                        navController.navigate(Screen.Friends.route)
                    },
                    onNavigateToDailyRecord = { dateString ->
                        navController.navigate(Screen.DailyRecord.createRoute(dateString))
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
                        onNavigateCharacterEdit = {
                            navController.navigate(Screen.DressingRoom.route)
                        },
                        onNavigateNotificationSetting = {
                            navController.navigate(Screen.NotificationSettings.route)
                        },
                        onNavigateToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        },
                        onNavigateMission = {
                            navController.navigate(Screen.Mission.route)
                        },
                        onNavigateCustomTest = {
                            navController.navigate(Screen.CustomTest.route)
                        }
                    )
                }
            }
        }
    }

    // 위치 동의 다이얼로그
    if (locationUiState == LocationAgreementUiState.ShouldShowDialog) {
        LocationAgreementDialog(
            onDismiss = {
                locationViewModel.dismissDialog()
            },
            onGrantPermission = {
                locationViewModel.grantPermission()
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDenyPermission = {
                locationViewModel.denyPermission()
            }
        )
    }
}

