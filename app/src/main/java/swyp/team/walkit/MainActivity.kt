package swyp.team.walkit

import android.content.Context
import android.os.Build
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
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var walkingDataStore: WalkingDataStore

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

                // 2. DB의 오래된 미완료 세션 정리 추가
                // TODO: walkingSessionRepository에 getAllSessions() 메소드 추가 후 구현
                // try {
                //     cleanupStaleSessionsFromDb()
                // } catch (t: Throwable) {
                //     Timber.e(t, "🏃 DB 세션 정리 실패")
                // }

            } catch (t: Throwable) {
                Timber.e(t, "🏃 오래된 산책 데이터 정리 실패")
            }
        }
    }

    /**
     * DB에서 오래된 미완료 세션을 정리
     * - 2시간 이상 지난 세션은 삭제
     * - 최근 24시간 내의 세션만 유지
     *
     * TODO: walkingSessionRepository에 getAllSessions() 메소드 추가 후 구현
     */
    // private suspend fun cleanupStaleSessionsFromDb() {
    //     try {
    //         val currentTime = System.currentTimeMillis()
    //         val twentyFourHoursAgo = currentTime - (24 * 60 * 60 * 1000) // 24시간 전
    //
    //         // 최근 24시간 내의 모든 세션을 조회 (더미 세션 포함)
    //         val recentSessions = walkingSessionRepository.getAllSessions()
    //
    //         var cleanedCount = 0
    //         for (session in recentSessions) {
    //             // endTime이 없거나(startTime과 같거나) 2시간 이상 지난 세션 삭제
    //             val sessionEndTime = session.endTime.takeIf { it > session.startTime } ?: session.startTime
    //             val hoursSinceEnd = (currentTime - sessionEndTime) / (1000 * 60 * 60)
    //
    //             if (hoursSinceEnd >= 2) {
    //                 try {
    //                     walkingSessionRepository.deleteSession(session.id)
    //                     cleanedCount++
    //                     Timber.d("🏃 오래된 DB 세션 삭제: ${session.id}, ${hoursSinceEnd}시간 경과")
    //                 } catch (e: Throwable) {
    //                     Timber.w(e, "🏃 세션 삭제 실패: ${session.id}")
    //                 }
    //             }
    //         }
    //
    //         if (cleanedCount > 0) {
    //             Timber.d("🏃 DB에서 ${cleanedCount}개의 오래된 세션 정리 완료")
    //         }
    //
    //     } catch (t: Throwable) {
    //         Timber.e(t, "🏃 DB 세션 정리 중 오류 발생")
    //     }
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 오래된 DataStore 데이터 정리 (강제종료 대응)
        cleanupStaleWalkingData()

        // Edge-to-Edge 비활성화하여 시스템 바 색상 제어 가능하도록 함
        // enableEdgeToEdge() // 제거하여 시스템 바 색상 제어 가능

        WindowCompat.setDecorFitsSystemWindows(window, true)

        // 시스템 바 색상 설정
        window.statusBarColor = getColor(R.color.white)
        window.navigationBarColor = getColor(R.color.white)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

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

                NavGraph(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
        }
    }

}