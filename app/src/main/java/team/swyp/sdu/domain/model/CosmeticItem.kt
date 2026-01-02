package team.swyp.sdu.domain.model


enum class EquipSlot(val value: String) {
    HEAD("HEAD"),
    BODY("BODY"),
    FEET("FEET")
}

data class CosmeticItem(
    val imageName: String,
    val itemId: Int,
    val name: String,
    val owned: Boolean,
    val position: EquipSlot,
    val point: Int,
    val tags: String? = null  // 아이템 태그 (Lottie asset 선택용)
)










