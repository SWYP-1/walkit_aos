package swyp.team.walkit.domain.model


enum class EquipSlot(val value: String, val displayName: String) {
    HEAD("HEAD", "헤어"),
    BODY("BODY", "목도리"),
    FEET("FEET", "신발")
}

data class CosmeticItem(
    val imageName: String,
    val itemId: Int,
    val name: String,
    val owned: Boolean,
    val worn: Boolean = false,  // 착용 여부
    val position: EquipSlot,
    val point: Int,
    val tags: String? = null  // 아이템 태그 (Lottie asset 선택용)
)










