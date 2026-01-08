package swyp.team.walkit.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.presentation.viewmodel.UserViewModel
import swyp.team.walkit.ui.walking.viewmodel.WalkingViewModel
import swyp.team.walkit.ui.login.LoginRoute
import swyp.team.walkit.ui.splash.SplashScreen
import swyp.team.walkit.ui.screens.MainScreen
import swyp.team.walkit.ui.walking.*
import swyp.team.walkit.ui.friend.FriendRoute
import swyp.team.walkit.ui.friend.FriendSearchScreen
import swyp.team.walkit.ui.mission.MissionRoute
import swyp.team.walkit.ui.mypage.goal.GoalManagementRoute
import swyp.team.walkit.ui.mypage.settings.NotificationSettingsRoute
import swyp.team.walkit.ui.onboarding.OnboardingScreen
import swyp.team.walkit.ui.alarm.AlarmScreen
import swyp.team.walkit.ui.customtest.CustomTestRoute
import swyp.team.walkit.ui.customtest.MapTestScreen
import swyp.team.walkit.ui.friend.FriendSearchDetailRoute
import swyp.team.walkit.ui.mypage.userInfo.UserInfoManagementRoute
import swyp.team.walkit.ui.record.dailyrecord.DailyRecordRoute
import timber.log.Timber

/* ----------------------- */
/* Screen 정의 */
/* ----------------------- */
sealed class Screen(val route: String) {

    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Main : Screen("main")

    // 🔥 Walking Graph
    data object WalkingGraph : Screen("walking_graph")
    data object Walking : Screen("walking")
    data object PostEmotionSelectionStep : Screen("emotion_selection_step")
    data object EmotionRecord : Screen("emotion_record")
    data object WalkingResult : Screen("walking_result")

    data object RouteDetail : Screen("route_detail/{locationsJson}") {
        fun createRoute(locations: List<LocationPoint>): String {
            val json = Json.encodeToString(locations)
            return "route_detail/$json"
        }
    }

    data object Onboarding : Screen("onboarding")
    data object Friends : Screen("friends")
    data object FriendSearch : Screen("friend_search")
    data object FriendSearchDetail : Screen("friend_search_detail/{nickname}/{followStatus}") {
        fun createRoute(nickname: String, followStatus: String) = "friend_search_detail/$nickname/$followStatus"
    }
    data object GoalManagement : Screen("goal_management")
    data object Mission : Screen("mission")
    data object DressingRoom : Screen("dressing_room")
    data object CharacterShop : Screen("character_shop")
    data object UserInfoManagement : Screen("user_info_management")
    data object NotificationSettings : Screen("notification_settings")
    data object Alarm : Screen("alarm")
    data object CustomTest : Screen("custom_test")
    data object MapTest : Screen("map_test")

    data object DailyRecord : Screen("daily_record/{dateString}") {
        fun createRoute(dateString: String): String {
            return "daily_record/$dateString"
        }
    }

}

/* ----------------------- */
/* NavGraph */
/* ----------------------- */
@Composable
fun NavGraph(
    navController: NavHostController,
    userViewModel: UserViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {

        /* Splash */
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }

        /* Login */
        composable(Screen.Login.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                LoginRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateToTermsAgreement = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToMain = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                )
            }
        }

        /* Main */
        composable(Screen.Main.route) {
            MainScreen(navController = navController) // Scaffold는 MainScreen 안에서 처리
        }

        /* Walking Graph */
        navigation(
            startDestination = Screen.Walking.route,
            route = Screen.WalkingGraph.route,
        ) {

            composable(Screen.Walking.route) { entry ->
                val viewModel = entry.sharedViewModel<WalkingViewModel>(navController)
                WalkingScreenRoute(
                    modifier = Modifier,
                    viewModel = viewModel,
                    onNavigateToPostWalkingEmotion = {
                        navController.navigate(Screen.PostEmotionSelectionStep.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable(Screen.PostEmotionSelectionStep.route) { entry ->
                val viewModel = entry.sharedViewModel<WalkingViewModel>(navController)
                Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                    PostWalkingEmotionSelectRoute(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        onNext = { navController.navigate(Screen.EmotionRecord.route) },
                        onClose = {
                            Timber.d("🚶 NavGraph - PostEmotionSelection onClose 호출")
                            val success = navController.popBackStack(Screen.Main.route, false)
                            Timber.d("🚶 NavGraph - popBackStack 결과: $success")
                            if (!success) {
                                Timber.w("🚶 NavGraph - popBackStack 실패, navigate 사용")
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(Screen.Main.route) { inclusive = true }
                                }
                            }
                        },
                    )
                }
            }

            composable(Screen.EmotionRecord.route) { entry ->
                val viewModel = entry.sharedViewModel<WalkingViewModel>(navController)
                Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                    EmotionRecordStepRoute(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        onNext = { navController.navigate(Screen.WalkingResult.route) },
                        onPrev = { navController.popBackStack() },
                    )
                }
            }

            composable(Screen.WalkingResult.route) { entry ->
                val viewModel = entry.sharedViewModel<WalkingViewModel>(navController)
                Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                    WalkingResultRoute(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        onNavigateToPrevious = { navController.popBackStack() },
                        onNavigateToHome = {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.WalkingGraph.route) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }

        /* Friends */
        composable(Screen.Friends.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                FriendRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = {
                        if (!navController.popBackStack(Screen.Main.route, false)) {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Main.route) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    onNavigateToSearch = { navController.navigate(Screen.FriendSearch.route) },
                )
            }
        }



        /* Friend Search */
        composable(Screen.FriendSearch.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                FriendSearchScreen(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateToDetail = { nickname, followStatus ->
                        navController.navigate(Screen.FriendSearchDetail.createRoute(nickname, followStatus.name))
                    },
                    onNavigateBack = {
                        if (!navController.popBackStack(Screen.Friends.route, false)) {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Main.route) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                )
            }
        }
        /* Friend Search Detail*/
        composable(
            route = Screen.FriendSearchDetail.route,
            arguments = listOf(
                navArgument("nickname") { type = NavType.StringType },
                navArgument("followStatus") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val nickname = backStackEntry.arguments?.getString("nickname")
            val followStatusString = backStackEntry.arguments?.getString("followStatus")
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                FriendSearchDetailRoute(
                    nickname = nickname,
                    followStatusString = followStatusString,
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }

        /* Onboarding */
        composable(Screen.Onboarding.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                OnboardingScreen(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                    onFinish = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }

        /* Custom Test */
        composable(Screen.CustomTest.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                CustomTestRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                    onStartOnboarding = { navController.navigate(Screen.Onboarding.route) },
                    onNavigateToMapTest = {
                        navController.navigate(Screen.MapTest.route)
                    },
                    onNavigateToGalleryTest = {
                        // TODO: 갤러리 사진 + 경로 테스트 화면으로 이동
                        Timber.d("갤러리 사진 + 경로 테스트 이동")
                    },
                )
            }
        }

        /* Map Test */
        composable(Screen.MapTest.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                MapTestScreen(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        /* MyPage */
//        composable(Screen.MyPage.route) {
//            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
//                MyPageRoute(
//                    modifier = Modifier.padding(paddingValues),
//                    onNavigateBack = { navController.popBackStack() },
//                    onNavigateCharacterEdit = { navController.navigate(Screen.DressingRoom.route) },
//                    onNavigateUserInfoEdit = { navController.navigate(Screen.UserInfoManagement.route) },
//                    onNavigateGoalManagement = { navController.navigate(Screen.GoalManagement.route) },
//                    onNavigateNotificationSetting = { navController.navigate(Screen.NotificationSettings.route) },
//                    onNavigateToLogin = {
//                        navController.navigate(Screen.Login.route) {
//                            popUpTo(Screen.Main.route) { inclusive = true }
//                        }
//                    },
//                )
//            }
//        }

        /* DressingRoom */
//        composable(Screen.DressingRoom.route) {
//            DressingRoomRoute(
//                onNavigateBack = { navController.popBackStack() }
//            )
//        }

        /* User Info Management */
        composable(Screen.UserInfoManagement.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                UserInfoManagementRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        /* Notification Settings */
        composable(Screen.NotificationSettings.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                NotificationSettingsRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        /* Goal Management */
        composable(Screen.GoalManagement.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                GoalManagementRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        /* Mission */
        composable(Screen.Mission.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                MissionRoute(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateToWalk = {
                        navController.navigate(Screen.WalkingGraph.route)
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        /* Alarm */
        composable(Screen.Alarm.route) {
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                AlarmScreen(
                    modifier = Modifier.padding(paddingValues),
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        /* Daily Record */
        composable(
            route = Screen.DailyRecord.route,
            arguments = listOf(navArgument("dateString") { type = NavType.StringType }),
        ) { entry ->
            val dateString = entry.arguments?.getString("dateString") ?: ""
            Scaffold(contentWindowInsets = WindowInsets.systemBars) { paddingValues ->
                DailyRecordRoute(
                    modifier = Modifier.padding(paddingValues),
                    dateString = dateString,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

    }
}

/* sharedViewModel */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    return when (T::class) {
        WalkingViewModel::class -> {
            // WalkingGraph의 backstack entry를 찾아서 ViewModel 공유
            val walkingGraphRoute = Screen.WalkingGraph.route
            val walkingGraphEntry = remember(this) {
                navController.getBackStackEntry(walkingGraphRoute)
            }
            Timber.d("WalkingViewModel: sharedViewModel - WalkingGraph entry 사용 (route=$walkingGraphRoute, hash=${walkingGraphEntry.hashCode()})")
            hiltViewModel(walkingGraphEntry)
        }
        else -> {
            val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
            val parentEntry = remember(this) { navController.getBackStackEntry(navGraphRoute) }
            Timber.d("${T::class.simpleName}: sharedViewModel - parent entry 사용 (route=$navGraphRoute, hash=${parentEntry.hashCode()})")
            hiltViewModel(parentEntry)
        }
    }
}


