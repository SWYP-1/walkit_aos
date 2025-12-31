package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.ui.home.LocationAgreementUiState
import team.swyp.sdu.ui.home.LocationAgreementViewModel
import team.swyp.sdu.ui.theme.Green4
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.White
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 위치 권한 동의 다이얼로그
 *
 * 산책 기능을 사용하기 위해 위치 권한이 필요하다는 안내 다이얼로그입니다.
 */
@Composable
fun LocationAgreementDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit,
    onDenyPermission: () -> Unit,
) {

    Dialog(
        onDismissRequest = onDenyPermission,
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
                .padding(top = 24.dp,start =24.dp, end = 24.dp),
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
                        text = "위치 서비스 사용 동의",
                        style = MaterialTheme.walkItTypography.bodyL.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SemanticColor.textBorderPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))

                    // 메시지
                    Text(
                        text = "산책 중인 위치를 바탕으로 날씨 정보를 \n" +
                                "알려주고 나만의 산책 경로를 기록해요",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                        color = SemanticColor.textBorderSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                // 버튼 영역
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 긍정 버튼 (예)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                onGrantPermission()
                                onDismiss()
                            })
                            .background(
                                color = SemanticColor.stateGreenPrimary,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "동의하고 시작하기",
                            style = MaterialTheme.walkItTypography.bodyM.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SemanticColor.textBorderPrimaryInverse,
                        )

                    }
                    val shape = RoundedCornerShape(8.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "나중에 할게요",
                        style = MaterialTheme.walkItTypography.captionM.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = SemanticColor.textBorderSecondary,
                        modifier = Modifier
                            .clip(shape)
                            .clickable(
                                onClick = {
                                    onDenyPermission()
                                    onDismiss()
                                }
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                    )
                    Spacer(Modifier.height(12.dp))

                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun PreviewLocationAgreementDialogWithTheme() {
    WalkItTheme {
        LocationAgreementDialog(
            onDismiss = {},
            onGrantPermission = {},
            onDenyPermission = {}
        )
    }
}

