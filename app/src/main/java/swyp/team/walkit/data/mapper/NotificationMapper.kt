package swyp.team.walkit.data.mapper

import swyp.team.walkit.data.remote.notification.dto.NotificationItemDto
import swyp.team.walkit.domain.model.NotificationItem
import swyp.team.walkit.domain.model.NotificationType

/**
 * NotificationItemDto를 NotificationItem으로 변환하는 매퍼
 */
object NotificationMapper {

    fun toDomain(dto: NotificationItemDto): NotificationItem {
        return NotificationItem(
            notificationId = dto.notificationId,
            type = NotificationType.fromValue(dto.type) ?: NotificationType.GOAL, // 기본값 GOAL
            title = dto.title,
            body = dto.body,
            senderId = dto.senderId?.toLong(),
            senderNickname = dto.senderNickname,
            targetId = dto.targetId,
            createdAt = dto.createdAt,
            read = dto.read
        )
    }

    fun toDomainList(dtos: List<NotificationItemDto>): List<NotificationItem> {
        return dtos.map { toDomain(it) }
    }
}
