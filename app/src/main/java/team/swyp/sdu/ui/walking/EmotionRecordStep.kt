package team.swyp.sdu.ui.walking

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.ui.walking.components.WalkingProgressBar
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel

/**
 * 감정 기록 단계 UI 상태
 */
sealed interface EmotionRecordStepUiState {
    data object Loading : EmotionRecordStepUiState
    data class Success(
        val emotionPhotoUri: Uri?,
        val emotionText: String,
    ) : EmotionRecordStepUiState

    data class Error(
        val message: String,
    ) : EmotionRecordStepUiState
}

/**
 * 감정 기록 단계 Route
 * ViewModel injection과 state collection을 담당하는 Route composable
 */
@Composable
fun EmotionRecordStepRoute(
    viewModel: WalkingViewModel,
    onNext: () -> Unit,
    onPrev: () -> Unit = {},
) {
    val emotionPhotoUri by viewModel.emotionPhotoUri.collectAsStateWithLifecycle()
    val emotionText by viewModel.emotionText.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // UI 상태 결정 (현재는 항상 Success, 추후 로딩/에러 상태 추가 가능)
    val uiState: EmotionRecordStepUiState = EmotionRecordStepUiState.Success(
        emotionPhotoUri = emotionPhotoUri,
        emotionText = emotionText,
    )

    // 카메라 촬영용 Uri 생성
    val cameraImageUri = remember {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "emotion_image_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Emotions")
            }
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    // 카메라 촬영 Activity Result Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraImageUri != null) {
            viewModel.setEmotionPhotoUri(cameraImageUri)
        }
    }

    // 갤러리 선택 Activity Result Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setEmotionPhotoUri(uri)
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
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        }
    }

    EmotionRecordStepScreen(
        uiState = uiState,
        onPhotoUriChange = viewModel::setEmotionPhotoUri,
        onTextChange = viewModel::setEmotionText,
        onUpdateSessionImageAndNote = viewModel::updateSessionImageAndNote,
        onNext = onNext,
        onPrevious = onPrev,
        cameraImageUri = cameraImageUri,
        cameraLauncher = cameraLauncher,
        galleryLauncher = galleryLauncher,
        cameraPermissionLauncher = cameraPermissionLauncher,
        galleryPermissionLauncher = galleryPermissionLauncher,
    )
}

/**
 * 감정 기록 단계 Screen
 * UI 상태에 따라 분기 처리
 */
@Composable
private fun EmotionRecordStepScreen(
    uiState: EmotionRecordStepUiState,
    onPhotoUriChange: (Uri?) -> Unit,
    onTextChange: (String) -> Unit,
    onUpdateSessionImageAndNote: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    cameraImageUri: Uri?,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    galleryPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    when (uiState) {
        is EmotionRecordStepUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
            }
        }

        is EmotionRecordStepUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "오류 발생",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onPrevious) {
                        Text("이전으로")
                    }
                }
            }
        }

        is EmotionRecordStepUiState.Success -> {
            EmotionRecordStepScreenContent(
                emotionPhotoUri = uiState.emotionPhotoUri,
                emotionText = uiState.emotionText,
                onPhotoUriChange = onPhotoUriChange,
                onTextChange = onTextChange,
                onUpdateSessionImageAndNote = onUpdateSessionImageAndNote,
                onNext = onNext,
                onPrevious = onPrevious,
                cameraImageUri = cameraImageUri,
                cameraLauncher = cameraLauncher,
                galleryLauncher = galleryLauncher,
                cameraPermissionLauncher = cameraPermissionLauncher,
                galleryPermissionLauncher = galleryPermissionLauncher,
            )
        }
    }
}

/**
 * 감정 기록 단계 Screen Content
 * 실제 UI 콘텐츠를 담당하는 컴포넌트
 */
@Composable
private fun EmotionRecordStepScreenContent(
    emotionPhotoUri: Uri?,
    emotionText: String,
    onPhotoUriChange: (Uri?) -> Unit,
    onTextChange: (String) -> Unit,
    onUpdateSessionImageAndNote: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    cameraImageUri: Uri?,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    galleryPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))

            WalkingProgressBar(
                currentStep = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
            // 제목: "산책 기록하기"
            Text(
                text = "산책 기록하기",
                style = MaterialTheme.walkItTypography.headingS,
                fontWeight = FontWeight.SemiBold,
                color = SemanticColor.textBorderPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 서브타이틀: "오늘의 산책을 사진과 일기로 기록해보세요."
            Text(
                text = "오늘의 산책을 사진과 일기로 기록해보세요.",
                style = MaterialTheme.walkItTypography.bodyM,
                fontWeight = FontWeight.Normal,
                color = SemanticColor.textBorderSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 산책 사진 섹션
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // 제목: "산책 사진 (최대 1장)"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "산책 사진",
                        style = MaterialTheme.walkItTypography.bodyL,
                        fontWeight = FontWeight.Medium,
                        color = SemanticColor.textBorderPrimary,
                    )
                    Text(
                        text = "(최대 1장)",
                        style = MaterialTheme.walkItTypography.bodyS,
                        fontWeight = FontWeight.Normal,
                        color = SemanticColor.textBorderTertiary,
                    )
                }
                Spacer(Modifier.height(4.dp))

                // 설명: "선택한 사진과 함께 산책 코스가 기록됩니다."
                Text(
                    text = "선택한 사진과 함께 산책 코스가 기록됩니다.",
                    style = MaterialTheme.walkItTypography.captionM,
                    fontWeight = FontWeight.Medium,
                    color = SemanticColor.textBorderSecondary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                // 사진 입력 영역
                PhotoInputArea(
                    photoUri = emotionPhotoUri,
                    onPhotoUriChange = onPhotoUriChange,
                    cameraLauncher = {
                        // 카메라 권한 체크 및 실행
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraImageUri?.let { uri ->
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        } else {
                            cameraImageUri?.let { uri ->
                                cameraLauncher.launch(uri)
                            }
                        }
                    },
                    galleryLauncher = {
                        // 갤러리 권한 체크 및 실행
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_MEDIA_IMAGES
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                galleryLauncher.launch("image/*")
                            } else {
                                galleryPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        } else {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                galleryLauncher.launch("image/*")
                            } else {
                                galleryPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 산책 일기 작성하기 섹션
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                // 제목: "산책 일기 작성하기"
                Text(
                    text = "산책 일기 작성하기",
                    style = MaterialTheme.walkItTypography.bodyL,
                    fontWeight = FontWeight.Medium,
                    color = SemanticColor.textBorderPrimary,
                )
                Spacer(Modifier.height(12.dp))

                // 텍스트 입력 영역
                Box(modifier = Modifier.fillMaxWidth()) {
                    BasicTextField(
                        value = emotionText,
                        onValueChange = { newText ->
                            if (newText.length <= 500) {
                                onTextChange(newText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(174.dp)
                            .background(
                                color = SemanticColor.backgroundWhiteTertiary,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(
                                16.dp
                            ),
                        textStyle = MaterialTheme.walkItTypography.bodyM.copy(
                            color = SemanticColor.textBorderPrimary,
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Default,
                        ),
                        maxLines = 8,
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (emotionText.isEmpty()) {
                                    Text(
                                        text = "작성한 산책 일기의 내용은 나만 볼 수 있어요.",
                                        style = MaterialTheme.walkItTypography.captionM,
                                        fontWeight = FontWeight.Normal,
                                        color = SemanticColor.textBorderSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.align(Alignment.TopStart),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    // 글자 수 표시 (우측 하단)
                    Text(
                        text = "${emotionText.length} / 500자",
                        style = MaterialTheme.walkItTypography.bodyS,
                        fontWeight = FontWeight.Normal,
                        color = SemanticColor.textBorderSecondary,
                        modifier = Modifier
                            .padding(end = 16.dp, bottom = 16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            // 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CtaButton(
                    text = "이전으로",
                    textColor = SemanticColor.buttonPrimaryDefault,
                    buttonColor = SemanticColor.backgroundWhitePrimary,
                    onClick = onPrevious,
                    modifier = Modifier.width(96.dp)
                )

                CtaButton(
                    text = "다음으로",
                    textColor = SemanticColor.textBorderPrimaryInverse,
                    onClick = {
                        onUpdateSessionImageAndNote()
                        onNext()
                    },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = "arrow forward",
                            tint = SemanticColor.iconWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 사진 입력 영역
 */
@Composable
private fun PhotoInputArea(
    photoUri: Uri?,
    onPhotoUriChange: (Uri?) -> Unit,
    cameraLauncher: () -> Unit,
    galleryLauncher: () -> Unit,
) {
    var showImageMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(92.dp)
            .background(
                color = SemanticColor.textBorderTertiary,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = { showImageMenu = true }),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(photoUri)
                            .build(),
                    ),
                    contentDescription = "선택한 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 12.dp, y = (-12).dp)
                        .background(color = SemanticColor.iconDisabled, shape = CircleShape)
                        .clickable(onClick = { onPhotoUriChange(null) })
                        .offset(),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_clear),
                        contentDescription = "이미지 삭제",
                        modifier = Modifier.size(16.dp),
                        tint = SemanticColor.iconGrey
                    )
                }
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_info_camera),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = SemanticColor.textBorderPrimaryInverse,
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
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = {
                    showImageMenu = false
                    cameraLauncher()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "갤러리에서 가져오기",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                onClick = {
                    showImageMenu = false
                    galleryLauncher()
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmotionRecordStepScreenPreview() {
    WalkItTheme {
        val context = LocalContext.current
        val cameraImageUri = remember {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "emotion_image_preview.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Emotions")
                }
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        val galleryPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        EmotionRecordStepScreen(
            uiState = EmotionRecordStepUiState.Success(
                emotionPhotoUri = null,
                emotionText = "오늘 산책은 정말 좋았어요!",
            ),
            onPhotoUriChange = {},
            onTextChange = {},
            onUpdateSessionImageAndNote = {},
            onNext = {},
            onPrevious = {},
            cameraImageUri = cameraImageUri,
            cameraLauncher = cameraLauncher,
            galleryLauncher = galleryLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            galleryPermissionLauncher = galleryPermissionLauncher,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmotionRecordStepScreenContentPreview() {
    WalkItTheme {
        val context = LocalContext.current
        val cameraImageUri = remember {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "emotion_image_preview.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Emotions")
                }
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        val galleryPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

        EmotionRecordStepScreenContent(
            emotionPhotoUri = null,
            emotionText = "오늘 산책은 정말 좋았어요!",
            onPhotoUriChange = {},
            onTextChange = {},
            onUpdateSessionImageAndNote = {},
            onNext = {},
            onPrevious = {},
            cameraImageUri = cameraImageUri,
            cameraLauncher = cameraLauncher,
            galleryLauncher = galleryLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            galleryPermissionLauncher = galleryPermissionLauncher,
        )
    }
}


