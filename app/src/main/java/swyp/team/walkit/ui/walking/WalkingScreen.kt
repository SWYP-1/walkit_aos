package swyp.team.walkit.ui.walking

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import swyp.team.walkit.ui.components.ConfirmDialog
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.components.InfoBadge
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.components.WalkingActionButton
import swyp.team.walkit.ui.walking.components.formatToHoursMinutesSeconds
import swyp.team.walkit.ui.walking.viewmodel.WalkingScreenState
import swyp.team.walkit.ui.walking.viewmodel.WalkingUiState
import swyp.team.walkit.ui.walking.viewmodel.WalkingViewModel
import swyp.team.walkit.utils.DateUtils
import swyp.team.walkit.utils.FormatUtils.formatStepCount
import swyp.team.walkit.utils.Season
import timber.log.Timber
import kotlin.io.path.Path
import kotlin.io.path.moveTo

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WalkingScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: WalkingViewModel,
    onNavigateToPostWalkingEmotion: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {

    val screenState by viewModel.walkingScreenState.collectAsStateWithLifecycle()
    val isSavingSession by viewModel.isSavingSession.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // 화면 진입 시 캐릭터 정보 로드 (최초 1회)
    val walkingCharacter by viewModel.walkingCharacter.collectAsStateWithLifecycle()

    // 캐릭터 정보가 없을 때만 로드 (중복 호출 방지)
    // viewModel을 key로 사용하여 ViewModel이 변경될 때만 재실행
    LaunchedEffect(viewModel) {
        Timber.d("🚶 WalkingScreen LaunchedEffect triggered - viewModel hash: ${viewModel.hashCode()}")
        if (walkingCharacter == null) {
            Timber.d("🚶 WalkingScreen: 캐릭터 정보 로드 시도")
            viewModel.loadWalkingCharacterIfNeeded()
        } else {
            Timber.d("🚶 WalkingScreen: 캐릭터 정보 이미 로드됨, 스킵 - ${walkingCharacter?.nickName}")
        }
    }

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
        modifier = modifier.fillMaxSize(),
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

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (characterState != null) {
            AsyncImage(
                model = characterState.backgroundImageName,
                error = painterResource(defaultBackground),
                contentDescription = "walking background",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        SubcomposeLayout(
            modifier = modifier.fillMaxSize()
        ) { constraints ->

            /* ---------- Character ---------- */
            val character = subcompose("character") {
                WalkitCharacter(
                    character = characterState,
                    lottieJson = screenState.characterLottieJson
                )
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

            /* ---------- Goal (SessionSaved 상태가 아닐 때) ---------- */
            val currentGoal = if (screenState.uiState !is WalkingUiState.SessionSaved) {
                subcompose("currentGoal") {
                    CurrentChanllgeGoal(challengeCount = screenState.currentWeekGoalChallengeCount)
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
                    y = centerY - character.height / 2 + 20.dp.roundToPx()
                )


                // stepCounter (top = 94dp)
                stepCounter?.place(
                    x = (constraints.maxWidth - stepCounter.width) / 2,
                    y = 94.dp.roundToPx()
                )

                // timer (top and bottom)
                timer?.place(
                    x = (constraints.maxWidth - timer.width) / 2,
                    y = 42.dp.roundToPx()
                )

                // Finish Text (top = 137.dp)
                finishText?.place(
                    x = (constraints.maxWidth - finishText.width) / 2,
                    y = 161.dp.roundToPx()
                )
                // Action buttons (bottom anchor)
                val actionRowTopY = actionRow?.let {
                    constraints.maxHeight -
                            it.height -
                            100.dp.roundToPx()
                }

                actionRow?.place(
                    x = (constraints.maxWidth - actionRow.width) / 2,
                    y = actionRowTopY ?: 0
                )

                // 🔥 Goal 말풍선 (ActionRow 기준 위 40dp)
                currentGoal?.let { goal ->
                    val spacing = 34.dp.roundToPx()
                    val minGoalY = 120.dp.roundToPx() // 너무 위로 못 가게 가드

                    val goalY = maxOf(
                        minGoalY,
                        (actionRowTopY ?: constraints.maxHeight) -
                                goal.height -
                                spacing
                    )

                    goal.place(
                        x = (constraints.maxWidth - goal.width) / 2,
                        y = goalY
                    )
                }

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
fun WalkitCharacter(
    modifier: Modifier = Modifier,
    character: Character?,
    lottieJson: String? = null
) {
    // Lottie JSON이 있으면 Lottie 애니메이션 사용, 없으면 기존 AsyncImage 사용
    if (lottieJson != null && character != null) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.JsonString(lottieJson)
        )

        LottieAnimation(
            composition = composition,
            modifier = modifier.size(280.dp), // Lottie 크기 증가로 이미지와 크기 맞춤
            iterations = Int.MAX_VALUE // 무한 반복
        )
    } else if (character != null) {
        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
    } else {
        CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
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

@Composable
fun CurrentChanllgeGoal(modifier: Modifier = Modifier, challengeCount: Int = 0) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ▲ 위쪽 삼각형
        Canvas(
            modifier = Modifier
                .size(width = 15.dp, height = 15.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)          // 꼭대기
                lineTo(0f, size.height)             // 왼쪽
                lineTo(size.width, size.height)     // 오른쪽
                close()
            }
            drawPath(
                path = path,
                color = SemanticColor.stateYellowTertiary
            )
        }

        // 말풍선 본체
        Box(
            modifier = Modifier
                .background(
                    color = SemanticColor.stateYellowTertiary,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${challengeCount + 1}번째 목표 진행중",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.stateYellowPrimary
            )
        }
    }
}

@Preview(showBackground = true, name = "Current Challenge Goal")
@Composable
fun CurrentChallengeGoalPreview() {
    WalkItTheme {
        CurrentChanllgeGoal(challengeCount = 2) // 3번째 목표 진행중으로 표시
    }
}

