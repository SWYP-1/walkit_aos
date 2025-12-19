package team.swyp.sdu.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.presentation.viewmodel.LoginUiState
import team.swyp.sdu.presentation.viewmodel.LoginViewModel
import team.swyp.sdu.ui.onboarding.OnboardingViewModel
import team.swyp.sdu.ui.login.components.LoginButton

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onSkipToMain: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val onboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle(initialValue = false)

    val naverLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result: ActivityResult ->
        viewModel.handleNaverLoginResult(result)
    }

    LaunchedEffect(isLoggedIn, onboardingCompleted) {
        if (isLoggedIn) {
            // 로그인 성공 시 온보딩 완료 여부 확인
            if (onboardingCompleted) {
                onLoginSuccess() // 온보딩 완료 -> 메인으로
            } else {
                onNavigateToOnboarding() // 온보딩 미완료 -> 온보딩으로
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "SWYP",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "산책 기록 앱",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(64.dp))

            when {
                isLoggedIn || uiState is LoginUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "로그인 상태 확인 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    // 카카오 로그인
                    Image(
                        painter = painterResource(id = R.drawable.kakao_login_large_wide),
                        contentDescription = "카카오 로그인",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                viewModel.loginWithKakaoTalk(context)
                            },
                        contentScale = ContentScale.FillWidth,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 네이버 로그인
                    LoginButton(
                        text = "네이버 로그인",
                        backgroundColor = Color(0xFF03C75A),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.loginWithNaver(context, naverLoginLauncher) },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 로그아웃 버튼
                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Text("로그아웃")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 메인화면으로 가기 버튼
                    Button(
                        onClick = onSkipToMain,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Text("무시하고 메인화면으로 가기")
                    }

                    if (uiState is LoginUiState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = (uiState as LoginUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}



