package com.group2.fse.notification_service.repository;

import com.group2.fse.notification_service.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Page<Notification> findByCustomerIdAndIsReadOrderByCreatedAtDesc(Long customerId, boolean isRead, Pageable pageable);

    boolean existsByEventId(String eventId);

    Optional<Notification> findByIdAndCustomerId(Long id, Long customerId);

    long countByCustomerIdAndIsReadFalse(Long customerId);
}
