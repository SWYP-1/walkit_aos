package team.swyp.sdu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import team.swyp.sdu.domain.service.LocationTrackingService
import team.swyp.sdu.navigation.NavGraph
import team.swyp.sdu.navigation.Screen
import team.swyp.sdu.presentation.viewmodel.UserViewModel
import team.swyp.sdu.ui.theme.WalkItTheme
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalkItTheme {
                val userViewModel: UserViewModel = hiltViewModel()
                val navController = rememberNavController()

                // LocationService 상태 구독 및 자동 네비게이션
                val isWorkoutActive by LocationTrackingService.isRunning.collectAsStateWithLifecycle()

                androidx.compose.runtime.LaunchedEffect(isWorkoutActive) {
                    Timber.d("🏃 LocationService 상태 변경: isWorkoutActive=$isWorkoutActive, currentRoute=${navController.currentBackStackEntry?.destination?.route}")
                    if (isWorkoutActive) {
                        // WalkingGraph가 이미 backstack에 있는지 확인
                        val isWalkingGraphInBackStack = try {
                            navController.getBackStackEntry(Screen.WalkingGraph.route)
                            true
                        } catch (t: Throwable) {
                            false
                        }

                        Timber.d("🏃 WalkingGraph가 backstack에 존재: $isWalkingGraphInBackStack")

                        if (!isWalkingGraphInBackStack) {
                            // WalkingGraph가 backstack에 없으면 이동
                            Timber.d("🏃 WalkingGraph로 자동 이동")
                            navController.navigate(Screen.WalkingGraph.route) {
                                popUpTo(Screen.Main.route) { saveState = true }
                                launchSingleTop = true
                            }
                        } else {
                            Timber.d("🏃 이미 WalkingGraph가 backstack에 있으므로 이동하지 않음")
                        }
                    } else {
                        // 현재 Walking 화면이면 홈으로 복귀
//                        val currentRoute = navController.currentBackStackEntry?.destination?.route
//                        if (currentRoute == Screen.Walking.route) {
//                            navController.popBackStack(Screen.Main.route, false)
//                        }
                    }
                }

                NavGraph(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
        }
    }
}