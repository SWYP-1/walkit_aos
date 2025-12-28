package team.swyp.sdu.ui.dressroom.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import org.json.JSONObject
import team.swyp.sdu.R
import team.swyp.sdu.data.remote.walking.dto.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils
import team.swyp.sdu.utils.Season
import timber.log.Timber

@Composable
fun CharacterAndBackground(
    modifier: Modifier = Modifier,
    character: Character,
    points: Int,
    lottieImageProcessor: LottieImageProcessor? = null,
    onBackClick: () -> Unit = {},
    onQuestionClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    // 오늘 날짜의 계절 확인
    val currentSeason = DateUtils.getCurrentSeason()
    val backgroundRes =
        when (currentSeason) {
            Season.SPRING -> R.drawable.bg_spring_cropped
            Season.SUMMER -> R.drawable.bg_summer_cropped
            Season.AUTUMN -> R.drawable.bg_autom_cropped
            Season.WINTER -> R.drawable.bg_winter_cropped
        }

    // Lottie 구성 및 이미지 교체 로직
    val context = LocalContext.current
    // 1️⃣ Base Lottie (fallback 용)
    val baseComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.character2)
    )

// 2️⃣ 서버 이미지 반영된 JSON 생성
    val processedJsonString by produceState<String?>(null, character.headImageName) {
        value = try {
            val inputStream = context.resources.openRawResource(R.raw.character2)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val originalJson = JSONObject(jsonString)

            if (lottieImageProcessor != null) {
                Timber.d("Lottie 이미지 교체: ${character.headImageName}")
                lottieImageProcessor
                    .replaceAssetWithImageUrl(
                        lottieJson = originalJson,
                        assetId = "Group 212_209b93c3-be87-4e55-a26a-57b71292675c",
                        imageUrl = "https://img.freepik.com/premium-photo/yellow-flower-png-gradient-holographic-transparent-background_53876-1040799.jpg"
                    )
                    .toString()
            } else {
                originalJson.toString()
            }
        } catch (e: Exception) {
            Timber.e(e, "Lottie JSON 처리 실패")
            null
        }
    }
    val processedComposition by rememberLottieComposition(
        processedJsonString?.let {
            LottieCompositionSpec.JsonString(it)
        } ?: LottieCompositionSpec.RawRes(R.raw.character2)
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // 1️⃣ 배경
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = "season background",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(25f / 32f),
            contentScale = ContentScale.Crop,
        )

        // 2️⃣ 헤더 (상단)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp) // System Bar 표준 높이 사용
        ) {
            DressingRoomHeader(
                grade = character.grade,
                nickName = character.nickName,
                onBack = onBackClick,
                onClickQuestion = onQuestionClick
            )
        }

        // 중앙에 캐릭터 Lottie 애니메이션 표시
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp), // 캐릭터 크기 조정
            contentAlignment = Alignment.Center
        ) {
            if (processedComposition != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    processedComposition?.let {
                        LottieAnimation(
                            composition = it,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                LottieAnimation(
                    composition = baseComposition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3️⃣ start / bottom 버튼
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(SemanticColor.backgroundDarkPrimary)
                .clickable(onClick = onRefreshClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_action_refresh),
                contentDescription = "refresh",
                tint = SemanticColor.iconWhite
            )
        }

        // 4️⃣ end / bottom 포인트 박스
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .background(
                    SemanticColor.stateYellowTertiary,
                    shape = RoundedCornerShape(9.6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "보유 포인트",
                    style = MaterialTheme.walkItTypography.captionM.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.stateYellowPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${points}P",
                    style = MaterialTheme.walkItTypography.bodyXL.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = SemanticColor.stateYellowPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CharacterAndBackgroundPreview() {
    val dummyCharacter = Character(
        nickName = "승우",
        grade = Grade.TREE,
        headImageName = "https://example.com/head.png" // 프리뷰용 더미 URL
    )
    WalkItTheme {
        CharacterAndBackground(
            character = dummyCharacter,
            points = 500,
            lottieImageProcessor = null, // 프리뷰에서는 null로 처리
            onBackClick = { /* 프리뷰용 클릭 */ },
            onQuestionClick = { /* 프리뷰용 클릭 */ },
            onRefreshClick = { /* 프리뷰용 클릭 */ }
        )
    }
}
