package com.payflow.notification.service;

import com.payflow.notification.dto.TransactionEvent;
import com.payflow.notification.dto.NotificationResponse;
import com.payflow.notification.dto.PagedResponse;
import com.payflow.notification.entity.*;
import com.payflow.notification.exception.NotificationFailedException;
import com.payflow.notification.exception.ResourceNotFoundException;
import com.payflow.notification.repository.NotificationRepository;
import com.payflow.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;

    @Transactional
    public Notification createNotification(TransactionEvent event) {
        String templateName = "COMPLETED".equalsIgnoreCase(event.getStatus())
                ? "TRANSACTION_SUCCESS_TEMPLATE"
                : "TRANSACTION_FAILED_TEMPLATE";

        String subject = "Transaction " + event.getTransactionRef() + " - " + event.getStatus();
        String body = "Transaction '" + event.getTransactionRef() + "' of type " + event.getType()
                + " for amount " + event.getAmount() + " finished with status: " + event.getStatus();

        Optional<NotificationTemplate> templateOpt = templateRepository.findByNameAndType(
                templateName, NotificationType.EMAIL);

        if (templateOpt.isPresent()) {
            NotificationTemplate template = templateOpt.get();
            subject = replacePlaceholders(template.getSubject(), event);
            body = replacePlaceholders(template.getBodyTemplate(), event);
        }

        Notification notification = Notification.builder()
                .type(NotificationType.EMAIL)
                .recipient(String.valueOf(event.getSenderWalletId()))
                .subject(subject)
                .message(body)
                .status(NotificationStatus.PENDING)
                .referenceId(String.valueOf(event.getTransactionId()))
                .referenceType("TRANSACTION")
                .retryCount(0)
                .maxRetries(3)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void sendNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (notification.getType() == NotificationType.EMAIL) {
            boolean sent = emailService.sendEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getMessage());

            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                notification.setRetryCount(notification.getRetryCount() + 1);
                if (notification.getRetryCount() >= notification.getMaxRetries()) {
                    notification.setStatus(NotificationStatus.FAILED);
                }
            }
        } else if (notification.getType() == NotificationType.LOG) {
            log.info("LOG Notification [{}]: {} - {}", notification.getRecipient(),
                    notification.getSubject(), notification.getMessage());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            log.warn("Webhook notifications not yet implemented");
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }

    public PagedResponse<NotificationResponse> getNotifications(String recipient, Pageable pageable) {
        Page<Notification> page;
        if (recipient != null && !recipient.isBlank()) {
            page = notificationRepository.findByRecipient(recipient, pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }

        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        return toResponse(notification);
    }

    public Map<String, Long> getNotificationStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", notificationRepository.countByStatus(NotificationStatus.PENDING));
        stats.put("SENT", notificationRepository.countByStatus(NotificationStatus.SENT));
        stats.put("FAILED", notificationRepository.countByStatus(NotificationStatus.FAILED));
        stats.put("TOTAL", notificationRepository.count());
        return stats;
    }

    @Transactional
    public int retryFailedNotifications() {
        List<Notification> failed = notificationRepository.findAll().stream()
                .filter(n -> n.getStatus() == NotificationStatus.FAILED)
                .filter(n -> n.getRetryCount() < n.getMaxRetries())
                .toList();

        int retried = 0;
        for (Notification notification : failed) {
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
            sendNotification(notification.getId());
            retried++;
        }

        log.info("Retried {} failed notifications", retried);
        return retried;
    }

    private String replacePlaceholders(String template, TransactionEvent event) {
        if (template == null) return "";
        return template
                .replace("{{transactionRef}}", nullSafe(event.getTransactionRef()))
                .replace("{{type}}", nullSafe(event.getType()))
                .replace("{{amount}}", nullSafe(event.getAmount()))
                .replace("{{status}}", nullSafe(event.getStatus()))
                .replace("{{description}}", nullSafe(event.getDescription()));
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .retryCount(notification.getRetryCount())
                .maxRetries(notification.getMaxRetries())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}
