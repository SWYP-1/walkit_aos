package swyp.team.walkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
 */
data class TextHighlight(
    val text: String,
    val color: Color = Red5,
)

/**
 * 산책 중 경고 다이얼로그
 */
@Composable
fun WalkingWarningDialog(
    title: String = "산책 기록이 저장되지 않습니다",
    message: String = "이대로 종료하시면 진행중인 산책 기록이\n모두 사라져요!",
    titleHighlight: TextHighlight? = null,
    cancelButtonText: String = "중단하기",
    continueButtonText: String = "계속하기",
    cancelButtonColor: Color = SemanticColor.backgroundWhitePrimary,
    cancelButtonTextColor: Color = SemanticColor.textBorderPrimary,
    cancelButtonBorderColor : Color = SemanticColor.textBorderPrimary,
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

                /** 제목 + 메시지 */
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val titleAnnotatedString = buildAnnotatedString {
                        if (titleHighlight != null && title.contains(titleHighlight.text)) {
                            var currentIndex = 0
                            var searchIndex = title.indexOf(titleHighlight.text)

                            while (searchIndex != -1) {
                                if (currentIndex < searchIndex) {
                                    append(title.substring(currentIndex, searchIndex))
                                }
                                withStyle(
                                    style = SpanStyle(color = titleHighlight.color),
                                ) {
                                    append(titleHighlight.text)
                                }
                                currentIndex = searchIndex + titleHighlight.text.length
                                searchIndex =
                                    title.indexOf(titleHighlight.text, currentIndex)
                            }

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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        message.split("\n").forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.walkItTypography.bodyS,
                                color = SemanticColor.textBorderSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                /** 버튼 영역 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 중단하기
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .border(
                                width = 1.dp,
                                color = cancelButtonBorderColor,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .background(
                                color = cancelButtonColor,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                            ) {
                                onCancel()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = cancelButtonText,
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = cancelButtonTextColor,
                        )
                    }

                    // 계속하기
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .background(
                                color = Grey10,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                            ) {
                                onContinue()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
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

/* ---------- Preview ---------- */

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
