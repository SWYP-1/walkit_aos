package team.swyp.sdu.ui.record.dailyrecord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.data.model.WalkingSession
import team.swyp.sdu.ui.components.RouteThumbnail
import team.swyp.sdu.ui.theme.SemanticColor
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

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().aspectRatio(1f),
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
            .height(200.dp)
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
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
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
    }
}

/**
 * 세션 인디케이터 행 (하단 원형 indicator)
 *
 * 현재 선택된 세션을 8dp 크기의 원형 indicator로 표시합니다.
 *
 * @param totalCount 전체 세션 개수
 * @param selectedIndex 현재 선택된 세션 인덱스
 * @param modifier Modifier
 */
@Composable
fun SessionIndicatorRow(
    totalCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalCount) { index ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            SemanticColor.textBorderSecondary // 선택된 경우: 검은색
                        } else {
                            SemanticColor.iconDisabled // 선택되지 않은 경우: 연한 회색
                        }
                    ),
            )

            // 마지막이 아니면 간격 추가
            if (index < totalCount - 1) {
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
    }
}




