package swyp.team.walkit.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.data.model.EmotionType
import swyp.team.walkit.data.model.EmotionType.*
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.ui.walking.utils.stringToEmotionType
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.FormatUtils
import java.io.File

@Composable
fun WeeklyRecordCard(
    session: WalkingSession,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(230.dp)
    ) {

        /* =========================
         * 1. 카드 본체
         * ========================= */
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = SemanticColor.backgroundWhitePrimary
            ),
            border = BorderStroke(
                width = 1.dp,
                color = SemanticColor.textBorderSecondaryInverse
            )
        ) {
            Column {

                /* ---------- 썸네일 영역 ---------- */
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .background(Color(0xFFE6E6E6)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    val imageUri = session.getImageUri()

                    if (imageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(
                                    if (
                                        imageUri.startsWith("http://") ||
                                        imageUri.startsWith("https://")
                                    ) {
                                        imageUri
                                    } else {
                                        File(imageUri)
                                    }
                                )
                                .crossfade(true)
                                .build(),
                            contentDescription = "산책 기록 썸네일",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PathThumbnail(
                            locations = session.locations,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            pathColor = Color(0xFF4A4A4A),
                        )
                    }
                }

                /* ---------- 하단 텍스트 영역 ---------- */
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
                ) {

                    Text(
                        text = FormatUtils.formatDate(session.startTime),
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.textBorderSecondary,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        /* 걸음 수 */
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "%,d".format(session.stepCount),
                                    maxLines = 1,
                                    style = MaterialTheme.walkItTypography.bodyL,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "걸음",
                                    maxLines = 1,
                                    style = MaterialTheme.walkItTypography.bodyS,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }


                        VerticalDivider(
                            thickness = 1.dp,
                            modifier = Modifier.height(18.dp),
                            color = SemanticColor.backgroundWhiteQuaternary
                        )

                        /* 시간 */
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val durationMillis = session.duration
                            val totalHours =
                                (durationMillis / (1000 * 60 * 60)).toInt()
                            val totalMinutes =
                                ((durationMillis / (1000 * 60)) % 60).toInt()

                            Text(
                                text = totalHours.toString(),
                                style = MaterialTheme.walkItTypography.bodyL.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = SemanticColor.textBorderPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "시간",
                                style = MaterialTheme.walkItTypography.bodyS,
                                color = SemanticColor.textBorderPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = totalMinutes.toString(),
                                style = MaterialTheme.walkItTypography.bodyL.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = SemanticColor.textBorderPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "분",
                                style = MaterialTheme.walkItTypography.bodyS,
                                color = SemanticColor.textBorderPrimary
                            )
                        }
                    }
                }
            }
        }

        /* =========================
         * 2. 감정 아이콘 (카드 오버레이)
         * ========================= */
        EmotionCircle(
            emotionType = stringToEmotionType(session.postWalkEmotion),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = -55.dp)
        )
    }
}


@Composable
fun EmotionCircle(emotionType: EmotionType,modifier: Modifier) {


    val drawable = when (emotionType) {
        JOYFUL -> R.drawable.ic_circle_joyful
        DELIGHTED -> R.drawable.ic_circle_delighted
        HAPPY -> R.drawable.ic_circle_happy
        DEPRESSED -> R.drawable.ic_circle_depressed
        TIRED -> R.drawable.ic_circle_tired
        IRRITATED -> R.drawable.ic_circle_anxious
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = "감정",
        modifier = modifier.size(52.dp),
    )
}

@Composable
fun PathThumbnail(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    pathColor: Color = Color(0xFF444444),
) {
    // locations이 비어있으면 아무것도 그리지 않음 (NoSuchElementException 방지)
    if (locations.isEmpty()) return

    val source = locations

    Canvas(modifier = modifier) {
        val minLat = source.minOf { it.latitude }
        val maxLat = source.maxOf { it.latitude }
        val minLon = source.minOf { it.longitude }
        val maxLon = source.maxOf { it.longitude }

        val latRange = (maxLat - minLat).coerceAtLeast(1e-6)
        val lonRange = (maxLon - minLon).coerceAtLeast(1e-6)

        val points =
            source.map { loc ->
                val x = ((loc.longitude - minLon) / lonRange).toFloat() * size.width
                val y = size.height - ((loc.latitude - minLat) / latRange).toFloat() * size.height
                Offset(x, y)
            }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }

        drawPath(
            path = path,
            color = pathColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        drawCircle(
            color = pathColor,
            radius = 10f,
            center = points.first(),
        )
    }
}

// FormatUtils로 통합됨


@Preview(name = "WeeklyRecordCard Preview - With Image")
@Composable
fun WeeklyRecordCardPreview() {
    // 더미 데이터 생성
    val dummySessionWithoutImage = WalkingSession(
        id = "session456",
        startTime = System.currentTimeMillis() - 172800000, // 그저께
        endTime = System.currentTimeMillis(),
        stepCount = 13100,
        totalDistance = 2500f,
        locations = listOf(
            LocationPoint(37.5665, 126.9780), // 서울 시청
            LocationPoint(37.5660, 126.9785),
            LocationPoint(37.5665, 126.9790),
            LocationPoint(37.5670, 126.9785)
        ),
        preWalkEmotion = "HAPPY",
        postWalkEmotion = "HAPPY",
        localImagePath = null,
        serverImageUrl = "https://picsum.photos/seed/picsum/400/300", // 이미지가 없는 상태
        createdDate = "2015-12-29",
        note = "짧은 산책",
    )

    WalkItTheme {
        WeeklyRecordCard(
            session = dummySessionWithoutImage,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "WeeklyRecordCard Preview - No Image (Path Thumbnail)")
@Composable
fun WeeklyRecordCardPathPreview() {
    // 이미지가 없는 더미 데이터 생성
    val dummySessionWithoutImage = WalkingSession(
        id = "session456",
        startTime = System.currentTimeMillis() - 172800000, // 그저께
        endTime = System.currentTimeMillis(),
        stepCount = 710,
        totalDistance = 2500f,
        locations = listOf(
            LocationPoint(37.5665, 126.9780), // 서울 시청
            LocationPoint(37.5660, 126.9785),
            LocationPoint(37.5665, 126.9790),
            LocationPoint(37.5670, 126.9785)
        ),
        preWalkEmotion = "HAPPY",
        postWalkEmotion = "CONTENT",
        localImagePath = null,
        serverImageUrl = null, // 이미지가 없는 상태
        createdDate = "2015-12-29",
        note = "짧은 산책",
    )

    WalkItTheme {
        WeeklyRecordCard(
            session = dummySessionWithoutImage,
            modifier = Modifier.padding(16.dp)
        )
    }
}

data class DurationParts(
    val hourNumber: String?,
    val minuteNumber: String?
)

fun parseDuration(durationText: String): DurationParts {
    val parts = durationText.split(" ")

    var hour: String? = null
    var minute: String? = null

    parts.forEach { part ->
        when {
            part.endsWith("시간") ->
                hour = part.removeSuffix("시간")
            part.endsWith("분") ->
                minute = part.removeSuffix("분")
        }
    }

    return DurationParts(
        hourNumber = hour,
        minuteNumber = minute
    )
}


@Composable
fun DurationText(
    durationText: String,
) {
    val (hour, minute) = remember(durationText) {
        parseDuration(durationText)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        hour?.let {
            Text(
                text = it,
                style = MaterialTheme.walkItTypography.bodyL,
                color = SemanticColor.textBorderPrimary
            )
            Text(
                text = "시간",
                style = MaterialTheme.walkItTypography.bodyXL,
                fontWeight = FontWeight.Medium,
                color = SemanticColor.textBorderPrimary
            )
            Spacer(Modifier.width(4.dp))
        }

        minute?.let {
            Text(
                text = it,
                style = MaterialTheme.walkItTypography.bodyL,
                color = SemanticColor.textBorderPrimary
            )
            Text(
                text = "분",
                style = MaterialTheme.walkItTypography.bodyXL,
                fontWeight = FontWeight.Medium,
                color = SemanticColor.textBorderPrimary
            )
        }
    }
}
