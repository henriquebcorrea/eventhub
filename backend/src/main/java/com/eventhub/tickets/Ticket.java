package com.eventhub.tickets;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id private UUID id;
    @Column(name = "registration_id", nullable = false, unique = true) private UUID registrationId;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "participant_id", nullable = false) private UUID participantId;
    @Column(name = "public_code", nullable = false, unique = true, length = 32) private String publicCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TicketStatus status;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;

    protected Ticket() {}
    Ticket(UUID registrationId, UUID eventId, UUID participantId, String publicCode) {
        this.id = UUID.randomUUID(); this.registrationId = registrationId; this.eventId = eventId;
        this.participantId = participantId; this.publicCode = publicCode;
        this.status = TicketStatus.ACTIVE; this.issuedAt = Instant.now();
    }
    void cancel() { this.status = TicketStatus.CANCELLED; }
    public UUID getId() { return id; }
    public UUID getRegistrationId() { return registrationId; }
    public UUID getEventId() { return eventId; }
    public UUID getParticipantId() { return participantId; }
    public String getPublicCode() { return publicCode; }
    public TicketStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
}

