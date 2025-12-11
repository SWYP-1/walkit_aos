package team.swyp.sdu.ui.walking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import team.swyp.sdu.presentation.viewmodel.WalkingSessionListUiState
import team.swyp.sdu.presentation.viewmodel.WalkingSessionListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingSessionListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRouteDetail: (List<LocationPoint>) -> Unit = {},
    viewModel: WalkingSessionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("산책 기록") },
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
        when (uiState) {
            is WalkingSessionListUiState.Success -> {
                val sessions = (uiState as WalkingSessionListUiState.Success).sessions
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sessions.forEach { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "세션 시작: ${session.startTime}")
                                Text(text = "걸음 수: ${session.stepCount}")
                                Text(text = "거리: ${session.totalDistance}")
                                Text(text = "시간: ${session.duration}")
                                Text(
                                    text = "상세 보기",
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            is WalkingSessionListUiState.Error -> {
                Text(
                    text = (uiState as WalkingSessionListUiState.Error).message,
                    modifier = Modifier.padding(paddingValues),
                )
            }

            is WalkingSessionListUiState.Loading -> {
                Text(
                    text = "로딩 중...",
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

