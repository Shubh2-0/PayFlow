package com.flowforge.notification.listener;

import com.flowforge.notification.dto.TransactionEvent;
import com.flowforge.notification.entity.Notification;
import com.flowforge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "payflow.notification.queue")
    public void onTransactionEvent(TransactionEvent event) {
        log.info("Received transaction event: transactionId={}, type={}, status={}",
                event.getTransactionId(), event.getType(), event.getStatus());

        try {
            Notification notification = notificationService.createNotification(event);
            notificationService.sendNotification(notification.getId());
            log.info("Notification created and sent: id={}", notification.getId());
        } catch (Exception e) {
            log.error("Failed to process transaction event: {}", e.getMessage(), e);
        }
    }
}
