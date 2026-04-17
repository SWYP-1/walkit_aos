package swyp.team.walkit.ui.interactivemap.bottomtab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.FollowerLatestWalkRecord
import swyp.team.walkit.domain.model.FollowerMapRecord
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.ui.components.GradeBadge
import swyp.team.walkit.ui.components.SummaryUnit
import swyp.team.walkit.ui.components.WalkingSummaryCard
import swyp.team.walkit.ui.record.friendrecord.LikeUiState
import swyp.team.walkit.ui.record.friendrecord.component.LikeButton
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.DateUtils.formatIsoToKoreanDate
import swyp.team.walkit.utils.FormatUtils.formatStepCount
import java.util.concurrent.TimeUnit

/**
 * 친구 핀 클릭 시 표시되는 바텀시트 콘텐츠
 *
 * 기본 친구 정보를 즉시 표시하고, 최근 산책 상세를 비동기로 로드하여 추가 표시한다.
 *
 * @param record               클릭된 팔로워 산책 기록 (지도 마커용)
 * @param latestWalkRecord     팔로워 최근 산책 상세 (null이면 로딩 중 또는 없음)
 * @param isLoadingWalkRecord  산책 상세 로딩 중 여부
 * @param likeState            좋아요 UI 상태
 * @param onToggleLike         좋아요 토글 콜백
 */
@Composable
fun FriendPinBottomTab(
    record: FollowerMapRecord?,
    latestWalkRecord: FollowerLatestWalkRecord?,
    isLoadingWalkRecord: Boolean,
    likeState: LikeUiState,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ── 친구 정보 헤더 ──────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                val nickName = latestWalkRecord?.nickName ?: "User"
                GradeBadge(
                    grade = Grade.fromApiString(latestWalkRecord?.grade ?: record?.grade ?: "SEED"),
                    level = latestWalkRecord?.level,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = nickName,
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = SemanticColor.textBorderPrimary
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "님",
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.Normal,
                        color = SemanticColor.textBorderPrimary
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))

        // ── 최근 산책 상세 ──────────────────────────────────────────────────
        when {
            isLoadingWalkRecord -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            latestWalkRecord != null -> {
                WalkRecordDetail(
                    walkRecord = latestWalkRecord,
                    likeState = likeState,
                    onToggleLike = onToggleLike,
                )
            }

            else -> {
                Text(
                    text = "아직 산책 기록이 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 최근 산책 기록 상세 콘텐츠
 */
@Composable
private fun WalkRecordDetail(
    walkRecord: FollowerLatestWalkRecord,
    likeState: LikeUiState,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {

        // 산책 날짜
        if (walkRecord.createdDate != null) {
            Text(
                text = walkRecord.createdDate.substringBefore("T"),
                style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.Medium,
                    color = SemanticColor.textBorderSecondary
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 산책 이미지
        if (!walkRecord.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(walkRecord.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_default_user)
                    .build(),
                contentDescription = "${walkRecord.nickName}의 산책 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        // 산책 통계 (시간 / 걸음수)
        WalkingSummaryCard(
            leftLabel = "걸음 수",
            leftValue = formatStepCount(walkRecord.stepCount),
            leftUnit = SummaryUnit.Step("걸음"),
            rightLabel = "누적 산책 시간",
            rightUnit = SummaryUnit.Time(walkRecord.totalTime),
//            header = {
//                walkRecord.createdDate.let { date ->
//                    Text(
//                        text = formatIsoToKoreanDate(date),
//                        // caption M/regular
//                        style = MaterialTheme.walkItTypography.captionM.copy(
//                            fontWeight = FontWeight.Normal
//                        ),
//                        color = SemanticColor.textBorderTertiary
//                    )
//                }
//            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 좋아요 버튼
        LikeButton(
            state = likeState,
            onToggleLike = onToggleLike,
        )
    }
}

@Composable
private fun WalkStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/** 밀리초를 "mm분 ss초" 형식으로 변환한다. */
private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (minutes > 0) "${minutes}분 ${seconds}초" else "${seconds}초"
}
