package com.eventhub.checkin;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckIn {
    @Id private UUID id;
    @Column(name = "ticket_id", nullable = false, unique = true) private UUID ticketId;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "scanned_by", nullable = false) private UUID scannedBy;
    @Column(name = "checked_in_at", nullable = false) private Instant checkedInAt;
    protected CheckIn() {}
    public UUID getId() { return id; } public UUID getTicketId() { return ticketId; } public UUID getEventId() { return eventId; }
    public UUID getScannedBy() { return scannedBy; } public Instant getCheckedInAt() { return checkedInAt; }
}

