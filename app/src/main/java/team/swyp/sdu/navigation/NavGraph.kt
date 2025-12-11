package team.swyp.sdu.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.ui.login.LoginScreen
import team.swyp.sdu.ui.screens.MainScreen
import team.swyp.sdu.ui.screens.RouteDetailScreen
import team.swyp.sdu.ui.screens.ShopScreen
import team.swyp.sdu.ui.walking.WalkingResultScreen
import team.swyp.sdu.ui.walking.WalkingScreen
import team.swyp.sdu.ui.calendar.CalendarScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.swyp.sdu.presentation.viewmodel.UserViewModel

sealed class Screen(
    val route: String,
) {
    data object Login : Screen("login")

    data object Main : Screen("main")

    data object Search : Screen("pokemon_search")

    data class Detail(
        val pokemonName: String = "{pokemonName}",
    ) : Screen("pokemon_detail/{pokemonName}") {
        fun createRoute(pokemonName: String) = "pokemon_detail/$pokemonName"
    }

    data object Walking : Screen("walking")

    data object WalkingResult : Screen("walking_result")

    data object RouteDetail : Screen("route_detail/{locationsJson}") {
        fun createRoute(locations: List<LocationPoint>): String {
            val json = Json.encodeToString(locations)
            return "route_detail/$json"
        }
    }

    data object Shop : Screen("shop")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    userViewModel: UserViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        // 로그인 화면을 백 스택에서 제거
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                navController = navController,
                userViewModel = userViewModel,
            )
        }

        composable(Screen.Shop.route) {
            ShopScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Walking.route) {
            WalkingScreen(
                onNavigateToRouteDetail = { locations ->
                    navController.navigate(Screen.RouteDetail.createRoute(locations))
                },
                onNavigateToResult = {
                    navController.navigate(Screen.WalkingResult.route)
                },
            )
        }

        composable(Screen.WalkingResult.route) {
            // WalkingResultScreen은 별도의 ViewModel 인스턴스를 사용하지 않고
            // MainScreen의 WalkingScreen과 같은 ViewModel을 공유해야 함
            // 하지만 현재 구조상 별도 인스턴스이므로, MainScreen으로 돌아가면
            // WalkingScreen의 LaunchedEffect에서 자동으로 초기화됨
            val viewModel: team.swyp.sdu.presentation.viewmodel.WalkingViewModel =
                androidx.hilt.navigation.compose
                    .hiltViewModel()

            WalkingResultScreen(
                onNavigateBack = {
                    // 결과 화면에서 뒤로가기: Main 화면으로 이동
                    // WalkingScreen의 LaunchedEffect에서 Completed 상태 감지 시 자동 초기화됨
                    navController.navigate(Screen.Main.route) {
                        // 백 스택에서 WalkingResult 제거
                        popUpTo(Screen.Main.route) { inclusive = false }
                    }
                },
                onNavigateToRouteDetail = { locations ->
                    navController.navigate(Screen.RouteDetail.createRoute(locations))
                },
                viewModel = viewModel,
            )
        }

        composable(
            route = Screen.RouteDetail.route,
            arguments =
                listOf(
                    navArgument("locationsJson") {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val locationsJson = backStackEntry.arguments?.getString("locationsJson") ?: "[]"
            val locations =
                try {
                    Json.decodeFromString<List<LocationPoint>>(locationsJson)
                } catch (e: Exception) {
                    emptyList()
                }

            RouteDetailScreen(
                locations = locations,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
