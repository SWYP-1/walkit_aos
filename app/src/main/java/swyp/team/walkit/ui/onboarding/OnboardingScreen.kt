package swyp.team.walkit.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import swyp.team.walkit.ui.theme.WalkItTheme
import timber.log.Timber

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onFinish: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = modifier,
            ) {
                when (uiState.currentStep) {
                    0 -> NicknameStep(
                        uiState = uiState,
                        onNicknameChange = viewModel::updateNickname,
                        onNext = {
                            try {
                                viewModel.registerNickname()
                            } catch (t: Throwable) {
                            }
                        },
                        onCheckDuplicate = viewModel::checkNicknameDuplicate
                    )

                    1 -> BirthYearStep(
                        uiState = uiState,
                        onYearChange = viewModel::updateBirthYear,
                        onMonthChange = viewModel::updateBirthMonth,
                        onDayChange = viewModel::updateBirthDay,
                        onNext = {
                            if (uiState.canProceed) {
                                viewModel.updateBirthDate()
                            }
                        },
                        onPrev = viewModel::previousStep,
                    )

                    2 -> GoalStep(
                        uiState = uiState,
                        goal = uiState.goalCount,
                        onGoalChange = viewModel::updateGoalCount,
                        steps = uiState.stepTarget,
                        onStepsChange = viewModel::updateStepTarget,
                        onNext = {
                            viewModel.setGoal {
                                viewModel.submitOnboarding {
                                    // 온보딩 완료 후 화면 전환
                                    // LoginScreen의 LaunchedEffect가 자동으로 메인으로 이동하지만
                                    // 명시적으로 onFinish를 호출하여 네비게이션 수행
                                    onFinish()
                                }
                            }
                        },
                        onPrev = viewModel::previousStep,
                    )
                }
            }
        }
    }
}




