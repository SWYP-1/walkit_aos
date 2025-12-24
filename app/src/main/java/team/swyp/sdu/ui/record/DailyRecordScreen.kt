package team.swyp.sdu.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.record.components.WalkingDiaryCard
import team.swyp.sdu.ui.record.components.WalkingStatsCard
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.utils.LocationTestData
import team.swyp.sdu.data.model.EmotionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 일간 기록 Route 컴포넌트
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 *
 * @param dateString 날짜 문자열 (예: "2024-12-23")
 * @param modifier Modifier
 * @param onNavigateBack 뒤로가기 콜백
 * @param viewModel CalendarViewModel (자동 주입)
 */
@Composable
fun DailyRecordRoute(
    dateString: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    // 날짜 문자열을 LocalDate로 파싱
    val selectedDate = remember(dateString) {
        try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            LocalDate.now() // 파싱 실패 시 오늘 날짜 사용
        }
    }

    // 해당 날짜의 세션 목록 로드
    val daySessions by viewModel.daySessions.collectAsStateWithLifecycle()

    // 선택된 날짜로 업데이트
    LaunchedEffect(selectedDate) {
        viewModel.setDate(selectedDate)
    }

    // 해당 날짜의 세션 필터링
    val sessionsForDate = remember(daySessions, selectedDate) {
        daySessions.filter { session ->
            val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            sessionDate == selectedDate
        }
    }

    DailyRecordScreen(
        selectedDate = selectedDate,
        sessionsForDate = sessionsForDate,
        modifier = modifier,
        onNavigateBack = onNavigateBack,
    )
}

/**
 * 일간 기록 Screen 컴포넌트
 *
 * 특정 날짜에 실행한 모든 산책 기록을 표시합니다.
 * - 상단: 해당 날짜의 산책 세션 썸네일 목록 (좌우 스크롤, 한 번에 하나만 표시)
 * - 하단: 현재 선택된 세션의 통계 및 일기
 *
 * @param selectedDate 표시할 날짜
 * @param sessionsForDate 해당 날짜의 세션 목록
 * @param modifier Modifier
 * @param onNavigateBack 뒤로가기 콜백
 */
@Composable
fun DailyRecordScreen(
    selectedDate: LocalDate,
    sessionsForDate: List<WalkingSession>,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
) {
    // 현재 선택된 세션 인덱스
    var selectedSessionIndex by remember(sessionsForDate) {
        mutableIntStateOf(if (sessionsForDate.isNotEmpty()) 0 else -1)
    }

    // 현재 선택된 세션
    val selectedSession = remember(selectedSessionIndex, sessionsForDate) {
        if (selectedSessionIndex in sessionsForDate.indices) {
            sessionsForDate[selectedSessionIndex]
        } else {
            null
        }
    }

    // 날짜 포맷팅 (예: "2024년 12월 5일")
    val dateLabel = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
    }

    Column(modifier = modifier) {
        // 헤더
        AppHeader(
            title = dateLabel,
            onNavigateBack = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 세션 썸네일 목록 (LazyRow)
            if (sessionsForDate.isNotEmpty()) {
                SessionThumbnailList(
                    sessions = sessionsForDate,
                    selectedIndex = selectedSessionIndex,
                    onSessionSelected = { selectedSessionIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                // 하단 원형 indicator
                SessionIndicatorRow(
                    totalCount = sessionsForDate.size,
                    selectedIndex = selectedSessionIndex,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // 세션이 없는 경우
                EmptySessionMessage()
            }

            // 선택된 세션이 있으면 통계 및 일기 표시
            selectedSession?.let { session ->
                // 일간 통계 카드 (평균 걸음, 산책 시간)
                WalkingStatsCard(
                    sessions = listOf(session),
                    modifier = Modifier.fillMaxWidth(),
                )

                // 현재 선택된 세션의 일기 카드
                WalkingDiaryCard(
                    session = session,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 세션이 없을 때 표시하는 메시지 컴포넌트
 */
@Composable
private fun EmptySessionMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "이 날짜에 산책 기록이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
    }
}

/**
 * Preview 함수
 */
@Composable
@Preview(showBackground = true)
fun DailyRecordScreenPreview() {
    WalkItTheme {
        val selectedDate = LocalDate.of(2024, 12, 5)
        val testLocations = LocationTestData.getSeoulTestLocations()
        val now = System.currentTimeMillis()

        val mockSessions = listOf(
            WalkingSession(
                id = "session-1",
                startTime = now - 3600000,
                endTime = now - 1800000,
                stepCount = 5000,
                locations = testLocations,
                totalDistance = 3500f,
                preWalkEmotion = EmotionType.TIRED,
                postWalkEmotion = EmotionType.HAPPY,
                note = "오늘은 날씨가 좋아서 산책하기 좋았어요.",
                createdDate = "2024-12-05",
            ),
            WalkingSession(
                id = "session-2",
                startTime = now - 7200000,
                endTime = now - 5400000,
                stepCount = 3000,
                locations = testLocations.take(10),
                totalDistance = 2000f,
                preWalkEmotion = EmotionType.ANXIOUS,
                postWalkEmotion = EmotionType.CONTENT,
                note = "스트레스 해소를 위해 짧게 산책했어요.",
                createdDate = "2024-12-05",
            ),
        )

        DailyRecordScreen(
            selectedDate = selectedDate,
            sessionsForDate = mockSessions,
            onNavigateBack = {},
        )
    }
}
