package swyp.team.walkit

import android.content.Context
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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import swyp.team.walkit.presentation.viewmodel.UserViewModel
import swyp.team.walkit.ui.theme.WalkItTheme
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // WalkingViewModel과 동일한 DataStore 키들
    private object PreferencesKeys {
        val IS_WALKING_ACTIVE = androidx.datastore.preferences.core.booleanPreferencesKey("is_walking_active")
        val WALKING_START_TIME = longPreferencesKey("walking_start_time")
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "walking_prefs")

    /**
     * 앱 시작 시 오래된 산책 데이터 정리 (강제종료 대응)
     * 산책 시작 후 2시간 이상 경과한 데이터는 자동 정리
     */
    private fun cleanupStaleWalkingData() {
        lifecycleScope.launch {
            try {
                val preferences = dataStore.data.firstOrNull()
                val isWalkingActive = preferences?.get(PreferencesKeys.IS_WALKING_ACTIVE) ?: false

                if (isWalkingActive) {
                    val startTime = preferences.get(PreferencesKeys.WALKING_START_TIME) ?: 0L
                    val currentTime = System.currentTimeMillis()
                    val hoursSinceStart = (currentTime - startTime) / (1000 * 60 * 60)

                    if (hoursSinceStart >= 2) {
                        Timber.w("🏃 앱 시작 시 오래된 산책 데이터 발견 (${hoursSinceStart}시간 경과), 자동 정리")
                        dataStore.edit { prefs ->
                            prefs.remove(PreferencesKeys.IS_WALKING_ACTIVE)
                            prefs.remove(PreferencesKeys.WALKING_START_TIME)
                            // 다른 walking 관련 키들도 정리
                            prefs.remove(longPreferencesKey("walking_step_count"))
                            prefs.remove(longPreferencesKey("walking_duration"))
                            prefs.remove(androidx.datastore.preferences.core.booleanPreferencesKey("walking_is_paused"))
                            prefs.remove(androidx.datastore.preferences.core.stringPreferencesKey("pre_walking_emotion"))
                            prefs.remove(androidx.datastore.preferences.core.stringPreferencesKey("post_walking_emotion"))
                        }
                        Timber.d("🏃 오래된 산책 데이터 정리 완료")
                    } else {
                        Timber.d("🏃 유효한 산책 데이터 발견 (${hoursSinceStart}시간 경과), 유지")
                    }
                } else {
                    Timber.d("🏃 산책 데이터 없음, 정리 불필요")
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