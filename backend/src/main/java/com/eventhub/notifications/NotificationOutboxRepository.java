package com.eventhub.notifications;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {
    List<NotificationOutbox> findByStatusAndNextAttemptAtBeforeOrderByCreatedAt(NotificationOutbox.Status status, Instant now, Pageable pageable);
}

