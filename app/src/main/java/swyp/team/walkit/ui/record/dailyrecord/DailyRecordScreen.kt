package swyp.team.walkit.ui.record.dailyrecord

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import swyp.team.walkit.R
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.presentation.viewmodel.CalendarViewModel
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.record.components.WalkingDiaryCard
import swyp.team.walkit.ui.record.components.WalkingStatsCard
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.utils.LocationTestData
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.ui.components.ConfirmDialog
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.components.ShareWalkingResultDialog
import swyp.team.walkit.ui.walking.components.SaveStatus
import swyp.team.walkit.utils.downloadImage
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
        Timber.d("📅 DailyRecordRoute - 받은 dateString: '$dateString'")
        if (dateString.isBlank()) {
            Timber.w("📅 dateString이 비어있음, 오늘 날짜 사용")
            LocalDate.now()
        } else {
            try {
                // ISO 형식 (yyyy-MM-dd) 또는 다른 형식 시도
                val parsedDate = try {
                    LocalDate.parse(dateString) // ISO 형식 시도
                } catch (e: Exception) {
                    // 다른 형식 시도: yyyy-MM-dd 명시적 포맷터 사용
                    try {
                        LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    } catch (e2: Exception) {
                        // 마지막 시도: yyyyMMdd 형식
                        try {
                            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd"))
                        } catch (e3: Exception) {
                            throw e // 원본 예외 throw
                        }
                    }
                }
                Timber.d("📅 날짜 파싱 성공: '$dateString' -> $parsedDate")
                parsedDate
            } catch (e: Throwable) {
                // ExceptionInInitializerError 등 Error 타입도 처리하기 위해 Throwable 사용
                Timber.e(
                    e,
                    "📅 날짜 파싱 실패: dateString='$dateString', 예외 타입=${e.javaClass.simpleName}, 메시지=${e.message}"
                )
                LocalDate.now() // 파싱 실패 시 오늘 날짜 사용
            }
        }
    }

    // 해당 날짜의 세션 목록 로드
    // collectAsStateWithLifecycle() 내부에서 예외가 발생할 수 있으므로
    // 안전하게 Flow를 collect하여 State로 변환
    val daySessionsState = remember {
        mutableStateOf<List<swyp.team.walkit.data.model.WalkingSession>>(emptyList())
    }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                viewModel.daySessions.collect { sessions ->
                    daySessionsState.value = sessions
                }
            } catch (e: Throwable) {
                // ExceptionInInitializerError 등 Error 타입도 처리
                Timber.e(
                    e,
                    "daySessions collect 실패: ${e.javaClass.simpleName}, message=${e.message}"
                )
                daySessionsState.value = emptyList()
            }
        }
    }

    val daySessions = daySessionsState.value
    val isLoadingDaySessions by viewModel.isLoadingDaySessions.collectAsStateWithLifecycle()

    // 선택된 날짜로 업데이트
    LaunchedEffect(selectedDate) {
        Timber.d("📅 DailyRecordRoute - setDate 호출: $selectedDate")
        viewModel.setDate(selectedDate)
    }

    // daySessions 디버깅 로그 추가
    LaunchedEffect(daySessions) {
        Timber.d("📅 DailyRecordRoute - daySessions 업데이트: size=${daySessions.size}")
        if (daySessions.isNotEmpty()) {
            daySessions.forEachIndexed { index, session ->
                val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                Timber.d("📅 daySessions[$index]: id=${session.id}, startTime=${session.startTime}, sessionDate=$sessionDate, selectedDate=$selectedDate")
            }
        } else {
            Timber.w("📅 daySessions가 비어있습니다. selectedDate=$selectedDate")
        }
    }

    // 해당 날짜의 세션 필터링
    // daySessions는 이미 Flow에서 예외 처리가 되어 있지만,
    // 필터링 과정에서 발생할 수 있는 예외도 처리
    val sessionsForDate = remember(daySessions, selectedDate) {
        try {
            Timber.d("📅 세션 필터링 시작: daySessions.size=${daySessions.size}, selectedDate=$selectedDate")
            val filtered = daySessions.filter { session ->
                try {
                    val sessionDate = java.time.Instant.ofEpochMilli(session.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val matches = sessionDate == selectedDate
                    if (!matches) {
                        Timber.d("📅 세션 날짜 불일치: sessionDate=$sessionDate, selectedDate=$selectedDate, sessionId=${session.id}")
                    }
                    matches
                } catch (e: Throwable) {
                    // ExceptionInInitializerError 등 Error 타입도 처리
                    Timber.e(
                        e,
                        "세션 날짜 파싱 실패: sessionId=${session.id}, startTime=${session.startTime}"
                    )
                    false // 파싱 실패 시 필터에서 제외
                }
            }
            Timber.d("📅 필터링 결과: ${filtered.size}개 세션")
            filtered
        } catch (e: Throwable) {
            // ExceptionInInitializerError 등 Error 타입도 처리
            // 특히 ClassCastException이 발생할 수 있는 경우 처리
            Timber.e(e, "세션 필터링 실패: ${e.javaClass.simpleName}, message=${e.message}")
            emptyList() // 전체 필터링 실패 시 빈 리스트 반환
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
    // 고유 팝업 표시 여부
    var showShareDialog by remember { mutableStateOf(false) }
    // 이미지 저장 상태
    var saveStatus by remember { mutableStateOf(SaveStatus.IDLE) }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current


    // 상위에서 편집 상태와 note 관리
    var isEditing by remember { mutableStateOf(false) }
    var editedNote by remember(selectedSession) { mutableStateOf(selectedSession?.note ?: "") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 포커스 관리를 위한 FocusRequester
    val focusRequester = remember { FocusRequester() }

    // 수정 모드로 전환 시 포커스 요청
    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    // 시스템/물리 뒤로가기 처리
    BackHandler(enabled = true) {
        if (isEditing && editedNote != (selectedSession?.note ?: "")) {
            // 실제 내용이 변경되었을 때만 확인 다이얼로그 표시
            showConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhiteSecondary)
    ) {
        Column(modifier = modifier) {
            AppHeader(
                title = "일일 산책 기록",
                onNavigateBack = {
                    if (isEditing && editedNote != (selectedSession?.note ?: "")) {
                        // 실제 내용이 변경되었을 때만 확인 다이얼로그 표시
                        showConfirmDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
            )

            when {
                selectedSession != null ->  DailyRecordContent(
                    sessionsForDate = sessionsForDate,
                    selectedSessionIndex = selectedSessionIndex,
                    selectedSession = selectedSession,
                    editedNote = editedNote,
                    onNoteChange = { editedNote = it },
                    onSessionSelected = { selectedSessionIndex = it },
                    onDeleteClick = onDeleteClick,
                    isEditing = isEditing,
                    setEditing = { isEditing = it },
                    onExternalClick = { showShareDialog = true },
                    focusRequester = focusRequester
                ) // 선택된 세션 있으면 항상 보여줌
                else -> LoadingSessionContent()
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
        if (showShareDialog && selectedSession != null) {
            ShareWalkingResultDialog(
                stepCount = selectedSession.stepCount.toString(),
                duration = selectedSession.duration,
                sessionThumbNailUri = selectedSession.getImageUri() ?: "",
                preWalkEmotion = selectedSession.preWalkEmotion,
                postWalkEmotion = selectedSession.postWalkEmotion,
                saveStatus = saveStatus,
                onDismiss = { showShareDialog = false },
                onPrev = { showShareDialog = false },
                onSave = {
                    scope.launch {
                        try {
                            saveStatus = SaveStatus.LOADING
                            downloadImage(
                                context = context,
                                path = selectedSession.getImageUri() ?: "",
                                fileName = "walking_result_${selectedSession.id}.png"
                            )
                            saveStatus = SaveStatus.SUCCESS
                            Timber.d("이미지 저장 성공")
                        } catch (t: Throwable) {
                            saveStatus = SaveStatus.FAILURE
                            Timber.e(t, "이미지 저장 실패")
                        }
                    }
                }
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
    onExternalClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SessionDailyTab(
            sessionCount = sessionsForDate.size,
            selectedSessionIndex = selectedSessionIndex,
            onSessionSelected = onSessionSelected,
        )

        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    SemanticColor.backgroundWhitePrimary,
                    shape = RoundedCornerShape(
                        topEnd = 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .padding(16.dp)
        ) {

            SessionThumbnailList(
                session = selectedSession,
                onExternalClick = onExternalClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        WalkingStatsCard(
            stepsLabel = "걸음",
            durationLabel = "산책 시간",
            sessions = listOf(selectedSession),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        WalkingDiaryCard(
            session = selectedSession,
            note = editedNote,
            onNoteChange = onNoteChange,
            onDeleteClick = onDeleteClick,
            isEditMode = isEditing,
            setEditing = setEditing,
            focusRequester = focusRequester
        )
    }
}

@Composable
fun SessionDailyTab(
    sessionCount: Int,
    selectedSessionIndex: Int,
    onSessionSelected: (Int) -> Unit,
) {
    fun getKoreanNumber(num: Int): String {
        val koreanNumbers =
            listOf("", "첫", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉", "열")
        return if (num in 1..3) koreanNumbers[num] else "$num"
    }

    var maxSessionCount = minOf(sessionCount, 3)
    val overlap = 85.dp   // 탭 실제 너비 중 겹칠 값

    Box(
        modifier = Modifier
    ) {
        repeat(maxSessionCount) { index ->
            val isSelected = index == selectedSessionIndex

            Box(
                modifier = Modifier
                    // ⭐ 핵심: 직접 위치 계산
                    .widthIn(min = 91.dp)   // ⭐ 핵심
                    .offset(x = overlap * index)
                    .clip(RoundedCornerShape(topEnd = 8.dp))
                    .background(
                        if (isSelected)
                            SemanticColor.backgroundWhitePrimary
                        else
                            SemanticColor.backgroundDarkSecondary
                    )
                    // ⭐ 항상 앞 index가 위
                    .zIndex((sessionCount - index).toFloat())
                    .clickable { onSessionSelected(index) }
            ) {
                Text(
                    text = "${getKoreanNumber(index + 1)}번째 기록",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isSelected)
                        SemanticColor.textBorderSecondary
                    else
                        SemanticColor.textBorderTertiary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
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
                preWalkEmotion = "TIRED",
                postWalkEmotion = "HAPPY",
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
                preWalkEmotion = "IRRITATED",
                postWalkEmotion = "CONTENT",
                note = "스트레스 해소를 위해 짧게 산책했어요.",
                createdDate = "2024-12-05",
            ),
            WalkingSession(
                id = "session-3",
                startTime = now - 7200000,
                endTime = now - 5400000,
                stepCount = 3000,
                locations = testLocations.take(10),
                totalDistance = 2000f,
                preWalkEmotion = "IRRITATED",
                postWalkEmotion = "CONTENT",
                note = "스트레스 해소를 위해 짧게 산책했어요.!",
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

