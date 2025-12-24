package team.swyp.sdu.ui.record.friend

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import team.swyp.sdu.core.Result
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.record.friendrecord.FriendRecordScreen
import team.swyp.sdu.ui.record.friendrecord.FriendRecordViewModel
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
 */
@Composable
fun FriendRecordScreen(
    uiState: Result<FollowerWalkRecord>,
    modifier: Modifier = Modifier,
) {
    when (val state = uiState) {
        is Result.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is Result.Success -> {
            FriendRecordContent(
                data = state.data,
                modifier = modifier,
            )
        }

        is Result.Error -> {
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 캐릭터 정보
        CharacterInfoSection(character = data.character)

        Spacer(modifier = Modifier.height(8.dp))

        // 산책 진행률
        if (data.walkProgressPercentage != null) {
            Text(
                text = "산책 진행률: ${data.walkProgressPercentage}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        // 걸음 수
        Text(
            text = "걸음 수: ${data.stepCount}걸음",
            style = MaterialTheme.typography.bodyMedium,
        )

        // 총 거리
        Text(
            text = "총 거리: ${data.totalDistance}m",
            style = MaterialTheme.typography.bodyMedium,
        )

        // 생성 날짜
        if (data.createdDate != null) {
            Text(
                text = "생성일: ${data.createdDate}",
                style = MaterialTheme.typography.bodySmall,
                color = SemanticColor.iconGrey,
            )
        }
    }
}

/**
 * 캐릭터 정보 섹션
 */
@Composable
private fun CharacterInfoSection(
    character: Character,
    onClickMore: () -> Unit = {},
) {

    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = SemanticColor.textBorderSecondaryInverse,
                shape = RoundedCornerShape(size = 12.dp)
            )
            .fillMaxWidth()
            .aspectRatio(343f / 404f)
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
            modifier = Modifier
        ) {

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    // Grade 배지
                    GradeBadge(
                        grade = character.grade,
                    )

                    Text(
                        text = "닉네임",
                        // heading M/semibold
                        style = MaterialTheme.walkItTypography.headingM.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = SemanticColor.textBorderPrimary
                    )
                }
                IconButton(
                    onClick = { onClickMore() },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_more),
                        contentDescription = "more"
                    )
                }

            }
        }
    }

}

@Preview(showBackground = true)
@Composable
private fun FriendRecordScreenPreview() {
    WalkItTheme {
        FriendRecordScreen(
            uiState = Result.Success(
                FollowerWalkRecord(
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
            ),
        )
    }
}

@Preview(showBackground = true, name = "로딩 상태")
@Composable
private fun FriendRecordScreenLoadingPreview() {
    WalkItTheme {
        FriendRecordScreen(
            uiState = Result.Loading,
        )
    }
}

@Preview(showBackground = true, name = "에러 상태")
@Composable
private fun FriendRecordScreenErrorPreview() {
    WalkItTheme {
        FriendRecordScreen(
            uiState = Result.Error(
                exception = Exception("네트워크 오류"),
                message = "데이터를 불러오는 중 오류가 발생했습니다",
            ),
        )
    }
}