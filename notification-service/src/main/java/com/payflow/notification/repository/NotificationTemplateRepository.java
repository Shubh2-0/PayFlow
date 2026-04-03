package com.payflow.notification.repository;

import com.payflow.notification.entity.NotificationTemplate;
import com.payflow.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByNameAndType(String name, NotificationType type);

    List<NotificationTemplate> findByActiveTrue();
}
