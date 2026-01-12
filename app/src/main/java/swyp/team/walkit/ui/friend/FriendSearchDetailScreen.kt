package swyp.team.walkit.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.FollowStatus
import timber.log.Timber
import swyp.team.walkit.domain.model.FollowerWalkRecord
import swyp.team.walkit.domain.model.UserSummary
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.GradeBadge
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import swyp.team.walkit.ui.components.SummaryUnit
import swyp.team.walkit.ui.components.WalkingSummaryCard
import swyp.team.walkit.ui.mypage.component.MyPageStatsSection
import swyp.team.walkit.ui.record.friendrecord.component.FriendRecordMoreMenu
import swyp.team.walkit.ui.theme.GradientUtils
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 친구 검색 상세 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 *
 * @param nickname 팔로워 닉네임 (null이면 내 최근 정보 조회)
 * @param lat 위도 (선택사항)
 * @param lon 경도 (선택사항)
 * @param onNavigateBack 뒤로가기 콜백
 */
@Composable
fun FriendSearchDetailRoute(
    nickname: String? = null,
    followStatusString: String? = null,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FriendSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val followStatus by viewModel.followStatus.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()

    // 화면 진입 시 데이터 로드
    LaunchedEffect(nickname, followStatusString) {
        Timber.d("FriendSearchDetailRoute: LaunchedEffect with nickname=$nickname, followStatusString=$followStatusString")

        // 네비게이션에서 전달받은 followStatus 설정
        if (followStatusString != null) {
            try {
                val followStatus = FollowStatus.valueOf(followStatusString)
                viewModel.setFollowStatus(followStatus)
                Timber.d("FriendSearchDetailRoute: set followStatus from navigation: $followStatus")
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Invalid followStatusString: $followStatusString")
                viewModel.setFollowStatus(FollowStatus.EMPTY)
            }
        }

        if (nickname != null) {
            viewModel.loadFollowerWalkRecord(nickname)
        } else {
            Timber.w("FriendSearchDetailRoute: nickname is null!")
        }
    }

    FriendSearchDetailScreen(
        uiState = uiState,
        followStatus = followStatus,
        isFollowing = isFollowing,
        onNavigateBack = onNavigateBack,
        onRequestFollow = {
            Timber.d("FriendSearchDetailRoute: onRequestFollow called, nickname=$nickname")
            if (nickname != null) {
                viewModel.followUser(nickname)
            } else {
                Timber.w("FriendSearchDetailRoute: onRequestFollow called but nickname is null")
            }
        },
        modifier = modifier,
    )
}

/**
 * 친구 검색 상세 화면
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 *
 * @param uiState 화면 상태
 * @param onNavigateBack 뒤로가기 콜백
 */
@Composable
fun FriendSearchDetailScreen(
    uiState: FriendSearchUiState,
    followStatus: FollowStatus,
    isFollowing: Boolean,
    onNavigateBack: () -> Unit = {},
    onRequestFollow: () -> Unit = {},
    modifier: Modifier = Modifier,
) {

    when (val state = uiState) {
        is FriendSearchUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
            }
        }

        is FriendSearchUiState.Success -> {
            FriendSearchDetailScreenContent(
                modifier = modifier,
                data = uiState.data,
                followStatus = followStatus,
                isFollowing = isFollowing,
                processedLottieJson = uiState.processedLottieJson, // Lottie JSON 전달
                onRequestFollow = onRequestFollow,
                onNavigateBack = onNavigateBack,
            )
        }

        is FriendSearchUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.message ?: "오류가 발생했습니다",
                )
            }
        }
    }
}

@Composable
fun FriendSearchDetailScreenContent(
    modifier: Modifier = Modifier,
    data: UserSummary,
    followStatus: FollowStatus,
    isFollowing: Boolean,
    processedLottieJson: String? = null, // Lottie JSON 추가
    onRequestFollow: () -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    var isMoreMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier
        .fillMaxSize()
        .background(SemanticColor.backgroundWhiteSecondary)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(263f / 375f)
        ) {
            // 배경 이미지
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data.character.backgroundImageName).build(),
                contentDescription = "배경",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // 콘텐츠 오버레이
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // 헤더
                AppHeader(
                    title = "",
                    onNavigateBack = onNavigateBack,
                    rightAction = {
                        Box {
                            IconButton(
                                onClick = { isMoreMenuExpanded = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = SemanticColor.textBorderPrimaryInverse,
                                )
                            }

                            FriendRecordMoreMenu(
                                expanded = isMoreMenuExpanded,
                                onDismiss = { isMoreMenuExpanded = false },
                                onBlockClick = {
                                    isMoreMenuExpanded = false
                                },
                            )
                        }
                    },
                )

                Spacer(modifier = Modifier.weight(1f))

                // 사용자 정보 섹션
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Lottie 캐릭터 표시
                    processedLottieJson?.let { lottieJson ->
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.JsonString(lottieJson)
                        )

                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier
                                .size(200.dp)
                                .scale(0.86f)
                                .offset(y = 20.dp),
                        )
                    } ?: run {
                        // Lottie가 없을 경우 기존 AsyncImage 사용 (fallback)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(data.character.characterImageName).build(),
                            contentDescription = "캐릭터",
                            modifier = Modifier.size(120.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GradientUtils.fadeToGray())
                            .padding(horizontal = 20.dp, vertical = 26.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 등급 배지

                        Row {
                            GradeBadge(
                                grade = data.character.grade,
                                level = data.character.level
                            )
                            Spacer(Modifier.width(8.dp))

                            // 닉네임
                            Text(
                                text = data.character.nickName ?: "닉네임 없음",
                                style = MaterialTheme.walkItTypography.headingM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = SemanticColor.textBorderPrimaryInverse,
                            )
                        }

                        // 팔로우 버튼
                        val (buttonText, buttonColor, isEnabled) = when (followStatus) {
                            FollowStatus.EMPTY -> Triple(
                                "팔로우",
                                SemanticColor.buttonPrimaryDefault,
                                true
                            )

                            FollowStatus.PENDING -> Triple(
                                "요청중",
                                SemanticColor.iconDisabled,
                                false
                            )

                            FollowStatus.ACCEPTED -> Triple(
                                "친구",
                                SemanticColor.buttonPrimaryDisabled,
                                false
                            )

                            FollowStatus.REJECTED -> Triple(
                                "거절됨",
                                SemanticColor.buttonPrimaryDisabled,
                                false
                            )

                            FollowStatus.MYSELF -> Triple(
                                "본인",
                                SemanticColor.buttonPrimaryDisabled,
                                false
                            )
                        }

                        if (followStatus != FollowStatus.ACCEPTED) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = buttonColor,
                                        shape = RoundedCornerShape(size = 8.dp),
                                    )
                                    .clickable(enabled = isEnabled && !isFollowing) {
                                        Timber.d("FriendSearchDetailScreenContent: follow button clicked, followStatus=$followStatus, isFollowing=$isFollowing")
                                        onRequestFollow()
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                            ) {
                                Text(
                                    text = if (isFollowing) "요청중..." else buttonText,
                                    style = MaterialTheme.walkItTypography.bodyS.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = if (isEnabled) SemanticColor.textBorderPrimaryInverse
                                    else SemanticColor.textBorderPrimaryInverse,
                                )
                            }
                        }

                    }
                }

            }
        }

        // 요약 카드 섹션
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            MyPageStatsSection(
                leftLabel = "누적 산책 횟수",
                leftValue = data.walkSummary.totalWalkCount,
                leftUnit = "회",
                rightLabel = "누적 산책 시간",
                rightValue = data.walkSummary.totalWalkTimeMillis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendSearchDetailScreenContentSeedGradePreview() {
    WalkItTheme {
        FriendSearchDetailScreenContent(
            data = swyp.team.walkit.domain.model.UserSummary(
                character = swyp.team.walkit.domain.model.Character(
                    headImage = CharacterImage("head_default", "TOP"),
                    bodyImage = CharacterImage("body_default", null),
                    feetImage = CharacterImage("feet_default", null),
                    characterImageName = "character_default",
                    backgroundImageName = "background_forest",
                    level = 1,
                    grade = swyp.team.walkit.domain.model.Grade.SEED,
                    nickName = "새싹사용자"
                ), walkSummary = swyp.team.walkit.domain.model.WalkSummary(
                    totalWalkCount = 5, totalWalkTimeMillis = 1800000L // 30분
                )
            ),
            followStatus = FollowStatus.EMPTY,
            isFollowing = false,
            processedLottieJson = null, // Preview에서는 null
            onNavigateBack = {},
            onRequestFollow = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendSearchDetailScreenContentSproutGradePreview() {
    WalkItTheme {
        FriendSearchDetailScreenContent(
            data = swyp.team.walkit.domain.model.UserSummary(
                character = swyp.team.walkit.domain.model.Character(
                    headImage = CharacterImage("head_sprout", "DECOR"),
                    bodyImage = CharacterImage("body_sprout", null),
                    feetImage = CharacterImage("feet_sprout", null),
                    characterImageName = "character_sprout",
                    backgroundImageName = "background_mountain",
                    level = 15,
                    grade = swyp.team.walkit.domain.model.Grade.SPROUT,
                    nickName = "성장중인나무"
                ), walkSummary = swyp.team.walkit.domain.model.WalkSummary(
                    totalWalkCount = 25, totalWalkTimeMillis = 7200000L // 2시간
                )
            ),
            followStatus = FollowStatus.EMPTY,
            isFollowing = false,
            processedLottieJson = null, // Preview에서는 null
            onNavigateBack = {},
            onRequestFollow = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendSearchDetailScreenContentTreeGradePreview() {
    WalkItTheme {
        FriendSearchDetailScreenContent(
            data = swyp.team.walkit.domain.model.UserSummary(
                character = swyp.team.walkit.domain.model.Character(
                    headImage = CharacterImage("head_tree", "TOP"),
                    bodyImage = CharacterImage("body_tree", null),
                    feetImage = CharacterImage("feet_tree", null),
                    characterImageName = "character_tree",
                    backgroundImageName = "background_ocean",
                    level = 50,
                    grade = swyp.team.walkit.domain.model.Grade.TREE,
                    nickName = "완성된나무"
                ), walkSummary = swyp.team.walkit.domain.model.WalkSummary(
                    totalWalkCount = 100, totalWalkTimeMillis = 36000000L // 10시간
                )
            ),
            followStatus = FollowStatus.EMPTY,
            isFollowing = false,
            processedLottieJson = null, // Preview에서는 null
            onNavigateBack = {},
            onRequestFollow = {})
    }
}
