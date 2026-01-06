package swyp.team.walkit.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.goal.GoalRange
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.components.PreviousButton
import swyp.team.walkit.ui.components.LoadingOverlay
import swyp.team.walkit.ui.mypage.goal.component.GoalSettingCard
import swyp.team.walkit.ui.onboarding.component.OnBoardingStepTag
import swyp.team.walkit.ui.theme.Black
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 목표 설정 단계 컴포넌트
 */
@Composable
fun GoalStep(
    uiState: OnboardingUiState,
    goal: Int,
    onGoalChange: (Int) -> Unit,
    steps: Int,
    onStepsChange: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    // 목표 설정 API 호출 시 로딩 표시
    val isLoading = uiState.isLoading && uiState.currentStep == 2
    // 주간 산책 횟수 범위: 1~7회 (미만, 초과 불가)
    val goalRange = remember { GoalRange(min = 1, max = 7) }

    // 걸음 수 목표 범위: 1000~100000보 (1000보 단위, 미만, 초과 불가)
    val stepRange = remember { GoalRange(min = 1000, max = 30000) }

    // 현재 값이 범위를 벗어나면 조정
    val safeGoal = remember(goal) { goal.coerceIn(goalRange.min, goalRange.max) }
    val safeSteps = remember(steps) { steps.coerceIn(stepRange.min, stepRange.max) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhiteSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnBoardingStepTag(text = "목표 설정")
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${uiState.nicknameState.value}님,\n" + "함께 걸어봐요",
                        style = MaterialTheme.walkItTypography.headingM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(42.dp))
                }

                // 주간 산책 횟수 설정
                GoalSettingCard(
                    title = "주간 산책 횟수",
                    currentNumber = safeGoal,
                    onNumberChange = { newValue ->
                        // 범위를 벗어난 값은 아예 설정하지 않음
                        if (newValue in goalRange.min..goalRange.max) {
                            onGoalChange(newValue)
                        }
                    },
                    range = goalRange,
                    unit = "회",
                    onClickMinus = {
                        // 범위를 벗어나면 아예 호출하지 않음
                        val newValue = safeGoal - 1
                        if (newValue >= goalRange.min) {
                            onGoalChange(newValue)
                        }
                    },
                    onClickPlus = {
                        // 범위를 벗어나면 아예 호출하지 않음
                        val newValue = safeGoal + 1
                        if (newValue <= goalRange.max) {
                            onGoalChange(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SemanticColor.textBorderPrimary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 걸음 수 목표 설정
                GoalSettingCard(
                    title = "걸음 수 목표",
                    currentNumber = safeSteps,
                    onNumberChange = { newValue ->
                        // 범위를 벗어난 값은 아예 설정하지 않음
                        if (newValue in stepRange.min..stepRange.max) {
                            onStepsChange(newValue)
                        }
                    },
                    range = stepRange,
                    unit = "걸음",
                    onClickMinus = {
                        // 범위를 벗어나면 아예 호출하지 않음
                        val newValue = safeSteps - 1000
                        if (newValue >= stepRange.min) {
                            onStepsChange(newValue)
                        }
                    },
                    onClickPlus = {
                        // 범위를 벗어나면 아예 호출하지 않음
                        val newValue = safeSteps + 1000
                        if (newValue <= stepRange.max) {
                            onStepsChange(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SemanticColor.textBorderPrimary,
                )

                Spacer(modifier = Modifier.weight(1f))

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreviousButton(
                        onClick = onPrev
                    )

                    CtaButton(
                        text = "다음으로",
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        iconResId = R.drawable.ic_arrow_forward,
                        // iconTint 생략하면 자동으로 content 색상 사용
                    )
                }
            }

            // API 요청 시 로딩 오버레이 표시
            LoadingOverlay(isLoading = isLoading)
        }
    }
}

@Preview(showBackground = true, name = "기본 상태 - 목표 미설정")
@Composable
private fun GoalStepEmptyPreview() {
    WalkItTheme {
        GoalStep(
            uiState = OnboardingUiState(
                nicknameState = NicknameState("홍길동"),
                goalCount = 1,
                stepTarget = 1000,
            ),
            goal = 1,
            onGoalChange = {},
            steps = 1000,
            onStepsChange = {},
            onNext = {},
            onPrev = {},
        )
    }
}
