package team.swyp.sdu.ui.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.NotificationPermissionViewModel
import team.swyp.sdu.presentation.viewmodel.NotificationPermissionUiState

/**
 * 알림 권한 안내 다이얼로그
 *
 * 홈 화면에서 표시되는 알림 권한 요청 다이얼로그입니다.
 */
@Composable
fun NotificationPermissionDialog(
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState) {
        is NotificationPermissionUiState.ShouldShowDialog -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = {
                    Text(
                        text = "알림 권한",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "알림을 받으면 좋은 이유:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "• 목표 달성 알림을 받을 수 있습니다\n" +
                                "• 새로운 미션이 등장하면 알려드립니다\n" +
                                "• 친구 요청을 확인할 수 있습니다",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.requestPermission()
                            onRequestPermission()
                        },
                    ) {
                        Text("알림 켜기")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::skipPermission) {
                        Text("나중에")
                    }
                },
            )
        }
        else -> {
            // 다이얼로그 표시 안 함
        }
    }
}

