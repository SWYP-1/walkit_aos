package team.swyp.sdu.ui.walking

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.walking.components.EmotionProgressIndicator

/**
 * 감정 기록 단계 화면 (단계 2)
 * 사진과 텍스트로 감정을 기록하는 화면
 */
@Composable
fun EmotionRecordStep(
    viewModel: WalkingViewModel = hiltViewModel(),
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val emotionPhotoUri by viewModel.emotionPhotoUri.collectAsStateWithLifecycle()
    val emotionText by viewModel.emotionText.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isFirstSectionExpanded by remember { mutableStateOf(false) }

    // 카메라 촬영용 Uri 생성
    val cameraImageUri = remember {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "emotion_image_${System.currentTimeMillis()}.jpg")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 오른쪽 상단 닫기 버튼
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "닫기",
            )
        }

        // 중앙 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 진행률 표시기
            EmotionProgressIndicator(
                currentStep = 2,
                totalSteps = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 첫 번째 섹션: "산책 후 나의 마음은 어떤가요?" (접을 수 있음)
            ExpandableSection(
                title = "산책 후 나의 마음은 어떤가요?",
                isExpanded = isFirstSectionExpanded,
                onExpandedChange = { isFirstSectionExpanded = it },
                isCompleted = true,
            ) {
                // 섹션 내용 (비어있음 - 완료 표시만)
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE5E5E5),
            )

            // 두 번째 섹션: "산책을 하며 느꼈던 감정을 기록해보세요!"
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.Gray,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "산책을 하며 느꼈던 감정을 기록해보세요!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // 사진 입력 영역
                PhotoInputArea(
                    photoUri = emotionPhotoUri,
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

                // 텍스트 입력 영역
                TextInputArea(
                    text = emotionText,
                    onTextChange = { viewModel.setEmotionText(it) },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 다음 버튼
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E2E2E),
                ),
            ) {
                Text(
                    text = "다음",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 접을 수 있는 섹션 컴포넌트
 */
@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isCompleted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!isExpanded) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
                    },
                    contentDescription = null,
                    tint = Color.Gray,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "완료",
                    tint = Color(0xFF4ECDC4),
                )
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * 사진 입력 영역
 */
@Composable
private fun PhotoInputArea(
    photoUri: Uri?,
    cameraLauncher: () -> Unit,
    galleryLauncher: () -> Unit,
) {
    var showImageMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = { showImageMenu = true })
            .border(
                width = 1.dp,
                color = Color(0xFFE5E5E5),
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
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
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray,
                )
                Text(
                    text = "사진",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
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

/**
 * 텍스트 입력 영역
 */
@Composable
private fun TextInputArea(
    text: String,
    onTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        placeholder = {
            Text(
                text = "텍스트",
                color = Color.Gray,
            )
        },
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions.Default,
        maxLines = 8,
    )
}


