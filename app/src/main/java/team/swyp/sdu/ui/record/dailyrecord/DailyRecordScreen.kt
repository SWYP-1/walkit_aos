package team.swyp.sdu.ui.record.dailyrecord

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.CalendarViewModel
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.record.components.WalkingDiaryCard
import team.swyp.sdu.ui.record.components.WalkingStatsCard
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.utils.LocationTestData
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.ConfirmDialog
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.walkItTypography
import timber.log.Timber
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
            Timber.i("파싱 실패!!오늘날짯용 $dateString")
            LocalDate.now() // 파싱 실패 시 오늘 날짜 사용
        }
    }

    // 해당 날짜의 세션 목록 로드
    val daySessions by viewModel.daySessions.collectAsStateWithLifecycle()
    val isLoadingDaySessions by viewModel.isLoadingDaySessions.collectAsStateWithLifecycle()

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
        isLoading = isLoadingDaySessions,
        modifier = modifier,
        onDeleteClick = { localId -> viewModel.deleteSessionNote(localId) },
        onUpdateNote = { localId, note -> viewModel.updateSessionNote(localId, note) },
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
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onUpdateNote: (String, String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    var selectedSessionIndex by remember(sessionsForDate, isLoading) {
        mutableIntStateOf(if (!isLoading && sessionsForDate.isNotEmpty()) 0 else -1)
    }
    val selectedSession = remember(selectedSessionIndex, sessionsForDate, isLoading) {
        if (isLoading) null else sessionsForDate.getOrNull(selectedSessionIndex)
    }

    // 상위에서 편집 상태와 note 관리
    var isEditing by remember { mutableStateOf(false) }
    var editedNote by remember { mutableStateOf(selectedSession?.note ?: "") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 시스템/물리 뒤로가기 처리
    BackHandler(enabled = true) {
        if (isEditing) {
            showConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        Column(modifier = modifier) {
            val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))

            // 헤더
            AppHeader(
                title = dateLabel,
                onNavigateBack = {
                    if (isEditing) showConfirmDialog = true
                    else onNavigateBack()
                },
            )

            when {
                isLoading -> {
                    LoadingSessionContent()
                }
                selectedSession == null -> {
                    EmptySessionContent()
                }
                else -> {
                    DailyRecordContent(
                        sessionsForDate = sessionsForDate,
                        selectedSessionIndex = selectedSessionIndex,
                        selectedSession = selectedSession,
                        editedNote = editedNote,
                        onNoteChange = { editedNote = it },
                        onSessionSelected = { selectedSessionIndex = it },
                        onDeleteClick = onDeleteClick,
                        isEditing = isEditing,
                        setEditing = { isEditing = it }
                    )
                }
            }
        }

        if (showConfirmDialog) {
            ConfirmDialog(
                title = "변경된 사항이 있습니다.",
                message = "저장하시겠습니까?",
                onPositive = {
                    onUpdateNote(selectedSession!!.id, editedNote)
                    isEditing = false
                    showConfirmDialog = false
                    onNavigateBack()
                },
                onNegative = {
                    isEditing = false
                    showConfirmDialog = false
                    onNavigateBack()
                },
                onDismiss = { showConfirmDialog = false }
            )
        }
    }
}

@Composable
fun DailyRecordContent(
    sessionsForDate: List<WalkingSession>,
    selectedSessionIndex: Int,
    selectedSession: WalkingSession,
    editedNote: String,
    onNoteChange: (String) -> Unit,
    onSessionSelected: (Int) -> Unit,
    onDeleteClick: (String) -> Unit,
    isEditing: Boolean,
    setEditing: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SessionThumbnailList(
            sessions = sessionsForDate,
            selectedIndex = selectedSessionIndex,
            onSessionSelected = onSessionSelected,
            modifier = Modifier.fillMaxWidth(),
        )

        SessionIndicatorRow(
            totalCount = sessionsForDate.size,
            selectedIndex = selectedSessionIndex,
            modifier = Modifier.fillMaxWidth(),
        )

        WalkingStatsCard(
            sessions = listOf(selectedSession),
            modifier = Modifier.fillMaxWidth(),
        )

        WalkingDiaryCard(
            session = selectedSession,
            note = editedNote,
            onNoteChange = onNoteChange,
            onDeleteClick = onDeleteClick,
            isEditMode = isEditing,
            setEditing = setEditing
        )
    }
}

@Composable
fun LoadingSessionContent() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CustomProgressIndicator()
    }
}

@Composable
fun EmptySessionContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_empty_session),
                contentDescription = "empty sessoin"
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "이 날짜에 산책 기록이 없어요",
                // body XL/semibold
                style = MaterialTheme.walkItTypography.bodyXL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "워킷과 함께 산책하고 나만의 산책 기록을 남겨보세요.",
                // body S/medium
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderSecondary
            )
        }
    }
}


@Composable
@Preview(showBackground = true)
fun DailyRecordScreenWithSessionsPreview() {
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
            isLoading = false,
            onNavigateBack = {},
            onUpdateNote = { _, _ -> },
            onDeleteClick = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DailyRecordScreenLoadingPreview() {
    WalkItTheme {
        val selectedDate = LocalDate.of(2024, 12, 5)

        DailyRecordScreen(
            selectedDate = selectedDate,
            sessionsForDate = emptyList(),
            isLoading = true,
            onNavigateBack = {},
            onUpdateNote = { _, _ -> },
            onDeleteClick = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun DailyRecordScreenEmptyPreview() {
    WalkItTheme {
        val selectedDate = LocalDate.of(2024, 12, 5)

        DailyRecordScreen(
            selectedDate = selectedDate,
            sessionsForDate = emptyList(),
            isLoading = false,
            onNavigateBack = {},
            onUpdateNote = { _, _ -> },
            onDeleteClick = {}
        )
    }
}

