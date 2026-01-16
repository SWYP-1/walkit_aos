package swyp.team.walkit.ui.record.dailyrecord

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.data.model.LocationPoint
import swyp.team.walkit.data.model.WalkingSession
import swyp.team.walkit.ui.components.KakaoMapView
import swyp.team.walkit.ui.walking.components.PathThumbnail
import swyp.team.walkit.ui.theme.Grey4
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.DateUtils
import timber.log.Timber
import java.io.File

/**
 * 세션 썸네일 컴포넌트 (선택된 세션 하나만 표시)
 *
 * @param session 선택된 세션 (nullable)
 * @param onExternalClick 세션 선택 콜백
 * @param modifier Modifier
 */
@Composable
fun SessionThumbnailList(
    session: WalkingSession?,
    isSnapshotLoading: Boolean,
    isDataLoading: Boolean = false, // 데이터 로딩 상태 추가
    onExternalClick: (WalkingSession) -> Unit,
    dateString: String,
    modifier: Modifier = Modifier,
    thumbnailCoordinates: androidx.compose.runtime.MutableState<androidx.compose.ui.layout.LayoutCoordinates?>? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateString, // 한국어 형식으로 변환된 날짜 표시
                // body L/medium
                style = MaterialTheme.walkItTypography.bodyL.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary
            )

            IconButton(
                onClick = { onExternalClick(session ?: return@IconButton) },
                enabled = !isDataLoading, // 데이터 로딩 중에는 버튼 비활성화
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_external),
                    contentDescription = "external",
                    tint = SemanticColor.iconGrey
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        if (session != null) {
            Timber.d("🔗 [SessionThumbnailList] SessionThumbnailItem으로 좌표 상태 전달 - thumbnailCoordinates: ${thumbnailCoordinates != null}")
            SessionThumbnailItem(
                session = session,
                isSelected = true,
                onClick = onExternalClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                isSnapshotLoading = isSnapshotLoading,
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
    onClick: (WalkingSession) -> Unit,
    modifier: Modifier = Modifier,
    isSnapshotLoading: Boolean = false,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(Color.White)
                        .padding(2.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 스냅샷 생성 로딩 오버레이
            if (isSnapshotLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                }
            }

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
                // 이미지가 없어도 서버 동기화된 세션이므로 경로 썸네일 표시
                PathThumbnail(
                    locations = session.locations,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}