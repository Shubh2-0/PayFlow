package com.flowforge.notification.dto;

import com.flowforge.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {

    private Long id;
    private String name;
    private String subject;
    private String bodyTemplate;
    private NotificationType type;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
