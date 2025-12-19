package team.swyp.sdu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.presentation.viewmodel.UserViewModel
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.login.LoginScreen
import team.swyp.sdu.ui.splash.SplashScreen
import team.swyp.sdu.ui.screens.MainScreen
import team.swyp.sdu.ui.screens.RouteDetailScreen
import team.swyp.sdu.ui.screens.ShopScreen
import team.swyp.sdu.ui.walking.*
import team.swyp.sdu.ui.friend.FriendScreen
import team.swyp.sdu.ui.friend.FriendSearchScreen
import team.swyp.sdu.ui.mission.MissionRoute
import team.swyp.sdu.ui.mypage.MyPageRoute
import team.swyp.sdu.ui.mypage.goal.GoalManagementRoute
import team.swyp.sdu.ui.mypage.settings.NotificationSettingsRoute
import team.swyp.sdu.ui.mypage.userInfo.UserInfoManagementScreen
import team.swyp.sdu.ui.onboarding.OnboardingScreen

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
    data object WalkingFinishStep : Screen("walking_finish_step")
    data object EmotionSelectionStep : Screen("emotion_selection_step")
    data object EmotionRecord : Screen("emotion_record")
    data object WalkingResult : Screen("walking_result")

    data object RouteDetail : Screen("route_detail/{locationsJson}") {
        fun createRoute(locations: List<LocationPoint>): String {
            val json = Json.encodeToString(locations)
            return "route_detail/$json"
        }
    }

    data object Shop : Screen("shop")
    data object Onboarding : Screen("onboarding")
    data object Friends : Screen("friends")
    data object FriendSearch : Screen("friend_search")
    data object GoalManagement : Screen("goal_management")
    data object Mission : Screen("mission")
    data object MyPage : Screen("mypage")
    data object UserInfoManagement : Screen("user_info_management")
    data object NotificationSettings : Screen("notification_settings")
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
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSkipToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        /* Main */
        composable(Screen.Main.route) {
            MainScreen(
                navController = navController,
            )
        }

        /* ----------------------- */
        /* Walking Nested Graph */
        /* ----------------------- */
        navigation(
            startDestination = Screen.Walking.route,
            route = Screen.WalkingGraph.route,
        ) {

            composable(Screen.Walking.route) { entry ->
                val viewModel =
                    entry.sharedViewModel<WalkingViewModel>(navController)

                WalkingScreen(
                    viewModel = viewModel,
                    onNavigateToFinish = {
                        navController.navigate(Screen.WalkingFinishStep.route)
                    },
                )
            }

            composable(Screen.WalkingFinishStep.route) { entry ->
                val viewModel =
                    entry.sharedViewModel<WalkingViewModel>(navController)

                WalkingFinishStep(
                    onClose = {
                        navController.popBackStack(Screen.Main.route, false)
                    },
                    onSkip = {
                        navController.navigate(Screen.WalkingResult.route)
                    },
                    onRecordEmotion = {
                        navController.navigate(Screen.EmotionSelectionStep.route)
                    },
                )
            }

            composable(Screen.EmotionSelectionStep.route) { entry ->
                val viewModel =
                    entry.sharedViewModel<WalkingViewModel>(navController)

                PostWalkingEmotionSelectScreen(
                    viewModel = viewModel,
                    onNext = {
                        navController.navigate(Screen.EmotionRecord.route)
                    },
                )
            }

            composable(Screen.EmotionRecord.route) { entry ->
                val viewModel =
                    entry.sharedViewModel<WalkingViewModel>(navController)

                EmotionRecordStep(
                    viewModel = viewModel,
                    onNext = {
                        navController.navigate(Screen.WalkingResult.route)
                    },
                    onClose = {
                        navController.popBackStack(Screen.Main.route, false)
                    },
                )
            }

            composable(Screen.WalkingResult.route) { entry ->
                val viewModel =
                    entry.sharedViewModel<WalkingViewModel>(navController)

                WalkingResultScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack(Screen.Main.route, false)
                    },
                    onNavigateToRouteDetail = { locations ->
                        navController.navigate(
                            Screen.RouteDetail.createRoute(locations)
                        )
                    },
                )
            }
        }

        /* Route Detail */
        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(
                navArgument("locationsJson") {
                    type = NavType.StringType
                },
            ),
        ) { entry ->
            val locationsJson =
                entry.arguments?.getString("locationsJson") ?: "[]"

            val locations =
                runCatching {
                    Json.decodeFromString<List<LocationPoint>>(locationsJson)
                }.getOrDefault(emptyList())

            RouteDetailScreen(
                locations = locations,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        /* Shop */
        composable(Screen.Shop.route) {
            ShopScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        /* Friends */
        composable(Screen.Friends.route) {
            FriendScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSearch = {
                    navController.navigate(Screen.FriendSearch.route)
                },
            )
        }

        composable(Screen.FriendSearch.route) {
            FriendSearchScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        /* Onboarding */
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        /* MyPage */
        composable(Screen.MyPage.route) {
            MyPageRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateCharacterEdit = {
                    navController.navigate(Screen.UserInfoManagement.route)
                },
                onNavigateUserInfoEdit = {
                    navController.navigate(Screen.UserInfoManagement.route)
                },
                onNavigateGoalManagement = {
                    navController.navigate(Screen.GoalManagement.route)
                },
                onNavigateNotificationSetting = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
            )
        }

        composable(Screen.UserInfoManagement.route) {
            UserInfoManagementScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.GoalManagement.route) {
            GoalManagementRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Mission.route) {
            MissionRoute(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

/* ----------------------- */
/* sharedViewModel extension */
/* ----------------------- */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
    navController: NavHostController,
): T {
    val navGraphRoute = destination.parent?.route ?: return hiltViewModel()
    val parentEntry = remember(this) {
        navController.getBackStackEntry(navGraphRoute)
    }
    return hiltViewModel(parentEntry)
}

