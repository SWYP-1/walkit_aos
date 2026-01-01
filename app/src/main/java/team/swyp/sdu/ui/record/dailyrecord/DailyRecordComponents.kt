package team.swyp.sdu.ui.record.dailyrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naver.maps.geometry.LatLng
import team.swyp.sdu.R
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.components.RouteThumbnail
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils
import java.io.File

/**
 * 세션 썸네일 목록 컴포넌트 (LazyRow)
 *
 * 한 번에 하나의 썸네일만 보이도록 구현 (스와이프 전까지 다음 항목이 보이지 않음)
 *
 * @param sessions 세션 목록
 * @param selectedIndex 현재 선택된 세션 인덱스
 * @param onSessionSelected 세션 선택 콜백
 * @param modifier Modifier
 */
@Composable
fun SessionThumbnailList(
    sessions: List<WalkingSession>,
    selectedIndex: Int,
    onSessionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 선택된 인덱스가 변경되면 스크롤
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in sessions.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    Column(
        modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "2025년 12월 15일 ",
            // body L/medium
            style = MaterialTheme.walkItTypography.bodyL.copy(
                fontWeight = FontWeight.Medium
            ),
            color = SemanticColor.textBorderPrimary
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(sessions) { index, session ->
                SessionThumbnailItem(
                    session = session,
                    isSelected = index == selectedIndex,
                    onClick = { onSessionSelected(index) },
                    modifier = Modifier.fillParentMaxWidth(), // 한 번에 하나만 보이도록
                )
            }
        }
    }


}

/**
 * 개별 세션 썸네일 아이템
 *
 * @param session 산책 세션
 * @param isSelected 선택 여부
 * @param onClick 클릭 콜백
 * @param modifier Modifier
 */
@Composable
fun SessionThumbnailItem(
    session: WalkingSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier
                        .background(Color.White)
                        .padding(2.dp)
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(if (isSelected) 10.dp else 12.dp)),
        ) {
            // 이미지 URI 가져오기 (localImagePath -> serverImageUrl 순서)
            val imageUri = session.getImageUri()

            if (imageUri != null) {
                // 이미지가 있으면 이미지 표시
                AsyncImage(
                    model = ImageRequest.Builder(context)
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
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                // 이미지가 없으면 경로 썸네일 표시
                RouteThumbnail(
                    locations = session.locations,
                    modifier = Modifier.fillMaxSize(),
                    height = 200.dp,
                )
            }
        }

        IconButton(
            onClick = {}, modifier = Modifier
                .size(24.dp)
                .padding(12.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_external),
                contentDescription = "external",
                tint = SemanticColor.iconBlack
            )
        }

        val startTimeWithSeconds = DateUtils.formatToTimeHHMMSS(session.startTime)
        val enTimeWithSeconds = DateUtils.formatToTimeHHMMSS(session.endTime)

        Text(
            text = "$startTimeWithSeconds ~ $enTimeWithSeconds",
            // body S/semibold
            style = MaterialTheme.walkItTypography.bodyS.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = SemanticColor.textBorderPrimaryInverse,
            modifier = Modifier.align(alignment = Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SessionThumbnailListPreview() {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val testLocations = listOf(
        LatLng(37.5665, 126.9780), // 서울 시청
        LatLng(37.5651, 126.9895), // 광화문
        LatLng(37.5796, 126.9770), // 경복궁
    )

    val mockSessions = listOf(
        WalkingSession(
            id = "session-1",
            startTime = now - 7200000, // 2시간 전
            endTime = now - 5400000,   // 1.5시간 전
            stepCount = 5000,
            locations = testLocations,
            totalDistance = 3500f,
            preWalkEmotion = team.swyp.sdu.data.model.EmotionType.HAPPY,
            postWalkEmotion = team.swyp.sdu.data.model.EmotionType.CONTENT,
            note = "오늘은 날씨가 좋아서 산책하기 좋았어요.",
            createdDate = "2024-12-05",
        ),
        WalkingSession(
            id = "session-2",
            startTime = now - 3600000, // 1시간 전
            endTime = now - 1800000,   // 30분 전
            stepCount = 3000,
            locations = testLocations.take(2),
            totalDistance = 2000f,
            preWalkEmotion = team.swyp.sdu.data.model.EmotionType.TIRED,
            postWalkEmotion = team.swyp.sdu.data.model.EmotionType.REFRESHED,
            note = "스트레스 해소를 위해 짧게 산책했어요.",
            createdDate = "2024-12-05",
        ),
        WalkingSession(
            id = "session-3",
            startTime = now - 1800000, // 30분 전
            endTime = now - 600000,    // 10분 전
            stepCount = 1500,
            locations = testLocations.take(1),
            totalDistance = 1000f,
            preWalkEmotion = team.swyp.sdu.data.model.EmotionType.CONTENT,
            postWalkEmotion = team.swyp.sdu.data.model.EmotionType.HAPPY,
            note = "퇴근 후 가벼운 산책.",
            createdDate = "2024-12-05",
        ),
    )

    team.swyp.sdu.ui.theme.WalkItTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(16.dp)
        ) {
            SessionThumbnailList(
                sessions = mockSessions,
                selectedIndex = 1, // 두 번째 세션 선택
                onSessionSelected = { /* Preview에서는 아무 동작 안 함 */ },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}