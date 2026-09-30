package com.eventhub.notifications;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
class NotificationOutbox {
    enum Status { PENDING, SENT, FAILED }
    @Id private UUID id;
    @Column(nullable = false) private String recipient;
    @Column(nullable = false) private String subject;
    @Column(name = "html_body", nullable = false, columnDefinition = "text") private String htmlBody;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected NotificationOutbox() {}
    NotificationOutbox(String recipient, String subject, String htmlBody) {
        this.id = UUID.randomUUID(); this.recipient = recipient; this.subject = subject; this.htmlBody = htmlBody;
        this.status = Status.PENDING; this.nextAttemptAt = Instant.now(); this.createdAt = Instant.now();
    }
    void sent() { status = Status.SENT; sentAt = Instant.now(); lastError = null; }
    void failed(Exception error) { attempts++; lastError = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage().substring(0, Math.min(1000, error.getMessage().length())); status = attempts >= 5 ? Status.FAILED : Status.PENDING; nextAttemptAt = Instant.now().plusSeconds((long) Math.pow(2, attempts) * 60); }
    UUID getId() { return id; } String getRecipient() { return recipient; } String getSubject() { return subject; } String getHtmlBody() { return htmlBody; }
}

