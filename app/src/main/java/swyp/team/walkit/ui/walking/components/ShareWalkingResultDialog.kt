package swyp.team.walkit.ui.walking.components

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.*
import swyp.team.walkit.ui.record.components.EmotionCircleIcon
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.utils.FormatUtils.formatDuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView

/* -------------------- 상태 -------------------- */

enum class SaveStatus {
    IDLE, LOADING, SUCCESS, FAILURE
}

/* -------------------- 캡처 컨트롤러 -------------------- */

private object CaptureController {
    var capture: (() -> Unit)? = null
}

/* -------------------- 캡처 박스 -------------------- */

@Composable
private fun CaptureBox(
    modifier: Modifier = Modifier,
    onCaptured: (Bitmap) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var composeView by remember { mutableStateOf<ComposeView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            ComposeView(context).apply {
                setContent { content() }
                composeView = this
            }
        }
    )

    DisposableEffect(Unit) {

        CaptureController.capture = capture@{
            val view = composeView ?: return@capture
            if (view.width <= 0 || view.height <= 0) return@capture

            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            onCaptured(bitmap)
        }

        onDispose {
            CaptureController.capture = null
            composeView = null
        }
    }
}



/* -------------------- 메인 다이얼로그 -------------------- */

@Composable
fun ShareWalkingResultDialog(
    stepCount: String,
    duration: Long,
    sessionThumbNailUri: String,
    preWalkEmotion: String,
    postWalkEmotion: String,
    saveStatus: SaveStatus = SaveStatus.IDLE,
    onDismiss: () -> Unit,
    onPrev: () -> Unit,
    onSave: (Bitmap) -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .background(
                        SemanticColor.backgroundWhitePrimary,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {

                /* ---------- 타이틀 ---------- */

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "기록 공유하기",
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "오늘의 산책 기록을 공유하시겠습니까?",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary
                    )
                }

                Spacer(Modifier.height(20.dp))

                /* ---------- 캡처 영역 ---------- */

                CaptureBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    onCaptured = onSave
                ) {
                    Box(Modifier.fillMaxSize()) {

                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(sessionThumbNailUri)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = {
                                Image(
                                    painter = painterResource(R.drawable.bg_summer_cropped),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        )

                        Image(
                            painterResource(R.drawable.logo_walkit),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmotionCircleIcon(stringToEmotionType(preWalkEmotion), 32.dp)
                            EmotionCircleIcon(stringToEmotionType(postWalkEmotion), 32.dp)
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    SemanticColor.backgroundDarkSecondary.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "$stepCount 걸음",
                                color = SemanticColor.textBorderPrimaryInverse,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                formatDuration(duration),
                                color = SemanticColor.textBorderPrimaryInverse,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (saveStatus == SaveStatus.LOADING) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomProgressIndicator(
                                    size = ProgressIndicatorSize.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                /* ---------- 버튼 ---------- */

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CtaButton(
                        text = "뒤로가기",
                        variant = CtaButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        onClick = onPrev
                    )

                    CtaButton(
                        text = "저장하기",
                        modifier = Modifier.weight(1f),
                        enabled = saveStatus != SaveStatus.LOADING,
                        onClick = {
                            CaptureController.capture?.invoke()
                        }
                    )
                }
            }
        }
    }
}

/* -------------------- Preview -------------------- */

@Preview(showBackground = true)
@Composable
private fun PreviewDialog() {
    WalkItTheme {
        ShareWalkingResultDialog(
            stepCount = "5,234",
            duration = 1800000L,
            sessionThumbNailUri = "",
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}
