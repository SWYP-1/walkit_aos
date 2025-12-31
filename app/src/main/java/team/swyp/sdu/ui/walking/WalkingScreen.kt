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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.InfoBadge
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.components.WalkingActionButton
import team.swyp.sdu.ui.walking.components.formatToHoursMinutesSeconds
import team.swyp.sdu.ui.walking.viewmodel.WalkingScreenState
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.utils.DateUtils
import team.swyp.sdu.utils.FormatUtils.formatStepCount
import team.swyp.sdu.utils.Season
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkingScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: WalkingViewModel = hiltViewModel(),
    onNavigateToPostWalkingEmotion: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val screenState by viewModel.walkingScreenState.collectAsStateWithLifecycle()
    val isSavingSession by viewModel.isSavingSession.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val permissionsState =
        rememberMultiplePermissionsState(
            permissions = buildList {
                // 필수 권한들
                add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                add(android.Manifest.permission.ACCESS_COARSE_LOCATION)

                // Android 10 이상에서 필요한 권한
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    add(android.Manifest.permission.ACTIVITY_RECOGNITION)
                }

                // Android 13 이상에서 필요한 권한
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
        )

    // 권한 상태 디버깅
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        Timber.tag("WalkingScreen")
            .d("권한 상태 변경: allPermissionsGranted = ${permissionsState.allPermissionsGranted}")
        permissionsState.permissions.forEach { permission ->
            Timber.tag("WalkingScreen").d("권한: ${permission.permission}, 상태: ${permission.status}")
        }
    }

    // 권한이 없는 경우 자동으로 권한 요청
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            Timber.tag("WalkingScreen").d("권한이 부족하여 자동으로 권한 요청")
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 백버튼 다이얼로그 상태
    val showBackDialog = remember { mutableStateOf(false) }

    // Walking 상태에서 백버튼 처리
    BackHandler(enabled = screenState.uiState is WalkingUiState.Walking) {
        showBackDialog.value = true
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = screenState.uiState) {
            is WalkingUiState.Loading -> {
                // 초기 로딩 화면
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                }
            }

            is WalkingUiState.PreWalkingEmotionSelection -> {
                // 산책 전 감정 선택
                PreWalkingEmotionSelectRoute(
                    viewModel = viewModel,
                    onNext = {
                        coroutineScope.launch {
                            viewModel.startWalking()
                        }
                    },
                    onPrev = onNavigateBack,
                    permissionsGranted = permissionsState.allPermissionsGranted,
                )
            }

            is WalkingUiState.Walking -> {
                WalkingScreenContent(
                    modifier = modifier,
                    screenState = screenState,
                    onPauseClick = viewModel::pauseWalking,
                    onResumeClick = viewModel::resumeWalking,
                    onFinishClick = {
                        // 즉시 UI 상태를 SessionSaved로 변경하여 isFinish = true로 만듦
                        viewModel.finishWalking()
                        // 백그라운드에서 세션 저장 실행
                        coroutineScope.launch {
                            viewModel.stopWalking()
                        }
                    },
                    onNextClick = onNavigateToPostWalkingEmotion
                )
            }


            is WalkingUiState.SessionSaved -> {
                // 세션 저장 완료 후 UI 표시
                WalkingScreenContent(
                    modifier = modifier,
                    screenState = screenState,
                    onPauseClick = viewModel::pauseWalking,
                    onResumeClick = viewModel::resumeWalking,
                    onFinishClick = {}, // SessionSaved 상태에서는 사용하지 않음
                    onNextClick = {
                        Timber.d("🚶 WalkingScreenRoute - CTA 버튼 클릭, PostWalkingEmotionSelect로 이동")
                        onNavigateToPostWalkingEmotion()
                    },
                )
            }

        }
    }

// 백버튼 확인 다이얼로그
    if (showBackDialog.value) {

        // 세션 저장 중 오버레이
        if (isSavingSession) {
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
        ConfirmDialog(
            title = "산책 중단",
            message = "산책을 중단하시겠습니까?",
            negativeButtonText = "중단하기",
            positiveButtonText = "계속하기",
            onDismiss = { showBackDialog.value = false },
            onNegative = {
                showBackDialog.value = false
                // 산책 취소 (추적 중단만 하고 세션 저장하지 않음)
                coroutineScope.launch {
                    viewModel.cancelWalking()
                }
                onNavigateBack()
            },
            onPositive = {
                showBackDialog.value = false
                // 산책 계속 진행
            }
        )
    }
}

@Composable
private fun WalkingScreenContent(
    modifier: Modifier = Modifier,
    screenState: WalkingScreenState,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onFinishClick: () -> Unit = {}, // 산책 종료 모든 기록 저장
    onNextClick: () -> Unit = {}, // 산책 완료 후 PostWalkingEmotionSelect 화면으로 이동
) {

    val walkingState = screenState.uiState as? WalkingUiState.Walking
    val characterState = screenState.character

    val currentSeason = DateUtils.getCurrentSeason()
    val defaultBackground = when (currentSeason) {
        Season.SPRING -> R.drawable.bg_spring_full
        Season.SUMMER -> R.drawable.bg_summer_full
        Season.AUTUMN -> R.drawable.bg_autumn_full
        Season.WINTER -> R.drawable.bg_winter_full
    }
    Box(modifier = Modifier.fillMaxSize()){
        if (characterState == null) {
            Image(
                painter = painterResource(defaultBackground),
                contentDescription = "walking background",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = characterState.backgroundImageName,
                error = painterResource(defaultBackground),
                contentDescription = "walking background"
            )
        }

        SubcomposeLayout(
            modifier = modifier.fillMaxSize()
        ) { constraints ->

            /* ---------- Character ---------- */
            val character = subcompose("character") {
                WalkitCharacter(character = characterState)
            }[0].measure(Constraints())

            /* ---------- StepCounter (SessionSaved 상태가 아닐 때) ---------- */
            val stepCounter =
                if (screenState.uiState !is WalkingUiState.SessionSaved && walkingState != null) {
                    subcompose("stepCounter") {
                        WalkitStepInfo(stepCount = walkingState.stepCount)
                    }[0].measure(Constraints())
                } else null

            /* ---------- Timer (SessionSaved 상태가 아닐 때) ---------- */
            val timer =
                if (screenState.uiState !is WalkingUiState.SessionSaved && walkingState != null) {
                    subcompose("timer") {
                        val painterResource = painterResource(id = R.drawable.ic_info_timer)
                        InfoBadge(
                            iconPainter = painterResource,
                            text = formatToHoursMinutesSeconds(walkingState.duration)
                        )
                    }[0].measure(Constraints())
                } else null


            val finishText = if (screenState.uiState is WalkingUiState.SessionSaved) {
                subcompose("finishText") {
                    FinishWalkingText()
                }[0].measure(Constraints())
            } else null

            /* ---------- Action Buttons (SessionSaved 상태가 아닐 때) ---------- */
            val actionRow =
                if (screenState.uiState !is WalkingUiState.SessionSaved && walkingState != null) {
                    subcompose("actionRow") {
                        WalkingActionButtonRow(
                            isPaused = walkingState.isPaused,
                            onClickPause = {
                                if (walkingState.isPaused) onResumeClick() else onPauseClick()
                            },
                            onClickFinish = onFinishClick
                        )
                    }[0].measure(Constraints())
                } else null

            /* ---------- CTA Button (SessionSaved 상태일 때) ---------- */
            val onNextButton = if (screenState.uiState is WalkingUiState.SessionSaved) {
                subcompose("onNext") {
                    CtaWrapper(
                        onClick = onNextClick,
                        enabled = true // 세션 저장 완료 상태이므로 항상 활성화
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

//                // Character (bottom = center line)
//                character.place(
//                    x = (constraints.maxWidth - character.width) / 2,
//                    y = centerY - character.height
//                )
                character.place(
                    x = (constraints.maxWidth - character.width) / 2,
                    y = centerY - character.height / 2
                )


                // stepCounter (top = 94dp)
                stepCounter?.place(
                    x = (constraints.maxWidth - stepCounter.width) / 2,
                    y = 94.dp.roundToPx()
                )

                // timer (top and bottom)
                timer?.place(
                    x = (constraints.maxWidth - timer.width) / 2,
                    y = 18.dp.roundToPx()
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




}

@Composable
fun WalkitCharacter(modifier: Modifier = Modifier, character: Character?) {
    if (character != null) {
        AsyncImage(
            model = character.characterImageName,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.walk_it_character),
            contentDescription = null,
        )
    }
}


@Composable
fun WalkitStepInfo(modifier: Modifier = Modifier, stepCount: Int) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "걸음 수 ",

            // body M/medium
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.iconGrey,
        )

        Text(
            text = formatStepCount(stepCount),
            style = MaterialTheme.walkItTypography.headingXL.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 67.6.sp,
            ),
            color = SemanticColor.logoGreen
        )
    }
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
fun CtaWrapper(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) { // PostWalkingEmotionSelect로 이동
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CtaButton(
            text = "다음으로 이동",
            onClick = onClick,
            enabled = enabled,
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
            screenState = WalkingScreenState(
                uiState = WalkingUiState.Walking(
                    stepCount = 1250,
                    duration = 1800000L, // 30분
                    isPaused = false
                ),
                character = null // 캐릭터 정보 없음
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
            screenState = WalkingScreenState(
                uiState = WalkingUiState.Walking(
                    stepCount = 1250,
                    duration = 1800000L, // 30분
                    isPaused = true
                ),
                character = null // 캐릭터 정보 없음
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
            screenState = WalkingScreenState(
                uiState = WalkingUiState.Walking(
                    stepCount = 1250,
                    duration = 1800000L, // 30분
                    isPaused = true
                ),
                character = null // 캐릭터 정보 없음
            ),
            onNextClick = {}
        )
    }
}
