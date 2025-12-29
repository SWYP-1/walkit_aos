package team.swyp.sdu.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.domain.model.FollowerWalkRecord
import team.swyp.sdu.domain.model.UserSummary
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.GradeBadge
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.components.SummaryUnit
import team.swyp.sdu.ui.components.WalkingSummaryCard
import team.swyp.sdu.ui.record.friendrecord.component.FriendRecordMoreMenu
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography

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
    lat: Double? = null,
    lon: Double? = null,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FriendSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면 진입 시 데이터 로드
    LaunchedEffect(nickname) {
        if (nickname != null) {
            viewModel.loadFollowerWalkRecord(nickname)
        }
    }

    FriendSearchDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
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
    onNavigateBack: () -> Unit = {},
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
                data = uiState.data,
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
    onNavigateBack: () -> Unit = {},
) {
    var isMoreMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            // 배경 이미지
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data.character.backgroundImageName)
                    .build(),
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

                Spacer(modifier = Modifier.height(16.dp))

                // 사용자 정보 섹션
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 등급 배지
                    GradeBadge(
                        grade = data.character.grade
                    )

                    // 닉네임
                    Text(
                        text = data.character.nickName ?: "닉네임 없음",
                        style = MaterialTheme.walkItTypography.headingM.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SemanticColor.textBorderPrimaryInverse,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 팔로우 버튼
                    Box(
                        modifier = Modifier
                            .background(
                                color = SemanticColor.buttonPrimaryDefault,
                                shape = RoundedCornerShape(size = 8.dp),
                            )
                            .clickable {
                                // TODO: 팔로우 기능 구현
                            }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                    ) {
                        Text(
                            text = "팔로우",
                            style = MaterialTheme.walkItTypography.bodyS.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SemanticColor.textBorderPrimaryInverse,
                        )
                    }
                }
            }
        }

        // 요약 카드 섹션
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            WalkingSummaryCard(
                leftLabel = "누적 산책 횟수",
                leftValue = data.walkSummary.totalWalkCount.toString(),
                leftUnit = SummaryUnit.Step("회"),
                rightLabel = "누적 산책 시간",
                rightUnit = SummaryUnit.Time( data.walkSummary.totalWalkTimeMillis),
            )
        }
    }
}