package com.eventhub.registrations;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registrations")
public class Registration {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "ticket_type_id", nullable = false) private UUID ticketTypeId;
    @Column(name = "participant_id", nullable = false) private UUID participantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RegistrationStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    protected Registration() {}
    Registration(UUID eventId, UUID ticketTypeId, UUID participantId) {
        this.id = UUID.randomUUID(); this.eventId = eventId; this.ticketTypeId = ticketTypeId; this.participantId = participantId;
        this.status = RegistrationStatus.CONFIRMED; this.createdAt = Instant.now();
    }
    void cancel() { status = RegistrationStatus.CANCELLED; cancelledAt = Instant.now(); }
    public UUID getId() { return id; } public UUID getEventId() { return eventId; } public UUID getTicketTypeId() { return ticketTypeId; }
    public UUID getParticipantId() { return participantId; } public RegistrationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; } public Instant getCancelledAt() { return cancelledAt; }
}

