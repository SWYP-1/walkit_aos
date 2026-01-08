package swyp.team.walkit.ui.walking.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.ui.components.CtaButton
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.ui.record.components.EmotionCircleIcon
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import swyp.team.walkit.ui.components.CtaButtonVariant
import swyp.team.walkit.ui.components.CustomProgressIndicator
import swyp.team.walkit.ui.components.InfoBanner
import swyp.team.walkit.ui.components.ProgressIndicatorSize
import swyp.team.walkit.utils.FormatUtils
import swyp.team.walkit.utils.FormatUtils.formatDuration
import timber.log.Timber

/**
 * 이미지 저장 상태
 */
enum class SaveStatus {
    IDLE,
    LOADING,
    SUCCESS,
    FAILURE
}

@Composable
fun ShareWalkingResultDialog(
    modifier: Modifier = Modifier,
    stepCount: String,
    duration: Long,
    sessionThumbNailUri: String,
    preWalkEmotion: String,
    postWalkEmotion: String,
    saveStatus: SaveStatus = SaveStatus.IDLE,
    onDismiss: () -> Unit,
    onPrev: () -> Unit,
    onSave: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 360.dp)
                .heightIn(max = 562.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = SemanticColor.backgroundWhitePrimary,
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // 세션 썸네일 이미지 (스냅샷 경로가 있으면 사용, 없으면 기본 이미지)
                    if (sessionThumbNailUri.isNotEmpty()) {
                        // 파일 경로에서 이미지 로드
                        val bitmap = remember(sessionThumbNailUri) {
                            try {
                                android.graphics.BitmapFactory.decodeFile(sessionThumbNailUri)
                            } catch (t: Throwable) {
                                Timber.e(t, "스냅샷 이미지 로드 실패: $sessionThumbNailUri")
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
                        // 산책 전 감정 (String을 EmotionType으로 변환)
                        EmotionCircleIcon(stringToEmotionType(preWalkEmotion), 32.dp)
                        // 산책 후 감정 (String을 EmotionType으로 변환)
                        EmotionCircleIcon(stringToEmotionType(postWalkEmotion), 32.dp)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
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
                    if (saveStatus == SaveStatus.LOADING) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 저장 상태 표시
                if (saveStatus == SaveStatus.SUCCESS) {
                    InfoBanner(
                        title = "이미지 저장이 완료되었습니다",
                        backgroundColor = SemanticColor.backgroundDarkSecondary,
                        borderColor = SemanticColor.backgroundDarkSecondary,
                        textColor = SemanticColor.textBorderPrimaryInverse,
                        iconTint = SemanticColor.textBorderPrimaryInverse,
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_info_check),
                                contentDescription = "info warning",
                                tint = SemanticColor.iconWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                } else if (saveStatus == SaveStatus.FAILURE) {
                    InfoBanner(
                        title = "이미지 저장이 완료되었습니다",
                        backgroundColor = SemanticColor.stateRedTertiary,
                        textColor = SemanticColor.stateRedPrimary,
                        borderColor = SemanticColor.stateRedSecondary,
                        iconTint = SemanticColor.stateRedPrimary,
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_clear),
                                contentDescription = "info warning",
                                tint = SemanticColor.stateRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

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
                        enabled = saveStatus != SaveStatus.LOADING,
                    )
                }
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
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareWalkingResultDialogLoadingPreview() {
    WalkItTheme {
        ShareWalkingResultDialog(
            stepCount = "5,234",
            duration = 1800000L, // 30분
            sessionThumbNailUri = "https://example.com/thumbnail.jpg",
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            saveStatus = SaveStatus.LOADING,
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareWalkingResultDialogSuccessPreview() {
    WalkItTheme {
        ShareWalkingResultDialog(
            stepCount = "5,234",
            duration = 1800000L, // 30분
            sessionThumbNailUri = "https://example.com/thumbnail.jpg",
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            saveStatus = SaveStatus.SUCCESS,
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareWalkingResultDialogFailurePreview() {
    WalkItTheme {
        ShareWalkingResultDialog(
            stepCount = "5,234",
            duration = 1800000L, // 30분
            sessionThumbNailUri = "https://example.com/thumbnail.jpg",
            preWalkEmotion = "TIRED",
            postWalkEmotion = "HAPPY",
            saveStatus = SaveStatus.FAILURE,
            onDismiss = {},
            onPrev = {},
            onSave = {}
        )
    }
}