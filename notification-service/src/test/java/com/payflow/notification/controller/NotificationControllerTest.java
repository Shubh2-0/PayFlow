package com.payflow.notification.controller;

import com.payflow.notification.dto.NotificationResponse;
import com.payflow.notification.dto.PagedResponse;
import com.payflow.notification.entity.NotificationStatus;
import com.payflow.notification.entity.NotificationType;
import com.payflow.notification.exception.GlobalExceptionHandler;
import com.payflow.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void test_getNotifications_returnsPaged() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .type(NotificationType.EMAIL)
                .recipient("admin@payflow.com")
                .subject("Pipeline completed")
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        PagedResponse<NotificationResponse> pagedResponse = PagedResponse.<NotificationResponse>builder()
                .content(List.of(response))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(notificationService.getNotifications(isNull(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void test_retryNotification_success() throws Exception {
        doNothing().when(notificationService).sendNotification(1L);

        mockMvc.perform(post("/api/notifications/1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification retry initiated for id: 1"));
    }

    @Test
    void test_getStats_returnsOk() throws Exception {
        Map<String, Long> stats = Map.of(
                "PENDING", 5L,
                "SENT", 20L,
                "FAILED", 2L,
                "TOTAL", 27L
        );

        when(notificationService.getNotificationStats()).thenReturn(stats);

        mockMvc.perform(get("/api/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PENDING").value(5))
                .andExpect(jsonPath("$.SENT").value(20))
                .andExpect(jsonPath("$.FAILED").value(2))
                .andExpect(jsonPath("$.TOTAL").value(27));
    }
}
