package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import swyp.team.walkit.ui.theme.Black
import swyp.team.walkit.ui.theme.Grey3
import swyp.team.walkit.ui.theme.Grey7
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.White
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.Red5
import swyp.team.walkit.ui.theme.walkItTypography

/**
 * 텍스트 강조 정보
 *
 * @param text 강조할 텍스트
 * @param color 강조 색상 (기본값: 빨간색)
 */
data class TextHighlight(
    val text: String,
    val color: Color = Red5,
)

/**
 * 산책 중 경고 다이얼로그
 *
 * 산책 종료 시 기록이 저장되지 않는다는 경고를 표시하는 다이얼로그입니다.
 * Figma 디자인 기반으로 구현되었습니다.
 *
 * @param title 다이얼로그 제목 (기본값: "산책 기록이 저장되지 않습니다")
 * @param message 다이얼로그 메시지 (기본값: "이대로 종료하시면 진행중인 산책 기록이\n모두 사라져요!")
 * @param titleHighlight 제목에서 강조할 텍스트 (선택사항)
 * @param cancelButtonText 중단하기 버튼 텍스트 (기본값: "중단하기")
 * @param continueButtonText 계속하기 버튼 텍스트 (기본값: "계속하기")
 * @param cancelButtonColor 중단하기 버튼 색상 (기본값: Grey3)
 * @param cancelButtonTextColor 중단하기 버튼 텍스트 색상 (기본값: Grey7)
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onCancel 중단하기 버튼 클릭 콜백
 * @param onContinue 계속하기 버튼 클릭 콜백
 */
@Composable
fun WalkingWarningDialog(
    title: String = "산책 기록이 저장되지 않습니다",
    message: String = "이대로 종료하시면 진행중인 산책 기록이\n모두 사라져요!",
    titleHighlight: TextHighlight? = null,
    cancelButtonText: String = "중단하기",
    continueButtonText: String = "계속하기",
    cancelButtonColor: Color = Grey3,
    cancelButtonTextColor: Color = Grey7,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
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
                    val titleAnnotatedString = buildAnnotatedString {
                        if (titleHighlight != null && title.contains(titleHighlight.text)) {
                            var currentIndex = 0
                            var searchIndex = title.indexOf(titleHighlight.text, currentIndex)
                            
                            while (searchIndex != -1) {
                                // 강조 전 텍스트 추가
                                if (currentIndex < searchIndex) {
                                    append(title.substring(currentIndex, searchIndex))
                                }
                                // 강조 텍스트 추가
                                withStyle(
                                    style = SpanStyle(
                                        color = titleHighlight.color,
                                    ),
                                ) {
                                    append(titleHighlight.text)
                                }
                                currentIndex = searchIndex + titleHighlight.text.length
                                searchIndex = title.indexOf(titleHighlight.text, currentIndex)
                            }
                            // 남은 텍스트 추가
                            if (currentIndex < title.length) {
                                append(title.substring(currentIndex))
                            }
                        } else {
                            append(title)
                        }
                    }

                    Text(
                        text = titleAnnotatedString,
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Black,
                        textAlign = TextAlign.Center,
                    )

                    // 메시지
                    val messageLines = message.split("\n")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        messageLines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.walkItTypography.bodyS.copy(
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = SemanticColor.textBorderSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // 버튼 영역
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 중단하기 버튼
                        Button(
                            onClick = {
                                onCancel()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cancelButtonColor,
                                contentColor = cancelButtonTextColor,
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                            ),
                        ) {
                            Text(
                                text = cancelButtonText,
                                style = MaterialTheme.walkItTypography.bodyM.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = cancelButtonTextColor,
                            )
                        }

                        // 계속하기 버튼
                        Button(
                            onClick = {
                                onContinue()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(47.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Grey10,
                                contentColor = White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                            ),
                        ) {
                            Text(
                                text = continueButtonText,
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
}

@Preview(showBackground = true, name = "기본 다이얼로그")
@Composable
private fun WalkingWarningDialogPreview() {
    swyp.team.walkit.ui.theme.WalkItTheme {
        WalkingWarningDialog(
            onDismiss = {},
            onCancel = {},
            onContinue = {},
        )
    }
}

@Preview(showBackground = true, name = "빨간색 강조 다이얼로그")
@Composable
private fun WalkingWarningDialogHighlightPreview() {
    swyp.team.walkit.ui.theme.WalkItTheme {
        WalkingWarningDialog(
            title = "산책 기록이 저장되지 않습니다",
            message = "이대로 종료하시면 진행중인 산책 기록이\n모두 사라져요!",
            titleHighlight = TextHighlight(
                text = "저장되지 않습니다",
                color = Red5,
            ),
            onDismiss = {},
            onCancel = {},
            onContinue = {},
        )
    }
}

