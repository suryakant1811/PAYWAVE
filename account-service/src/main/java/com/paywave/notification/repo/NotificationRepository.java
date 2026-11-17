package com.paywave.notification.repo;

import com.paywave.notification.domain.NotificationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationMessage, UUID> {
    Optional<NotificationMessage> findByDedupKey(String dedupKey);
    Page<NotificationMessage> findByUserId(UUID userId, Pageable pageable);
}
