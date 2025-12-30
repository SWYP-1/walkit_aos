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
import team.swyp.sdu.domain.model.Grade
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.domain.service.LottieImageProcessor
import team.swyp.sdu.ui.theme.SemanticColor
import team.swyp.sdu.ui.theme.WalkItTheme
import team.swyp.sdu.ui.theme.walkItTypography
import team.swyp.sdu.utils.DateUtils
import team.swyp.sdu.utils.Season
import timber.log.Timber

/**
 * EquipSlot과 Lottie assetId의 매핑
 */
private val SLOT_ASSET_MAPPING = mapOf(
    EquipSlot.HEAD to "head_ribbon",  // HEAD 슬롯의 asset ID
    EquipSlot.BODY to "body_cloth",   // BODY 슬롯의 asset ID
    EquipSlot.FEET to "feet_shoes"    // FEET 슬롯의 asset ID
)

/**
 * 슬롯별 이미지 URL 결정 데이터 클래스
 */
private data class SlotImageConfig(
    val assetId: String,
    val imageUrl: String?
)

/**
 * EquipSlot Map과 Character 정보를 기반으로 각 슬롯의 이미지 설정을 생성
 *
 * 우선순위:
 * 1. 착용된 아이템의 이미지 (wornItemsByPosition)
 * 2. Character의 기본 이미지 (headImageName, bodyImageName, feetImageName)
 */
private fun createSlotImageConfigs(
    character: Character,
    wornItemsByPosition: Map<EquipSlot, Int>,
    cosmeticItems: List<CosmeticItem>
): List<SlotImageConfig> {
    return EquipSlot.entries.map { slot ->
        val assetId = SLOT_ASSET_MAPPING[slot] ?: return@map SlotImageConfig(slot.name.lowercase(), null)

        // 우선순위에 따른 이미지 URL 결정
        val imageUrl = when (slot) {
            EquipSlot.HEAD -> {
                // 착용된 HEAD 아이템이 있으면 해당 아이템 사용, 없으면 Character 기본값
                wornItemsByPosition[slot]?.let { itemId ->
                    getImageUrlForCosmeticItem(itemId, cosmeticItems)
                } ?: character.headImageName
            }
            EquipSlot.BODY -> {
                wornItemsByPosition[slot]?.let { itemId ->
                    getImageUrlForCosmeticItem(itemId, cosmeticItems)
                } ?: character.bodyImageName
            }
            EquipSlot.FEET -> {
                wornItemsByPosition[slot]?.let { itemId ->
                    getImageUrlForCosmeticItem(itemId, cosmeticItems)
                } ?: character.feetImageName
            }
        }

        SlotImageConfig(assetId, imageUrl)
    }
}

/**
 * 아이템 ID로부터 이미지 URL을 가져오는 헬퍼 함수
 * cosmeticItems 리스트에서 해당 itemId의 imageName을 찾아 반환
 */
private fun getImageUrlForCosmeticItem(itemId: Int, cosmeticItems: List<CosmeticItem>): String? {
    return cosmeticItems.find { it.itemId == itemId }?.imageName
}

@Composable
fun CharacterAndBackground(
    modifier: Modifier = Modifier,
    currentSeason : Season = Season.SPRING,
    character: Character,
    points: Int,
    wornItemsByPosition: Map<EquipSlot, Int> = emptyMap(),
    cosmeticItems: List<CosmeticItem> = emptyList(),
    lottieImageProcessor: LottieImageProcessor? = null,
    onBackClick: () -> Unit = {},
    onQuestionClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    processedLottieJson: String? = null, // ViewModel에서 처리된 Lottie JSON
) {
    Timber.d("🎭 CharacterAndBackground 컴포넌트 렌더링")
    Timber.d("📄 processedLottieJson 길이: ${processedLottieJson?.length ?: 0}")
    Timber.d("🧷 wornItemsByPosition: $wornItemsByPosition")
    // 오늘 날짜의 계절 확인
    val backgroundRes =
        when (currentSeason) {
            Season.SPRING -> R.drawable.bg_spring_cropped
            Season.SUMMER -> R.drawable.bg_summer_cropped
            Season.AUTUMN -> R.drawable.bg_autom_cropped
            Season.WINTER -> R.drawable.bg_winter_cropped
        }

    // Lottie 구성 - ViewModel에서 처리된 JSON 사용
    val processedComposition by rememberLottieComposition(
        processedLottieJson?.let {
            Timber.d("🎨 LottieCompositionSpec.JsonString 사용 (길이: ${it.length})")
            LottieCompositionSpec.JsonString(it)
        } ?: run {
            Timber.d("🎨 LottieCompositionSpec.RawRes 사용 (기본 리소스)")
            LottieCompositionSpec.RawRes(R.raw.seedblueribbon)
        }
    )

    Timber.d("🎨 processedComposition 로드됨: ${processedComposition != null}")

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
                .padding(top = 36.dp) // System Bar 표준 높이 사용
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
                .align(Alignment.Center),
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
//                LottieAnimation(
//                    composition = baseComposition,
//                    iterations = LottieConstants.IterateForever,
//                    modifier = Modifier.fillMaxSize()
//                )
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
        headImageName = "https://example.com/head.png", // 프리뷰용 더미 URL
        bodyImageName = "https://example.com/body.png",
        feetImageName = "https://example.com/feet.png"
    )

    // 프리뷰용 착용 아이템 설정
    val wornItemsByPosition = mapOf(
        EquipSlot.HEAD to 1, // HEAD 아이템 착용
        EquipSlot.BODY to 2  // BODY 아이템 착용
    )

    WalkItTheme {
        CharacterAndBackground(
            character = dummyCharacter,
            points = 500,
            wornItemsByPosition = wornItemsByPosition,
            cosmeticItems = emptyList(), // 프리뷰에서는 빈 리스트
            lottieImageProcessor = null, // 프리뷰에서는 null로 처리
            onBackClick = { /* 프리뷰용 클릭 */ },
            onQuestionClick = { /* 프리뷰용 클릭 */ },
            onRefreshClick = { /* 프리뷰용 클릭 */ }
        )
    }
}
