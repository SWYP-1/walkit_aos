package team.swyp.sdu.ui.login.terms

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 약관 동의 다이얼로그 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun TermsAgreementDialogRoute(
    onDismiss: () -> Unit,
    onTermsAgreedUpdated: () -> Unit,
    viewModel: TermsAgreementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    TermsAgreementDialog(
        uiState = uiState,
        onTermsAgreedChange = viewModel::updateTermsAgreed,
        onPrivacyAgreedChange = viewModel::updatePrivacyAgreed,
        onLocationAgreedChange = viewModel::updateLocationAgreed,
        onMarketingConsentChange = viewModel::updateMarketingConsent,
        onAllAgreedChange = { agreed ->
            viewModel.toggleAllAgreements(agreed)
        },
        onTermsClick = {
            // 서비스 이용 약관 링크 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b98027b91ccde7032ce622")
            )
            context.startActivity(intent)
        },
        onPrivacyClick = {
            // 개인정보처리방침 링크 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b9805f9f4df589697a27c5")
            )
            context.startActivity(intent)
        },
        onLocationClick = {
            // 위치 정보 제공 동의 상세 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b980a09bafdba8e79fb042")
            )
            context.startActivity(intent)
        },
        onMarketingClick = {
            // 마케팅 수신 동의 상세 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://walkit.app/marketing-consent"))
            context.startActivity(intent)
        },
        onSubmit = {
            viewModel.submitTermsAgreement(
                onTermsAgreedUpdated = onTermsAgreedUpdated,
                onError = { /* 에러는 UI State에 표시됨 */ },
            )
        },
        onDismiss = onDismiss,
    )
}

/**
 * 약관 동의 다이얼로그 내용
 *
 * 다이얼로그의 실제 내용을 렌더링합니다.
 * 프리뷰에서도 사용할 수 있도록 별도 함수로 분리했습니다.
 */
@Composable
internal fun TermsAgreementDialogContent(
    uiState: TermsAgreementUiState,
    onTermsAgreedChange: (Boolean) -> Unit,
    onPrivacyAgreedChange: (Boolean) -> Unit,
    onLocationAgreedChange: (Boolean) -> Unit,
    onMarketingConsentChange: (Boolean) -> Unit,
    onAllAgreedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLocationClick: () -> Unit,
    onMarketingClick: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SemanticColor.backgroundWhiteSecondary,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(20.dp).align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(
                        rememberSaveable(saver = ScrollState.Saver) {
                            ScrollState(0)
                        }
                    ),
            ) {
                // 전체 동의하기
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Row 전체 클릭 시 전체 동의 상태 토글
                            onAllAgreedChange(!uiState.isAllAgreed)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.isAllAgreed,
                        onCheckedChange = { newValue ->
                            // Checkbox 클릭 시에도 동일하게 처리
                            onAllAgreedChange(newValue)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SemanticColor.stateGreenPrimary,
                            uncheckedColor = SemanticColor.textBorderTertiary,
                        ),
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(Modifier.width(16.dp))

                    Text(
                        text = "전체 동의하기",
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                        color = SemanticColor.textBorderPrimary,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 구분선
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SemanticColor.backgroundWhiteQuaternary),
                )

                Spacer(Modifier.height(12.dp))

                // 약관 목록
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 서비스 이용 약관
                    TermsItem(
                        checked = uiState.termsAgreed,
                        onCheckedChange = onTermsAgreedChange,
                        label = "서비스 이용 약관",
                        isRequired = true,
                        onArrowClick = onTermsClick,
                    )

                    // 개인정보처리방침
                    TermsItem(
                        checked = uiState.privacyAgreed,
                        onCheckedChange = onPrivacyAgreedChange,
                        label = "개인정보처리방침",
                        isRequired = true,
                        onArrowClick = onPrivacyClick,
                    )

                    // 위치 정보 제공 동의
                    TermsItem(
                        checked = uiState.locationAgreed,
                        onCheckedChange = onLocationAgreedChange,
                        label = "위치 정보 제공 동의",
                        isRequired = true,
                        onArrowClick = onLocationClick,
                    )

                    // 마케팅 수신 동의 (선택)
                    TermsItem(
                        checked = uiState.marketingConsent,
                        onCheckedChange = onMarketingConsentChange,
                        label = "마케팅 수신 동의 (선택)",
                        isRequired = false,
                        onArrowClick = onMarketingClick,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // 하단 버튼
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CtaButton(
                    text = "가입하기",
                    onClick = onSubmit,
                    enabled = uiState.canProceed && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current, // 최신 권장
                            onClick = onDismiss
                        )
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "닫기",
                        style = MaterialTheme.walkItTypography.bodyM.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = SemanticColor.textBorderSecondary
                    )
                }



            }
        }

        // 로딩 오버레이
        LoadingOverlay(isLoading = uiState.isLoading)
    }
}

/**
 * 약관 동의 다이얼로그
 *
 * Figma 디자인에 맞춘 약관 동의 다이얼로그입니다.
 * 로그인 성공 시 하단에 표시됩니다.
 */
@Composable
fun TermsAgreementDialog(
    uiState: TermsAgreementUiState,
    onTermsAgreedChange: (Boolean) -> Unit,
    onPrivacyAgreedChange: (Boolean) -> Unit,
    onLocationAgreedChange: (Boolean) -> Unit,
    onMarketingConsentChange: (Boolean) -> Unit,
    onAllAgreedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLocationClick: () -> Unit,
    onMarketingClick: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            TermsAgreementDialogContent(
                uiState = uiState,
                onTermsAgreedChange = onTermsAgreedChange,
                onPrivacyAgreedChange = onPrivacyAgreedChange,
                onLocationAgreedChange = onLocationAgreedChange,
                onMarketingConsentChange = onMarketingConsentChange,
                onAllAgreedChange = onAllAgreedChange,
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick,
                onLocationClick = onLocationClick,
                onMarketingClick = onMarketingClick,
                onSubmit = onSubmit,
                onDismiss = onDismiss,
                modifier = modifier.padding(bottom = 16.dp),
            )
        }
    }
}

/**
 * 약관 항목 컴포넌트
 */
@Composable
private fun TermsItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean,
    onArrowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 체크박스와 텍스트 영역: 체크박스 토글
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    // 체크박스와 텍스트 영역 클릭 시 체크박스 토글
                    onCheckedChange(!checked)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { newValue ->
                    // Checkbox 직접 클릭 시에도 동일하게 처리
                    onCheckedChange(newValue)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = SemanticColor.stateGreenPrimary,
                    uncheckedColor = SemanticColor.textBorderTertiary,
                ),
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                    color = SemanticColor.textBorderPrimary,
                )
                Spacer(Modifier.width(2.dp))
                if (isRequired) {
                    Text(
                        text = "*",
                        style = MaterialTheme.walkItTypography.bodyS,
                        color = SemanticColor.stateRedPrimary,
                    )
                }
            }
        }

        // 화살표 아이콘: 약관 상세 보기
        IconButton(
            onClick = onArrowClick,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "약관 상세 보기",
                tint = SemanticColor.iconGrey,
            )
        }

    }
}

@Preview(showBackground = true, name = "약관 동의 다이얼로그 - 기본 상태")
@Composable
private fun TermsAgreementDialogPreview() {
    WalkItTheme {
        TermsAgreementDialog(
            uiState = TermsAgreementUiState(
                termsAgreed = true,
                privacyAgreed = true,
                locationAgreed = false,
                marketingConsent = false,
            ),
            onTermsAgreedChange = {},
            onPrivacyAgreedChange = {},
            onLocationAgreedChange = {},
            onMarketingConsentChange = {},
            onAllAgreedChange = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onLocationClick = {},
            onMarketingClick = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, name = "약관 동의 콘텐츠 - 모두 동의", heightDp = 600)
@Composable
private fun TermsAgreementContentPreview_AllChecked() {
    WalkItTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            TermsAgreementDialogContent(
                uiState = TermsAgreementUiState(
                    termsAgreed = true,
                    privacyAgreed = true,
                    locationAgreed = true,
                    marketingConsent = true,
                ),
                onTermsAgreedChange = {},
                onPrivacyAgreedChange = {},
                onLocationAgreedChange = {},
                onMarketingConsentChange = {},
                onAllAgreedChange = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onLocationClick = {},
                onMarketingClick = {},
                onSubmit = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "약관 동의 콘텐츠 - 모두 미동의", heightDp = 600)
@Composable
private fun TermsAgreementContentPreview_NoneChecked() {
    WalkItTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            TermsAgreementDialogContent(
                uiState = TermsAgreementUiState(
                    termsAgreed = false,
                    privacyAgreed = false,
                    locationAgreed = false,
                    marketingConsent = false,
                ),
                onTermsAgreedChange = {},
                onPrivacyAgreedChange = {},
                onLocationAgreedChange = {},
                onMarketingConsentChange = {},
                onAllAgreedChange = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onLocationClick = {},
                onMarketingClick = {},
                onSubmit = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "약관 동의 콘텐츠 - 필수만 동의", heightDp = 600)
@Composable
private fun TermsAgreementContentPreview_RequiredOnly() {
    WalkItTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            TermsAgreementDialogContent(
                uiState = TermsAgreementUiState(
                    termsAgreed = true,
                    privacyAgreed = true,
                    locationAgreed = true,
                    marketingConsent = false,
                ),
                onTermsAgreedChange = {},
                onPrivacyAgreedChange = {},
                onLocationAgreedChange = {},
                onMarketingConsentChange = {},
                onAllAgreedChange = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onLocationClick = {},
                onMarketingClick = {},
                onSubmit = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "약관 동의 콘텐츠 - 부분 동의", heightDp = 600)
@Composable
private fun TermsAgreementContentPreview_PartialChecked() {
    WalkItTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            TermsAgreementDialogContent(
                uiState = TermsAgreementUiState(
                    termsAgreed = true,
                    privacyAgreed = false,
                    locationAgreed = true,
                    marketingConsent = false,
                ),
                onTermsAgreedChange = {},
                onPrivacyAgreedChange = {},
                onLocationAgreedChange = {},
                onMarketingConsentChange = {},
                onAllAgreedChange = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onLocationClick = {},
                onMarketingClick = {},
                onSubmit = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "약관 동의 콘텐츠 - 로딩 상태", heightDp = 600)
@Composable
private fun TermsAgreementContentPreview_Loading() {
    WalkItTheme {
        Surface(color = androidx.compose.ui.graphics.Color.White) {
            TermsAgreementDialogContent(
                uiState = TermsAgreementUiState(
                    termsAgreed = true,
                    privacyAgreed = true,
                    locationAgreed = true,
                    marketingConsent = true,
                    isLoading = true,
                ),
                onTermsAgreedChange = {},
                onPrivacyAgreedChange = {},
                onLocationAgreedChange = {},
                onMarketingConsentChange = {},
                onAllAgreedChange = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onLocationClick = {},
                onMarketingClick = {},
                onSubmit = {},
                onDismiss = {},
            )
        }
    }
}

/**
 * 약관 동의 전체 화면 오버레이
 *
 * 기존 Dialog 대신 Box를 사용한 전체 화면 오버레이 방식
 */
@Composable
fun TermsAgreementOverlay(
    isVisible: Boolean,
    uiState: TermsAgreementUiState,
    onTermsAgreedChange: (Boolean) -> Unit,
    onPrivacyAgreedChange: (Boolean) -> Unit,
    onLocationAgreedChange: (Boolean) -> Unit,
    onMarketingConsentChange: (Boolean) -> Unit,
    onAllAgreedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLocationClick: () -> Unit,
    onMarketingClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // 반투명 배경
        ) {
            TermsAgreementDialogContent(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                onTermsAgreedChange = onTermsAgreedChange,
                onPrivacyAgreedChange = onPrivacyAgreedChange,
                onLocationAgreedChange = onLocationAgreedChange,
                onMarketingConsentChange = onMarketingConsentChange,
                onAllAgreedChange = onAllAgreedChange,
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick,
                onLocationClick = onLocationClick,
                onSubmit = onConfirm,
                onMarketingClick = onMarketingClick,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * 약관 동의 오버레이 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun TermsAgreementOverlayRoute(
    modifier: Modifier,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onTermsAgreedUpdated: () -> Unit,
    viewModel: TermsAgreementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    TermsAgreementOverlay(
        modifier = modifier,
        isVisible = isVisible,
        uiState = uiState,
        onTermsAgreedChange = viewModel::updateTermsAgreed,
        onPrivacyAgreedChange = viewModel::updatePrivacyAgreed,
        onLocationAgreedChange = viewModel::updateLocationAgreed,
        onMarketingConsentChange = viewModel::updateMarketingConsent,
        onAllAgreedChange = { agreed ->
            viewModel.toggleAllAgreements(agreed)
        },
        onTermsClick = {
            // 서비스 이용 약관 링크 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b98027b91ccde7032ce622")
            )
            context.startActivity(intent)
        },
        onPrivacyClick = {
            // 개인정보처리방침 링크 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b98027b91ccde7032ce622")
            )
            context.startActivity(intent)
        },
        onLocationClick = {
            // 위치정보 이용약관 링크 (웹사이트에 호스팅 후 실제 URL로 교체)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.notion.so/2d59b82980b98027b91ccde7032ce622")
            )
            context.startActivity(intent)
        },
        onConfirm = {
            onTermsAgreedUpdated()
        },
        onMarketingClick =  {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/2d59b82980b9802cb0e2c7f58ec65ec1"))
            context.startActivity(intent)
        },
        onDismiss = onDismiss,
    )
}




