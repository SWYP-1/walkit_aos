package team.swyp.sdu.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 아이템 카테고리
 */
enum class ItemCategory {
    SHOES,      // 신발
    HAT,        // 모자
    GLOVES,     // 장갑
    NECKLACE    // 목걸이
}

/**
 * 아이템 희귀도
 */
enum class ItemRarity {
    COMMON,     // 일반
    RARE,       // 희귀
    EPIC,       // 영웅
    LEGENDARY   // 전설
}

/**
 * 캐릭터 꾸미기 아이템
 *
 * @param productId Google Play 제품 ID
 * @param name 아이템 이름
 * @param description 아이템 설명
 * @param category 아이템 카테고리
 * @param rarity 아이템 희귀도
 * @param price 가격 (예: "₩1,000")
 * @param resourceUrl 리소스 URL (이미지/Lottie) - 구매 후에만 제공됨
 * @param thumbnailUrl 썸네일 URL (상점 표시용)
 * @param isPurchased 구매 여부
 * @param isApplied 적용 여부
 */
@Parcelize
data class CosmeticItem(
    val productId: String,
    val name: String,
    val description: String,
    val category: ItemCategory,
    val rarity: ItemRarity,
    val price: String,
    val resourceUrl: String? = null,      // 구매 후에만 제공됨
    val thumbnailUrl: String? = null,     // 상점 표시용
    val isPurchased: Boolean = false,
    val isApplied: Boolean = false,
) : Parcelable

/**
 * 현재 적용된 캐릭터 커스터마이징
 */
@Parcelize
data class CharacterCustomization(
    val shoes: CosmeticItem? = null,
    val hat: CosmeticItem? = null,
    val gloves: CosmeticItem? = null,
    val necklace: CosmeticItem? = null,
) : Parcelable








