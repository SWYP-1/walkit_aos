package team.swyp.sdu.ui.mypage.userInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.InfoBanner
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Error
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Loading
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Success
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Updating
import team.swyp.sdu.ui.mypage.userInfo.UserInput
import javax.inject.Inject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import team.swyp.sdu.R
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.ConfirmDialog
import team.swyp.sdu.ui.mypage.userInfo.component.DateDropdown
import team.swyp.sdu.ui.mypage.userInfo.component.FilledTextField
import team.swyp.sdu.ui.mypage.userInfo.component.ImageUploadMenu
import team.swyp.sdu.ui.mypage.userInfo.component.UserInfoDisplaySection
import team.swyp.sdu.ui.mypage.userInfo.component.UserInfoFormSection
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey2
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.createCameraImageUri
import team.swyp.sdu.utils.formatBirthDate
import team.swyp.sdu.utils.parseBirthDate
import team.swyp.sdu.utils.ProfileImageState
import team.swyp.sdu.utils.launchCameraWithPermission
import team.swyp.sdu.utils.CameraLaunchConfig
import team.swyp.sdu.utils.handleCameraPermissionResult

/**
 * 내 정보 관리 화면 Route (ViewModel 연결)
 */
@Composable
fun UserInfoManagementRoute(
    modifier: Modifier = Modifier,
    viewModel: UserInfoManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    val goal by viewModel.goalFlow.collectAsStateWithLifecycle()
    val provider by viewModel.provider.collectAsStateWithLifecycle()
    val hasChange by viewModel.hasChange.collectAsStateWithLifecycle()

    UserInfoManagementScreen(
        modifier = modifier,
        uiState = uiState,
        userInput = userInput,
        provider = provider ?: "알수없음",
        goal = goal,
        hasChange = hasChange,
        onNavigateBack = onNavigateBack,
        onSaveUserProfile = viewModel::saveUserProfile,
        onUpdateProfileImageUri = { uri -> viewModel.updateProfileImageUri(uri) }
    )
}

/**
 * 내 정보 관리 화면
 *
 * 사용자의 정보를 관리하는 화면
 * - 프로필 이미지 업로드
 * - 이름 변경
 * - 생년월일 설정
 * - 닉네임 변경
 * - 유저 ID 표시
 * - 연동된 계정 표시
 *
 * @param modifier Modifier
 * @param uiState UI 상태
 * @param userInput 사용자 입력 데이터
 * @param provider 연동된 계정 정보
 * @param goal 목표 정보
 * @param onNavigateBack 뒤로가기 클릭 핸들러
 * @param onSaveUserProfile 사용자 프로필 저장 핸들러
 * @param onUpdateProfileImageUri 프로필 이미지 URI 업데이트 핸들러
 */
// 상수 정의
private object UserInfoConstants {
    const val MAX_FILE_SIZE_MB = 10
    const val SUCCESS_MESSAGE_DURATION_MS = 3000L
    const val FILE_SIZE_GUIDE_TEXT = "10MB 이내의 파일만 업로드 가능합니다."
}

// Banner 상태
private sealed class BannerState {
    data object Hidden : BannerState()
    data object Success : BannerState()
    data class Error(val message: String) : BannerState()
}

@Composable
fun UserInfoManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: UserInfoManagementViewModel = hiltViewModel(),
    uiState: UserInfoUiState,
    userInput: UserInput,
    provider: String,
    goal: team.swyp.sdu.domain.model.Goal?,
    hasChange: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onSaveUserProfile: (birthYear: String, birthMonth: String, birthDay: String, nickname: String) -> Unit = { _, _, _, _ -> },
    onUpdateProfileImageUri: (Uri?) -> Unit = {},
) {
    val context = LocalContext.current

    // ViewModel 상태
    val imageDeleted by viewModel.imageDeleted.collectAsStateWithLifecycle()

    var updateSuccess by remember { mutableStateOf(userInput.name) }

    // 로컬 상태
    var name by remember { mutableStateOf(userInput.name) }
    var birthYear by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(userInput.nickname) }
    var profileImageState by remember {
        mutableStateOf(
            ProfileImageState.fromUserInput(
                userInput.imageName,
                userInput.selectedImageUri
            )
        )
    }
    // Banner 상태
    var bannerState by remember { mutableStateOf<BannerState>(BannerState.Hidden) }

    // 이전 UI 상태 (banner 표시 조건 판별용)
    var previousUiState by remember { mutableStateOf<UserInfoUiState?>(null) }

    // 스크롤 상태
    val scrollState = rememberScrollState()

    // userInput에서 로컬 상태로 데이터 복사하는 함수
    fun updateLocalStateFromUserInput() {
        // 닉네임 중복 검사 중이거나 에러가 있는 경우에는 사용자가 입력한 값을 유지
        if (userInput.isNicknameDuplicate != true && userInput.nicknameValidationError == null) {
            nickname = userInput.nickname
        }
        name = userInput.name

        // 이미지 삭제 상태에 따라 다르게 처리
        // 단, 수동으로 빈 상태를 설정한 경우에는 그대로 유지
        if (!imageDeleted) {
            profileImageState = ProfileImageState.fromUserInput(
                userInput.imageName,
                userInput.selectedImageUri
            )
        }
        // imageDeleted가 true일 때는 수동으로 설정한 빈 상태 유지

        parseBirthDate(userInput.birthDate)?.let { (year, month, day) ->
            birthYear = year
            birthMonth = month
            birthDay = day
        }
    }

    // userInput 또는 imageDeleted가 변경될 때 로컬 상태 업데이트
    // 단, 닉네임 검증 상태가 변경될 때는 nickname 필드를 업데이트하지 않음
    LaunchedEffect(
        userInput.name,
        userInput.birthDate,
        userInput.email,
        userInput.imageName,
        userInput.selectedImageUri,
        imageDeleted
    ) {
        updateLocalStateFromUserInput()
    }

    // UI 상태 변경 감지하여 Banner 표시
    LaunchedEffect(uiState) {
        when (uiState) {
            is UserInfoUiState.Success -> {
                // 저장 성공 시 (Updating -> Success로 바뀔 때만)
                if (previousUiState is UserInfoUiState.Updating) {
                    bannerState = BannerState.Success
                    // 3초 후 자동 숨김
                    kotlinx.coroutines.delay(UserInfoConstants.SUCCESS_MESSAGE_DURATION_MS)
                    bannerState = BannerState.Hidden
                }
            }

            is UserInfoUiState.Error -> {
                // 저장 실패 시
                bannerState = BannerState.Error(uiState.message)
            }

            else -> {
                // 다른 상태에서는 banner 숨김
            }
        }
        // 이전 상태 저장
        previousUiState = uiState
    }

    // 카메라 촬영용 Uri 생성
    val cameraImageUri = remember { createCameraImageUri(context) }

    // 카메라 촬영 Activity Result Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            Timber.d("카메라 촬영 전 profileImageState: original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri}, display=${profileImageState.currentDisplayUrl}")
            profileImageState = ProfileImageState(
                originalImageName = profileImageState.originalImageName,
                selectedImageUri = cameraImageUri,
                displayUrl = cameraImageUri.toString()
            )
            Timber.d("카메라 촬영 후 profileImageState: original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri}, display=${profileImageState.currentDisplayUrl}")
            onUpdateProfileImageUri(cameraImageUri)
        }
    }

    // 갤러리 선택 Activity Result Launcher (PickVisualMedia 사용)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        Timber.d("갤러리 결과 수신: $uri")
        if (uri != null) {
            Timber.d("갤러리 선택 전 profileImageState: original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri}, display=${profileImageState.currentDisplayUrl}")
            profileImageState = ProfileImageState(
                originalImageName = profileImageState.originalImageName,
                selectedImageUri = uri,
                displayUrl = uri.toString()
            )
            Timber.d("갤러리 선택 후 profileImageState: original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri}, display=${profileImageState.currentDisplayUrl}")
            onUpdateProfileImageUri(uri)
        } else {
            Timber.d("갤러리에서 이미지를 선택하지 않음")
        }
    }

    // 권한 요청 Launcher들
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 권한 요청 결과 처리 (재사용 가능한 패턴 사용)
        handleCameraPermissionResult(
            context = context,
            cameraLauncher = cameraLauncher,
            imageUri = cameraImageUri,
            createUri = { createCameraImageUri(context) },
            isGranted = isGranted
        )
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("갤러리 권한 요청 결과: $isGranted")
        if (isGranted) {
            Timber.d("권한 승인됨, 갤러리 Launcher 실행")
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            Timber.d("갤러리 권한 거부됨")
//            errorMessage = "갤러리 접근 권한이 필요합니다. 다시 시도해주세요."
        }
    }

    val (greenPrimary, redPrimary, tertiaryText) = Triple(
        SemanticColor.stateGreenPrimary,
        SemanticColor.stateRedPrimary,
        SemanticColor.textBorderTertiary
    )

    // 변경사항 확인 다이얼로그 표시 여부
    var showConfirmDialog by remember { mutableStateOf(false) }


    // 뒤로가기 핸들러 (변경사항 확인)
    fun handleNavigateBack() {
        if (viewModel.hasChange.value) {
            showConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    // 저장 후 뒤로가기 핸들러
    fun handleSaveAndNavigateBack() {
        onSaveUserProfile(
            birthYear,
            birthMonth,
            birthDay,
            nickname,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        // 로딩 상태 표시
        if (uiState is Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                AppHeader(
                    title = "내 정보 관리",
                    onNavigateBack = ::handleNavigateBack,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // 프로필 업로드 섹션
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 프로필 이미지
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val displayUrl = profileImageState.currentDisplayUrl
                            Timber.d("🎨 UI 표시용 이미지 URL: $displayUrl (original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri})")
                            // 이미지 표시 로직 - profileImageState 변경 시 강제 리컴포지션
                            androidx.compose.runtime.key(profileImageState) { // profileImageState 변경 시 컴포넌트 재생성
                                if (!displayUrl.isNullOrBlank()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(displayUrl),
                                        contentDescription = "프로필 이미지",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Grey2, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Grey7,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // 이미지 업로드 버튼 및 안내 텍스트
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // 이미지 업로드 버튼 및 드랍다운 메뉴
                            ImageUploadMenu(
                                onCameraClick = {
                                    // Android 12+에서는 카메라 실행 전에 권한 재확인 필수
                                    // 카메라 권한 체크 및 실행 (재사용 가능한 패턴 사용)
                                    launchCameraWithPermission(
                                        context = context,
                                        config = CameraLaunchConfig(
                                            cameraLauncher = cameraLauncher,
                                            permissionLauncher = cameraPermissionLauncher,
                                            imageUri = cameraImageUri,
                                            onImageCaptured = { uri ->
                                                // 이미 촬영 완료 후 처리되므로 여기서는 로깅만
                                                Timber.d("UserInfoManagement: 카메라 촬영 완료: $uri")
                                            }
                                        )
                                    )
                                },
                                onGalleryClick = {
                                    Timber.d("갤러리 선택 클릭됨")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        // Android 13+에서는 READ_MEDIA_IMAGES 권한 사용
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.READ_MEDIA_IMAGES
                                        ) == PackageManager.PERMISSION_GRANTED
                                        Timber.d("Android 13+ 권한 상태: $hasPermission")
                                        if (hasPermission) {
                                            Timber.d("갤러리 Launcher 실행")
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        } else {
                                            Timber.d("READ_MEDIA_IMAGES 권한 요청")
                                            galleryPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                                        }
                                    } else {
                                        // Android 12 이하에서는 READ_EXTERNAL_STORAGE 권한 사용
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED
                                        Timber.d("Android 12 이하 권한 상태: $hasPermission")
                                        if (hasPermission) {
                                            Timber.d("갤러리 Launcher 실행")
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        } else {
                                            Timber.d("READ_EXTERNAL_STORAGE 권한 요청")
                                            galleryPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                        }
                                    }
                                },
                                showDeleteOption = true,
                                onDeleteClick = {
                                    Timber.d("🔥 이미지 삭제 클릭 시작!")
                                    Timber.d("📸 삭제 전 상태: original=${profileImageState.originalImageName}, selected=${profileImageState.selectedImageUri}, display=${profileImageState.currentDisplayUrl}")

                                    // 이미지 삭제: uri를 null로 설정
                                    viewModel.updateProfileImageUri(null)

                                    // 화면 상태도 업데이트하여 이미지 즉시 사라지게 함
                                    val newState = ProfileImageState(
                                        originalImageName = null, // 기존 이미지 정보도 제거하여 완전 빈 상태
                                        selectedImageUri = null, // 선택된 이미지는 제거
                                        displayUrl = null // 삭제 시 무조건 빈 상태로 만듦
                                    )

                                    profileImageState = newState

                                    Timber.d("📸 삭제 후 상태: original=${newState.originalImageName}, selected=${newState.selectedImageUri}, display=${newState.currentDisplayUrl}")
                                    Timber.d("🔥 이미지 삭제 클릭 완료 - UI 리컴포지션 대기")
                                }
                            )

                            // 안내 텍스트
                            Text(
                                text = UserInfoConstants.FILE_SIZE_GUIDE_TEXT,
                                style = MaterialTheme.walkItTypography.captionM.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Grey7,
                            )
                        }
                    }

                    // 사용자 정보 입력 폼
                    UserInfoFormSection(
                        nickname = nickname,
                        isLoading = uiState is UserInfoUiState.Loading,
                        onNicknameChange = {
                            val newNickname = it
                            nickname = newNickname
                            // 닉네임 변경 시 유효성 검증 수행 (ViewModel에서 실제 변경 판별)
                            viewModel.updateNickname(newNickname)
                        },
                        birthYear = birthYear,
                        onBirthYearChange = {
                            birthYear = it
                            viewModel.updateBirthDate(it, birthMonth, birthDay)
                        },
                        birthMonth = birthMonth,
                        onBirthMonthChange = {
                            birthMonth = it
                            viewModel.updateBirthDate(birthYear, it, birthDay)
                        },
                        birthDay = birthDay,
                        onBirthDayChange = {
                            birthDay = it
                            viewModel.updateBirthDate(birthYear, birthMonth, it)
                        },
                        isNicknameDuplicate = userInput.isNicknameDuplicate,
                        nicknameValidationError = userInput.nicknameValidationError,
                    )

                    // 사용자 정보 표시 섹션
                    UserInfoDisplaySection(
                        provider = provider,
                        email = userInput.email
                    )

                    Spacer(Modifier.weight(1f))

                    // 하단 버튼들
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 뒤로가기 버튼
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(onClick = ::handleNavigateBack)
                                .border(
                                    width = 1.dp,
                                    color = Grey3,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "뒤로가기",
                                style = MaterialTheme.walkItTypography.bodyM.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = tertiaryText,
                            )
                        }

                        // 저장하기 버튼
                        // canSave 계산 - userInput 상태 변경 시 자동으로 recomposition
                        val canSave = hasChange &&  // 변경사항이 있어야 함
                                nickname.isNotBlank() &&
                                birthYear.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank() &&
                                userInput.nicknameValidationError == null &&  // 닉네임 검증 에러가 없음
                                userInput.isNicknameDuplicate != true  // 중복 상태가 아님

                        // 디버깅용 로그
                        Timber.d("=== canSave 계산 ===")
                        Timber.d("로컬 상태: nickname=$nickname, birthYear=$birthYear, birthMonth=$birthMonth, birthDay=$birthDay")
                        Timber.d("userInput 상태: nickname=${userInput.nickname}, isNicknameDuplicate=${userInput.isNicknameDuplicate}, nicknameValidationError=${userInput.nicknameValidationError}")
                        Timber.d("조건 결과: hasChange=$hasChange, nicknameNotBlank=${nickname.isNotBlank()}, datesValid=${birthYear.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank()}, validationErrorNull=${userInput.nicknameValidationError == null}, notDuplicate=${userInput.isNicknameDuplicate != true}")
                        Timber.d("최종 canSave=$canSave")
                        Timber.d("uiState=$uiState")
                        Timber.d("==================")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp)
                                .then(
                                    if (uiState is UserInfoUiState.Updating || uiState is UserInfoUiState.CheckingDuplicate) {
                                        Timber.d("버튼 상태: 로딩중 (uiState=$uiState)")
                                        Modifier.background(Grey3, RoundedCornerShape(8.dp))
                                    } else if (canSave) {
                                        Timber.d("버튼 상태: 활성화됨 (canSave=$canSave)")
                                        Modifier
                                            .clickable {
                                                onSaveUserProfile(
                                                    birthYear,
                                                    birthMonth,
                                                    birthDay,
                                                    nickname,
                                                )
                                            }
                                            .background(greenPrimary, RoundedCornerShape(8.dp))
                                    } else {
                                        Timber.d("버튼 상태: 비활성화됨 (canSave=$canSave, uiState=$uiState)")
                                        Modifier.background(Grey3, RoundedCornerShape(8.dp))
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "저장하기",
                                style = MaterialTheme.walkItTypography.bodyM.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            // 변경사항 확인 다이얼로그
            if (showConfirmDialog) {
                ConfirmDialog(
                    title = "변경된 사항이 있습니다.",
                    message = "저장하시겠습니까?",
                    negativeButtonText = "아니요",
                    positiveButtonText = "예",
                    onDismiss = { showConfirmDialog = false },
                    onNegative = {
                        showConfirmDialog = false
                        onNavigateBack()
                    },
                    onPositive = {
                        showConfirmDialog = false
                        handleSaveAndNavigateBack()
                    },
                )
            }
            if (uiState is UserInfoUiState.Updating || uiState is UserInfoUiState.CheckingDuplicate) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Small)
            }


            // Banner 오버레이 (하단)
            when (val currentBannerState = bannerState) {
                is BannerState.Success -> {
                    InfoBanner(
                        title = "저장 완료",
                        description = "프로필 정보가 성공적으로 저장되었습니다.",
                        backgroundColor = SemanticColor.backgroundDarkSecondary,
                        borderColor = SemanticColor.backgroundDarkSecondary,
                        iconTint = SemanticColor.iconWhite,
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        icon = { tint ->
                            androidx.compose.material3.Icon(
                                painter = painterResource(R.drawable.ic_info_check),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp + 32.dp + 16.dp) // CTA 버튼 높이 + 여백
                            .padding(horizontal = 16.dp)
                    )
                }

                is BannerState.Error -> {
                    InfoBanner(
                        title = "저장 실패",
                        description = currentBannerState.message,
                        backgroundColor = SemanticColor.stateRedSecondary,
                        borderColor = SemanticColor.stateRedPrimary,
                        iconTint = SemanticColor.stateRedPrimary,
                        textColor = SemanticColor.stateRedPrimary,
                        icon = { tint ->
                            androidx.compose.material3.Icon(
                                painter = painterResource(R.drawable.ic_action_clear),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp + 32.dp + 16.dp) // CTA 버튼 높이 + 여백
                            .padding(horizontal = 16.dp)
                            .clickable { bannerState = BannerState.Hidden } // 클릭하여 숨김
                    )
                }

                BannerState.Hidden -> {
                    // 표시하지 않음
                }
            }

            // 변경사항 확인 다이얼로그
            if (showConfirmDialog) {
                ConfirmDialog(
                    title = "변경된 사항이 있습니다.",
                    message = "저장하시겠습니까?",
                    negativeButtonText = "아니요",
                    positiveButtonText = "예",
                    onDismiss = { showConfirmDialog = false },
                    onNegative = {
                        showConfirmDialog = false
                        onNavigateBack()
                    },
                    onPositive = {
                        showConfirmDialog = false
                        handleSaveAndNavigateBack()
                    },
                )
            }
        }
    }
}


@Composable
@Preview(showBackground = true, heightDp = 800)
private fun UserInfoManagementScreenPreview() {
    WalkItTheme {
        val mockUserInput = UserInput(
            name = "홍길동",
            nickname = "길동이",
            birthDate = "1990-01-01",
            email = "test@example.com",
            imageName = "https://example.com/profile.jpg",
            selectedImageUri = null
        )

        val mockUser = team.swyp.sdu.domain.model.User(
            userId = 12345,
            nickname = "길동이",
            birthDate = "1990-01-01",
            imageName = "https://example.com/profile.jpg",
            email = "test@example.com"
        )

        val mockGoal = Goal(
            targetStepCount = 10000,
            targetWalkCount = 5
        )

        UserInfoManagementScreen(
            uiState = UserInfoUiState.Success(mockUser),
            userInput = mockUserInput,
            provider = "카카오",
            goal = mockGoal,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserInfoManagementScreenLoadingPreview() {
    WalkItTheme {
        val mockUserInput = UserInput(
            name = "",
            nickname = "",
            birthDate = "",
            email = null,
            imageName = null,
            selectedImageUri = null
        )

        UserInfoManagementScreen(
            uiState = UserInfoUiState.Loading,
            userInput = mockUserInput,
            provider = "null",
            goal = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UserInfoManagementScreenErrorPreview() {
    WalkItTheme {
        val mockUserInput = UserInput(
            name = "",
            nickname = "",
            birthDate = "",
            email = null,
            imageName = null,
            selectedImageUri = null
        )

        UserInfoManagementScreen(
            uiState = UserInfoUiState.Error("사용자 정보를 불러올 수 없습니다"),
            userInput = mockUserInput,
            provider = "null",
            goal = null,
        )
    }
}