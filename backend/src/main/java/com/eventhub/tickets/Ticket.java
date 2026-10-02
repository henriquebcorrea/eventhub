package com.eventhub.tickets;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id private UUID id;
    @Column(name = "registration_id", nullable = false) private UUID registrationId;
    @Column(name = "ticket_type_id", nullable = false) private UUID ticketTypeId;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "participant_id", nullable = false) private UUID participantId;
    @Column(name = "public_code", nullable = false, unique = true, length = 32) private String publicCode;
    @Column(name = "attendee_name", nullable = false, length = 120) private String attendeeName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TicketStatus status;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;

    protected Ticket() {}
    Ticket(UUID registrationId, UUID ticketTypeId, UUID eventId, UUID participantId, String attendeeName, String publicCode) {
        this.id = UUID.randomUUID(); this.registrationId = registrationId; this.eventId = eventId;
        this.ticketTypeId = ticketTypeId; this.participantId = participantId; this.attendeeName = attendeeName; this.publicCode = publicCode;
        this.status = TicketStatus.ACTIVE; this.issuedAt = Instant.now();
    }
    void cancel() { this.status = TicketStatus.CANCELLED; }
    void rename(String name) { this.attendeeName = name; }
    public UUID getId() { return id; }
    public UUID getRegistrationId() { return registrationId; }
    public UUID getTicketTypeId() { return ticketTypeId; }
    public String getAttendeeName() { return attendeeName; }
    public UUID getEventId() { return eventId; }
    public UUID getParticipantId() { return participantId; }
    public String getPublicCode() { return publicCode; }
    public TicketStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
}

