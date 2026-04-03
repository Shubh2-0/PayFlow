package com.flowforge.notification.repository;

import com.flowforge.notification.entity.NotificationTemplate;
import com.flowforge.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByNameAndType(String name, NotificationType type);

    List<NotificationTemplate> findByActiveTrue();
}
