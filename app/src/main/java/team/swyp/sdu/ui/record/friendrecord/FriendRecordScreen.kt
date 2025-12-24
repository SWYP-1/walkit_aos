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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.components.SummaryUnit
import team.swyp.sdu.ui.components.WalkingSummaryCard
import team.swyp.sdu.ui.components.formatStepCount
import team.swyp.sdu.ui.home.components.WalkProgressBar
import team.swyp.sdu.ui.record.friendrecord.component.FriendRecordMoreMenu
import team.swyp.sdu.ui.record.friendrecord.component.LikeButton
import team.swyp.sdu.ui.record.friendrecord.LikeUiState
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 친구 기록 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 *
 * @param nickname 팔로워 닉네임
 * @param onNavigateBack 뒤로가기 콜백
 */
@Composable
fun FriendRecordRoute(
    nickname: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendRecordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면 진입 시 데이터 로드
    LaunchedEffect(nickname) {
        viewModel.loadFollowerWalkRecord(nickname)
    }

    FriendRecordScreen(
        uiState = uiState,
        onLikeClick = viewModel::toggleLike,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
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
fun FriendRecordScreen(
    uiState: FriendRecordUiState,
    modifier: Modifier = Modifier,
    onLikeClick: () -> Unit = {},
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
    ) {
        // 캐릭터 정보
        CharacterInfoSection(
            character = data.character,
            walkProgressPercentage = data.walkProgressPercentage ?: "0"
        )
        Spacer(Modifier.height(16.dp))

        WalkingSummaryCard(
            leftLabel = "걸음 수",
            leftValue = formatStepCount(data.stepCount),
            leftUnit = SummaryUnit.Step("걸음"),
            rightLabel = "이동거리",
            rightUnit = SummaryUnit.Distance(data.totalDistance.toFloat()),
            header = {
                data.createdDate?.let { date ->
                    Text(
                        text = date,
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

/**
 * 캐릭터 정보 섹션
 */
@Composable
private fun CharacterInfoSection(
    character: Character,
    walkProgressPercentage: String = "",
    onClickMore: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = SemanticColor.textBorderSecondaryInverse,
                shape = RoundedCornerShape(size = 12.dp)
            )
            .fillMaxWidth()
            .aspectRatio(343f / 404f)
            .padding(16.dp)
    ) {

        // 1. 배경
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(character.backgroundImageName)
                .build(),
            contentDescription = "배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    // Grade 배지
                    GradeBadge(
                        grade = character.grade,
                    )
                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "닉네임",
                        // heading M/semibold
                        style = MaterialTheme.walkItTypography.headingM.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary
                    )
                }
                Box() {
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
                        onDismiss = { showMenu = false },
                        onDeleteClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                    )
                }

            }
            Spacer(Modifier.height(4.dp))

            Row {
                Text(
                    text = "누적 목표 달성",

                    // body S/regular
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = SemanticColor.textBorderPrimary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "00",
                    // body S/medium
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.textBorderGreenPrimary
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "일",
                    // body S/medium
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.textBorderPrimary

                )
            }
            Spacer(Modifier.weight(1f))


            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "목표 달성률",

                    // body M/medium
                    style = MaterialTheme.walkItTypography.bodyM.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.backgroundWhitePrimary
                )
                Text(
                    text = "${walkProgressPercentage}%",
                    // body M/medium
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

@Preview(showBackground = true)
@Composable
private fun FriendRecordScreenPreview() {
    WalkItTheme {
        FriendRecordScreen(
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
                    totalDistance = 6500,
                    createdDate = "2024-01-15",
                ),
                walkId = 1L,
                like = LikeUiState(count = 5, isLiked = false),
            ),
        )
    }
}

@Preview(showBackground = true, name = "좋아요 상태")
@Composable
private fun FriendRecordScreenLikedPreview() {
    WalkItTheme {
        FriendRecordScreen(
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
                    totalDistance = 6500,
                    createdDate = "2024-01-15",
                ),
                walkId = 1L,
                like = LikeUiState(count = 12, isLiked = true),
            ),
        )
    }
}

@Preview(showBackground = true, name = "로딩 상태")
@Composable
private fun FriendRecordScreenLoadingPreview() {
    WalkItTheme {
        FriendRecordScreen(
            uiState = FriendRecordUiState.Loading,
        )
    }
}

@Preview(showBackground = true, name = "에러 상태")
@Composable
private fun FriendRecordScreenErrorPreview() {
    WalkItTheme {
        FriendRecordScreen(
            uiState = FriendRecordUiState.Error(
                message = "데이터를 불러오는 중 오류가 발생했습니다",
            ),
        )
    }
}