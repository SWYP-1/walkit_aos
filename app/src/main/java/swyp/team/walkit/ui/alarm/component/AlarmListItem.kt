package swyp.team.walkit.ui.alarm.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.AlarmType
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun AlarmListItem(
    alarmType: AlarmType,
    message: String,
    date: String,
    onConfirm: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SemanticColor.backgroundWhitePrimary)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            /* -------------------- 왼쪽 영역 -------------------- */
            Row(
                modifier = Modifier
                    .weight(1f, fill = true),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = when (alarmType) {
                        AlarmType.FOLLOW -> painterResource(R.drawable.ic_persion_plus)
                        AlarmType.GOAL,
                        AlarmType.INACTIVE_USER,
                        AlarmType.MISSION_OPEN -> painterResource(R.drawable.ic_award_start)
                    },
                    contentDescription = null,
                    tint = SemanticColor.iconGrey,
                    modifier = Modifier.size(24.dp),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(), // ⭐ 핵심
                ) {
                    Text(
                        text = message,
                        overflow = TextOverflow.Ellipsis,
                        style = if (alarmType == AlarmType.FOLLOW) {
                            MaterialTheme.walkItTypography.captionM.copy(
                                fontWeight = FontWeight.Medium,
                            )
                        } else {
                            MaterialTheme.walkItTypography.bodyS.copy(
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        color = SemanticColor.textBorderPrimary,
                    )

                    Text(
                        text = date,
                        style = MaterialTheme.walkItTypography.captionM,
                        color = SemanticColor.textBorderSecondary,
                    )
                }
            }

            /* -------------------- 오른쪽 버튼 영역 -------------------- */
            if (alarmType == AlarmType.FOLLOW) {
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth(), // ⭐ 고정폭 제거
                ) {
                    onConfirm?.let {
                        Button(
                            onClick = it,
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SemanticColor.buttonPrimaryDefault,
                                contentColor = SemanticColor.textBorderPrimaryInverse,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            Text(
                                text = "확인",
                                style = MaterialTheme.walkItTypography.captionM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }

                    onDelete?.let {
                        Button(
                            onClick = it,
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SemanticColor.buttonDisabled,
                                contentColor = SemanticColor.textBorderSecondary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            Text(
                                text = "삭제",
                                style = MaterialTheme.walkItTypography.captionM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = SemanticColor.textBorderSecondaryInverse,
            thickness = 1.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListItemFollowPreview() {
    WalkItTheme {
        AlarmListItem(
            alarmType = AlarmType.FOLLOW,
            message = "닉네임님이 워킷님을 팔로우합니다. 메시지가 길어질 경우에도 버튼은 잘리지 않습니다.",
            date = "2025년 12월 16일",
            onConfirm = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListItemGoalPreview() {
    WalkItTheme {
        AlarmListItem(
            alarmType = AlarmType.GOAL,
            message = "목표 달성까지 1회 남았어요!",
            date = "2025년 12월 16일",
        )
    }
}
