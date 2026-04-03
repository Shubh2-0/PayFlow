package com.payflow.notification.repository;

import com.payflow.notification.entity.Notification;
import com.payflow.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient(String recipient, Pageable pageable);

    List<Notification> findByReferenceIdAndReferenceType(String referenceId, String referenceType);

    long countByStatus(NotificationStatus status);
}
