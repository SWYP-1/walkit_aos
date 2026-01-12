package swyp.team.walkit.ui.dressroom.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import swyp.team.walkit.R
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterImage
import swyp.team.walkit.domain.model.CharacterPart
import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.domain.service.LottieImageProcessor
import swyp.team.walkit.ui.theme.SemanticColor
import swyp.team.walkit.ui.theme.WalkItTheme
import swyp.team.walkit.ui.theme.walkItTypography
import swyp.team.walkit.utils.Season
import timber.log.Timber

/**
 * EquipSlot과 Lottie assetId의 매핑
 */
private val SLOT_ASSET_MAPPING = mapOf(
    EquipSlot.HEAD to "head",  // HEAD 슬롯의 asset ID
    EquipSlot.BODY to "body",   // BODY 슬롯의 asset ID
    EquipSlot.FEET to "feet"    // FEET 슬롯의 asset ID
)

/**
 * 슬롯별 이미지 URL 결정 데이터 클래스
 */
private data class SlotImageConfig(
    val assetId: String,
    val imageUrl: String?,
    val tags: String? = null  // HEAD 슬롯에서 assetId 결정에 사용
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
    return EquipSlot.values().map { slot ->
        // 우선순위에 따른 이미지 URL과 태그 결정
        val (imageUrl, tags) = when (slot) {
            EquipSlot.HEAD -> {
                wornItemsByPosition[slot]?.let { itemId ->
                    // 착용된 HEAD 아이템이 있으면 해당 아이템의 정보 사용
                    cosmeticItems.find { it.itemId == itemId }?.let { item ->
                        item.imageName to item.tags
                    } ?: (null to null)
                } ?: (character.headImageName to character.headImageTag)
            }

            EquipSlot.BODY -> {
                val url = wornItemsByPosition[slot]?.let { itemId ->
                    getImageUrlForCosmeticItem(itemId, cosmeticItems)
                } ?: character.bodyImageName
                url to null
            }

            EquipSlot.FEET -> {
                val url = wornItemsByPosition[slot]?.let { itemId ->
                    getImageUrlForCosmeticItem(itemId, cosmeticItems)
                } ?: character.feetImageName
                url to null
            }
        }

        // HEAD 슬롯의 경우 tags를 활용해서 assetId 결정
        val assetId = when (slot) {
            EquipSlot.HEAD -> {
                tags?.let {
                    CharacterPart.HEAD.getLottieAssetId(it)
                } ?: run {
                    // tags가 없으면 이미지 URL에서 힌트 추출
                    when {
                        imageUrl?.contains("decor", ignoreCase = true) == true ||
                                imageUrl?.contains("earring", ignoreCase = true) == true ||
                                imageUrl?.contains("accessory", ignoreCase = true) == true -> {
                            "headdecor"
                        }

                        imageUrl?.contains("top", ignoreCase = true) == true ||
                                imageUrl?.contains("hat", ignoreCase = true) == true ||
                                imageUrl?.contains("cap", ignoreCase = true) == true -> {
                            "headtop"
                        }

                        else -> {
                            "headdecor" // 기본값으로 headdecor 사용
                        }
                    }
                }
            }

            else -> {
                SLOT_ASSET_MAPPING[slot] ?: slot.name.lowercase()
            }
        }

        SlotImageConfig(assetId, imageUrl, tags)
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
    currentSeason: Season = Season.SPRING,
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
    // processedLottieJson이 없을 때 createSlotImageConfigs를 사용해서 기본 Lottie 생성
    val slotConfigs = remember(character, wornItemsByPosition, cosmeticItems) {
        createSlotImageConfigs(character, wornItemsByPosition, cosmeticItems)
    }
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
            LottieCompositionSpec.RawRes(R.raw.seed)
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    )
    {
        // 1️⃣ 배경
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = "season background",
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp), // 고정 높이로 설정하여 스크롤 가능하게 함
            contentScale = ContentScale.Crop,
        )

        // 2️⃣ 헤더 (상단)
        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .windowInsetsPadding(WindowInsets.statusBars) // 상태바 영역만 패딩 적용// 캐릭터 영역의 고정 높이
        ) {
            DressingRoomHeader(
                grade = character.grade,
                nickName = character.nickName,
                onBack = onBackClick,
                points = points,
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
                        .size(200.dp)
                        .scale(0.86f)
                        .offset(y = 20.dp),
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
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(horizontal = 16.dp, vertical = 24.dp)
//                .size(40.dp)
//                .clip(CircleShape)
//                .background(SemanticColor.backgroundDarkPrimary)
//                .clickable(onClick = onRefreshClick),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                painter = painterResource(R.drawable.ic_action_refresh),
//                contentDescription = "refresh",
//                tint = SemanticColor.iconWhite
//            )
//        }


        // 4️⃣ end / bottom 포인트 박스
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(horizontal = 16.dp, vertical = 24.dp)
//                .background(
//                    SemanticColor.stateYellowTertiary,
//                    shape = RoundedCornerShape(9.6.dp)
//                )
//                .padding(horizontal = 12.dp, vertical = 6.dp)
//        ) {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Text(
//                    text = "보유 포인트",
//                    style = MaterialTheme.walkItTypography.captionM.copy(
//                        fontWeight = FontWeight.SemiBold
//                    ),
//                    color = SemanticColor.stateYellowPrimary
//                )
//                Spacer(Modifier.width(8.dp))
//                Text(
//                    text = "${points}P",
//                    style = MaterialTheme.walkItTypography.bodyXL.copy(
//                        fontWeight = FontWeight.SemiBold
//                    ),
//                    color = SemanticColor.stateYellowPrimary
//                )
//            }
//        }
    }
}

@Preview(showBackground = true)
@Composable
fun CharacterAndBackgroundPreview() {
    val dummyCharacter = Character(
        nickName = "승우",
        grade = Grade.TREE,
        headImage = CharacterImage("https://example.com/head.png", "TOP"), // 프리뷰용 더미 CharacterImage
        bodyImage = CharacterImage("https://example.com/body.png", null),
        feetImage = CharacterImage("https://example.com/feet.png", null)
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
