package team.swyp.sdu.ui.walking.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

/**
 * 감정 값에 따라 표정이 변하는 캐릭터
 */
@Composable
fun EmotionCharacter(
    emotionValue: Float,
    modifier: Modifier = Modifier,
) {
    val animatedValue by animateFloatAsState(
        targetValue = emotionValue,
        animationSpec = tween(durationMillis = 300),
        label = "emotion",
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawEmotionFace(animatedValue)
        }
    }
}

/**
 * 감정 값에 따라 얼굴을 그립니다
 * 0.0: 매우 부정적 (슬픔)
 * 0.5: 중립
 * 1.0: 매우 긍정적 (행복)
 */
private fun DrawScope.drawEmotionFace(emotionValue: Float) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = size.minDimension / 3f

    // 얼굴 배경 (원)
    drawCircle(
        color = Color.White,
        radius = radius,
        center = Offset(centerX, centerY),
    )

    // 눈 (항상 동일)
    val eyeRadius = radius * 0.15f
    val eyeY = centerY - radius * 0.2f
    val eyeSpacing = radius * 0.4f

    // 왼쪽 눈
    drawCircle(
        color = Color.Black,
        radius = eyeRadius,
        center = Offset(centerX - eyeSpacing, eyeY),
    )

    // 오른쪽 눈
    drawCircle(
        color = Color.Black,
        radius = eyeRadius,
        center = Offset(centerX + eyeSpacing, eyeY),
    )

    // 코 (작은 빨간 원)
    val noseRadius = radius * 0.08f
    drawCircle(
        color = Color(0xFFFF6B6B),
        radius = noseRadius,
        center = Offset(centerX, centerY),
    )

    // 입 (감정에 따라 변화)
    val mouthY = centerY + radius * 0.3f
    val mouthWidth = radius * 0.6f
    val mouthHeight = radius * 0.2f * (if (emotionValue > 0.5f) 1f else -1f)

    val mouthPath = Path().apply {
        if (emotionValue > 0.5f) {
            // 긍정적: 위로 올라간 곡선 (미소)
            moveTo(centerX - mouthWidth / 2f, mouthY)
            quadraticBezierTo(
                x1 = centerX,
                y1 = mouthY - mouthHeight,
                x2 = centerX + mouthWidth / 2f,
                y2 = mouthY,
            )
        } else {
            // 부정적: 아래로 내려간 곡선 (슬픔)
            moveTo(centerX - mouthWidth / 2f, mouthY)
            quadraticBezierTo(
                x1 = centerX,
                y1 = mouthY + mouthHeight,
                x2 = centerX + mouthWidth / 2f,
                y2 = mouthY,
            )
        }
    }

    drawPath(
        path = mouthPath,
        color = Color(0xFF4ECDC4),
        style = Stroke(
            width = 8f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        ),
    )
}

/**
 * 접을 수 있는 섹션 컴포넌트
 */
@Composable
fun ExpandableSection(
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
fun PhotoInputArea(
    photoUri: Uri?,
    onPickImage: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onPickImage)
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
    }
}

/**
 * 텍스트 입력 영역
 */
@Composable
fun TextInputArea(
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







