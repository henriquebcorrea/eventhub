package com.eventhub.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test
    void outboxIdIsForwardedAsTheResendIdempotencyKey() {
        var repository = mock(NotificationOutboxRepository.class);
        var message = new NotificationOutbox("pessoa@test.dev", "Ingresso confirmado", "<p>Ingresso</p>");
        when(repository.findByStatusAndNextAttemptAtBeforeOrderByCreatedAt(
                eq(NotificationOutbox.Status.PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(message));
        var receivedKey = new AtomicReference<UUID>();
        EmailSender sender = (key, recipient, subject, html) -> receivedKey.set(key);
        var service = new NotificationService(repository, sender, "http://localhost:3000");

        service.deliverPending();

        assertThat(receivedKey.get()).isEqualTo(message.getId());
    }
}
