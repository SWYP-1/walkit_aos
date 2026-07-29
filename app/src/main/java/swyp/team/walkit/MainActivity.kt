package swyp.team.walkit

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import swyp.team.walkit.domain.service.LocationTrackingService
import swyp.team.walkit.navigation.NavGraph
import swyp.team.walkit.navigation.Screen
import swyp.team.walkit.data.local.datastore.WalkingDataStore
import swyp.team.walkit.presentation.viewmodel.UserViewModel
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.core.AuthEventBus
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var walkingDataStore: WalkingDataStore

    @Inject
    lateinit var authEventBus: AuthEventBus

    /**
     * 앱 시작 시 오래된 산책 데이터 정리 (강제종료 대응)
     * 산책 시작 후 2시간 이상 경과한 데이터는 자동 정리
     */
    private fun cleanupStaleWalkingData() {
        lifecycleScope.launch {
            try {
                // 1. DataStore 정리 (기존 로직)
                val isWalkingActive = walkingDataStore.getIsWalkingActive() ?: false

                if (isWalkingActive) {
                    val startTime = walkingDataStore.getWalkingStartTime() ?: 0L
                    val currentTime = System.currentTimeMillis()
                    val hoursSinceStart = (currentTime - startTime) / (1000 * 60 * 60)

                    if (hoursSinceStart >= 2) {
                        Timber.w("🏃 앱 시작 시 오래된 산책 DataStore 데이터 발견 (${hoursSinceStart}시간 경과), 자동 정리")
                        walkingDataStore.clearWalkingData()
                        Timber.d("🏃 오래된 산책 DataStore 데이터 정리 완료")
                    } else {
                        Timber.d("🏃 유효한 산책 DataStore 데이터 발견 (${hoursSinceStart}시간 경과), 유지")
                    }
                } else {
                    Timber.d("🏃 산책 DataStore 데이터 없음, 정리 불필요")
                }

            } catch (t: Throwable) {
                Timber.e(t, "🏃 오래된 산책 데이터 정리 실패")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 오래된 DataStore 데이터 정리 (강제종료 대응)
        cleanupStaleWalkingData()

        // Edge-to-Edge 비활성화하여 시스템 바 색상 제어 가능하도록 함
        // enableEdgeToEdge() // 제거하여 시스템 바 색상 제어 가능

        // 전역 edge-to-edge: 콘텐츠가 상태바/네비게이션바 뒤까지 그려짐
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        // 상태바/네비게이션바 아이콘 색상 설정 (흰 배경 기준 다크 아이콘)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }


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

                // 401 응답 시 로그인 화면으로 이동 처리
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    authEventBus.requireLogin.collect {
                        Timber.w("401 응답 감지 - 로그인 화면으로 이동")
                        // 토큰 삭제
                        lifecycleScope.launch {
                            userViewModel.logout()
                        }
                        // 로그인 화면으로 이동
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true } // 모든 백스택 제거
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    NavGraph(
                        navController = navController,
                        userViewModel = userViewModel
                    )
                }
            }
        }
    }

}