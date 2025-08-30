package com.smartcollab.prod.dto;

import com.smartcollab.prod.entity.Notification;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class NotificationResponseDto {
    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;
    private final Notification.NotificationType type;
    private final Long invitationId;

    public NotificationResponseDto(Notification notification) {
        this.id = notification.getId();
        this.content = notification.getContent();
        this.createdAt = notification.getCreatedAt();
        this.type = notification.getType();
        this.invitationId = (notification.getInvitation() != null) ? notification.getInvitation().getId() : null;
    }
}