package team.swyp.sdu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.presentation.viewmodel.NotificationSettingsUiState
import team.swyp.sdu.presentation.viewmodel.NotificationSettingsViewModel
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.SectionCard
import team.swyp.sdu.ui.components.ToggleMenuItem
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 알림 설정 Route
 *
 * ViewModel을 주입받고 상태를 수집하여 Screen에 전달합니다.
 */
@Composable
fun NotificationSettingsRoute(
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationSettingsScreen(
        uiState = uiState,
        onNotificationEnabledChange = viewModel::setNotificationEnabled,
        onGoalNotificationEnabledChange = viewModel::setGoalNotificationEnabled,
        onNewMissionNotificationEnabledChange = viewModel::setNewMissionNotificationEnabled,
        onFriendRequestNotificationEnabledChange = viewModel::setFriendRequestNotificationEnabled,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * 알림 설정 Screen
 *
 * 순수 UI 컴포넌트로, 상태와 콜백만 받습니다.
 * Preview에서 ViewModel 없이도 작동합니다.
 *
 * @param uiState 알림 설정 UI 상태
 * @param onNotificationEnabledChange 전체 알림 설정 변경 핸들러
 * @param onGoalNotificationEnabledChange 목표 알림 설정 변경 핸들러
 * @param onNewMissionNotificationEnabledChange 신규 미션 알림 설정 변경 핸들러
 * @param onFriendRequestNotificationEnabledChange 친구 요청 알림 설정 변경 핸들러
 * @param onNavigateBack 뒤로가기 핸들러
 */
@Composable
fun NotificationSettingsScreen(
    uiState: NotificationSettingsUiState,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onGoalNotificationEnabledChange: (Boolean) -> Unit,
    onNewMissionNotificationEnabledChange: (Boolean) -> Unit,
    onFriendRequestNotificationEnabledChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppHeader(
            title = "알림 설정",
            onNavigateBack = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 전체 알림 섹션
            SectionCard {
                Text(
                    text = "전체 알림",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Grey10,
                )
                Spacer(Modifier.height(8.dp))

                ToggleMenuItem(
                    title = "알림 수신",
                    checked = uiState.notificationEnabled,
                    onCheckedChange = onNotificationEnabledChange,
                )
            }

            // 목표 알림 섹션
            SectionCard {
                Text(
                    text = "목표",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Grey10,
                )
                Spacer(Modifier.height(8.dp))

                ToggleMenuItem(
                    title = "목표 알림",
                    checked = uiState.goalNotificationEnabled,
                    onCheckedChange = onGoalNotificationEnabledChange,
                )
            }

            // 미션 알림 섹션
            SectionCard {
                Text(
                    text = "미션",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Grey10,
                )
                Spacer(Modifier.height(8.dp))

                ToggleMenuItem(
                    title = "신규 미션",
                    checked = uiState.newMissionNotificationEnabled,
                    onCheckedChange = onNewMissionNotificationEnabledChange,
                )
            }

            // 친구 알림 섹션
            SectionCard {
                Text(
                    text = "친구",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Grey10,
                )
                Spacer(Modifier.height(8.dp))

                ToggleMenuItem(
                    title = "신규 요청",
                    checked = uiState.friendRequestNotificationEnabled,
                    onCheckedChange = onFriendRequestNotificationEnabledChange,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotificationSettingsScreenPreview() {
    WalkItTheme {
        NotificationSettingsScreen(
            uiState = NotificationSettingsUiState(
                notificationEnabled = true,
                goalNotificationEnabled = true,
                newMissionNotificationEnabled = false,
                friendRequestNotificationEnabled = true,
            ),
            onNotificationEnabledChange = {},
            onGoalNotificationEnabledChange = {},
            onNewMissionNotificationEnabledChange = {},
            onFriendRequestNotificationEnabledChange = {},
            onNavigateBack = {},
        )
    }
}

