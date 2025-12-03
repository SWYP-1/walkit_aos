package team.swyp.sdu.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.components.KakaoMapView

/**
 * 산책 결과 화면
 *
 * 산책 완료 후 결과를 표시하는 별도 화면입니다.
 * 지도에 경로를 표시합니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRouteDetail: (List<LocationPoint>) -> Unit = {},
    viewModel: WalkingViewModel = hiltViewModel(),
) {
    // ViewModel에서 세션 가져오기
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Completed 상태가 아니면 에러 처리
    val session =
        when (val state = uiState) {
            is team.swyp.sdu.presentation.viewmodel.WalkingUiState.Completed -> {
                state.session
            }

            else -> {
                // Completed 상태가 아니면 기본 세션 반환 (또는 에러 처리)
                WalkingSession(startTime = System.currentTimeMillis())
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("산책 완료") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 지도 뷰에 경로 표시
            // 지도는 스크롤 가능한 영역 밖에 배치하여 터치 이벤트가 제대로 전달되도록 함
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                // KakaoMapView로 경로 표시
                KakaoMapView(
                    locations = session.locations,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // 나머지 콘텐츠는 스크롤 가능하도록 별도 Column에 배치
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 다시 시작하기 버튼
                Button(
                    onClick = {
                        // ViewModel 초기화 후 Main 화면으로 이동
                        // Main 화면의 WalkingScreen에서 LaunchedEffect가 Completed 상태를 감지하고
                        // 이미 초기화되어 있으므로 추가 초기화는 불필요하지만, 명시적으로 호출
                        viewModel.reset()
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("다시 시작하기")
                }
            }
        }
    }
}
