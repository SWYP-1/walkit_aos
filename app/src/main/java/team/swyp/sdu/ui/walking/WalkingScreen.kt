package team.swyp.sdu.ui.walking

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import team.swyp.sdu.ui.components.ConfirmDialog
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.components.WalkingActionButton
import team.swyp.sdu.ui.walking.components.WalkitTimer
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkingScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: WalkingViewModel = hiltViewModel(),
    onNavigateToFinish: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onStopClick: (() -> Unit)? = null, // 집중모드용 커스텀 스탑 핸들러
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSessionSaved by viewModel.isSessionSaved.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val permissionsState =
        rememberMultiplePermissionsState(
            permissions =
                listOf(
                    android.Manifest.permission.ACTIVITY_RECOGNITION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ),
        )

    // 백버튼 다이얼로그 상태
    val showBackDialog = remember { mutableStateOf(false) }

    // Walking 상태에서 백버튼 처리
    BackHandler(enabled = uiState is WalkingUiState.Walking) {
        showBackDialog.value = true
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is WalkingUiState.PreWalkingEmotionSelection -> {
                // 산책 전 감정 선택
                PreWalkingEmotionSelectRoute(
                    viewModel = viewModel,
                    onNext = {
                        if (permissionsState.allPermissionsGranted) {
                            coroutineScope.launch {
                                viewModel.startWalking()
                            }
                        }
                    },
                    onPrev = onNavigateBack,
                    permissionsGranted = permissionsState.allPermissionsGranted,
                )
            }

            is WalkingUiState.Walking -> {
                // 기본 스탑 핸들러 (함수형 스타일)
                val defaultStopHandler: () -> Unit = remember {
                    {
                        coroutineScope.launch {
                            viewModel.stopWalking()
                        }
                    }
                }

                WalkingScreenContent(
                    modifier = modifier,
                    uiState = uiState,
                    onPauseClick = viewModel::pauseWalking,
                    onResumeClick = viewModel::resumeWalking,
                    onStopClick = onStopClick ?: defaultStopHandler,
                    onNextClick = onNavigateToFinish,
                )
            }

            is WalkingUiState.SavingSession -> {
                // 세션 저장 중 로딩 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                        Text(
                            text = "산책 기록을 저장하는 중...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            is WalkingUiState.SessionSaved -> {
                // 세션 저장 완료 후 자동으로 다음 화면으로 이동
                // isSessionSaved 플래그가 true인 경우에만 이동 (DB 저장 완료 확인)
                if (isSessionSaved) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        onNavigateToFinish()
                    }
                } else {
                    // 세션 저장이 아직 완료되지 않은 경우 로딩 유지
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                            Text(
                                text = "세션 저장 완료 대기 중...",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            is WalkingUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "오류",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    androidx.compose.material3.Button(onClick = {}) {
                        Text("다시 시도")
                    }
                }
            }
        }

        // 백버튼 확인 다이얼로그
        if (showBackDialog.value) {
            ConfirmDialog(
                title = "산책 중단",
                message = "산책을 중단하시겠습니까?",
                negativeButtonText = "중단하기",
                positiveButtonText = "계속하기",
                onDismiss = { showBackDialog.value = false },
                onNegative = {
                    showBackDialog.value = false
                    // 세션 저장 없이 그냥 종료
                    onNavigateBack()
                },
                onPositive = {
                    showBackDialog.value = false
                    // 산책 계속 진행
                }
            )
        }
    }
}

@Composable
private fun WalkingScreenContent(
    modifier: Modifier = Modifier,
    uiState: WalkingUiState,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    val isFinish = uiState is WalkingUiState.SessionSaved
    val isWalking = uiState is WalkingUiState.Walking
    val walkingState = uiState as? WalkingUiState.Walking

    SubcomposeLayout(
        modifier = modifier.fillMaxSize()
    ) { constraints ->

        /* ---------- Background ---------- */
        val bg = subcompose("bg") {
            Image(
                painter = painterResource(R.drawable.bg_winter_cropped),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }[0].measure(constraints)

        /* ---------- Character ---------- */
        val character = subcompose("character") {
            Image(
                painter = painterResource(R.drawable.walk_it_character),
                contentDescription = null
            )
        }[0].measure(Constraints())

        /* ---------- Timer (isFinish == false) ---------- */
        val timer = if (!isFinish && walkingState != null) {
            subcompose("timer") {
                WalkitTimer(duration = walkingState.duration)
            }[0].measure(Constraints())
        } else null

        val finishText = if (isFinish) {
            subcompose("timer") {
                FinishWalkingText()
            }[0].measure(Constraints())
        } else null

        /* ---------- Action Buttons (isFinish == false) ---------- */
        val actionRow = if (!isFinish && walkingState != null) {
            subcompose("actionRow") {
                WalkingActionButtonRow(
                    isPaused = walkingState.isPaused,
                    onClickPause = {
                        if (walkingState.isPaused) onResumeClick() else onPauseClick()
                    },
                    onClickFinish = onStopClick
                )
            }[0].measure(Constraints())
        } else null

        /* ---------- CTA Button (isFinish == true) ---------- */
        val onNextButton = if (isFinish) {
            subcompose("onNext") {
                CtaWrapper(
                    onClick = onNextClick
                )
            }[0].measure(
                Constraints(
                    minWidth = constraints.maxWidth,
                    maxWidth = constraints.maxWidth
                )
            )
        } else null

        /* ---------- Layout ---------- */
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            val centerY = constraints.maxHeight / 2

            // Background
            bg.place(0, 0)

            // Character (bottom = center line)
            character.place(
                x = (constraints.maxWidth - character.width) / 2,
                y = centerY - character.height
            )

            // Timer (top = 94dp)
            timer?.place(
                x = (constraints.maxWidth - timer.width) / 2,
                y = 94.dp.roundToPx()
            )

            // Finish Text (top = 137.dp)
            finishText?.place(
                x = (constraints.maxWidth - finishText.width) / 2,
                y = 137.dp.roundToPx()
            )

            // Action buttons (bottom = 100dp)
            actionRow?.place(
                x = (constraints.maxWidth - actionRow.width) / 2,
                y = constraints.maxHeight -
                        actionRow.height -
                        100.dp.roundToPx()
            )

            // CTA button (bottom = 40dp)
            onNextButton?.place(
                x = (constraints.maxWidth - onNextButton.width) / 2,
                y = constraints.maxHeight -
                        onNextButton.height -
                        70.dp.roundToPx()
            )
        }
    }
}


@Composable
fun WalkitStepInfo(modifier: Modifier = Modifier) {

}

@Composable
fun WalkingActionButtonRow(
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    onClickPause: () -> Unit,
    onClickFinish: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WalkingActionButton(
            textColor = SemanticColor.textBorderGreenSecondary,
            iconRes = if (isPaused) R.drawable.ic_action_restart else R.drawable.ic_action_pause,
            backgroundColor = SemanticColor.backgroundWhitePrimary,
            text = if (isPaused) "다시 시작" else "일시정지",
            onClick = onClickPause,
        )
        Spacer(Modifier.width(56.dp))
        WalkingActionButton(
            textColor = SemanticColor.textBorderGreenSecondary,
            backgroundColor = Color(0xFFD8FFD6),
            text = "산책 끝내기",
            iconRes = R.drawable.ic_action_finish_walk,
            onClick = onClickFinish
        )
    }
}

@Composable
fun CtaWrapper(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CtaButton(
            text = "다음으로 이동",
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun FinishWalkingText(modifier: Modifier = Modifier) {
    Column(
        Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "산책 종료",

            // heading L/semibold
            style = MaterialTheme.walkItTypography.headingL.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = SemanticColor.textBorderPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "산책 후 감정을 기록하시겠습니까?",
            // body M/regular
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Normal
            ),
            color = SemanticColor.textBorderSecondary
        )
    }

}

@Preview(
    name = "Walking - In Progress",
    showBackground = true
)
@Composable
fun WalkingScreenPreviewInProgress() {
    WalkItTheme {
        // Preview에서는 mock 데이터를 사용
        WalkingScreenContent(
            uiState = WalkingUiState.Walking(
                stepCount = 1250,
                duration = 1800000L, // 30분
                isPaused = false
            )
        )
    }
}

@Preview(
    name = "Walking - Paused",
    showBackground = true
)
@Composable
fun WalkingScreenPreviewPaused() {
    WalkItTheme {
        // Preview에서는 mock 데이터를 사용
        WalkingScreenContent(
            uiState = WalkingUiState.Walking(
                stepCount = 1250,
                duration = 1800000L, // 30분
                isPaused = true
            )
        )
    }
}

@Preview(
    name = "Walking - Finished",
    showBackground = true
)
@Composable
fun WalkingScreenPreviewFinished() {
    WalkItTheme {
        WalkingScreenContent(
            uiState = WalkingUiState.SessionSaved
        )
    }
}
