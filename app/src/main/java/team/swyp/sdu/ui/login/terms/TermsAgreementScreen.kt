package team.swyp.sdu.ui.login.terms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import team.swyp.sdu.R
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.components.CtaButton
import team.swyp.sdu.ui.components.LoadingOverlay
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 약관 동의 화면 Route
 *
 * ViewModel 주입 및 상태 수집을 담당합니다.
 */
@Composable
fun TermsAgreementRoute(
    onNavigateBack: () -> Unit = {},
    onNavigateNext: () -> Unit = {},
    viewModel: TermsAgreementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TermsAgreementScreen(
        uiState = uiState,
        onTermsAgreedChange = viewModel::updateTermsAgreed,
        onPrivacyAgreedChange = viewModel::updatePrivacyAgreed,
        onLocationAgreedChange = viewModel::updateLocationAgreed,
        onMarketingConsentChange = viewModel::updateMarketingConsent,
        onSubmit = {
            viewModel.submitTermsAgreement(
                onSuccess = onNavigateNext,
                onError = { /* 에러는 UI State에 표시됨 */ },
            )
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * 약관 동의 화면
 *
 * 실제 UI 컴포넌트를 렌더링합니다.
 */
@Composable
fun TermsAgreementScreen(
    uiState: TermsAgreementUiState,
    onTermsAgreedChange: (Boolean) -> Unit,
    onPrivacyAgreedChange: (Boolean) -> Unit,
    onLocationAgreedChange: (Boolean) -> Unit,
    onMarketingConsentChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        AppHeader(
            title = "",
            onNavigateBack = onNavigateBack,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_clear),
                contentDescription = "닫기",
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // 제목
            Text(
                text = "약관에 동의해주세요",
                style = MaterialTheme.walkItTypography.headingXL.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = SemanticColor.textBorderPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "서비스를 이용하기 위해\n필수 약관에 동의가 필요합니다",
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = SemanticColor.textBorderTertiary,
            )

            Spacer(Modifier.height(40.dp))

            // 필수 약관 섹션
            Text(
                text = "필수 약관",
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary,
            )

            Spacer(Modifier.height(16.dp))

            // 서비스 이용약관
            TermsCheckboxItem(
                checked = uiState.termsAgreed,
                onCheckedChange = onTermsAgreedChange,
                label = "서비스 이용약관",
                isRequired = true,
            )

            Spacer(Modifier.height(12.dp))

            // 개인정보 처리방침
            TermsCheckboxItem(
                checked = uiState.privacyAgreed,
                onCheckedChange = onPrivacyAgreedChange,
                label = "개인정보 처리방침",
                isRequired = true,
            )

            Spacer(Modifier.height(12.dp))

            // 위치 정보 이용약관
            TermsCheckboxItem(
                checked = uiState.locationAgreed,
                onCheckedChange = onLocationAgreedChange,
                label = "위치 정보 이용약관",
                isRequired = true,
            )

            Spacer(Modifier.height(32.dp))

            // 선택 약관 섹션
            Text(
                text = "선택 약관",
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary,
            )

            Spacer(Modifier.height(16.dp))

            // 마케팅 정보 수신 동의
            TermsCheckboxItem(
                checked = uiState.marketingConsent,
                onCheckedChange = onMarketingConsentChange,
                label = "마케팅 정보 수신 동의",
                isRequired = false,
            )

            Spacer(Modifier.height(24.dp))

            // 에러 메시지 표시
            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.walkItTypography.captionM,
                    color = Color(0xFFFF3B30), // 에러 색상
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // 하단 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            CtaButton(
                text = "동의하고 시작하기",
                onClick = onSubmit,
                enabled = uiState.canProceed && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // 로딩 오버레이
    LoadingOverlay(isLoading = uiState.isLoading)
}

/**
 * 약관 체크박스 아이템
 */
@Composable
private fun TermsCheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isRequired: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SemanticColor.stateGreenPrimary,
                uncheckedColor = SemanticColor.textBorderSecondary,
            ),
        )

        Spacer(Modifier.padding(start = 8.dp))

        Text(
            text = if (isRequired) "$label (필수)" else label,
            style = MaterialTheme.walkItTypography.bodyM.copy(
                fontWeight = if (isRequired) FontWeight.Medium else FontWeight.Normal
            ),
            color = SemanticColor.textBorderPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsAgreementScreenPreview() {
    WalkItTheme {
        TermsAgreementScreen(
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
            onSubmit = {},
            onNavigateBack = {},
        )
    }
}
