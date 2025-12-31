package team.swyp.sdu.ui.walking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.White
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 산책 감정 기록 완료 팝업
 * Figma 디자인 기반
 */
@Composable
fun WalkingResultCompletionDialog(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onConfirm,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = SemanticColor.backgroundWhitePrimary,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 제목 및 설명
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("산책 감정 기록이 ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF2ABB42), // 완료 텍스트 색상
                                ),
                            ) {
                                append("완료")
                            }
                            append("되었습니다!")
                        },
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SemanticColor.textBorderPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = "완료된 산책 기록을 친구들에게 공유할 수 있어요.",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.textBorderSecondary,
                    )
                    Spacer(Modifier.height(20.dp))

                    // 확인 버튼
                    CtaButton(
                        text = "확인",
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                

            }
        }
    }
}


/**
 * 산책 완료 다이얼로그 미리보기
 */
@Preview(showBackground = true)
@Composable
private fun WalkingCompleteDialogPreview() {
    WalkItTheme {
        WalkingResultCompletionDialog(
            onConfirm = {}
        )
    }
}

