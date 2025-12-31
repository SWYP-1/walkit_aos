package team.swyp.sdu.ui.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.ui.friend.FriendViewModel
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.SearchBar
import team.swyp.sdu.ui.friend.component.FriendCard
import team.swyp.sdu.ui.theme.SemanticColor

@Composable
fun FriendSearchScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: FriendViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()

    Column(modifier = modifier.background(SemanticColor.backgroundWhitePrimary)) {
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
                if (searchUiState is SearchUiState.Success) {
                    viewModel.followUser((searchUiState as SearchUiState.Success).result.nickname)
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
    onNavigateToDetail: (String, String) -> Unit,
    onFollowClick: () -> Unit,
) {
    Surface(
        tonalElevation = 0.dp,
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        LazyColumn(

        ) {
            when (searchUiState) {
                SearchUiState.Idle -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
//                            Text("검색어를 입력해주세요")
                        }
                    }
                }

                SearchUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("검색 중...")
                        }
                    }
                }

                is SearchUiState.Success -> {
                    item {
                        FriendCard(
                            nickname = searchUiState.result.nickname,
                            imageName = searchUiState.result.imageName,
                            followStatus = searchUiState.result.followStatus,
                            onFollowClick = onFollowClick,
                            onCardClick = onNavigateToDetail,
                            enabled = !isFollowing,
                        )
                    }
                }

                is SearchUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(searchUiState.message)
                        }
                    }
                }
            }
        }
    }
}

