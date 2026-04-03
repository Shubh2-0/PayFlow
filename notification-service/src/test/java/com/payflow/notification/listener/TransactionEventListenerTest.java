package com.payflow.notification.listener;

import com.payflow.notification.dto.TransactionEvent;
import com.payflow.notification.entity.Notification;
import com.payflow.notification.entity.NotificationStatus;
import com.payflow.notification.entity.NotificationType;
import com.payflow.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionEventListener transactionEventListener;

    @Test
    void test_onTransactionEvent_createsNotification() {
        TransactionEvent event = TransactionEvent.builder()
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

        Notification notification = Notification.builder()
                .id(1L)
                .type(NotificationType.EMAIL)
                .recipient("100")
                .subject("Transaction TXN-20260403-001 - COMPLETED")
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationService.createNotification(any(TransactionEvent.class)))
                .thenReturn(notification);
        doNothing().when(notificationService).sendNotification(1L);

        transactionEventListener.onTransactionEvent(event);

        verify(notificationService).createNotification(event);
        verify(notificationService).sendNotification(1L);
    }
}
