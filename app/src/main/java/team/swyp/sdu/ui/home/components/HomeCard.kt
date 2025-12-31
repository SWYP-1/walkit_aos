package team.swyp.sdu.ui.home.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.EmotionType.*
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.FormatUtils
import java.io.File

@Composable
fun WeeklyRecordCard(
    session: WalkingSession,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.width(230.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SemanticColor.backgroundWhitePrimary),
        border = BorderStroke(width = 1.dp, color = SemanticColor.backgroundWhiteQuaternary)
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(
                            Color(0xFFE6E6E6),
                        ),
                contentAlignment = Alignment.BottomStart,
            ) {
                // 이미지 URI 가져오기 (localImage -> serverImage 순서)
                val imageUri = session.getImageUri()
                if (imageUri != null) {
                    // 이미지가 있으면 이미지 표시
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                                    // 서버 URL인 경우
                                    imageUri
                                } else {
                                    // 로컬 파일 경로인 경우
                                    File(imageUri)
                                }
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = "산책 기록 썸네일",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    // 이미지가 없으면 경로 썸네일 표시
                    PathThumbnail(
                        locations = session.locations,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        pathColor = Color(0xFF4A4A4A),
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = -16.dp,y = 26.dp),
                ) {
                    EmotionCircle(emotionType = session.postWalkEmotion)
                }
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = FormatUtils.formatDate(session.startTime),
                    // caption M/medium
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = SemanticColor.textBorderSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                ) {
                    // 걸음 수 (AnnotatedString)
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("%,d".format(session.stepCount))
                                withStyle(
                                    SpanStyle(
                                        fontSize = MaterialTheme.walkItTypography.bodyS.fontSize,
                                        fontWeight = FontWeight.Normal,
                                        color = SemanticColor.textBorderPrimary
                                    )
                                ) {
                                    append(" 걸음")
                                }
                            },
                            style = MaterialTheme.walkItTypography.bodyXL,
                            color = SemanticColor.textBorderPrimary,
                        )
                    }
                    VerticalDivider(
                        thickness = 1.dp,
                        modifier = Modifier.height(18.dp),
                        color = SemanticColor.textBorderPrimary
                    )

                    // 시간 분 (AnnotatedString)
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val timeString = FormatUtils.formatToMinutesSeconds(session.duration)

                        Text(
                            text = buildAnnotatedString {
                                // 분 부분 (콜론 앞)
                                val colonIndex = timeString.indexOf(':')
                                if (colonIndex > 0) {
                                    append(timeString.substring(0, colonIndex))
                                    withStyle(
                                        SpanStyle(
                                            fontSize = MaterialTheme.walkItTypography.bodyS.fontSize,
                                            fontWeight = FontWeight.Normal,
                                            color = SemanticColor.textBorderPrimary
                                        )
                                    ) {
                                        append(timeString.substring(colonIndex)) // 콜론과 초 부분
                                    }
                                } else {
                                    append(timeString)
                                }
                            },
                            style = MaterialTheme.walkItTypography.bodyXL.copy(
                                fontWeight = FontWeight.Medium
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionCircle(emotionType: EmotionType) {


    val drawable = when (emotionType) {
        HAPPY -> R.drawable.ic_circle_happy
        JOYFUL -> R.drawable.ic_circle_joyful
        CONTENT -> R.drawable.ic_circle_content
        DEPRESSED -> R.drawable.ic_circle_depressed
        TIRED -> R.drawable.ic_circle_tired
        ANXIOUS -> R.drawable.ic_circle_anxious
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = "감정",
        modifier = Modifier.size(52.dp),
    )
}

@Composable
fun PathThumbnail(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    pathColor: Color = Color(0xFF444444),
) {
    val source =
        if (locations.size < 2) {
            // TODO(2025-12-10): 더미 경로 제거하고 실제 위치 데이터만 사용하도록 교체
            listOf(
                LocationPoint(37.286, 127.046),
                LocationPoint(37.2875, 127.047),
                LocationPoint(37.288, 127.0455),
                LocationPoint(37.287, 127.044),
            )
        } else {
            locations
        }

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
        stepCount = 3100,
        totalDistance = 2500f,
        locations = listOf(
            LocationPoint(37.5665, 126.9780), // 서울 시청
            LocationPoint(37.5660, 126.9785),
            LocationPoint(37.5665, 126.9790),
            LocationPoint(37.5670, 126.9785)
        ),
        preWalkEmotion = EmotionType.HAPPY,
        postWalkEmotion = EmotionType.CONTENT,
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
        stepCount = 3100,
        totalDistance = 2500f,
        locations = listOf(
            LocationPoint(37.5665, 126.9780), // 서울 시청
            LocationPoint(37.5660, 126.9785),
            LocationPoint(37.5665, 126.9790),
            LocationPoint(37.5670, 126.9785)
        ),
        preWalkEmotion = EmotionType.HAPPY,
        postWalkEmotion = EmotionType.CONTENT,
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