package team.swyp.sdu.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import team.swyp.sdu.navigation.Screen
import team.swyp.sdu.presentation.viewmodel.LoginViewModel
import team.swyp.sdu.presentation.viewmodel.OnboardingViewModel
import team.swyp.sdu.ui.components.LottieAnimationView
import timber.log.Timber

/**
 * 스플래시: 로그인/온보딩 완료 여부에 따라 최초 진입 화면 결정
 */
@Composable
fun SplashScreen(
    navController: NavHostController,
    loginViewModel: LoginViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isLoginChecked by loginViewModel.isLoginChecked.collectAsStateWithLifecycle()
    val onboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle(initialValue = false)
    var navigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLoginChecked, isLoggedIn, onboardingCompleted) {
        if (!isLoginChecked || navigated) return@LaunchedEffect

        val targetRoute =
            when {
                !isLoggedIn -> Screen.Login.route
                !onboardingCompleted -> Screen.Onboarding.route
                else -> Screen.Main.route
            }

        Timber.i(
            "스플래시 분기 - loginChecked:%s, loggedIn:%s, onboardingCompleted:%s, target:%s",
            isLoginChecked,
            isLoggedIn,
            onboardingCompleted,
            targetRoute,
        )

        navigated = true
        navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimationView(
            modifier = Modifier.size(260.dp),
        )
    }
}

