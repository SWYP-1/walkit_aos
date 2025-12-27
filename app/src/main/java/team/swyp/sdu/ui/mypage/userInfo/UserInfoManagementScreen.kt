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
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import java.io.File
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Error
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Loading
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Success
import team.swyp.sdu.ui.mypage.userInfo.UserInfoUiState.Updating
import javax.inject.Inject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.ConfirmDialog
import team.swyp.sdu.ui.mypage.userInfo.component.FilledTextField
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey2
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

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
 * @param onNavigateBack 뒤로가기 클릭 핸들러
 * @param onSave 저장 버튼 클릭 핸들러
 * @param onBack 뒤로가기 버튼 클릭 핸들러
 */
@Composable
fun UserInfoManagementScreen(
    modifier: Modifier = Modifier,
    viewModel: UserInfoManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current

    // ViewModel 상태 수집
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userInput by viewModel.userInput.collectAsStateWithLifecycle()
    
    // GoalRepository에서 Goal 가져오기 (ViewModel을 통해)
    val goal by viewModel.goalFlow.collectAsStateWithLifecycle()

    // 로컬 상태
    var name by remember { mutableStateOf(userInput.name) }
    var birthYear by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(userInput.nickname) }
    var profileImageUrl by remember { mutableStateOf<String?>(userInput.selectedImageUri ?: userInput.imageName) }

    // userInput이 변경될 때 로컬 상태 업데이트
    LaunchedEffect(userInput) {
        // 닉네임 업데이트
        nickname = userInput.nickname
        
        // 이름 업데이트
        name = userInput.name
        
        // 프로필 이미지 업데이트
        profileImageUrl = userInput.selectedImageUri ?: userInput.imageName
        
        // 생년월일 업데이트
        if (userInput.birthDate.isNotBlank()) {
            userInput.birthDate.split("-").let { parts ->
                if (parts.size == 3) {
                    birthYear = parts[0]
                    birthMonth = parts[1]
                    birthDay = parts[2]
                }
            }
        }
    }

    // 카메라 촬영용 Uri 생성
    val cameraImageUri = remember {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "camera_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Camera")
            }
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    // 카메라 촬영 Activity Result Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            profileImageUrl = cameraImageUri.toString()
        }
    }

    // 갤러리 선택 Activity Result Launcher (PickVisualMedia 사용)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        Timber.d("갤러리 결과 수신: $uri")
        if (uri != null) {
            profileImageUrl = uri.toString()
            Timber.d("프로필 이미지 URL 설정됨: $profileImageUrl")
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

    val greenPrimary = Color(0xFF52CE4B)
    val redPrimary = Color(0xFFE65C4A)
    val tertiaryText = Color(0xFFC2C3CA)

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
        val originalBirthDate = userInput.birthDate
        val currentBirthDate = if (birthYear.isNotBlank() && birthMonth.isNotBlank() && birthDay.isNotBlank()) {
            String.format("%04d-%02d-%02d", 
                birthYear.toIntOrNull() ?: 0,
                birthMonth.toIntOrNull() ?: 0,
                birthDay.toIntOrNull() ?: 0
            )
        } else {
            ""
        }
        
        val originalImageUrl = userInput.selectedImageUri ?: userInput.imageName
        val currentImageUrl = profileImageUrl
        
        return nickname != userInput.nickname ||
                currentBirthDate != originalBirthDate ||
                currentImageUrl != originalImageUrl
    }

    // UI 상태에 따른 처리
    LaunchedEffect(uiState) {
        when (uiState) {
            is Error -> {
                errorMessage = (uiState as Error).message
                successMessage = null
            }
            is Success -> {
                errorMessage = null
                // 저장 업데이트 성공 시 성공 메시지 표시
                if (previousState is Updating) {
                    successMessage = "프로필이 성공적으로 업데이트되었습니다."
                }
            }
            else -> {
                errorMessage = null
                successMessage = null
            }
        }
        previousState = uiState
    }
    
    // 성공 메시지 자동 숨김 (3초 후)
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
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
        viewModel.saveUserProfile(
            birthYear = birthYear,
            birthMonth = birthMonth,
            birthDay = birthDay,
            nickname = nickname,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
            .padding(horizontal = 16.dp)
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

        // 에러 메시지 표시
        errorMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = message,
                    color = Color(0xFFC62828),
                    style = MaterialTheme.walkItTypography.bodyM,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 성공 메시지 표시
        successMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = message,
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.walkItTypography.bodyM,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        AppHeader(
            title = "내 정보 관리",
            onNavigateBack = ::handleNavigateBack,
        )

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
                if (profileImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUrl),
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
                Box {
                    var showImageMenu by remember { mutableStateOf(false) }

                    // 이미지 업로드 버튼
                    Row(
                        modifier = Modifier
                            .clickable(onClick = { showImageMenu = true })
                            .border(
                                width = 1.dp,
                                color = greenPrimary,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = greenPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "이미지 업로드",
                            style = MaterialTheme.walkItTypography.bodyS.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = greenPrimary,
                        )
                    }

                    // 이미지 선택 드랍다운 메뉴
                    DropdownMenu(
                        expanded = showImageMenu,
                        onDismissRequest = { showImageMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "촬영하기",
                                    style = MaterialTheme.walkItTypography.bodyM,
                                )
                            },
                            onClick = {
                                showImageMenu = false
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
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "갤러리에서 가져오기",
                                    style = MaterialTheme.walkItTypography.bodyM,
                                )
                            },
                            onClick = {
                                showImageMenu = false
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
                                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } else {
                                        Timber.d("READ_EXTERNAL_STORAGE 권한 요청")
                                        galleryPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                        )
                    }
                }

                // 안내 텍스트
                Text(
                    text = "10MB 이내의 파일만 업로드 가능합니다.",
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Grey7,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 이름 입력 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Grey2, RoundedCornerShape(8.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Grey10,
                    unfocusedTextColor = Grey10,
                    disabledTextColor = tertiaryText,
                    disabledBorderColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Bold,
                ),
                singleLine = true,
                enabled = true,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 생년월일 선택 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "생년월일",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey10,
                )
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 년도 선택
                DateDropdown(
                    value = birthYear,
                    onValueChange = { birthYear = it },
                    placeholder = "년도",
                    modifier = Modifier.weight(1f),
                )

                // 월 선택
                DateDropdown(
                    value = birthMonth,
                    onValueChange = { birthMonth = it },
                    placeholder = "월",
                    modifier = Modifier.weight(1f),
                )

                // 일 선택
                DateDropdown(
                    value = birthDay,
                    onValueChange = { birthDay = it },
                    placeholder = "일",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 닉네임 입력 필드
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "닉네임",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = Grey10,
                )
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = redPrimary,
                )
            }
            FilledTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = "닉네임을 입력해주세요.",
            )
        }



        Spacer(modifier = Modifier.height(24.dp))

        // 유저 ID 표시 필드 (비활성화)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "유저 ID",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Grey7,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 연동된 계정 표시 필드 (비활성화)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "연동된 계정",
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = Grey7,
            )

            OutlinedTextField(
                value =  "카카오",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Grey2, RoundedCornerShape(8.dp)),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = tertiaryText,
                    disabledBorderColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Bold,
                ),
                singleLine = true,
            )
        }

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
                                    viewModel.saveUserProfile(
                                        birthYear = birthYear,
                                        birthMonth = birthMonth,
                                        birthDay = birthDay,
                                        nickname = nickname,
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

/**
 * 생년월일 드롭다운 컴포넌트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val tertiaryText = Color(0xFFC2C3CA)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(4.dp))
                .border(1.dp, Grey3, RoundedCornerShape(4.dp)),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = tertiaryText,
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Grey10,
                    unfocusedTextColor = Grey10,
                ),
                shape = RoundedCornerShape(4.dp),
                textStyle = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // 년도/월/일 목록 생성
            when (placeholder) {
                "년도" -> {
                    // 최근 100년
                    (1924..2024).reversed().forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$year",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$year")
                                expanded = false
                            },
                        )
                    }
                }

                "월" -> {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$month",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$month")
                                expanded = false
                            },
                        )
                    }
                }

                "일" -> {
                    (1..31).forEach { day ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.walkItTypography.bodyS,
                                )
                            },
                            onClick = {
                                onValueChange("$day")
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
@Preview
private fun UserInfoManagementScreenPreview() {
    WalkItTheme {
        UserInfoManagementScreen()
    }
}