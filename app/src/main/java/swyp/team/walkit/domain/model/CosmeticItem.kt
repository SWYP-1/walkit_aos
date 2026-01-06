package swyp.team.walkit.domain.model


enum class EquipSlot(val value: String, val displayName: String) {
    HEAD("HEAD", "머리"),
    BODY("BODY", "상의"),
    FEET("FEET", "하의")
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










