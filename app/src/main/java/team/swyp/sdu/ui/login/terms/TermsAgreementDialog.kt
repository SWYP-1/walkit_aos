package team.swyp.sdu.ui.login.terms

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    TermsAgreementDialog(
        uiState = uiState,
        onTermsAgreedChange = viewModel::updateTermsAgreed,
        onPrivacyAgreedChange = viewModel::updatePrivacyAgreed,
        onLocationAgreedChange = viewModel::updateLocationAgreed,
        onMarketingConsentChange = viewModel::updateMarketingConsent,
        onAllAgreedChange = { agreed ->
            viewModel.toggleAllAgreements(agreed)
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
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = SemanticColor.backgroundWhiteSecondary,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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

                Spacer(Modifier.height(16.dp))

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
                        onArrowClick = { /* 약관 상세 보기 */ },
                    )

                    // 개인정보처리방침
                    TermsItem(
                        checked = uiState.privacyAgreed,
                        onCheckedChange = onPrivacyAgreedChange,
                        label = "개인정보처리방침",
                        isRequired = true,
                        onArrowClick = { /* 약관 상세 보기 */ },
                    )

                    // 위치 정보 제공 동의
                    TermsItem(
                        checked = uiState.locationAgreed,
                        onCheckedChange = onLocationAgreedChange,
                        label = "위치 정보 제공 동의",
                        isRequired = true,
                        onArrowClick = { /* 약관 상세 보기 */ },
                    )

                    // 마케팅 수신 동의 (선택)
                    TermsItem(
                        checked = uiState.marketingConsent,
                        onCheckedChange = onMarketingConsentChange,
                        label = "마케팅 수신 동의 (선택)",
                        isRequired = false,
                        onArrowClick = { /* 약관 상세 보기 */ },
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

                Text(
                    text = "닫기",
                    style = MaterialTheme.walkItTypography.bodyM.copy(
                        fontWeight = FontWeight.Normal,
                    ),
                    color = SemanticColor.textBorderSecondary,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 8.dp),
                )
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
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
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

@Preview(showBackground = true)
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
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsAgreementContentPreview_AllChecked() {
    WalkItTheme {
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
            onSubmit = {},
            onDismiss = {},
        )
    }
}




