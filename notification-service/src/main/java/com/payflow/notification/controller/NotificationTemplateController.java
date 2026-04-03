package com.payflow.notification.controller;

import com.payflow.notification.dto.NotificationTemplateRequest;
import com.payflow.notification.dto.NotificationTemplateResponse;
import com.payflow.notification.entity.NotificationTemplate;
import com.payflow.notification.exception.ResourceNotFoundException;
import com.payflow.notification.repository.NotificationTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-templates")
@RequiredArgsConstructor
public class NotificationTemplateController {

    private final NotificationTemplateRepository templateRepository;

    @PostMapping
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.getName())
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .type(request.getType())
                .active(true)
                .build();

        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<NotificationTemplateResponse>> getTemplates() {
        List<NotificationTemplateResponse> templates = templateRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));

        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setType(request.getType());

        NotificationTemplate updated = templateRepository.save(template);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id));
        templateRepository.delete(template);
        return ResponseEntity.noContent().build();
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .subject(template.getSubject())
                .bodyTemplate(template.getBodyTemplate())
                .type(template.getType())
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
