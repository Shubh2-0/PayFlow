package com.payflow.notification.service;

import com.payflow.notification.dto.TransactionEvent;
import com.payflow.notification.dto.PagedResponse;
import com.payflow.notification.dto.NotificationResponse;
import com.payflow.notification.entity.*;
import com.payflow.notification.repository.NotificationRepository;
import com.payflow.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private TransactionEvent transactionEvent;
    private Notification notification;

    @BeforeEach
    void setUp() {
        transactionEvent = TransactionEvent.builder()
                .transactionId(1L)
                .transactionRef("TXN-20260403-001")
                .type("CREDIT")
                .amount("1500.00")
                .senderWalletId(100L)
                .receiverWalletId(200L)
                .status("COMPLETED")
                .description("Wallet top-up")
                .timestamp("2026-04-03T12:00:00")
                .build();

        notification = Notification.builder()
                .id(1L)
                .type(NotificationType.EMAIL)
                .recipient("100")
                .subject("Transaction TXN-20260403-001 - COMPLETED")
                .message("Transaction 'TXN-20260403-001' of type CREDIT for amount 1500.00 finished with status: COMPLETED")
                .status(NotificationStatus.PENDING)
                .referenceId("1")
                .referenceType("TRANSACTION")
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void test_createNotification_success() {
        when(templateRepository.findByNameAndType(anyString(), any(NotificationType.class)))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        Notification result = notificationService.createNotification(transactionEvent);

        assertThat(result).isNotNull();
        assertThat(result.getRecipient()).isEqualTo("100");
        assertThat(result.getReferenceType()).isEqualTo("TRANSACTION");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void test_createNotification_withTemplate() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(1L)
                .name("TRANSACTION_SUCCESS_TEMPLATE")
                .subject("Success: {{transactionRef}}")
                .bodyTemplate("Transaction {{transactionRef}} of type {{type}} for {{amount}} completed successfully.")
                .type(NotificationType.EMAIL)
                .active(true)
                .build();

        when(templateRepository.findByNameAndType("TRANSACTION_SUCCESS_TEMPLATE", NotificationType.EMAIL))
                .thenReturn(Optional.of(template));

        Notification savedNotification = Notification.builder()
                .id(2L)
                .type(NotificationType.EMAIL)
                .recipient("100")
                .subject("Success: TXN-20260403-001")
                .message("Transaction TXN-20260403-001 of type CREDIT for 1500.00 completed successfully.")
                .status(NotificationStatus.PENDING)
                .referenceId("1")
                .referenceType("TRANSACTION")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        Notification result = notificationService.createNotification(transactionEvent);

        assertThat(result).isNotNull();
        assertThat(result.getSubject()).isEqualTo("Success: TXN-20260403-001");
        assertThat(result.getMessage()).contains("TXN-20260403-001");
        verify(templateRepository).findByNameAndType("TRANSACTION_SUCCESS_TEMPLATE", NotificationType.EMAIL);
    }

    @Test
    void test_retryFailedNotifications() {
        Notification failedNotification = Notification.builder()
                .id(5L)
                .type(NotificationType.EMAIL)
                .recipient("user@test.com")
                .subject("Test")
                .message("Test message")
                .status(NotificationStatus.FAILED)
                .retryCount(1)
                .maxRetries(3)
                .build();

        when(notificationRepository.findAll()).thenReturn(List.of(failedNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(failedNotification);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(failedNotification));
        when(emailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);

        int retried = notificationService.retryFailedNotifications();

        assertThat(retried).isEqualTo(1);
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void test_getNotificationStats() {
        when(notificationRepository.countByStatus(NotificationStatus.PENDING)).thenReturn(5L);
        when(notificationRepository.countByStatus(NotificationStatus.SENT)).thenReturn(20L);
        when(notificationRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(2L);
        when(notificationRepository.count()).thenReturn(27L);

        Map<String, Long> stats = notificationService.getNotificationStats();

        assertThat(stats).containsEntry("PENDING", 5L);
        assertThat(stats).containsEntry("SENT", 20L);
        assertThat(stats).containsEntry("FAILED", 2L);
        assertThat(stats).containsEntry("TOTAL", 27L);
    }
}
