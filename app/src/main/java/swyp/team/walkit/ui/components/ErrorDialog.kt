package swyp.team.walkit.ui.components

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
import swyp.team.walkit.ui.theme.Green4
import swyp.team.walkit.ui.theme.Grey10
import swyp.team.walkit.ui.theme.Grey7
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.White
import swyp.team.walkit.ui.theme.walkItTypography

@Composable
fun ErrorDialog(
    title: String = "오류",
    message: String,
    onDismiss: () -> Unit
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
                    // 긍정 버튼 (예)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .clickable(onClick = {
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
                            text = "확인",
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

@Preview
@Composable
fun ErrorDialogPreview(modifier: Modifier = Modifier) {
    WalkItTheme {
        ErrorDialog(title = "title", message = "오류가 발생했습니다.", onDismiss = {})

    }
}