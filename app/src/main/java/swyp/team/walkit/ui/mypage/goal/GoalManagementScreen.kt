package swyp.team.walkit.ui.mypage.goal

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.domain.goal.GoalRange
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.ConfirmDialog
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.mypage.goal.GoalError
import swyp.team.walkit.ui.mypage.goal.components.GoalInfoBanner
import swyp.team.walkit.ui.mypage.goal.component.GoalSettingCard
import swyp.team.walkit.ui.mypage.goal.components.GoalErrorBanner
import swyp.team.walkit.ui.mypage.goal.model.GoalState
import swyp.team.walkit.ui.mypage.goal.model.hasChangesComparedTo
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import timber.log.Timber
import kotlin.math.min

@Composable
fun GoalManagementRoute(
    viewModel: GoalManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goalError by viewModel.goalError.collectAsStateWithLifecycle()

    GoalManagementScreen(
        goalState = uiState,
        goalError = goalError,
        onNavigateBack = onNavigateBack,
        onUpdateGoal = viewModel::updateGoal,
        onResetGoal = viewModel::resetGoal,
        onClearError = viewModel::clearGoalError,
        modifier = modifier,
    )
}

@Composable
fun GoalManagementScreen(
    goalState: GoalState,
    goalError: GoalError?,
    onNavigateBack: () -> Unit,
    onUpdateGoal: suspend (Int, Int) -> Unit,
    onResetGoal: suspend () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSteps by remember { mutableIntStateOf(goalState.targetSteps) }
    var selectedFrequency by remember { mutableIntStateOf(goalState.walkFrequency) }

    // 서버 데이터가 로드되면 로컬 상태 업데이트
    LaunchedEffect(goalState.targetSteps, goalState.walkFrequency) {
        selectedSteps = goalState.targetSteps
        selectedFrequency = goalState.walkFrequency
    }

    var showConfirmDialog by remember { mutableStateOf(false) }

    // 에러 메시지 표시 상태 관리 (스낵바처럼 3초 후 사라짐)
    var showErrorMessage by remember { mutableStateOf(false) }
    var currentErrorMessage by remember { mutableStateOf<Pair<String, String>?>(null) }

    // goalError가 변경될 때 스낵바 표시
    LaunchedEffect(goalError) {
        if (goalError != null) {
            val (title, description) = when (goalError) {
                is GoalError.UpdateNotAllowed -> "이번 달 목표 수정이 불가능합니다" to "목표는 한 달에 한 번만 변경 가능합니다"
                is GoalError.SaveFailed -> "목표 저장에 실패했습니다" to "네트워크 연결을 확인하고 다시 시도해주세요"
                else -> null to null
            }

            if (title != null && description != null) {
                currentErrorMessage = title to description
                showErrorMessage = true

                // 3초 후 자동으로 사라짐
                kotlinx.coroutines.delay(3000)
                showErrorMessage = false
                currentErrorMessage = null
                onClearError() // ViewModel의 에러 상태도 클리어
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(title = "내 목표 관리", onNavigateBack = onNavigateBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // GoalInfoBanner 컴포넌트 사용
                GoalInfoBanner(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                GoalSettingCard(
                    title = "주간 산책 횟수",
                    currentNumber = selectedFrequency,
                    onClickPlus = { if (selectedFrequency < 7) selectedFrequency++ },
                    onClickMinus = { if (selectedFrequency > 1) selectedFrequency-- },
                    onNumberChange = { selectedFrequency = it },
                    range = GoalRange(1, 7),
                    unit = "회",
                    accentColor = SemanticColor.textBorderPrimary
                )

                Spacer(Modifier.height(24.dp))

                GoalSettingCard(
                    title = "목표 걸음 수",
                    currentNumber = selectedSteps,
                    onClickPlus = { if (selectedSteps < 100_000) selectedSteps += 1000 },
                    onClickMinus = { if (selectedSteps > 1000) selectedSteps -= 1000 },
                    onNumberChange = { selectedSteps = it },
                    range = GoalRange(1000, 30_000),
                    unit = "걸음",
                    accentColor = SemanticColor.textBorderPrimary
                )

                Spacer(Modifier.weight(1f))

                // 스낵바 스타일 에러 메시지 (CTA 버튼 위)
                AnimatedVisibility(
                    visible = showErrorMessage,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    currentErrorMessage?.let { (title, description) ->
                        GoalErrorBanner(
                            title = title,
                            description = description,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 액션 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CtaButton(
                        text = "초기화", variant = CtaButtonVariant.SECONDARY, onClick = {
                            scope.launch { onResetGoal() }
                            // 초기화 후 로컬 상태도 즉시 업데이트
                            val defaultState = GoalState()
                            selectedSteps = defaultState.targetSteps
                            selectedFrequency = defaultState.walkFrequency
                        }, modifier = Modifier.weight(1f)
                    )

                    CtaButton(
                        text = "저장하기",
                        enabled = goalError != GoalError.UpdateNotAllowed, // 한 달에 1번 변경 제한 에러에서만 비활성화
                        onClick = {
                            if (GoalState(selectedSteps, selectedFrequency).hasChangesComparedTo(
                                    goalState
                                )
                            ) {
                                showConfirmDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }, modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // 저장 확인 다이얼로그
        if (showConfirmDialog) {
            ConfirmDialog(
                title = "목표 저장",
                message = "변경한 목표를 저장하시겠습니까?",
                onPositive = {
                    showConfirmDialog = false
                    scope.launch {
                        try {
                            onUpdateGoal(selectedSteps, selectedFrequency)
                        } catch (t: Throwable) {
                            // ViewModel에서 에러 처리를 하므로 여기서는 별도 처리하지 않음
                            Timber.e(t, "목표 저장 중 예외 발생")
                        }
                    }
                },
                onNegative = { showConfirmDialog = false },
                onDismiss = { showConfirmDialog = false })
        }
    }
}


@Composable
@Preview(showBackground = true)
fun GoalManagementScreenPreview() {
    WalkItTheme {
        GoalManagementScreen(
            goalState = GoalState(),
            goalError = null,
            onNavigateBack = {},
            onUpdateGoal = { _, _ -> },
            onResetGoal = {},
            onClearError = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun GoalManagementScreenErrorPreview() {
    WalkItTheme {
        GoalManagementScreen(
            goalState = GoalState(),
            goalError = GoalError.UpdateNotAllowed,
            onNavigateBack = {},
            onUpdateGoal = { _, _ -> },
            onResetGoal = {},
            onClearError = {}
        )
    }
}
