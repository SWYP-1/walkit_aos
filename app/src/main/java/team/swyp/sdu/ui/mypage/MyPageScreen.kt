package team.swyp.sdu.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import team.swyp.sdu.core.DataState
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.ui.components.AppHeader
import team.swyp.sdu.ui.mypage.component.MyPageAccountActions
import team.swyp.sdu.ui.mypage.component.MyPageCharacterEditButton
import team.swyp.sdu.ui.mypage.component.MyPageUserInfo
import team.swyp.sdu.ui.mypage.component.MyPageSettingsSection
import team.swyp.sdu.ui.mypage.component.MyPageStatsSection
import team.swyp.sdu.ui.mypage.model.StatsData
import team.swyp.sdu.ui.mypage.model.UserInfoData
import team.swyp.sdu.ui.theme.Grey3
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme

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
                onNavigateBack = onNavigateBack,
            )
        }

        // 사용자 정보
        item {
            when (val userState = uiState.userInfo) {
                is DataState.Loading -> {
                    MyPageUserInfo(
                        nickname = "",
                        grade = null
                    )
                }
                is DataState.Success -> {
                    MyPageUserInfo(
                        nickname = userState.data.nickname,
                        profileImageUrl = userState.data.profileImageUrl,
                        grade = userState.data.grade
                    )
                }
                is DataState.Error -> {
                    MyPageUserInfo(
                        nickname = "게스트",
                        grade = null
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 캐릭터 수정 버튼
        item {
            MyPageCharacterEditButton(onNavigateCharacterEdit = onNavigateCharacterEdit)
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
                    onNavigateMission = onNavigateMission
                )

                Spacer(Modifier.height(32.dp))

                // 계정 액션
                MyPageAccountActions(
                    onLogout = onLogout,
                    onWithdraw = onWithdraw,
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
                )
            ),
            onNavigateCharacterEdit = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateNotificationSetting = {},
            onNavigateBack = {},
            onLogout = {},
            onNavigateMission = {},
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
                stats = DataState.Error("통계 로딩 실패")
            ),
            onNavigateCharacterEdit = {},
            onNavigateUserInfoEdit = {},
            onNavigateGoalManagement = {},
            onNavigateNotificationSetting = {},
            onNavigateBack = {},
            onLogout = {},
            onNavigateMission = {},
            onWithdraw = {},
        )
    }
}