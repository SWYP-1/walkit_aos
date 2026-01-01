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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import team.swyp.sdu.presentation.viewmodel.LoginUiState
import team.swyp.sdu.presentation.viewmodel.LoginViewModel
import team.swyp.sdu.ui.onboarding.OnboardingViewModel
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.login.components.LoginButton
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.kakaoYellow
import team.swyp.sdu.ui.theme.naverGreen
import team.swyp.sdu.ui.login.terms.TermsAgreementDialogContent
import team.swyp.sdu.ui.login.terms.TermsAgreementDialogRoute
import team.swyp.sdu.ui.login.terms.TermsAgreementUiState
import team.swyp.sdu.ui.theme.Blue3
import team.swyp.sdu.ui.theme.Red5


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

    val naverLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleNaverLoginResult(result)
    }
    val scope = rememberCoroutineScope()


    /**
     * ✅ 단 하나의 Navigation 진입점
     */
    LaunchedEffect(isLoggedIn, termsAgreed, onboardingCompleted) {
        if (!isLoggedIn) return@LaunchedEffect

        when {
            onboardingCompleted -> {
                awaitFrame()
                onNavigateToMain()
            }

            termsAgreed -> {
                awaitFrame()
                onNavigateToTermsAgreement()
            }
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
     * ✅ 약관 동의 다이얼로그
     * - 조건 단순화: onboardingCompleted 체크 제거
     * - termsAgreed 상태 변경 시 LaunchedEffect가 자동으로 navigation 처리
     */
    if (isLoggedIn && !termsAgreed) {
        TermsAgreementDialogRoute(
            onDismiss = {
                // 약관 거부 → 로그아웃
                viewModel.logout()
            },
            onTermsAgreedUpdated = {
                // 상태만 업데이트 (navigation은 LaunchedEffect에서 처리)
                onboardingViewModel.updateServiceTermsChecked(true)
                onboardingViewModel.updatePrivacyPolicyChecked(true)

                // 깜빡임 방지
                scope.launch {
                    delay(200L)
                }

            },
        )
    }
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
            AlertDialog(
                onDismissRequest = onDismissError,
                title = {
                    Text(
                        text = "로그인 실패",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                text = {
                    Text(
                        text = (uiState as LoginUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onDismissError,
                    ) {
                        Text("확인")
                    }
                },
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
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}