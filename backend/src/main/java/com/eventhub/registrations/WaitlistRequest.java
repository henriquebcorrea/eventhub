package com.eventhub.registrations;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "waitlist_requests")
public class WaitlistRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "ticket_type_id", nullable = false) private UUID ticketTypeId;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "participant_id", nullable = false) private UUID participantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private WaitlistStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "waitlist_attendees", joinColumns = @JoinColumn(name = "request_id"))
    @OrderColumn(name = "position")
    @Column(name = "attendee_name", nullable = false, length = 120)
    private List<String> attendeeNames = new ArrayList<>();
    protected WaitlistRequest() {}
    WaitlistRequest(UUID eventId, UUID ticketTypeId, UUID participantId, List<String> names) {
        this.eventId = eventId; this.ticketTypeId = ticketTypeId; this.participantId = participantId;
        this.status = WaitlistStatus.WAITING; this.createdAt = Instant.now(); this.attendeeNames = new ArrayList<>(names);
    }
    void promote() { status = WaitlistStatus.PROMOTED; resolvedAt = Instant.now(); }
    void cancel() { status = WaitlistStatus.CANCELLED; resolvedAt = Instant.now(); }
    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public UUID getTicketTypeId() { return ticketTypeId; }
    public UUID getParticipantId() { return participantId; }
    public WaitlistStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<String> getAttendeeNames() { return List.copyOf(attendeeNames); }
}
