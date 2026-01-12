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
import swyp.team.walkit.ui.components.captureMapViewSnapshot
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.walking.viewmodel.WalkingResultViewModel
import swyp.team.walkit.utils.downloadImage
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.graphics.createBitmap


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

    // TODO: 개발용 순서 거꾸로
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
        dateString = dateString,
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
    dateString: String? = null,
) {
    var selectedSessionIndex by remember(sessionsForDate, isLoading) {
        val index = if (!isLoading && sessionsForDate.isNotEmpty()) 0 else -1
        Timber.d("🎯 selectedSessionIndex 설정: isLoading=$isLoading, sessionsForDate.size=${sessionsForDate.size}, index=$index")
        mutableIntStateOf(index)
    }
    val selectedSession = remember(selectedSessionIndex, sessionsForDate, isLoading) {
        val session = if (isLoading) null else sessionsForDate.getOrNull(selectedSessionIndex)
        Timber.d("🎯 selectedSession 설정: isLoading=$isLoading, selectedSessionIndex=$selectedSessionIndex, sessionsForDate.size=${sessionsForDate.size}, session=${session?.id}")
        session
    }
    // 고유 팝업 표시 여부
    var showShareDialog by remember { mutableStateOf(false) }
    // 이미지 저장 상태
    var saveStatus by remember { mutableStateOf(SaveStatus.IDLE) }

    // 스냅샷 생성을 위한 상태들
    var snapshotUri by remember { mutableStateOf<String?>(null) }
    var isGeneratingSnapshot by remember { mutableStateOf(false) }

    // 스냅샷 생성 로딩 상태
    var isSnapshotLoading by remember { mutableStateOf(false) }

    // 썸네일 좌표 상태 (SessionThumbnailItem에서 직접 업데이트)
    val thumbnailCoordinatesState = remember { mutableStateOf<LayoutCoordinates?>(null) }
    
    // thumbnailCoordinatesState 변경을 thumbnailCoordinates에 동기화
    var thumbnailCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    LaunchedEffect(thumbnailCoordinatesState.value) {
        thumbnailCoordinates = thumbnailCoordinatesState.value
        if (thumbnailCoordinatesState.value != null) {
            Timber.d("📸 [DailyRecordScreen] 썸네일 좌표 동기화됨 - size: ${thumbnailCoordinatesState.value?.size}")
        }
    }


    // 날짜 문자열을 한국어 형식으로 변환
    val formattedDateString = remember(dateString) {
        dateString?.let { dateStr ->
            try {
                // "2026-01-10" 형식을 LocalDate로 파싱
                val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                // 한국어 형식으로 포맷팅 (예: "2026년 1월 10일")
                val koreanFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN)
                date.format(koreanFormatter)
            } catch (e: Exception) {
                Timber.e(e, "날짜 변환 실패: $dateStr")
                dateStr // 변환 실패 시 원본 반환
            }
        } ?: "2025년 12월 25일" // 기본값
    }

    // MapView 참조 (스냅샷 생성용)
    var mapViewRef by remember { mutableStateOf<com.kakao.vectormap.MapView?>(null) }

    // MapView를 위한 ViewModel (WalkingResultScreen에서 사용하는 것과 동일)
    val mapViewModel: WalkingResultViewModel = hiltViewModel()

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

    // 공유하기 버튼 클릭 시 스냅샷 생성 및 다이얼로그 표시
    val onShareClick: (WalkingSession) -> Unit = remember {
        { session ->
            Timber.d("🖱️ 공유하기 버튼 클릭됨 - session: $session")
            scope.launch {
                // ✅ 스냅샷 생성 및 다이얼로그 표시 로직
                if (!session.isSynced && snapshotUri == null) {
                    isSnapshotLoading = true
                    try {
                        Timber.d("공유하기: isSynced가 false이므로 스냅샷 생성 시작")
                        // ✅ SessionThumbnailItem 영역만 캡쳐
                        val coordinates = thumbnailCoordinates
                        Timber.d("📸 [onShareClick] 썸네일 스냅샷 좌표 정보 - coordinates: $coordinates, isNull: ${coordinates == null}")

                        if (coordinates != null) {
                            val bounds = coordinates.boundsInWindow()
                            val size = coordinates.size
                            Timber.d("📸 썸네일 좌표 상세 - size: $size, bounds: $bounds")

                            snapshotUri = captureDailyRecordSnapshot(
                                coordinates = coordinates,
                                context = context
                            )
                            Timber.d("공유하기: SessionThumbnailItem 스냅샷 생성 완료: $snapshotUri")
                        } else {
                            Timber.w("공유하기: SessionThumbnailItem 좌표 정보가 없습니다")
                            Timber.w("공유하기: 썸네일 좌표가 설정되지 않았습니다. UI 재렌더링을 기다려주세요.")

                            // 좌표가 설정될 때까지 잠시 대기 후 재시도
                            Timber.d("공유하기: 썸네일 좌표 설정 대기 시작...")
                            kotlinx.coroutines.delay(300) // 0.3초 대기

                            val retryCoordinates = thumbnailCoordinates
                            if (retryCoordinates != null) {
                                Timber.d("공유하기: 재시도 성공 - 썸네일 좌표를 얻었습니다")
                                snapshotUri = captureDailyRecordSnapshot(
                                    coordinates = retryCoordinates,
                                    context = context
                                )
                                Timber.d("공유하기: 재시도 SessionThumbnailItem 스냅샷 생성 완료: $snapshotUri")
                            } else {
                                Timber.e("공유하기: 재시도 실패 - 썸네일 좌표가 여전히 없습니다")
                                android.widget.Toast.makeText(
                                    context,
                                    "썸네일 캡쳐를 위한 좌표를 가져올 수 없습니다. 다시 시도해주세요.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "공유하기: 스냅샷 생성 실패")
                    } finally {
                        isSnapshotLoading = false
                    }
                } else {
                    Timber.d("공유하기: isSynced가 true이므로 저장된 이미지 사용")
                }

                // 다이얼로그 표시
                showShareDialog = true
                Timber.d("✅ 공유 다이얼로그 표시 설정됨 - session: $session")
            }
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
                selectedSession != null -> {
                    Timber.d("🎨 UI 렌더링 - selectedSession: $selectedSession, index: $selectedSessionIndex")
                    Timber.d("🎨 UI 렌더링 - session.id: ${selectedSession.id}, stepCount: ${selectedSession.stepCount}")
                    DailyRecordContent(
                        sessionsForDate = sessionsForDate,
                        selectedSessionIndex = selectedSessionIndex,
                        selectedSession = selectedSession,
                        editedNote = editedNote,
                        onNoteChange = { editedNote = it },
                        onSessionSelected = { selectedSessionIndex = it },
                        onDeleteClick = onDeleteClick,
                        isEditing = isEditing,
                        setEditing = { isEditing = it },
                        onExternalClick = {
                            selectedSession?.let { session ->
                                onShareClick(session)
                            }
                        },
                        thumbnailCoordinatesState = thumbnailCoordinatesState,
                        isSnapshotLoading = isSnapshotLoading,
                        formattedDateString = formattedDateString,
                        focusRequester = focusRequester
                    ) // 선택된 세션 있으면 항상 보여줌
                }

                else -> {
                    Timber.d("🎨 UI 렌더링 - selectedSession is null, showing LoadingSessionContent")
                    Timber.d("🎨 UI 렌더링 - isLoading: $isLoading, sessionsForDate.size: ${sessionsForDate.size}, selectedSessionIndex: $selectedSessionIndex")
                    LoadingSessionContent()
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
        // ✅ 다이얼로그 표시 (WalkingSession을 직접 파라미터로 받음)
        if (showShareDialog) {
            Timber.d("🎉 공유 다이얼로그 표시 시작")
            // 다이얼로그에서 사용할 세션 (onShareClick에서 전달받은 세션)
            val dialogSession = selectedSession // 현재 선택된 세션 사용

            if (dialogSession != null) {
                // ✅ 이미지 URI 결정 로직 개선
                val imageUri = if (dialogSession.isSynced) {
                    // isSynced가 true: 저장된 이미지 사용 (localImagePath 우선, 없으면 serverImageUrl)
                    dialogSession.getImageUri()
                } else {
                    // isSynced가 false: 방금 생성한 스냅샷 사용
                    snapshotUri
                }

                ShareWalkingResultDialog(
                    stepCount = dialogSession.stepCount.toString(),
                    duration = dialogSession.duration,
                    sessionThumbNailUri = imageUri ?: "",
                    preWalkEmotion = dialogSession.preWalkEmotion,
                    postWalkEmotion = dialogSession.postWalkEmotion,
                    saveStatus = saveStatus,
                    onDismiss = {
                        showShareDialog = false
                        snapshotUri = null // 다이얼로그 닫을 때 초기화
                    },
                    onPrev = {
                        showShareDialog = false
                    },
                    onSave = {
                        scope.launch {
                            try {
                                saveStatus = SaveStatus.LOADING
                                val imagePath = imageUri ?: ""

                                downloadImage(
                                    context = context,
                                    path = imagePath,
                                    fileName = "walking_result_${dialogSession.id}.png"
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
}

@Composable
fun DailyRecordContent(
    sessionsForDate: List<WalkingSession>,
    formattedDateString: String,
    selectedSessionIndex: Int,
    selectedSession: WalkingSession,
    editedNote: String,
    onNoteChange: (String) -> Unit,
    onSessionSelected: (Int) -> Unit,
    onDeleteClick: (String) -> Unit,
    isEditing: Boolean,
    isSnapshotLoading: Boolean,
    setEditing: (Boolean) -> Unit,
    thumbnailCoordinatesState: androidx.compose.runtime.MutableState<LayoutCoordinates?>,
    onExternalClick: (WalkingSession) -> Unit,
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
                isSnapshotLoading = isSnapshotLoading,
                dateString = formattedDateString,
                thumbnailCoordinates = thumbnailCoordinatesState
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

/**
 * DailyRecord 전체 화면 PixelCopy 캡처 (사진 + 경로 방식)
 */
private suspend fun captureDailyRecordSnapshot(
    coordinates: LayoutCoordinates?,
    context: android.content.Context,
): String? {
    if (coordinates == null) {
        Timber.w("DailyRecord Box 위치 정보가 없습니다")
        return null
    }

    val width = coordinates.size.width.toInt()
    val height = coordinates.size.height.toInt()

    if (width <= 0 || height <= 0) {
        Timber.w("Box 크기가 0입니다: ${width}x${height}")
        return null
    }

    val activity = context as? android.app.Activity
    if (activity == null) {
        Timber.w("Activity를 찾을 수 없습니다")
        return null
    }

    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
        Timber.w("PixelCopy는 Android 8.0 이상에서만 사용 가능")
        return null
    }

    val boundsInWindow = coordinates.boundsInWindow()
    val window = activity.window

    Timber.d("📸 PixelCopy 캡쳐 영역 - width: $width, height: $height")
    Timber.d("📸 PixelCopy bounds - left: ${boundsInWindow.left}, top: ${boundsInWindow.top}, right: ${boundsInWindow.right}, bottom: ${boundsInWindow.bottom}")

    val bitmap = createBitmap(
        width,
        height
    )

    val rect = android.graphics.Rect(
        boundsInWindow.left.toInt(),
        boundsInWindow.top.toInt(),
        boundsInWindow.right.toInt(),
        boundsInWindow.bottom.toInt()
    )

    Timber.d("📸 PixelCopy Rect - ${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom} (크기: ${rect.width()}x${rect.height()})")

    // PixelCopy의 콜백을 suspend 함수로 변환
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        android.view.PixelCopy.request(
            window,
            rect,
            bitmap,
            { copyResult ->
                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    Timber.d("DailyRecord PixelCopy 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
                    val savedPath = saveDailyRecordSnapshotToFile(context, bitmap)
                    Timber.d("DailyRecord 스냅샷 파일 저장: $savedPath")
                    continuation.resume(savedPath) {}
                } else {
                    Timber.e("DailyRecord PixelCopy 실패: $copyResult")
                    continuation.resume(null) {}
                }
            },
            android.os.Handler(android.os.Looper.getMainLooper())
        )

        // 코루틴이 취소되면 PixelCopy 요청도 취소할 수 있도록 설정
        continuation.invokeOnCancellation {
            Timber.d("DailyRecord PixelCopy 요청 취소됨")
        }
    }
}

/**
 * DailyRecord 스냅샷을 파일로 저장
 */
private fun saveDailyRecordSnapshotToFile(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap,
): String? {
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "daily_record_snapshot_${timestamp}.png"

        val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(fileDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }

        val absolutePath = file.absolutePath
        Timber.d("DailyRecord 스냅샷 파일 저장 완료: $absolutePath")
        absolutePath
    } catch (t: Throwable) {
        Timber.e(t, "DailyRecord 스냅샷 파일 저장 실패: ${t.message}")
        null
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
            onDeleteClick = {},
            dateString = "2025년 1월 10일 금요일"
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
            onDeleteClick = {},
            dateString = "2025년 1월 10일 금요일"
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
            onDeleteClick = {},
            dateString = "2025년 1월 10일 금요일"
        )
    }
}

