package com.flowforge.notification.controller;

import com.flowforge.notification.dto.NotificationResponse;
import com.flowforge.notification.dto.PagedResponse;
import com.flowforge.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(required = false) String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(notificationService.getNotifications(recipient, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Map<String, String>> retryNotification(@PathVariable Long id) {
        notificationService.sendNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification retry initiated for id: " + id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }
}
