package team.swyp.sdu.ui.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.domain.model.AlarmType
import team.swyp.sdu.ui.alarm.component.AlarmListItem
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize

/**
 * 알람 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun AlarmScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AlarmScreenContent(
        alarms = alarms,
        isLoading = isLoading,
        onNavigateBack = onNavigateBack,
        onConfirmFollow = viewModel::confirmFollow,
        onDeleteAlarm = viewModel::deleteAlarm,
    )
}

/**
 * 알람 화면 Content
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 */
@Composable
private fun AlarmScreenContent(
    alarms: List<AlarmItem>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onConfirmFollow: (String) -> Unit,
    onDeleteAlarm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        AppHeader(
            title = "알림",
            onNavigateBack = onNavigateBack,
        )

        Surface(
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CustomProgressIndicator(
                        size = ProgressIndicatorSize.Medium,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(
                        bottom = with(LocalDensity.current) {
                            WindowInsets.navigationBars.getBottom(this).toDp() + 12.dp
                        },
                    ),
                ) {
                    items(
                        items = alarms,
                        key = { it.id },
                    ) { alarm ->
                        AlarmListItem(
                            alarmType = alarm.type,
                            message = alarm.message,
                            date = alarm.date,
                            onConfirm = if (alarm.type == AlarmType.FOLLOW) {
                                { onConfirmFollow(alarm.id) }
                            } else {
                                null
                            },
                            onDelete = if (alarm.type == AlarmType.FOLLOW) {
                                { onDeleteAlarm(alarm.id) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 알람 아이템 데이터 모델
 */
data class AlarmItem(
    val id: String,
    val type: AlarmType,
    val message: String,
    val date: String,
    val senderNickname: String? = null,
)

