package com.payflow.wallet.event;

import com.payflow.wallet.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publishes transaction events to RabbitMQ for asynchronous processing
 * by downstream services (notifications, analytics, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "payflow.exchange";
    private static final String ROUTING_KEY = "transaction.completed";

    /**
     * Publishes a transaction event to the message broker after a transaction completes.
     *
     * @param transaction the completed transaction entity
     */
    public void publish(Transaction transaction) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .senderWalletId(transaction.getSenderWalletId())
                .receiverWalletId(transaction.getReceiverWalletId())
                .status(transaction.getStatus())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
            log.info("Published transaction event: transactionId={}, type={}, amount={}",
                    event.getTransactionId(), event.getType(), event.getAmount());
        } catch (Exception e) {
            log.error("Failed to publish transaction event for transactionId={}: {}",
                    transaction.getId(), e.getMessage());
        }
    }
}
