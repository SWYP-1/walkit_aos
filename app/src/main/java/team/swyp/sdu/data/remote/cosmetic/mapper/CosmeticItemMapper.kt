package team.swyp.sdu.data.remote.cosmetic.mapper

import team.swyp.sdu.data.remote.cosmetic.dto.CosmeticItemDto
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import timber.log.Timber

/**
 * 코스메틱 아이템 매퍼
 */
object CosmeticItemMapper {

    /**
     * 문자열을 EquipSlot으로 안전하게 변환
     * 대소문자를 무시하고, API 응답의 다양한 형식을 지원
     */
    private fun equipSlotFromString(position: String): EquipSlot {
        val upperPosition = position.uppercase()
        Timber.d("Position 변환: '$position' -> '$upperPosition'")

        return when (upperPosition) {
            "HEAD" -> EquipSlot.HEAD
            "BODY" -> EquipSlot.BODY
            "FEET" -> EquipSlot.FEET
            else -> {
                Timber.w("알 수 없는 position 값: '$position' (uppercase: '$upperPosition'), HEAD로 기본 설정")
                EquipSlot.HEAD // 기본값으로 HEAD 설정
            }
        }
    }

    /**
     * DTO를 도메인 모델로 변환
     */
    fun toDomain(dto: CosmeticItemDto): CosmeticItem {
        return CosmeticItem(
            imageName = dto.imageName,
            itemId = dto.itemId,
            position = equipSlotFromString(dto.position),
            name = dto.name,
            owned = dto.owned,
            worn = dto.worn ?: false,  // 서버에서 받은 worn 정보 (기본값: false)
            point = dto.point,
            tags = dto.tag,  // 서버에서 받은 tags 정보
        )
    }

    /**
     * DTO 리스트를 도메인 모델 리스트로 변환
     */
    fun toDomainList(dtos: List<CosmeticItemDto>): List<CosmeticItem> {
        Timber.d("CosmeticItemMapper.toDomainList 시작: ${dtos.size}개 아이템 변환")
        val result = dtos.map { dto ->
            try {
                toDomain(dto)
            } catch (t: Throwable) {
                Timber.e(t, "아이템 변환 실패: $dto")
                throw t
            }
        }
        Timber.d("CosmeticItemMapper.toDomainList 완료: ${result.size}개 아이템 변환됨")
        return result
    }
}
