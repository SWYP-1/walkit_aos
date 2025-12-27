package team.swyp.sdu.data.remote.cosmetic.mapper

import team.swyp.sdu.data.remote.cosmetic.dto.CosmeticItemDto
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot

/**
 * 코스메틱 아이템 매퍼
 */
object CosmeticItemMapper {

    /**
     * DTO를 도메인 모델로 변환
     */
    fun toDomain(dto: CosmeticItemDto): CosmeticItem {
        return CosmeticItem(
            imageName = dto.imageName,
            itemId = dto.itemId,
            position = EquipSlot.valueOf(dto.position),
            name = dto.name,
            owned = dto.owned,
            price = 200,
        )
    }

    /**
     * DTO 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(dtos: List<CosmeticItemDto>): List<CosmeticItem> {
        return dtos.map { toDomain(it) }
    }
}
