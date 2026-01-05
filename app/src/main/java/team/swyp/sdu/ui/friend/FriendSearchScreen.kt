package team.swyp.sdu.ui.friend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.FollowStatus
import team.swyp.sdu.ui.friend.FriendViewModel
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.SearchBar
import team.swyp.sdu.ui.friend.component.FriendCard
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

@Composable
fun FriendSearchScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, FollowStatus) -> Unit,
    viewModel: FriendViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()

    Column(modifier = modifier
        .fillMaxSize()
        .background(SemanticColor.backgroundWhitePrimary)) {
        AppHeader(title = "친구 찾기", onNavigateBack = {
            viewModel.clearQuery()
            onNavigateBack()
        })

        // 검색 바 (서버 검색)
        SearchBar(
            query = query,
            onQueryChange = viewModel::updateQuery,
            onClear = {
                viewModel.clearQuery()
            },
            onSearch = {
                val trimmedQuery = query.trim()
                if (trimmedQuery.isNotBlank()) {
                    viewModel.searchUser(trimmedQuery)
                }
            },
            placeholder = "친구의 닉네임을 검색해보세요.",
            textColor = SemanticColor.textBorderSecondary,
            borderColor = SemanticColor.textBorderSecondary,
            iconColor = SemanticColor.iconGrey,
            backgroundColor = SemanticColor.backgroundWhitePrimary,
            modifier = Modifier.padding(16.dp)
        )

        // 검색 결과 화면 표시
        SearchResultScreen(
            searchUiState = searchUiState,
            isFollowing = isFollowing,
            onNavigateToDetail = { nickName, followStatus ->
                onNavigateToDetail(nickName, followStatus)
            },
            onFollowClick = {
                // 검색 결과가 Success 상태일 때만 팔로우 가능
                if (searchUiState is SearchUiState.Success && (searchUiState as SearchUiState.Success).results.isNotEmpty()) {
                    viewModel.followUser((searchUiState as SearchUiState.Success).results.first().nickname)
                }
            },
        )
    }


}


/**
 * 검색 결과 화면
 */
@Composable
private fun SearchResultScreen(
    searchUiState: SearchUiState,
    isFollowing: Boolean,
    onNavigateToDetail: (String, FollowStatus) -> Unit,
    onFollowClick: () -> Unit,
) {
    Surface(
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (searchUiState) {
            SearchUiState.Idle -> {
                // 검색어 입력 대기 상태
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    // 빈 상태 표시 (필요시 추가)
                }
            }

            SearchUiState.Loading -> {
                // 로딩 상태
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CustomProgressIndicator()
                }
            }

            is SearchUiState.Success -> {
                // 성공 상태 - LazyColumn 사용
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchUiState.results) { result ->
                        FriendCard(
                            nickname = result.nickname,
                            imageName = result.imageName,
                            followStatus = result.followStatus,
                            onFollowClick = onFollowClick,
                            onCardClick = onNavigateToDetail,
                            enabled = !isFollowing,
                        )
                    }
                }
            }

            is SearchUiState.Error -> {
                // 에러 상태 - 전체 화면 표시
                FriendSearchEmptyScreen()
            }
        }
    }
}

@Composable
fun FriendSearchEmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = SemanticColor.backgroundWhiteSecondary,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 40.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_face_default),
                contentDescription = null
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "검색 결과가 없어요",
                // body XL/semibold
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary
            )
            Text(
                text = "다른 검색어를 입력하세요",
                // body S/medium
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary
            )
        }
    }
}

@Preview
@Composable
fun FriendSearchEmptyScreenPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        FriendSearchEmptyScreen()
    }
}
