package team.swyp.sdu.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import team.swyp.sdu.core.DataState
import team.swyp.sdu.data.model.EmotionType
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.ui.components.CustomProgressIndicator
import team.swyp.sdu.ui.components.ProgressIndicatorSize
import team.swyp.sdu.ui.home.HomeUiState
import team.swyp.sdu.domain.model.Goal
import team.swyp.sdu.domain.model.Weather
import team.swyp.sdu.ui.components.TestCharacterWithAnchor
import team.swyp.sdu.ui.components.createCharacterParts
import team.swyp.sdu.ui.home.ProfileUiState
import team.swyp.sdu.ui.home.WalkingSessionData
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.NumberUtils.formatNumber

/**
 * Grade에 따라 레벨 라벨 텍스트를 생성합니다.
 * 레벨은 Grade enum의 level 속성에서 자동으로 가져옵니다.
 *
 * @param grade 등급 (SEED, SPROUT, TREE)
 * @return "Lv.{level} {등급명}" 형식의 문자열
 */
fun getGradeLevelText(grade: Grade): String {
    val gradeName = when (grade) {
        Grade.SEED -> "씨앗"
        Grade.SPROUT -> "묘목"
        Grade.TREE -> "나무"
    }
    return "Lv.${grade.level} $gradeName"
}

@Composable
fun HomeNameAndGoalContent(
    nickName: String,
    goal: Goal,
    grade: Grade,
    level: Int,
    walkProgressPercentage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row() {
            Text(
                text = nickName,

                // heading L/semibold
                style = MaterialTheme.walkItTypography.headingL.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = SemanticColor.backgroundWhitePrimary
            )
            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = SemanticColor.stateGreenTertiary,
                        shape = RoundedCornerShape(size = 16.dp)
                    )
                    .padding(
                        vertical = 8.dp,
                        horizontal = 12.dp
                    )
            ) {
                Text(
                    text = " ${getGradeLevelText(grade)}",
                    // body S/semibold
                    style = MaterialTheme.walkItTypography.bodyS.copy(
                        fontWeight = FontWeight.SemiBold
                    ), color = SemanticColor.stateGreenPrimary
                )
            }

        }

        // 오늘 걸음 수 표시
        Spacer(modifier = Modifier.height(8.dp))

        // 진행률 바 추가
        Spacer(modifier = Modifier.height(10.dp))
        WalkProgressBar(
            progressPercentage = walkProgressPercentage,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${goal.targetStepCount}일 / ${goal.targetWalkCount} 걸음씩",

                // body M/medium
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.backgroundWhitePrimary
            )
            Text(
                text = "${walkProgressPercentage.toFloatOrNull()?.toInt() ?: 0}%",
                // body M/medium
                style = MaterialTheme.walkItTypography.bodyM.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = SemanticColor.backgroundWhitePrimary
            )
        }
    }
}

@Composable
fun WalkProgressBar(
    progressPercentage: String,
    modifier: Modifier = Modifier,
) {
    val progress = progressPercentage.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val progressRatio = progress / 100f

    Box(
        modifier = modifier
            .height(16.dp)
            .background(
                color = SemanticColor.backgroundWhiteTertiary, // #F3F3F5
                shape = RoundedCornerShape(size = 16.dp)
            )
    ) {
        // 진행된 부분
        Box(
            modifier = Modifier
                .fillMaxWidth(progressRatio)
                .fillMaxHeight()
                .background(
                    color = SemanticColor.buttonPrimaryDefault, // 초록색
                    shape = RoundedCornerShape(size = 16.dp)
                )
        )
    }
}

//@Composable
//fun HomeInfoCard(
//    modifier: Modifier = Modifier,
//    profileUiState: ProfileUiState,
//    walkingSessionDataState: DataState<WalkingSessionData>,
//) {
//    val context = LocalContext.current
//
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(480.dp)
//    ) {
//        when (profileUiState) {
//            is ProfileUiState.Success -> {
//                val nickname = profileUiState.nickname
//                val walkProgressPercentage = profileUiState.walkProgressPercentage
//                val goal = profileUiState.goal
//                val backgroundImageName = profileUiState.character.backgroundImageName
//
//                // character Domain 모델에서 grade와 level 가져오기
//                val grade = profileUiState.character.grade
//                val level = profileUiState.character.level
//
//                val tempC = profileUiState.weather?.tempC ?: 0.0
//
//                // 배경 이미지
//                // backgroundImageName이 이미 전체 URL이므로 그대로 사용
//                val bgImageUrl = backgroundImageName
//                val charImageUrl = profileUiState.character.characterImageName
//
//                // walkingSessionDataState에서 데이터 가져오기
//                val walkingData = when (walkingSessionDataState) {
//                    is DataState.Success -> walkingSessionDataState.data
//                    else -> WalkingSessionData(
//                        sessionsThisWeek = emptyList(),
//                        dominantEmotion = null,
//                        recentEmotions = emptyList()
//                    )
//                }
//
//                val todaySteps = walkingData.sessionsThisWeek.sumOf { it.stepCount }
//                val levelLabel = "${grade} Lv.${level}"
//
//                // 이미지 렌더링: AsyncImage가 자체적으로 로딩/에러 처리 (회색 박스 없음)
//                if (bgImageUrl != null) {
//                    AsyncImage(
//                        model = ImageRequest.Builder(context)
//                            .data(bgImageUrl)
//                            .crossfade(true)
//                            .build(),
//                        contentDescription = "배경 이미지",
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                }
//                // 이미지 URL이 없는 경우: 아무것도 표시하지 않음
//
//                // 바텀쪽에 HomeNameAndGoalContent 배치
//
//                Column(
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//
////                    AsyncImage(
////                        model = ImageRequest.Builder(context)
////                            .data(charImageUrl)
////                            .crossfade(true)
////                            .build(),
////                        contentDescription = "캐릭터 이미지",
////                        contentScale = ContentScale.Fit,
////                        modifier = Modifier.size(130.dp)
////                    )
//
//
//
//                    HomeNameAndGoalContent(
//                        nickName = nickname,
//                        goal = goal ?: Goal.EMPTY,
//                        grade = grade,
//                        level = level,
//                        walkProgressPercentage = walkProgressPercentage,
//                        modifier = Modifier.padding(
//                            horizontal = 16.dp, vertical = 20.dp
//                        )
//                    )
//                }
//
//            }
//
//            is ProfileUiState.Loading -> {
//                // 로딩 중: 프로그레스 인디케이터만 표시 (회색 박스 없음)
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CustomProgressIndicator(size = ProgressIndicatorSize.Medium)
//                }
//            }
//
//            is ProfileUiState.Error -> {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = "오류가 발생했습니다",
//                        color = SemanticColor.textBorderPrimary
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true, name = "Success - 이미지 있음")
//@Composable
//private fun HomeInfoCardSuccessPreview() {
//    WalkItTheme {
//        HomeInfoCard(
//            profileUiState = ProfileUiState.Success(
//                nickname = "홍길동",
//                character = team.swyp.sdu.domain.model.Character(
//                    headImageName = null,
//                    bodyImageName = null,
//                    feetImageName = null,
//                    characterImageName = "https://kr.object.ncloudstorage.com/walkit-bucket/CHARACTER_SEED.png",
//                    backgroundImageName = "https://kr.object.ncloudstorage.com/walkit-bucket/BACKGROUND_NOT_LONG_WINTER_OVERCAST.png",
//                    level = 5,
//                    grade = Grade.TREE,
//                    nickName = "홍길동"
//                ),
//                walkProgressPercentage = "75",
//                weather = Weather(
//                    tempC = 15.0,
//                    precipType = PrecipType.NONE,
//                    sky = Sky.CLOUDY_MANY
//                ),
//                goal = Goal(
//                    targetStepCount = 10000,
//                    targetWalkCount = 30
//                )
//            ),
//            walkingSessionDataState = DataState.Success(
//                WalkingSessionData(
//                    sessionsThisWeek = emptyList(),
//                    dominantEmotion = EmotionType.HAPPY,
//                    recentEmotions = listOf(EmotionType.HAPPY, EmotionType.DEPRESSED)
//                )
//            )
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "Success - 이미지 없음")
//@Composable
//private fun HomeInfoCardSuccessNoImagePreview() {
//    WalkItTheme {
//        HomeInfoCard(
//            profileUiState = ProfileUiState.Success(
//                nickname = "김철수",
//                character = team.swyp.sdu.domain.model.Character(
//                    headImageName = null,
//                    bodyImageName = null,
//                    feetImageName = null,
//                    characterImageName = null,
//                    backgroundImageName = null, // 이미지 없음
//                    level = 2,
//                    grade = Grade.SPROUT,
//                    nickName = "김철수"
//                ),
//                walkProgressPercentage = "35",
//                goal = Goal(
//                    targetStepCount = 5000,
//                    targetWalkCount = 20
//                ),
//                weather = null,
//                todaySteps = 3500
//            ),
//            walkingSessionDataState = DataState.Success(
//                WalkingSessionData(
//                    sessionsThisWeek = emptyList(),
//                    dominantEmotion = null,
//                    recentEmotions = emptyList()
//                )
//            )
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "Loading")
//@Composable
//private fun HomeInfoCardLoadingPreview() {
//    WalkItTheme {
//        HomeInfoCard(
//            profileUiState = ProfileUiState.Loading,
//            walkingSessionDataState = DataState.Loading
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "Error")
//@Composable
//private fun HomeInfoCardErrorPreview() {
//    WalkItTheme {
//        HomeInfoCard(
//            profileUiState = ProfileUiState.Error("데이터를 불러오는 중 오류가 발생했습니다"),
//            walkingSessionDataState = DataState.Error(message = "데이터 로드 실패")
//        )
//    }
//}