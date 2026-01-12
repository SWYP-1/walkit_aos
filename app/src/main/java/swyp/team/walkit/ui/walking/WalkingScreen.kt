package swyp.team.walkit.ui.walking

import android.app.Activity
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
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import swyp.team.walkit.ui.components.WalkingWarningDialog
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import swyp.team.walkit.ui.record.components.GoalCheckRow
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
import swyp.team.walkit.utils.SetStatusBarConfig
import swyp.team.walkit.utils.TransparentStatusBarConfig
import timber.log.Timber
import kotlin.io.path.Path
import kotlin.io.path.moveTo

/**
 * WalkingScreen Route
 * ViewModel injection과 state collection을 담당하는 Route composable
 */
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
    val walkingCharacter by viewModel.walkingCharacter.collectAsStateWithLifecycle()

//    SetStatusBarConfig(config = TransparentStatusBarConfig)

    WalkingScreen(
        modifier = modifier,
        screenState = screenState,
        isSavingSession = isSavingSession,
        walkingCharacter = walkingCharacter,
        viewModel = viewModel,
        onNavigateToPostWalkingEmotion = onNavigateToPostWalkingEmotion,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * WalkingScreen
 * UI와 로직을 담당하는 Screen composable
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun WalkingScreen(
    modifier: Modifier = Modifier,
    screenState: WalkingScreenState,
    isSavingSession: Boolean,
    walkingCharacter: Character?,
    viewModel: WalkingViewModel,
    onNavigateToPostWalkingEmotion: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // 산책 종료 확인 다이얼로그 상태
    val showFinishConfirmDialog = remember { mutableStateOf(false) }

    // 화면 진입 시 캐릭터 정보 로드 (최초 1회)
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

    val permissionsState = rememberMultiplePermissionsState(
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

    // Walking 상태에서 백버튼 처리 (다이얼로그 표시)
    BackHandler(enabled = screenState.uiState is WalkingUiState.Walking || screenState.uiState is WalkingUiState.SessionSaved) {
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
                        // 산책 시간 확인 (1분 미만이면 경고 다이얼로그 표시)
                        val walkingState = screenState.uiState as? WalkingUiState.Walking
                        val durationInSeconds = (walkingState?.duration ?: 0L) / 1000

                        // TODO : 삭제 60 으로 바꾸기
                        if (durationInSeconds < 1) {
                            // 1분 미만이면 확인 다이얼로그 표시
                            showFinishConfirmDialog.value = true
                        } else {
                            // 1분 이상이면 바로 종료
                            viewModel.finishWalking()
                            coroutineScope.launch {
                                viewModel.stopWalking()
                            }
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
            }
        }
    }

    // 백버튼 확인 다이얼로그
    if (showBackDialog.value) {
        WalkingWarningDialog(
            title = "산책 기록이 저장되지 않습니다",
            message = "이대로 종료하시면 진행 중인 \n" + "산책 기록이 모두 사라져요!",
            cancelButtonText = "중단하기",
            continueButtonText = "계속하기",
            cancelButtonTextColor = SemanticColor.textBorderSecondary,
            cancelButtonColor = SemanticColor.buttonPrimaryDisabled,
            cancelButtonBorderColor = SemanticColor.buttonPrimaryDisabled,
            onDismiss = { showBackDialog.value = false },
            onCancel = {
                showBackDialog.value = false
                // 산책 취소 (추적 중단만 하고 세션 저장하지 않음)
                coroutineScope.launch {
                    viewModel.cancelWalking()
                }
                onNavigateBack()
            },
            onContinue = {
                showBackDialog.value = false
                // 산책 계속 진행
            })
    }

    // 산책 종료 확인 다이얼로그 (1분 미만 시)
    if (showFinishConfirmDialog.value) {
        WalkingWarningDialog(
            title = "산책 기록이 저장되지 않아요!",
            message = "1분 미만의 산책은 기록되지 않습니다.\n" + "정말로 산책을 끝내시겠습니까?",
            cancelButtonText = "취소",
            continueButtonText = "끝내기",
            cancelButtonTextColor = SemanticColor.textBorderPrimary,
            cancelButtonColor = SemanticColor.backgroundWhitePrimary,
            cancelButtonBorderColor = SemanticColor.buttonPrimaryDisabled,
            onDismiss = { showFinishConfirmDialog.value = false },
            onCancel = {
                showFinishConfirmDialog.value = false
                // 취소 - 아무것도 하지 않음
            },
            onContinue = {
                showFinishConfirmDialog.value = false
                // 산책 종료 (세션 저장하지 않음)
                coroutineScope.launch {
                    viewModel.cancelWalking()
                }
                onNavigateBack()
            })
    }
}

@Composable
private fun WalkingScreenContent(
    modifier: Modifier = Modifier,
    screenState: WalkingScreenState,
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onFinishClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
) {
    val walkingState = screenState.uiState as? WalkingUiState.Walking

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {

        /* ---------- Background ---------- */
        AsyncImage(
            model = screenState.character?.backgroundImageName,
            error = painterResource(R.drawable.bg_spring_full),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )

        /* ---------- 🔥 중앙 캐릭터 (절대 위치) ---------- */
        WalkitCharacter(
            modifier = Modifier.align(Alignment.Center),
            character = screenState.character,
            lottieJson = screenState.characterLottieJson
        )

        /* ---------- 상단 영역 ---------- */
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = screenState.uiState) {
                is WalkingUiState.Walking -> {
                    WalkitTimer(state.duration)
                    Spacer(Modifier.height(52.dp))
                    WalkitStepInfo(stepCount = state.stepCount)
                }

                is WalkingUiState.SessionSaved -> {
                    Spacer(Modifier.height(130.dp)) // ❗️얼마든지 커져도 OK
                    FinishWalkingText()
                }

                else -> Unit
            }
        }

        /* ---------- 하단 영역 ---------- */
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = screenState.uiState) {
                is WalkingUiState.Walking -> {
                    CurrentChanllgeGoal(
                        challengeCount = screenState.currentWeekGoalChallengeCount
                    )
                    Spacer(Modifier.height(36.dp))
                    WalkingActionButtonRow(
                        isPaused = state.isPaused, onClickPause = {
                            if (state.isPaused) onResumeClick() else onPauseClick()
                        }, onClickFinish = onFinishClick
                    )
                }

                is WalkingUiState.SessionSaved -> {
                    CtaWrapper(onClick = onNextClick)
                }

                else -> Unit
            }
        }
    }
}


@Composable
fun WalkitCharacter(
    modifier: Modifier = Modifier, character: Character?, lottieJson: String? = null
) {
    // ⭐️ 캐릭터가 차지하는 "고정 레이아웃 박스"
    Box(
        modifier = modifier.size(200.dp), contentAlignment = Alignment.Center
    ) {

        when {
            lottieJson != null && character != null -> {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.JsonString(lottieJson)
                )

                LottieAnimation(
                    composition = composition, modifier = Modifier
                        .fillMaxSize()      // 🔥 박스에 맞춤
                        .scale(0.86f), iterations = Int.MAX_VALUE
                )
            }

            else -> {
                // 🔥 로딩도 동일 박스 안에서 중앙 정렬
                CustomProgressIndicator(
                    size = ProgressIndicatorSize.Medium
                )
            }
        }
    }
}

@Composable
fun WalkitTimer(duration: Long) {
    Row(
        modifier = Modifier
            .background(
                color = Color(0x1A000000), shape = RoundedCornerShape(24.dp)
            )
            .padding(
                horizontal = 10.dp, vertical = 4.dp
            )
    ) {
        Image(painter = painterResource(R.drawable.ic_info_timer), contentDescription = "timer")
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatToHoursMinutesSeconds(duration),
            style = MaterialTheme.walkItTypography.bodyXL.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderPrimaryInverse
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
            text = "현재 걸음 수 ",

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
    modifier: Modifier = Modifier, onClick: () -> Unit, enabled: Boolean = true
) { // PostWalkingEmotionSelect로 이동
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CtaButton(
            text = "감정 기록하기",
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
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "산책 종료",

            // heading L/semibold
            style = MaterialTheme.walkItTypography.headingL.copy(
                fontWeight = FontWeight.SemiBold, lineHeight = 42.sp
            ),
            color = SemanticColor.textBorderPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "산책 후 감정을 기록하시겠습니까?",
            // body M/regular
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = FontWeight.Normal
            ), color = SemanticColor.textBorderSecondary
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
            modifier = Modifier.size(width = 15.dp, height = 15.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)          // 꼭대기
                lineTo(0f, size.height)             // 왼쪽
                lineTo(size.width, size.height)     // 오른쪽
                close()
            }
            drawPath(
                path = path, color = SemanticColor.stateYellowTertiary
            )
        }

        // 말풍선 본체
        Box(
            modifier = Modifier
                .background(
                    color = SemanticColor.stateYellowTertiary, shape = RoundedCornerShape(12.dp)
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


