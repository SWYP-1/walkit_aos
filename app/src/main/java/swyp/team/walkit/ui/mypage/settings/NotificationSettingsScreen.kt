package swyp.team.walkit.ui.mypage.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.CustomSwitch
import swyp.team.walkit.ui.components.InfoBanner
import swyp.team.walkit.ui.components.SectionCard
import swyp.team.walkit.ui.components.ToggleMenuItem
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.Grey2
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 알림 설정 Route
 *
 * ViewModel을 주입받고 상태를 수집하여 Screen에 전달합니다.
 */
@Composable
fun NotificationSettingsRoute(
    modifier : Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 시스템 뒤로가기 버튼 처리 (알림 설정 화면에서만 저장)
    BackHandler {
        viewModel.saveSettings()
        onNavigateBack()
    }

    // 패턴 B: Ready 상태일 때만 뷰 표시
    if (uiState is NotificationSettingsUiState.Ready) {
        NotificationSettingsScreen(
            modifier = modifier,
            uiState = uiState as NotificationSettingsUiState.Ready,
            onNotificationEnabledChange = viewModel::setNotificationEnabled,
            onGoalNotificationEnabledChange = viewModel::setGoalNotificationEnabled,
            onMissionNotificationEnabledChange = viewModel::setMissionNotificationEnabled,
            onFriendNotificationEnabledChange = viewModel::setFriendNotificationEnabled,
            onMarketingPushEnabledChange = viewModel::setMarketingPushEnabled,
            onGetNotificationSettings = viewModel::getNotificationSettings,
            onNavigateBack = {
                // AppHeader 뒤로가기 버튼 클릭 시에도 저장
                viewModel.saveSettings()
                onNavigateBack()
            },
        )
    }
}

/**
 * 알림 설정 Screen
 *
 * 순수 UI 컴포넌트로, 상태와 콜백만 받습니다.
 * Preview에서 ViewModel 없이도 작동합니다.
 *
 * @param uiState 알림 설정 UI 상태 (Ready 상태만 받음)
 * @param onNotificationEnabledChange 전체 알림 설정 변경 핸들러
 * @param onGoalNotificationEnabledChange 목표 알림 설정 변경 핸들러
 * @param onMissionNotificationEnabledChange 미션 알림 설정 변경 핸들러
 * @param onFriendNotificationEnabledChange 친구 알림 설정 변경 핸들러
 * @param onMarketingPushEnabledChange 마케팅 푸시 동의 설정 변경 핸들러
 * @param onGetNotificationSettings 알림 설정 GET 테스트 핸들러
 * @param onNavigateBack 뒤로가기 핸들러
 */
@Composable
fun NotificationSettingsScreen(
    uiState: NotificationSettingsUiState.Ready,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onGoalNotificationEnabledChange: (Boolean) -> Unit,
    onMissionNotificationEnabledChange: (Boolean) -> Unit,
    onFriendNotificationEnabledChange: (Boolean) -> Unit,
    onMarketingPushEnabledChange: (Boolean) -> Unit,
    onGetNotificationSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        AppHeader(
            title = "알림 설정",
            onNavigateBack = onNavigateBack,
        )


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            InfoBanner(
                title = "기기 알람을 켜주세요",
                description = "정보 알림을 받기 위해 기기 알람을 켜주세요"
            )

            Spacer(Modifier.height(20.dp))

            // 전체 알림 섹션 
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Grey2, // color/background/white-secondary
                        shape = RoundedCornerShape(8.dp), // radius/8px
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "전체 알림",
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Grey10,
                )

                CustomSwitch(
                    checked = uiState.notificationEnabled,
                    onCheckedChange = onNotificationEnabledChange,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 알림 설정 섹션 (목표 알림, 신규 미션, 신규 요청)

            val verticalPadding = 12.dp
            val horizontalPadding = 16.dp
            SectionCard(
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding
                    )
                ) {
                    Text(
                        text = "알림 설정",
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
                    Spacer(Modifier.height(8.dp))

                    ToggleMenuItem(
                        title = "신규 미션",
                        checked = uiState.missionNotificationEnabled,
                        onCheckedChange = onMissionNotificationEnabledChange,
                    )
                    Spacer(Modifier.height(8.dp))

                    ToggleMenuItem(
                        title = "신규 요청",
                        checked = uiState.friendNotificationEnabled,
                        onCheckedChange = onFriendNotificationEnabledChange,
                    )
                }


            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard {
                Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
                    ToggleMenuItem(
                        title = "마케팅 푸쉬 동의",
                        checked = uiState.marketingPushEnabled,
                        onCheckedChange = onMarketingPushEnabledChange,
                    )
                }

            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun NotificationSettingsScreenPreview() {
    WalkItTheme {
        NotificationSettingsScreen(
            uiState = NotificationSettingsUiState.Ready(
                notificationEnabled = true,
                goalNotificationEnabled = true,
                missionNotificationEnabled = false,
                friendNotificationEnabled = true,
                marketingPushEnabled = false,
            ),
            onNotificationEnabledChange = {},
            onGoalNotificationEnabledChange = {},
            onMissionNotificationEnabledChange = {},
            onFriendNotificationEnabledChange = {},
            onMarketingPushEnabledChange = {},
            onGetNotificationSettings = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun NotificationSettingsRoutePreview() {
    WalkItTheme {
        // Route는 ViewModel을 사용하므로 Screen만 Preview
        NotificationSettingsScreen(
            uiState = NotificationSettingsUiState.Ready(
                notificationEnabled = true,
                goalNotificationEnabled = true,
                missionNotificationEnabled = false,
                friendNotificationEnabled = true,
                marketingPushEnabled = false,
            ),
            onNotificationEnabledChange = {},
            onGoalNotificationEnabledChange = {},
            onMissionNotificationEnabledChange = {},
            onFriendNotificationEnabledChange = {},
            onMarketingPushEnabledChange = {},
            onGetNotificationSettings = {},
            onNavigateBack = {},
        )
    }
}

