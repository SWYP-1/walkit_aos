package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.painterResource
import team.swyp.sdu.R
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.data.model.EmotionType.*
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.home.components.HomeMission
import team.swyp.sdu.ui.home.components.clickableNoRipple

@Composable
fun CharacterSection(
    onClickWalk: () -> Unit,
    todaySteps: Int,
) {
    // 닉네임/레벨 위젯과 캐릭터, 오늘 걸음 수/버튼 묶음
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.walk_it_character),
            contentDescription = "산책 캐릭터",
            modifier = Modifier.size(220.dp),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "오늘 걸음 수",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%,d".format(todaySteps),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .background(Color(0xFFCCCCCC), shape = CircleShape)
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "산책하기",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 4.dp)
                    .clickableNoRipple(onClickWalk),
            )
        }
    }
}

@Composable
fun GoalCard(
    title: String,
    progress: Float,
    onClickGoal: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "현재 목표",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "목표 설정",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickableNoRipple(onClickGoal),
                )
            }

            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp),
            ) {
                val clamped = progress.coerceIn(0f, 1f)
                val markerOffset = maxWidth * clamped

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color(0xFF9E9E9E), shape = CircleShape),
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(maxWidth * clamped)
                            .height(10.dp)
                            .background(Color(0xFFB71C1C), shape = CircleShape),
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = markerOffset - 9.dp)
                            .size(18.dp)
                            .background(Color(0xFF000000), shape = CircleShape),
                )
            }
        }
    }
}

@Composable
fun MissionCard(
    mission: HomeMission,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .clickableNoRipple(onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = mission.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = mission.reward,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "미션 상세",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
fun WeeklyRecordCard(
    session: WalkingSession,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F4F4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(Color(0xFFE6E6E6), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                contentAlignment = Alignment.BottomStart,
            ) {
                PathThumbnail(
                    locations = session.locations,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    pathColor = Color(0xFF4A4A4A),
                )

                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                ) {
                    //TODO : 개발을 위해 해놓음 여기서 터지면 뭔가 에러임 출시 때 제거해야함
                    EmotionCircle(emotionType = session.postWalkEmotion!!)
                }
            }

            Text(
                text = formatDate(session.startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = formatDuration(session.duration),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "%,d".format(session.stepCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatDistance(session.totalDistance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun EmotionCircle(emotionType: EmotionType) {


    val drawable = when(emotionType){
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
        modifier = Modifier.size(78.dp),
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

private fun formatDate(startTime: Long): String {
    val date = java.time.Instant.ofEpochMilli(startTime)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return date.toString()
}

private fun formatDuration(durationMillis: Long): String {
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(durationMillis)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) {
        String.format("%.2f km", meters / 1000f)
    } else {
        String.format("%.0f m", meters)
    }

