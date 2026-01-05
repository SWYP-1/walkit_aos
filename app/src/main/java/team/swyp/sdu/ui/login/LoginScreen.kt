package team.swyp.sdu.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import team.swyp.sdu.ui.components.ErrorDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import team.swyp.sdu.R
import team.swyp.sdu.ui.onboarding.OnboardingViewModel
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.login.components.LoginButton
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.kakaoYellow
import team.swyp.sdu.ui.theme.naverGreen
import team.swyp.sdu.ui.login.terms.TermsAgreementDialogContent
import team.swyp.sdu.ui.login.terms.TermsAgreementOverlayRoute
import team.swyp.sdu.ui.login.terms.TermsAgreementUiState
import team.swyp.sdu.ui.theme.Blue3
import team.swyp.sdu.ui.theme.Red5
import timber.log.Timber


/**
 * 로그인 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    onNavigateToTermsAgreement: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val onboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle(false)
    val termsAgreed by onboardingViewModel.isTermsAgreed.collectAsStateWithLifecycle(false)

    // 다이얼로그 표시 상태 관리
    var showTermsDialog by remember { mutableStateOf(false) }
    // 네비게이션 딜레이 상태
    var isNavigating by remember { mutableStateOf(false) }

    val naverLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleNaverLoginResult(result)
    }
    val scope = rememberCoroutineScope()

    // 네비게이션 콜백 설정
    LaunchedEffect(Unit) {
        viewModel.setNavigationCallbacks(
            onNavigateToMain = onNavigateToMain,
            onNavigateToTermsAgreement = onNavigateToTermsAgreement
        )
    }

    // 다이얼로그 표시 여부 결정
    LaunchedEffect(isLoggedIn, termsAgreed, isNavigating) {
        showTermsDialog = isLoggedIn && !termsAgreed && !isNavigating
    }

    /**
     * ✅ 단 하나의 Navigation 진입점
     * - isNavigating 상태를 체크하여 중복 네비게이션 방지
     */
    LaunchedEffect(isLoggedIn, termsAgreed, onboardingCompleted) {
        Timber.d("여기 오냐?")
        if (!isLoggedIn || isNavigating) return@LaunchedEffect

        if(termsAgreed){
            onNavigateToTermsAgreement()
        }
    }
    LaunchedEffect(isNavigating) {
        if (isNavigating == true) {
            onNavigateToMain()
        }
    }


    /**
     * 로그인 화면
     */
    LoginScreen(
        modifier = modifier,
        uiState = uiState,
        onKakaoLogin = { viewModel.loginWithKakaoTalk(context) },
        onNaverLogin = { viewModel.loginWithNaver(context, naverLoginLauncher) },
        onDismissError = {
            viewModel.clearError()
        },
    )

    /**
     * ✅ 약관 동의 전체 화면 오버레이
     * - showTermsDialog로 제어하여 깜박임 방지
     */
    TermsAgreementOverlayRoute(
        modifier = modifier,
        isVisible = showTermsDialog,
        onDismiss = {
            // 약관 거부 → 로그아웃
            isNavigating = true
            showTermsDialog = false
            scope.launch {
                delay(300)
                viewModel.logout()
                isNavigating = false
            }
        },
        onTermsAgreedUpdated = {
            // 즉시 오버레이 숨기고 네비게이션 시작
            showTermsDialog = false

            scope.launch {
                // 상태 업데이트 (LaunchedEffect가 네비게이션 처리)
                onboardingViewModel.updateServiceTermsChecked(true)
                onboardingViewModel.updatePrivacyPolicyChecked(true)
            }
        },
    )
}


/**
 * 로그인 화면
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onKakaoLogin: () -> Unit,
    onNaverLogin: () -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.bg_login),
            contentDescription = "bg login",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                uiState is LoginUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CustomProgressIndicator()
                    }
                }
                else -> {
                    Spacer(Modifier.weight(1f))

                    // 카카오 로그인
                    LoginButton(
                        backgroundColor = kakaoYellow,
                        modifier = Modifier.fillMaxWidth(),
                        provider = "카카오",
                        onClick = onKakaoLogin,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 네이버 로그인
                    LoginButton(
                        backgroundColor = naverGreen,
                        modifier = Modifier.fillMaxWidth(),
                        provider = "네이버",
                        onClick = onNaverLogin,
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // 에러 다이얼로그
        if (uiState is LoginUiState.Error) {
            ErrorDialog(
                title = "로그인 실패",
                message = (uiState as LoginUiState.Error).message,
                onDismiss = onDismissError,
            )
        }
    }
}

@Preview(showBackground = true, name = "기본 상태")
@Composable
private fun LoginScreenIdlePreview() {
    WalkItTheme {
        LoginScreen(
            uiState = LoginUiState.Idle,
            onKakaoLogin = {},
            onNaverLogin = {},
        )
    }
}

@Preview(showBackground = true, name = "로딩 중")
@Composable
private fun LoginScreenLoadingPreview() {
    WalkItTheme {
        LoginScreen(
            uiState = LoginUiState.Loading,
            onKakaoLogin = {},
            onNaverLogin = {},
        )
    }
}

@Preview(showBackground = true, name = "에러 상태")
@Composable
private fun LoginScreenErrorPreview() {
    WalkItTheme {
        LoginScreen(
            uiState = LoginUiState.Error("로그인 중 오류가 발생했습니다"),
            onKakaoLogin = {},
            onNaverLogin = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "약관 동의 다이얼로그")
@Composable
private fun LoginScreenWithTermsDialogPreview() {
    WalkItTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginScreen(
                uiState = LoginUiState.Idle,
                onKakaoLogin = {},
                onNaverLogin = {},
            )

            // 약관 동의 다이얼로그 내용 표시 (프리뷰용 - Dialog 없이 직접 표시)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                TermsAgreementDialogContent(
                    uiState = TermsAgreementUiState(
                        termsAgreed = false,
                        privacyAgreed = false,
                        locationAgreed = false,
                        marketingConsent = false,
                    ),
                    onTermsAgreedChange = {},
                    onPrivacyAgreedChange = {},
                    onLocationAgreedChange = {},
                    onMarketingConsentChange = {},
                    onAllAgreedChange = {},
                    onSubmit = {},
                    onDismiss = {},
                    onTermsClick = {},
                    onPrivacyClick = {},
                    onLocationClick = {},
                    onMarketingClick = {},
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}