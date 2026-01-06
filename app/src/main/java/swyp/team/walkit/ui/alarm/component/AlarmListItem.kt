package swyp.team.walkit.ui.alarm.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.AlarmType
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 알람 리스트 아이템 컴포넌트
 *
 * Figma 디자인 기반 알람 리스트 아이템
 * - 팔로우 알람: person_add 아이콘, 확인/삭제 버튼
 * - 목표 달성 알람: award_star 아이콘, 버튼 없음
 *
 * @param alarmType 알람 타입 (FOLLOW, GOAL_ACHIEVEMENT)
 * @param message 알람 메시지
 * @param date 날짜 문자열
 * @param onConfirm 확인 버튼 클릭 핸들러 (팔로우 알람일 때만 표시)
 * @param onDelete 삭제 버튼 클릭 핸들러 (팔로우 알람일 때만 표시)
 * @param modifier Modifier
 */
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 아이콘 + 텍스트
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // 아이콘 (24x24)
                Icon(
                    painter = when (alarmType) {
                        AlarmType.FOLLOW -> painterResource(R.drawable.ic_persion_plus)
                        AlarmType.GOAL -> painterResource(R.drawable.ic_award_start)
                        AlarmType.INACTIVE_USER -> painterResource(R.drawable.ic_award_start)
                        AlarmType.MISSION_OPEN -> painterResource(R.drawable.ic_award_start)
                    },
                    contentDescription = null,
                    tint = SemanticColor.iconGrey,
                    modifier = Modifier.size(24.dp),
                )

                // 텍스트 영역
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 메시지 (body S/medium 또는 caption M/medium)
                    Text(
                        text = message,
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

                    // 날짜 (caption M/regular)
                    Text(
                        text = date,
                        style = MaterialTheme.walkItTypography.captionM,
                        color = SemanticColor.textBorderSecondary,
                    )
                }
            }

            // 오른쪽: 버튼 (팔로우 알람일 때만 표시)
            if (alarmType == AlarmType.FOLLOW) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 확인 버튼
                    onConfirm?.let {
                        Button(
                            onClick = it,
                            modifier = Modifier
                                .width(54.dp)
                                .height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SemanticColor.buttonPrimaryDefault,
                                contentColor = SemanticColor.textBorderPrimaryInverse,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 4.dp,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            Text(
                                text = "확인",
                                style = MaterialTheme.walkItTypography.captionM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = SemanticColor.textBorderPrimaryInverse,
                            )
                        }
                    }

                    // 삭제 버튼
                    onDelete?.let {
                        Button(
                            onClick = it,
                            modifier = Modifier
                                .width(54.dp)
                                .height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SemanticColor.buttonDisabled,
                                contentColor = SemanticColor.textBorderSecondary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        ) {
                            Text(
                                text = "삭제",
                                style = MaterialTheme.walkItTypography.captionM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = SemanticColor.textBorderSecondary,
                            )
                        }
                    }
                }
            }
        }

        // 하단 Divider
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
            message = "닉네임님이 워킷님을 팔로우합니다.",
            date = "2025년 12월 16일",
            onConfirm = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlarmListItemGoalAchievementPreview() {
    WalkItTheme {
        AlarmListItem(
            alarmType = AlarmType.GOAL,
            message = "목표 달성까지 1회 남았어요!",
            date = "2025년 12월 16일",
        )
    }
}

