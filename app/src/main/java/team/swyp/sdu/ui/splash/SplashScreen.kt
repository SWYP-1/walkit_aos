package team.swyp.sdu.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import team.swyp.sdu.R
import team.swyp.sdu.navigation.Screen
import team.swyp.sdu.ui.login.LoginViewModel
import team.swyp.sdu.ui.theme.SemanticColor
import timber.log.Timber

/**
 * 스플래시: 로그인/온보딩 완료 여부에 따라 최초 진입 화면 결정
 */
@Composable
fun SplashScreen(
    navController: NavHostController,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isLoginChecked by loginViewModel.isLoginChecked.collectAsStateWithLifecycle()
    var navigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLoginChecked, isLoggedIn) {
        Timber.d("스플래시 LaunchedEffect 실행 - loginChecked:$isLoginChecked, loggedIn:$isLoggedIn, navigated:$navigated")

        if (!isLoginChecked || navigated) {
            Timber.d("스플래시 네비게이션 스킵 - loginChecked:$isLoginChecked, navigated:$navigated")
            return@LaunchedEffect
        }

        val targetRoute = if (isLoggedIn) {
            Screen.Main.route
        } else {
            Screen.Login.route  // 로그인 실패 시 로그인 화면으로 이동
        }

        Timber.i(
            "스플래시 분기 결정 - loginChecked:%s, loggedIn:%s, target:%s",
            isLoginChecked,
            isLoggedIn,
            targetRoute,
        )

        navigated = true
        Timber.i("스플래시 네비게이션 실행: $targetRoute")

        navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }

        Timber.i("스플래시 네비게이션 완료")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary),
        contentAlignment = Alignment.Center,
    ) {
//        LottieAnimationView(
//            modifier = Modifier.size(260.dp),
//        )
        Image(
            painter = painterResource(R.drawable.logo_splash),
            contentDescription = "splash",
        )
    }
}


