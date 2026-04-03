package com.payflow.notification.dto;

import com.payflow.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Body template is required")
    private String bodyTemplate;

    @NotNull(message = "Notification type is required")
    private NotificationType type;
}
