package swyp.team.walkit.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import swyp.team.walkit.core.DataState
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.ui.login.LoginViewModel
import timber.log.Timber
import swyp.team.walkit.ui.components.AppHeader
import swyp.team.walkit.ui.components.TextHighlight
import swyp.team.walkit.ui.components.WalkingWarningDialog
import swyp.team.walkit.ui.mypage.component.MyPageAccountActions
import swyp.team.walkit.ui.mypage.component.MyPageCharacterEditButton
import swyp.team.walkit.ui.mypage.component.MyPageUserInfo
import swyp.team.walkit.ui.mypage.component.MyPageSettingsSection
import swyp.team.walkit.ui.mypage.component.MyPageStatsSection
import swyp.team.walkit.ui.mypage.component.ServiceInfoFooter
import swyp.team.walkit.ui.mypage.model.StatsData
import swyp.team.walkit.ui.mypage.model.UserInfoData
import swyp.team.walkit.ui.mypage.userInfo.UserInfoManagementViewModel
import swyp.team.walkit.ui.theme.Grey3
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme



@Composable
fun MyPageRoute(
    viewModel: MyPageViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    userInfoViewModel: UserInfoManagementViewModel = hiltViewModel(),
    onNavigateCharacterEdit: () -> Unit = {},
    onNavigateUserInfoEdit: () -> Unit = {},
    onNavigateGoalManagement: () -> Unit = {},
    onNavigateNotificationSetting: () -> Unit = {},
    onNavigateMission: () -> Unit = {},
    onNavigateCustomTest: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val withdrawState by viewModel.withdrawState.collectAsStateWithLifecycle()

    // 탈퇴 확인 다이얼로그 표시 상태
    var showWithdrawConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // 탈퇴 상태에 따른 처리
    LaunchedEffect(withdrawState) {
        when (withdrawState) {
            WithdrawState.Success -> {
                Timber.d("사용자 탈퇴 성공")
                // 인증 정보 클리어
                loginViewModel.logout()
                // 백스택 제거 후 로그인 화면으로 이동
                // navController.navigate("login") { popUpTo(0) { inclusive = true } }
                onNavigateToLogin()
                // "탈퇴 완료" 토스트 메시지 표시
                Toast.makeText(context, "탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }
            is WithdrawState.Error -> {
                Timber.e("사용자 탈퇴 실패: ${(withdrawState as WithdrawState.Error).message}")
                // 사용자에게 탈퇴 실패 알림 표시
                Toast.makeText(context, "탈퇴 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                // 상태 초기화
                viewModel.resetWithdrawState()
            }
            else -> {
                // Loading, Idle 상태는 별도 처리 없음
            }
        }
    }

    MyPageScreen(
        uiState = uiState,
        onNavigateCharacterEdit = onNavigateCharacterEdit,
        onNavigateUserInfoEdit = onNavigateUserInfoEdit,
        onNavigateGoalManagement = onNavigateGoalManagement,
        onNavigateNotificationSetting = onNavigateNotificationSetting,
        onNavigateBack = onNavigateBack,
        onNavigateMission = onNavigateMission,
        onNavigateCustomTest = onNavigateCustomTest,
        onServiceTermsClick = {
            // 서비스 이용 약관 링크 (onboarding과 동일)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/2d59b82980b98027b91ccde7032ce622"))
            context.startActivity(intent)
        },
        onPrivacyPolicyClick = {
            // 개인정보처리방침 링크 (onboarding과 동일)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/2d59b82980b9805f9f4df589697a27c5"))
            context.startActivity(intent)
        },
        onMarketingConsentClick = {
            // 마케팅 수신 동의 링크 (onboarding과 동일)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/2d59b82980b9802cb0e2c7f58ec65ec1"))
            context.startActivity(intent)
        },
        onContactClick = {
            // 문의하기 - 이메일 인텐트
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:walk0it2025@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Walk It 문의사항")
            }
            context.startActivity(intent)
        },
        onCsChannelClick = {
            // CS 채널 안내 링크
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://walkit.app/cs"))
            context.startActivity(intent)
        },
        onLogout = {
            loginViewModel.logout()
            // 로그아웃 처리 후 약간의 딜레이를 두고 로그인 화면으로 이동
            // (상태 초기화가 완료될 시간을 줌)
            MainScope().launch {
                delay(100) // 100ms 딜레이
                onNavigateToLogin()
            }
        },
        onWithdraw = {
            // 탈퇴 확인 다이얼로그 표시
            showWithdrawConfirmDialog = true
        },
    )

    // 탈퇴 확인 다이얼로그
    if (showWithdrawConfirmDialog) {
        WalkingWarningDialog(
            title = "정말로 탈퇴하시겠습니까?",
            message = "탈퇴 시 모든 정보는 6개월 간 보관됩니다\n" +
                    "탈퇴한 계정은 다시 복구되지 않습니다",
            titleHighlight = TextHighlight(
                text = "탈퇴",
                color = SemanticColor.stateRedPrimary
            ),
            cancelButtonText = "아니오",
            continueButtonText = "네",
            onDismiss = { showWithdrawConfirmDialog = false },
            onCancel = { showWithdrawConfirmDialog = false },
            onContinue = {
                showWithdrawConfirmDialog = false
                viewModel.withdraw()
            }
        )
    }
}
/**
 * 마이 페이지 Screen
 *
 * 순수 UI 컴포넌트로, 상태와 콜백만 받습니다.
 * 각 데이터는 독립적으로 로딩/성공/실패 상태를 가지므로
 * 일부 데이터 로딩 실패해도 메뉴는 정상 동작합니다.
 *
 * @param uiState 마이페이지 UI 상태
 * @param onNavigateCharacterEdit 캐릭터 정보 수정 네비게이션 핸들러
 * @param onNavigateUserInfoEdit 내 정보 관리 네비게이션 핸들러
 * @param onNavigateGoalManagement 목표 관리 네비게이션 핸들러
 * @param onNavigateNotificationSetting 알림 설정 네비게이션 핸들러
 * @param onNavigateBack 뒤로가기 핸들러
 * @param onLogout 로그아웃 핸들러
 * @param onWithdraw 탈퇴하기 핸들러
 * @param modifier Modifier
 */
@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onNavigateCharacterEdit: () -> Unit,
    onNavigateUserInfoEdit: () -> Unit,
    onNavigateGoalManagement: () -> Unit,
    onNavigateNotificationSetting: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateMission: () -> Unit,
    onNavigateCustomTest: () -> Unit,
    onServiceTermsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onMarketingConsentClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onCsChannelClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onWithdraw: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SemanticColor.backgroundWhitePrimary),
        contentPadding = PaddingValues(bottom = 32.dp) // 하단 여유 공간
    ) {
        // 헤더
        item {
            AppHeader(
                title = "마이 페이지",
                showBackButton = false,
                onNavigateBack = { },
            )
        }

        // 사용자 정보
        item {
            when (val userState = uiState.userInfo) {
                is DataState.Loading -> {
                    MyPageUserInfo(
                        nickname = "",
                        grade = null,
                        consecutiveDays = 0
                    )
                }
                is DataState.Success -> {
                    val consecutiveDays = when (val consecutiveState = uiState.consecutiveDays) {
                        is DataState.Success -> consecutiveState.data
                        else -> 0
                    }
                    MyPageUserInfo(
                        nickname = userState.data.nickname,
                        profileImageUrl = userState.data.profileImageUrl,
                        grade = userState.data.grade,
                        consecutiveDays = consecutiveDays,
                    )
                }
                is DataState.Error -> {
                    MyPageUserInfo(
                        nickname = "게스트",
                        grade = null,
                        consecutiveDays = 0
                    )
                }
            }
        }

        // 캐릭터 수정 버튼
        item {
            MyPageCharacterEditButton(onNavigateCharacterEdit = onNavigateUserInfoEdit)
            Spacer(Modifier.height(32.dp))
        }

        // 구분선
        item {
            HorizontalDivider(thickness = 10.dp, color = Grey3)
        }

        // 통계 & 설정 & 계정 액션
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) {
                // 통계
                when (val statsState = uiState.stats) {
                    is DataState.Loading -> {
                        MyPageStatsSection(
                            totalStepCount = 0,
                            totalWalkingTime = 0L
                        )
                    }
                    is DataState.Success -> {
                        MyPageStatsSection(
                            totalStepCount = statsState.data.totalStepCount,
                            totalWalkingTime = statsState.data.totalWalkingTime
                        )
                    }
                    is DataState.Error -> {
                        MyPageStatsSection(
                            totalStepCount = 0,
                            totalWalkingTime = 0L
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 설정 메뉴
                MyPageSettingsSection(
                    onNavigateNotificationSetting = onNavigateNotificationSetting,
                    onNavigateUserInfoEdit = onNavigateUserInfoEdit,
                    onNavigateGoalManagement = onNavigateGoalManagement,
                    onNavigateMission = onNavigateMission,
                    onNavigateCustomTest = onNavigateCustomTest
                )

                Spacer(Modifier.height(32.dp))

                // 계정 액션
                MyPageAccountActions(
                    onLogout = onLogout,
                    onWithdraw = onWithdraw,
                )

                Spacer(Modifier.weight(1f))
                ServiceInfoFooter(
                    onTermsClick = onServiceTermsClick,
                    onPrivacyClick = onPrivacyPolicyClick,
                    onMarketingClick = onMarketingConsentClick,
                    onContactClick = onContactClick,
                    onCsChannelClick = onCsChannelClick,
                )
            }
        }
    }
}


@Preview
@Composable
private fun MyPageScreenPreview() {
    WalkItTheme {
        MyPageScreen(
            uiState = MyPageUiState(
                userInfo = DataState.Success(
                    UserInfoData(
                        nickname = "홍길동",
                        grade = Grade.TREE
                    )
                ),
                stats = DataState.Success(
                    StatsData(
                        totalStepCount = 12345,
                        totalWalkingTime = 331200000L // 92시간 (밀리초)
                    )
                ),
                consecutiveDays = DataState.Success(7)
            ),
            onNavigateCharacterEdit = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateNotificationSetting = {},
            onNavigateBack = {},
            onNavigateMission = {},
            onNavigateCustomTest = {},
            onServiceTermsClick = {},
            onPrivacyPolicyClick = {},
            onMarketingConsentClick = {},
            onContactClick = {},
            onCsChannelClick = {},
            onLogout = {},
            onWithdraw = {},
        )
    }
}

@Preview
@Composable
private fun MyPageScreenErrorPreview() {
    WalkItTheme {
        MyPageScreen(
            uiState = MyPageUiState(
                userInfo = DataState.Error("사용자 정보 로딩 실패"),
                stats = DataState.Error("통계 로딩 실패"),
                consecutiveDays = DataState.Error("연속 출석일 로딩 실패")
            ),
            onNavigateCharacterEdit = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateNotificationSetting = {},
            onNavigateBack = {},
            onNavigateMission = {},
            onNavigateCustomTest = {},
            onServiceTermsClick = {},
            onPrivacyPolicyClick = {},
            onMarketingConsentClick = {},
            onContactClick = {},
            onCsChannelClick = {},
            onLogout = {},
            onWithdraw = {},
        )
    }
}