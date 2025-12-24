package team.swyp.sdu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import team.swyp.sdu.ui.theme.Green4
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.White
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 확인 다이얼로그 컴포넌트
 *
 * 사용자에게 확인을 요청하는 재사용 가능한 다이얼로그입니다.
 * Figma 디자인 기반으로 구현되었습니다.
 *
 * @param title 다이얼로그 제목
 * @param message 다이얼로그 메시지
 * @param negativeButtonText 부정 버튼 텍스트 (기본값: "아니요")
 * @param positiveButtonText 긍정 버튼 텍스트 (기본값: "예")
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onNegative 부정 버튼 클릭 콜백
 * @param onPositive 긍정 버튼 클릭 콜백
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    negativeButtonText: String = "아니요",
    positiveButtonText: String = "예",
    onDismiss: () -> Unit,
    onNegative: () -> Unit,
    onPositive: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color(0x0F000000),
                    ambientColor = Color(0x0F000000),
                )
                .background(
                    color = White,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 제목 및 메시지 영역
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 제목
                    Text(
                        text = title,
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Grey10,
                        textAlign = TextAlign.Center,
                    )

                    // 메시지
                    Text(
                        text = message,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Grey7,
                        textAlign = TextAlign.Center,
                    )
                }

                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 부정 버튼 (아니요)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .clickable(onClick = {
                                onNegative()
                                onDismiss()
                            })
                            .border(
                                width = 1.dp,
                                color = Green4,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .background(
                                color = White,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = negativeButtonText,
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = Green4,
                        )
                    }

                    // 긍정 버튼 (예)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .clickable(onClick = {
                                onPositive()
                                onDismiss()
                            })
                            .background(
                                color = Green4,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = positiveButtonText,
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = White,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "기본 확인 다이얼로그")
@Composable
private fun ConfirmDialogPreview() {
    team.swyp.sdu.ui.theme.WalkItTheme {
        ConfirmDialog(
            title = "변경된 사항이 있습니다.",
            message = "저장하시겠습니까?",
            onDismiss = {},
            onNegative = {},
            onPositive = {},
        )
    }
}




