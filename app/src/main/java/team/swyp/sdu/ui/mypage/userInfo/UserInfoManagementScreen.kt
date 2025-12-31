package team.swyp.sdu.ui.mypage.userInfo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.ui.components.CustomProgressIndicator
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
import team.swyp.sdu.utils.hasUserInfoChanges
import team.swyp.sdu.utils.parseBirthDate
import team.swyp.sdu.utils.ProfileImageState

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

    UserInfoManagementScreen(
        modifier = modifier,
        uiState = uiState,
        userInput = userInput,
        provider = "provider",
        goal = goal,
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

@Composable
fun UserInfoManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: UserInfoManagementViewModel = hiltViewModel(),
    uiState: UserInfoUiState,
    userInput: UserInput,
    provider: String,
    goal: team.swyp.sdu.domain.model.Goal?,
    onNavigateBack: () -> Unit = {},
    onSaveUserProfile: (birthYear: String, birthMonth: String, birthDay: String, nickname: String) -> Unit = { _, _, _, _ -> },
    onUpdateProfileImageUri: (Uri?) -> Unit = {},
) {
    val context = LocalContext.current

    // ViewModel 상태
    val imageDeleted by viewModel.imageDeleted.collectAsStateWithLifecycle()

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
                userInput.selectedImageUri)
        )
    }

    // userInput에서 로컬 상태로 데이터 복사하는 함수
    fun updateLocalStateFromUserInput() {
        nickname = userInput.nickname
        name = userInput.name

        // 이미지 삭제 상태에 따라 다르게 처리
        profileImageState = if (imageDeleted) {
            // 삭제된 상태: imageName은 유지하되 selectedImageUri는 null
            ProfileImageState.fromUserInput(userInput.imageName, null)
        } else {
            ProfileImageState.fromUserInput(
                userInput.imageName,
                userInput.selectedImageUri)
        }

        parseBirthDate(userInput.birthDate)?.let { (year, month, day) ->
            birthYear = year
            birthMonth = month
            birthDay = day
        }
    }

    // userInput 또는 imageDeleted가 변경될 때 로컬 상태 업데이트
    LaunchedEffect(userInput, imageDeleted) {
        updateLocalStateFromUserInput()
    }

    // 카메라 촬영용 Uri 생성
    val cameraImageUri = remember { createCameraImageUri(context) }

    // 카메라 촬영 Activity Result Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            profileImageState = profileImageState.copy(selectedImageUri = cameraImageUri)
            onUpdateProfileImageUri(cameraImageUri)
        }
    }

    // 갤러리 선택 Activity Result Launcher (PickVisualMedia 사용)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        Timber.d("갤러리 결과 수신: $uri")
        if (uri != null) {
            profileImageState = profileImageState.copy(selectedImageUri = uri)
            onUpdateProfileImageUri(uri)
            Timber.d("프로필 이미지 URL 설정됨: ${profileImageState.currentDisplayUrl}")
        } else {
            Timber.d("갤러리에서 이미지를 선택하지 않음")
        }
    }

    // 권한 요청 Launcher들
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            // 권한 거부 시 사용자에게 알림 (다시 시도할 수 있음)
            Timber.d("카메라 권한 거부됨")
//            errorMessage = "카메라 권한이 필요합니다. 다시 시도해주세요."
        }
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

    // 에러 메시지 표시
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 성공 메시지 표시
    var successMessage by remember { mutableStateOf<String?>(null) }

    // 이전 상태 추적
    var previousState by remember { mutableStateOf<UserInfoUiState?>(null) }

    // 변경사항 확인 다이얼로그 표시 여부
    var showConfirmDialog by remember { mutableStateOf(false) }

    // 변경사항이 있는지 확인하는 함수
    fun hasChanges(): Boolean {
        return hasUserInfoChanges(
            originalName = userInput.name,
            currentName = name,
            originalNickname = userInput.nickname,
            currentNickname = nickname,
            originalBirthDate = userInput.birthDate,
            currentBirthYear = birthYear,
            currentBirthMonth = birthMonth,
            currentBirthDay = birthDay,
            originalImageUrl = userInput.selectedImageUri ?: userInput.imageName,
            currentImageUrl = profileImageState.currentDisplayUrl
        )
    }

    // 뒤로가기 핸들러 (변경사항 확인)
    fun handleNavigateBack() {
        if (hasChanges()) {
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

    Column(
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
            return@Column
        }

        AppHeader(
            title = "내 정보 관리",
            onNavigateBack = ::handleNavigateBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

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
                    if (!profileImageState.currentDisplayUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageState.currentDisplayUrl),
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

                // 이미지 업로드 버튼 및 안내 텍스트
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 이미지 업로드 버튼 및 드랍다운 메뉴
                    ImageUploadMenu(
                        onCameraClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraImageUri?.let { uri ->
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
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

            Spacer(modifier = Modifier.height(24.dp))

            // 사용자 정보 입력 폼
            UserInfoFormSection(
                name = name,
                onNameChange = { name = it },
                nickname = nickname,
                onNicknameChange = { nickname = it },
                birthYear = birthYear,
                onBirthYearChange = { birthYear = it },
                birthMonth = birthMonth,
                onBirthMonthChange = { birthMonth = it },
                birthDay = birthDay,
                onBirthDayChange = { birthDay = it },
            )



            Spacer(modifier = Modifier.height(24.dp))

            // 사용자 정보 표시 섹션
            UserInfoDisplaySection(
                provider = provider,
                email = userInput.email
            )

            Spacer(modifier = Modifier.weight(1f))

            // 하단 버튼들
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 뒤로가기 버튼
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(47.dp)
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(47.dp)
                        .then(
                            if (uiState is UserInfoUiState.Updating) {
                                Modifier.background(Grey3, RoundedCornerShape(8.dp))
                            } else {
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
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (uiState is Updating) "저장 중..." else "저장하기",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (uiState is Updating) Grey7 else Color.White,
                    )
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
            provider = "KAKAO",
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