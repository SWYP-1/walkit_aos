package team.swyp.sdu.ui.record.friendrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.theme.GradientUtils
import team.swyp.sdu.ui.components.SummaryUnit
import team.swyp.sdu.ui.components.WalkingSummaryCard
import team.swyp.sdu.ui.home.components.WalkProgressBar
import team.swyp.sdu.ui.record.friendrecord.component.FriendRecordMoreMenu
import team.swyp.sdu.ui.record.friendrecord.component.LikeButton
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils.formatIsoToKoreanDate
import team.swyp.sdu.utils.FormatUtils.formatStepCount

/**
 * 친구 기록 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 *
 * @param nickname 팔로워 닉네임
 * @param onNavigateBack 뒤로가기 콜백
 */
@Composable
fun FriendRecordScreen(
    nickname: String,
    onNavigateBack: () -> Unit,
    onBlockUser: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    // 화면 진입 시 데이터 로드
    LaunchedEffect(nickname) {
        viewModel.loadFollowerWalkRecord(nickname)
    }
    FriendRecordScreenContent(
        uiState = uiState,
        onBlockUser = onBlockUser,
        onLikeClick = viewModel::toggleLike,
        onClickMore = { showMenu = true },
        showMenu = showMenu,
        onDismissMenu = { showMenu = false },
        modifier = modifier,
    )
}

/**
 * 친구 기록 화면
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 *
 * @param uiState 화면 상태
 * @param onLikeClick 좋아요 클릭 콜백
 */
@Composable
fun FriendRecordScreenContent(
    uiState: FriendRecordUiState,
    modifier: Modifier = Modifier,
    onBlockUser: (String) -> Unit = {},
    onLikeClick: () -> Unit = {},
    onClickMore: () -> Unit = {},
    showMenu: Boolean = false,
    onDismissMenu: () -> Unit = {},
) {
    when (val state = uiState) {
        is FriendRecordUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
            }
        }

        is FriendRecordUiState.Success -> {
            FriendRecordContent(
                data = state.data,
                likeState = state.like,
                onLikeClick = onLikeClick,
                onBlockUser = onBlockUser,
                onClickMore = onClickMore,
                showMenu = showMenu,
                onDismissMenu = onDismissMenu,
                modifier = modifier,
            )
        }

        is FriendRecordUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "오류가 발생했습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = SemanticColor.textBorderPrimary,
                    )
                    Text(
                        text = state.message ?: "알 수 없는 오류",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SemanticColor.iconGrey,
                    )
                }
            }
        }
    }
}

/**
 * 친구 기록 내용 표시
 */
@Composable
private fun FriendRecordContent(
    data: FollowerWalkRecord,
    likeState: LikeUiState,
    onLikeClick: () -> Unit,
    onBlockUser: (String) -> Unit,
    onClickMore: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // 캐릭터 정보
        CharacterInfoSection(
            character = data.character,
            walkProgressPercentage = data.walkProgressPercentage ?: "0",
            onClickMore = onClickMore,
            onBlockUser = onBlockUser,
            showMenu = showMenu,
            onDismissMenu = onDismissMenu
        )
        Spacer(Modifier.height(16.dp))

        WalkingSummaryCard(
            leftLabel = "걸음 수",
            leftValue = formatStepCount(data.stepCount),
            leftUnit = SummaryUnit.Step("걸음"),
            rightLabel = "누적 산책 시간",
            rightUnit = SummaryUnit.Time(data.totalTime),
            header = {
                data.createdDate.let { date ->
                    Text(
                        text = formatIsoToKoreanDate(date),
                        // caption M/regular
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = SemanticColor.textBorderTertiary
                    )
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        // 좋아요 버튼
        LikeButton(
            state = likeState,
            onToggleLike = onLikeClick,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun CharacterInfoSection(
    character: Character,
    walkProgressPercentage: String = "",
    onClickMore: () -> Unit = {},
    onBlockUser: (String) -> Unit = {},
    showMenu: Boolean = false,
    onDismissMenu: () -> Unit = {},
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(343f / 404f)
    ) {
        // 1️⃣ 배경 이미지: Box 전체에 채움
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(character.backgroundImageName)
                .build(),
            contentDescription = "배경",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        // 2️⃣ 콘텐츠: padding + border 적용
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = SemanticColor.textBorderSecondaryInverse,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Top Row: Grade + Nickname + More 버튼
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradeBadge(grade = character.grade)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = character.nickName,
                            style = MaterialTheme.walkItTypography.headingM.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = SemanticColor.textBorderPrimary
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { onClickMore() },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_more),
                                contentDescription = "more"
                            )
                        }
                        FriendRecordMoreMenu(
                            expanded = showMenu,
                            onDismiss = onDismissMenu,
                            onBlockClick = {
                                onDismissMenu()
                                onBlockUser(character.nickName)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 누적 목표 달성
                Row {
                    Text(
                        text = "누적 목표 달성",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = SemanticColor.textBorderPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "00",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.textBorderGreenPrimary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "일",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.textBorderPrimary
                    )
                }

            }



            Spacer(Modifier.weight(1f))

            val color = Color(0xFF444444) // 부드러운 검정
            Column(
                modifier = Modifier.background(
                    GradientUtils.fadeToGray(color), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                ).padding(16.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "목표 달성률",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.backgroundWhitePrimary
                    )
                    Text(
                        text = "${walkProgressPercentage}%",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.backgroundWhitePrimary
                    )
                }

                Spacer(Modifier.height(6.5.dp))

                WalkProgressBar(
                    progressPercentage = walkProgressPercentage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun FriendRecordScreenPreview() {
    WalkItTheme {
        FriendRecordScreenContent(
            modifier = Modifier.background(Grey7),
            uiState = FriendRecordUiState.Success(
                data = FollowerWalkRecord(
                    character = Character(
                        nickName = "친구닉네임",
                        level = 5,
                        grade = Grade.SPROUT,
                        characterImageName = "character_01",
                    ),
                    walkProgressPercentage = "75",
                    stepCount = 8500,
                    totalDistance = 6500.0,
                    createdDate = "2024-01-15",
                    walkId = 0L
                ),
                like = LikeUiState(count = 5, isLiked = false),
            ),
        )
    }
}

@Preview(showBackground = true, name = "좋아요 상태")
@Composable
private fun FriendRecordScreenLikedPreview() {
    WalkItTheme {
        FriendRecordScreenContent(
            modifier = Modifier.background(Grey7),
            uiState = FriendRecordUiState.Success(
                data = FollowerWalkRecord(
                    character = Character(
                        nickName = "친구닉네임",
                        level = 5,
                        grade = Grade.SPROUT,
                        characterImageName = "character_01",
                    ),
                    walkProgressPercentage = "75",
                    stepCount = 8500,
                    totalDistance = 6500.0,
                    createdDate = "2024-01-15",
                    walkId = 0L
                ),
                like = LikeUiState(count = 12, isLiked = true),
            ),
        )
    }
}

@Preview(showBackground = true, name = "로딩 상태")
@Composable
private fun FriendRecordScreenLoadingPreview() {
    WalkItTheme {
        FriendRecordScreenContent(
            uiState = FriendRecordUiState.Loading,
        )
    }
}

@Preview(showBackground = true, name = "에러 상태")
@Composable
private fun FriendRecordScreenErrorPreview() {
    WalkItTheme {
        FriendRecordScreenContent(
            uiState = FriendRecordUiState.Error(
                message = "데이터를 불러오는 중 오류가 발생했습니다",
            ),
        )
    }
}