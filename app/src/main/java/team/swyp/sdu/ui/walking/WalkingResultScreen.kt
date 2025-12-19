package team.swyp.sdu.ui.walking

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber
import team.swyp.sdu.data.model.LocationPoint
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.presentation.viewmodel.KakaoMapViewModel
import team.swyp.sdu.ui.walking.viewmodel.WalkingUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingViewModel
import team.swyp.sdu.ui.walking.viewmodel.WalkingResultUiState
import team.swyp.sdu.ui.walking.viewmodel.WalkingResultViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Composable
private fun StatItem(title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WeekCompletionRow(
    sessionsThisWeek: List<WalkingSession>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val startOfWeek = today.with(DayOfWeek.MONDAY)
    val completionMap =
        sessionsThisWeek.groupBy { session ->
            Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { true }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val labels = listOf("월", "화", "수", "목", "금", "토", "일")
        labels.forEachIndexed { index, label ->
            val date = startOfWeek.plusDays(index.toLong())
            val isDone = completionMap[date] == true
            WeekCircle(label = label, isDone = isDone)
        }
    }
}

@Composable
private fun WeekCircle(label: String, isDone: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color = if (isDone) Color(0xFF2E2E2E) else Color(0xFFEAEAEA),
                        shape = RoundedCornerShape(50),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isDone) {
                Text(
                    text = "✔",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PathThumbnail(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    pathColor: Color = Color.White,
    endpointColor: Color = Color.White,
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
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        drawCircle(
            color = endpointColor,
            radius = 10f,
            center = points.last(),
        )
    }
}

private fun formatDuration(durationMillis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis)
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

/**
 * 산책 결과 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkingResultScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRouteDetail: (List<LocationPoint>) -> Unit = {},
    viewModel: WalkingViewModel = hiltViewModel(),
    mapViewModel: KakaoMapViewModel = hiltViewModel(),
    resultViewModel: WalkingResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val emotionPhotoUri by viewModel.emotionPhotoUri.collectAsStateWithLifecycle()
    val snapshotState by mapViewModel.snapshotState.collectAsStateWithLifecycle()
    val resultUiState by resultViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current


    val session = when (val state = uiState) {
        is WalkingUiState.Completed -> {
            state.session
        }
        else -> {
            // 정상적인 플로우에서는 Completed 상태여야 함
            // 예외 상황이 발생한 경우 에러 처리
            Timber.e("WalkingResultScreen에 도달했지만 세션이 완료되지 않았습니다. 상태: $state")
            // 에러 상태로 처리하거나 기본 세션 반환 (필요시 에러 화면 표시)
            null
        }
    }

    // 세션이 없으면 에러 처리
    if (session == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "세션 정보를 불러올 수 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onNavigateBack) {
                    Text("돌아가기")
                }
            }
        }
        return
    }

    LaunchedEffect(locations) {
        if (locations.isNotEmpty()) {
            mapViewModel.setLocations(locations)
        }
    }

    // ViewModel 정보 로깅 (디버깅용)
    LaunchedEffect(viewModel, emotionPhotoUri, locations) {
        Timber.d("🚶 WalkingResultScreen ViewModel 상태:")
        Timber.d("  📸 emotionPhotoUri: $emotionPhotoUri")
        Timber.d("  📍 locations: ${locations.size}개")
        Timber.d("  🎯 emotionText: ${viewModel.emotionText.value}")
        Timber.d("  📊 uiState: ${viewModel.uiState.value}")
        Timber.d("  🗺️ snapshotState: ${snapshotState != null}")
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "오늘 산책도 성공하셨군요!\n오늘의 산책을 요약해드립니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "닫기",
                )
            }
        }

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // emotionPhotoUri가 있으면 사진 표시, 없으면 지도 스냅샷 표시
                if (emotionPhotoUri != null) {
                    // PhotoUrl에서 bitmap으로 변환하여 표시
                    val bitmap = remember(emotionPhotoUri) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(emotionPhotoUri!!)
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "산책 사진",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                } else {
                    // emotionPhotoUri가 없으면 지도 스냅샷 표시
                    snapshotState?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "지도 스냅샷",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                // 항상 경로 표시 (사진이나 지도 위에)
                PathThumbnail(
                    locations = locations.ifEmpty { session.locations },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                    pathColor = if (emotionPhotoUri != null) Color(0xFF2196F3) else Color.White, // 사진 위에서는 파란색, 지도 위에서는 흰색
                    endpointColor = if (emotionPhotoUri != null) Color(0xFF2196F3) else Color.White,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatItem(title = "산책 시간", value = formatDuration(session.duration))
                    StatItem(title = "걸음 수", value = "%,d".format(session.stepCount))
                    StatItem(title = "총 거리", value = formatDistance(session.totalDistance))
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFDDDDDD),
                )

                Text(
                    text = "이번주 목표 달성 현황",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                WeekCompletionRow(
                    sessionsThisWeek =
                        (resultUiState as? WalkingResultUiState.Success)?.sessionsThisWeek.orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onNavigateBack() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "홈 으로")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Button(
                onClick = { /* TODO: 공유 기능 연동 */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "결과 공유하기")
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

