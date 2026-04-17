package swyp.team.walkit.ui.interactivemap.friendwalk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.interactivemap.bottomtab.FriendPinBottomTab

/**
 * 친구 산책 전체 화면
 *
 * 지도에서 친구 핀 클릭 시 네비게이션으로 진입한다.
 * 백버튼으로 이전 지도 화면으로 복귀한다.
 */
@Composable
fun FriendWalkRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FriendWalkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FriendWalkScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleLike = viewModel::toggleLike,
        modifier = modifier,
    )
}

@Composable
fun FriendWalkScreen(
    uiState: FriendWalkUiState,
    onNavigateBack: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppHeader(
                title = "",
                showBackButton = true,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                    )
                }

                uiState.walkRecord != null -> {
                    FriendPinBottomTab(
                        record = null,
                        latestWalkRecord = uiState.walkRecord,
                        isLoadingWalkRecord = false,
                        likeState = uiState.likeState,
                        onToggleLike = onToggleLike,
                    )
                }
            }
        }
    }
}
