package team.swyp.sdu.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.ui.theme.Grey10
import team.swyp.sdu.ui.theme.Grey2
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.Grey7
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography

/**
 * 약관 동의 단계 컴포넌트
 *
 * @param serviceTermsChecked 서비스 이용 약관 체크 상태
 * @param privacyPolicyChecked 개인정보처리방침 체크 상태
 * @param marketingConsentChecked 마케팅 수신 동의 체크 상태
 * @param onServiceTermsChecked 서비스 이용 약관 체크 변경 핸들러
 * @param onPrivacyPolicyChecked 개인정보처리방침 체크 변경 핸들러
 * @param onMarketingConsentChecked 마케팅 수신 동의 체크 변경 핸들러
 * @param onNext 다음 단계로 이동 핸들러
 * @param onNavigateBack 뒤로가기 핸들러
 */
@Composable
fun TermsAgreementStep(
    serviceTermsChecked: Boolean,
    privacyPolicyChecked: Boolean,
    marketingConsentChecked: Boolean,
    onServiceTermsChecked: (Boolean) -> Unit,
    onPrivacyPolicyChecked: (Boolean) -> Unit,
    onMarketingConsentChecked: (Boolean) -> Unit,
    onNext: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val canProceed = serviceTermsChecked && privacyPolicyChecked

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 약관 내용 스크롤 영역
            val termsContent = "# 개인정보 수집·이용 동의서 (샘플)\n" +
                    "\n" +
                    "본 약관은 **[서비스명]**(이하 \"회사\")가 제공하는 서비스 이용과 관련하여, 이용자의 개인정보를 어떠한 목적과 범위로 수집·이용하는지에 대해 설명합니다.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 1. 개인정보 수집 항목\n" +
                    "\n" +
                    "회사는 서비스 제공을 위해 아래와 같은 개인정보를 수집할 수 있습니다.\n" +
                    "\n" +
                    "### ① 필수 수집 항목\n" +
                    "\n" +
                    "* 이름\n" +
                    "* 이메일 주소\n" +
                    "* 휴대전화 번호\n" +
                    "* 로그인 정보 (소셜 로그인 식별자 포함)\n" +
                    "* 서비스 이용 기록, 접속 로그, 쿠키, 접속 IP 정보\n" +
                    "\n" +
                    "### ② 선택 수집 항목\n" +
                    "\n" +
                    "* 프로필 사진\n" +
                    "* 위치 정보\n" +
                    "* 생년월일\n" +
                    "* 마케팅 수신 동의 여부\n" +
                    "\n" +
                    "※ 선택 항목은 입력하지 않아도 서비스 이용에 제한이 없습니다.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 2. 개인정보 수집 및 이용 목적\n" +
                    "\n" +
                    "회사는 수집한 개인정보를 다음의 목적을 위해 이용합니다.\n" +
                    "\n" +
                    "* 회원 가입 및 본인 확인\n" +
                    "* 서비스 제공 및 운영\n" +
                    "* 고객 문의 응대 및 공지사항 전달\n" +
                    "* 서비스 이용 통계 및 품질 개선\n" +
                    "* 부정 이용 방지 및 보안 관리\n" +
                    "* 마케팅 및 프로모션 활용 (별도 동의 시)\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 3. 개인정보 보유 및 이용 기간\n" +
                    "\n" +
                    "회사는 원칙적으로 개인정보 수집 및 이용 목적이 달성된 후에는 해당 정보를 지체 없이 파기합니다.\n" +
                    "\n" +
                    "단, 관계 법령에 따라 일정 기간 보관이 필요한 경우에는 아래와 같이 보관합니다.\n" +
                    "\n" +
                    "* 계약 또는 청약 철회 기록: 5년\n" +
                    "* 대금 결제 및 재화 공급 기록: 5년\n" +
                    "* 소비자 불만 또는 분쟁 처리 기록: 3년\n" +
                    "* 로그인 기록: 3개월\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 4. 개인정보의 제3자 제공\n" +
                    "\n" +
                    "회사는 이용자의 개인정보를 원칙적으로 외부에 제공하지 않습니다.\n" +
                    "\n" +
                    "다만, 아래의 경우에는 예외로 합니다.\n" +
                    "\n" +
                    "* 이용자가 사전에 동의한 경우\n" +
                    "* 법령에 따라 제공이 요구되는 경우\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 5. 개인정보 처리 위탁\n" +
                    "\n" +
                    "회사는 원활한 서비스 제공을 위해 일부 업무를 외부 업체에 위탁할 수 있습니다.\n" +
                    "\n" +
                    "* 위탁 업무 내용: 서버 운영, 알림 발송, 고객 지원\n" +
                    "* 위탁 업체: **[위탁사명]**\n" +
                    "\n" +
                    "회사는 위탁 계약 시 개인정보 보호 관련 법령을 준수하도록 관리·감독합니다.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 6. 이용자의 권리와 행사 방법\n" +
                    "\n" +
                    "이용자는 언제든지 다음과 같은 권리를 행사할 수 있습니다.\n" +
                    "\n" +
                    "* 개인정보 열람, 수정, 삭제 요청\n" +
                    "* 개인정보 처리 정지 요청\n" +
                    "* 동의 철회 요청\n" +
                    "\n" +
                    "권리 행사는 고객센터 또는 설정 메뉴를 통해 가능합니다.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 7. 개인정보 파기 절차 및 방법\n" +
                    "\n" +
                    "* 파기 절차: 목적 달성 후 별도 보관 없이 즉시 파기\n" +
                    "* 파기 방법: 전자 파일은 복구 불가능한 방식으로 삭제, 종이 문서는 분쇄 또는 소각\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 8. 동의 거부 권리 및 불이익\n" +
                    "\n" +
                    "이용자는 개인정보 수집·이용에 대한 동의를 거부할 권리가 있습니다.\n" +
                    "\n" +
                    "다만, 필수 항목에 대한 동의를 거부할 경우 서비스 이용이 제한될 수 있습니다.\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "## 9. 개인정보 보호 책임자\n" +
                    "\n" +
                    "* 성명: **[이름]**\n" +
                    "* 직책: **[직책]**\n" +
                    "* 연락처: **[이메일 / 전화번호]**\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "본인은 위 내용을 충분히 이해하였으며, 개인정보 수집·이용에 동의합니다.\n" +
                    "\n" +
                    "* 동의 일자: _________\n" +
                    "* 이용자 서명: _________\n" +
                    "\n" +
                    "---\n" +
                    "\n" +
                    "※ 본 문서는 예시용 샘플이며, 실제 서비스 적용 시 「개인정보 보호법」 및 관련 법령에 맞게 법무 검토가 필요합니다.\n"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(445.dp)
                    .padding(horizontal = 26.dp, vertical = 48.dp)
                    .background(Grey3, RoundedCornerShape(12.dp))

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = termsContent,
                        style = MaterialTheme.walkItTypography.bodyS.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Grey10,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 체크박스 영역
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 서비스 이용 약관

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    TermsCheckbox(
                        text = "서비스 이용 약관",
                        isRequired = true,
                        checked = serviceTermsChecked,
                        onCheckedChange = onServiceTermsChecked,
                    )

                    // 개인정보처리방침
                    TermsCheckbox(
                        text = "개인정보처리방침",
                        isRequired = true,
                        checked = privacyPolicyChecked,
                        onCheckedChange = onPrivacyPolicyChecked,
                    )

                    // 마케팅 수신 동의 (선택)
                    TermsCheckbox(
                        text = "마케팅 수신 동의",
                        isRequired = false,
                        checked = marketingConsentChecked,
                        onCheckedChange = onMarketingConsentChecked,
                    )
                }

            }

            Spacer(modifier = Modifier.weight(1f))

            // 버튼 영역
            Button(
                onClick = onNext,
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canProceed) {
                        Color(0xFF767676)
                    } else {
                        Color(0xFF767676).copy(alpha = 0.5f)
                    },
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "회원가입 완료",
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * 약관 체크박스 컴포넌트
 */
@Composable
private fun TermsCheckbox(
    text: String,
    isRequired: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF52CE4B),
                uncheckedColor = Grey3,
            ),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.walkItTypography.bodyS,
                color = Grey10,
            )

            if (isRequired) {
                Text(
                    text = "*",
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color(0xFFE65C4A),
                )
            } else {
                Text(
                    text = "(선택)",
                    style = MaterialTheme.walkItTypography.bodyS,
                    color = Grey7,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsAgreementStepPreview() {
    WalkItTheme {
        TermsAgreementStep(
            serviceTermsChecked = false,
            privacyPolicyChecked = false,
            marketingConsentChecked = false,
            onServiceTermsChecked = {},
            onPrivacyPolicyChecked = {},
            onMarketingConsentChecked = {},
            onNext = {},
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsAgreementStepCheckedPreview() {
    WalkItTheme {
        TermsAgreementStep(
            serviceTermsChecked = true,
            privacyPolicyChecked = true,
            marketingConsentChecked = true,
            onServiceTermsChecked = {},
            onPrivacyPolicyChecked = {},
            onMarketingConsentChecked = {},
            onNext = {},
            onNavigateBack = {},
        )
    }
}

