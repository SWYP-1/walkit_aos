package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.record.components.EmotionCircleIcon
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import team.swyp.sdu.ui.components.CtaButtonVariant
import team.swyp.sdu.utils.FormatUtils
import team.swyp.sdu.utils.FormatUtils.formatDuration
import timber.log.Timber

@Composable
fun ShareWalkingResultDialog(
    modifier: Modifier = Modifier,
    stepCount: String,
    duration: Long,
    sessionThumbNailUri: String,
    preWalkEmotion: EmotionType,
    postWalkEmotion: EmotionType,
    onDismiss: () -> Unit,
    onPrev: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = SemanticColor.backgroundWhitePrimary,
                    shape = RoundedCornerShape(size = 12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "기록 공유하기",

                    // body L/semibold
                    style = MaterialTheme.walkItTypography.bodyL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.textBorderPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "오늘의 산책 기록을 공유하시겠습니까?",

                    // body S/regular
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = SemanticColor.textBorderSecondary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(312.dp)
            ) {
                // 세션 썸네일 이미지 (스냅샷 경로가 있으면 사용, 없으면 기본 이미지)
                if (sessionThumbNailUri.isNotEmpty()) {
                    // 파일 경로에서 이미지 로드
                    val bitmap = remember(sessionThumbNailUri) {
                        try {
                            android.graphics.BitmapFactory.decodeFile(sessionThumbNailUri)
                        } catch (e: Exception) {
                            Timber.e(e, "스냅샷 이미지 로드 실패: $sessionThumbNailUri")
                            null
                        }
                    }

                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "session Thumbnail Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Image(
                        painter = painterResource(R.drawable.bg_summer_cropped),
                        contentDescription = "session Thumbnail Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 기본 배경 이미지
                    Image(
                        painter = painterResource(R.drawable.bg_summer_cropped),
                        contentDescription = "session Thumbnail Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp, start = 16.dp)
                ) {
                    // 산책 전 감정
                    EmotionCircleIcon(preWalkEmotion,32.dp)
                    // 산책 후 감정
                    EmotionCircleIcon(postWalkEmotion,32.dp)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    // TODO : 시간이랑 분 값 피그마에 없음
                    Text(
                        text = "$stepCount 걸음",
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,  // 600
                            color = SemanticColor.textBorderPrimaryInverse,
                        ),
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = formatDuration(
                            duration,
                            style = FormatUtils.DurationStyle.HOURS_MINUTES
                        ),
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,  // 600
                            color = SemanticColor.textBorderPrimaryInverse,
                        ), textAlign = TextAlign.Right
                    )
                }

            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CtaButton(
                    text = "뒤로가기",
                    variant = CtaButtonVariant.SECONDARY,
                    onClick = onPrev,
                    modifier = Modifier.weight(1f),
                )

                CtaButton(
                    text = "저장하기",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    iconResId = R.drawable.ic_arrow_forward
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareWalkingResultDialogPreview() {
    WalkItTheme {
        ShareWalkingResultDialog(
            stepCount = "5,234",
            duration = 1800000L, // 30분
            sessionThumbNailUri = "https://example.com/thumbnail.jpg",
            preWalkEmotion = EmotionType.TIRED,
            postWalkEmotion = EmotionType.HAPPY,
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}