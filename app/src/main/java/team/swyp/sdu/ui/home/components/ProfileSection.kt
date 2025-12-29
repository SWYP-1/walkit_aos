package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.R
import team.swyp.sdu.core.DataState
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.model.Weather
import team.swyp.sdu.ui.home.ProfileUiState
import team.swyp.sdu.ui.home.utils.resolveWeatherIcon
import team.swyp.sdu.ui.home.utils.resolveWeatherIconRes
import team.swyp.sdu.ui.mypage.goal.model.GoalState
import team.swyp.sdu.ui.components.InfoBadge
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.shimmer
import androidx.compose.ui.tooling.preview.Preview
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.ui.components.TestCharacterWithAnchor
import team.swyp.sdu.ui.components.createCharacterParts
import team.swyp.sdu.utils.NumberUtils.formatNumber
import kotlin.toString

/**
 * 프로필 섹션 (사용자 정보, 캐릭터, 목표 진행률)
 */
@Composable
fun ProfileSection(
    uiState: ProfileUiState, goalState: DataState<Goal>, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp) // 영역 고정: height 숫자 고정이 아님
    ) {
        when (uiState) {
            is ProfileUiState.Loading -> ProfileSkeleton()
            is ProfileUiState.Success -> ProfileContent(uiState, goalState)
            is ProfileUiState.Error -> ProfileError(message = uiState.message, onRetry = {

            })
        }
    }
}

/**
 * 프로필 콘텐츠 (성공 상태)
 */
@Composable
private fun ProfileContent(
    uiState: ProfileUiState.Success, goalState: DataState<Goal>, modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        // 배경 이미지
        uiState.character.backgroundImageName?.let { bgImageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(bgImageUrl).crossfade(true).build(),
                contentDescription = "배경 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${formatNumber(uiState.todaySteps)}",
                style = MaterialTheme.walkItTypography.headingXL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.textBorderPrimary,
                modifier = Modifier.alignBy { it[FirstBaseline] }
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "걸음",
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.textBorderPrimary,
                modifier = Modifier.alignBy { it[FirstBaseline] } // 숫자와 베이스라인 맞춤
            )
        }


        // 우측 상단 날씨 텍스트
        uiState.weather?.let { weather ->
            val weatherRes = resolveWeatherIconRes(precipType = weather.precipType, sky = weather.sky)
            InfoBadge(
                iconPainter = painterResource(weatherRes),
                text = "${weather.tempC}",
                modifier = modifier
                    .padding(vertical = 20.5.dp, horizontal = 16.dp)
                    .align(Alignment.TopEnd)
            )
        }

        // 바텀 컨텐츠
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 캐릭터 이미지
            uiState.character.characterImageName?.let { charImageUrl ->
//                AsyncImage(
//                    model = ImageRequest.Builder(context)
//                        .data(charImageUrl)
//                        .crossfade(true)
//                        .build(),
//                    contentDescription = "캐릭터 이미지",
//                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
//                    modifier = Modifier.size(130.dp)
//                )
                val (feetPart, bodyPart, headPart) = createCharacterParts(
                    feetUrl = "https://kr.object.ncloudstorage.com/walkit-bucket/ITEM_FEET_BLACK_BOOTS.png",
                    bodyUrl = "https://kr.object.ncloudstorage.com/walkit-bucket/ITEM_BODY_RED_SCARF.png",
                    headUrl = "https://kr.object.ncloudstorage.com/walkit-bucket/ITEM_HEAD_BLUE_RIBBON.png",
                )
// Compose에서
                TestCharacterWithAnchor(
                    feet = feetPart,
                    body = bodyPart,
                    head = headPart,
                    characterUrl = charImageUrl.toString(),
                    modifier = Modifier.size(120.dp)
                )
            }

            // 사용자 정보 및 목표
            HomeNameAndGoalContent(
                nickName = uiState.nickname,
                goal = uiState.goal ?: Goal.EMPTY,
                grade = uiState.character.grade,
                level = uiState.character.level,
                walkProgressPercentage = uiState.walkProgressPercentage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }
    }
}

/**
 * 프로필 스켈레톤 (레이아웃 계약 준수)
 * - 최종 Content와 1:1 레이아웃 구조 유지
 * - padding / spacing / shape / weight 동일
 * - height를 직접 지정하지 않음 (영역 고정)
 */
@Composable
private fun ProfileSkeleton(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 배경 스켈레톤 (회색 박스)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = SemanticColor.backgroundWhiteTertiary, shape = RoundedCornerShape(0.dp)
                )
                .shimmer() // Shimmer 적용: 카드 단위
        )

        // 우측 상단 날씨 텍스트 스켈레톤
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(60.dp)
                .height(24.dp)
                .background(
                    color = SemanticColor.backgroundWhiteTertiary, shape = RoundedCornerShape(4.dp)
                )
                .shimmer() // Shimmer 적용: 텍스트 영역 단위
        )

        // 바텀 컨텐츠 스켈레톤
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 사용자 정보 스켈레톤
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 닉네임과 레벨 박스
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 닉네임
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(28.dp)
                            .background(
                                color = SemanticColor.backgroundWhiteTertiary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmer()
                    )

                    // 레벨 박스
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(28.dp)
                            .background(
                                color = SemanticColor.backgroundWhiteTertiary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmer()
                    )
                }

                // 진행률 바
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(
                            color = SemanticColor.backgroundWhiteTertiary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shimmer()
                )

                // 목표 텍스트들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .background(
                                color = SemanticColor.backgroundWhiteTertiary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmer()
                    )

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(20.dp)
                            .background(
                                color = SemanticColor.backgroundWhiteTertiary,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .shimmer()
                    )
                }
            }
        }
    }
}

/**
 * 프로필 빈 상태
 */
@Composable
private fun ProfileEmpty(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.bg_summer_cropped),
            contentDescription = "기본 캐릭터 배경 이미지",
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * 프로필 에러 상태 (재시도 포함)
 */
@Composable
private fun ProfileError(
    message: String, onRetry: () -> Unit = {}, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.walkItTypography.bodyM,
                color = SemanticColor.textBorderPrimary
            )

            // 재시도 버튼 (간단한 텍스트로 구현)
            Text(
                text = "다시 시도", style = MaterialTheme.walkItTypography.bodyS.copy(
                    fontWeight = FontWeight.SemiBold
                ), color = SemanticColor.buttonPrimaryDefault, modifier = Modifier
                    .background(
                        color = SemanticColor.buttonPrimaryDisabled,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onRetry)
            )
        }
    }
}

/* =====================================================
 * Preview
 * ===================================================== */

@Preview(showBackground = true, name = "ProfileSection - Loading")
@Composable
private fun ProfileSectionLoadingPreview() {
    WalkItTheme {
        ProfileSection(
            uiState = ProfileUiState.Loading, goalState = DataState.Loading
        )
    }
}

@Preview(showBackground = true, name = "ProfileSection - Success")
@Composable
private fun ProfileSectionSuccessPreview() {
    WalkItTheme {
        ProfileSection(
            uiState = ProfileUiState.Success(
                nickname = "테스트사용자", character = Character(
                    nickName = "테스트사용자",
                    level = 5,
                    grade = Grade.TREE,
                    headImageName = null,
                    bodyImageName = null,
                    feetImageName = null,
                    characterImageName = "https://example.com/character.png",
                    backgroundImageName = "https://example.com/background.png"
                ), walkProgressPercentage = "75", goal = Goal(
                    targetStepCount = 10000, targetWalkCount = 30
                ), weather = Weather(
                    tempC = 25.0,
                ), todaySteps = 8500
            ), goalState = DataState.Success(
                Goal(
                    targetStepCount = 10000, targetWalkCount = 30
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "ProfileSection - Error")
@Composable
private fun ProfileSectionErrorPreview() {
    WalkItTheme {
        ProfileSection(
            uiState = ProfileUiState.Error("서버 연결에 문제가 있습니다"), goalState = DataState.Loading
        )
    }
}
