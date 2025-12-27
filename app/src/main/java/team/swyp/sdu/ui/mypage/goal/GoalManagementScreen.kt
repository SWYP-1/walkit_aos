package team.swyp.sdu.ui.mypage.goal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import team.swyp.sdu.domain.goal.GoalRange
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.ConfirmDialog
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.InfoBanner
import team.swyp.sdu.ui.mypage.goal.component.GoalSettingCard
import team.swyp.sdu.ui.mypage.goal.model.GoalState
import team.swyp.sdu.ui.mypage.goal.model.hasChangesComparedTo
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme

@Composable
fun GoalManagementRoute(
    viewModel: GoalManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GoalManagementScreen(
        goalState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateGoal = viewModel::updateGoal,
        onResetGoal = viewModel::resetGoal,
        modifier = modifier,
    )
}

@Composable
fun GoalManagementScreen(
    goalState: GoalState,
    onNavigateBack: () -> Unit,
    onUpdateGoal: suspend (Int, Int) -> Unit,
    onResetGoal: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSteps by remember { mutableIntStateOf(goalState.targetSteps) }
    var selectedFrequency by remember { mutableIntStateOf(goalState.walkFrequency) }

    // 서버 데이터가 로드되면 로컬 상태 업데이트
    LaunchedEffect(goalState) {
        selectedSteps = goalState.targetSteps
        selectedFrequency = goalState.walkFrequency
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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

                InfoBanner(
                    title = "목표는 설정일부터 1주일 기준으로 설정 가능합니다.",
                    description = "목표는 1주 내 최소 1회, 최대 7회까지 설정 가능합니다"
                )
                Spacer(Modifier.height(20.dp))

                GoalSettingCard(
                    title = "주간 산책 횟수",
                    currentNumber = selectedFrequency,
                    onClickPlus = { if (selectedFrequency < 7) selectedFrequency++ },
                    onClickMinus = { if (selectedFrequency > 1) selectedFrequency-- },
                    onNumberChange = { selectedFrequency = it },
                    range = GoalRange(1, 7),
                    unit = "회"
                )

                Spacer(Modifier.height(24.dp))

                GoalSettingCard(
                    title = "목표 걸음 수",
                    currentNumber = selectedSteps,
                    onClickPlus = { if (selectedSteps < 100_000) selectedSteps += 1000 },
                    onClickMinus = { if (selectedSteps > 1000) selectedSteps -= 1000 },
                    onNumberChange = { selectedSteps = it },
                    range = GoalRange(1000, 100_000),
                    unit = "보"
                )

                Spacer(Modifier.weight(1f))

                // 액션 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CtaButton(
                        text = "초기화",
                        textColor = SemanticColor.buttonPrimaryDefault,
                        buttonColor = SemanticColor.backgroundWhitePrimary,
                        onClick = { scope.launch { onResetGoal() } },
                        modifier = Modifier.weight(1f)
                    )

                    CtaButton(
                        text = "저장하기",
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        onClick = {
                            if (GoalState(selectedSteps, selectedFrequency)
                                    .hasChangesComparedTo(goalState)
                            ) {
                                showConfirmDialog = true
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.weight(1f)
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
                            onNavigateBack()
                        } catch (e: Exception) {
                            errorMessage = "목표 저장에 실패했습니다. 다시 시도해주세요."
                            showErrorDialog = true
                        }
                    }
                },
                onNegative = { showConfirmDialog = false },
                onDismiss = { showConfirmDialog = false }
            )
        }

        // 저장 실패 다이얼로그
        if (showErrorDialog) {
            ConfirmDialog(
                title = "오류",
                message = errorMessage,
                onPositive = { showErrorDialog = false },
                onNegative = {  },
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}


@Composable
@Preview
fun GoalManagementScreenPreview() {
    WalkItTheme {
        GoalManagementScreen(
            goalState = GoalState(),
            onNavigateBack = {},
            onUpdateGoal = { _, _ -> },
            onResetGoal = {}
        )
    }
}
