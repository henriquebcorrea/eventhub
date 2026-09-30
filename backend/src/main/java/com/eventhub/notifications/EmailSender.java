package com.eventhub.notifications;

import java.util.UUID;

public interface EmailSender {
    void send(UUID idempotencyKey, String recipient, String subject, String htmlBody);
}

