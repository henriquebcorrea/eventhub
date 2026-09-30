package com.eventhub.events;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
    @Id private UUID id;
    @Column(name = "organizer_id", nullable = false) private UUID organizerId;
    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(nullable = false, length = 140) private String title;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(nullable = false, length = 160) private String venue;
    @Column(nullable = false, length = 220) private String address;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false, length = 2) private String state;
    @Column(nullable = false, length = 80) private String timezone;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EventStatus status;
    @Column(name = "cover_url", length = 600) private String coverUrl;
    @Column(name = "cover_public_id") private String coverPublicId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Event() {}
    Event(UUID organizerId, String slug, String title, String description, String venue, String address,
          String city, String state, String timezone, Instant startsAt, Instant endsAt, String coverUrl, String coverPublicId) {
        this.id = UUID.randomUUID(); this.organizerId = organizerId; this.slug = slug;
        update(title, description, venue, address, city, state, timezone, startsAt, endsAt, coverUrl, coverPublicId);
        this.status = EventStatus.DRAFT; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    void update(String title, String description, String venue, String address, String city, String state,
                String timezone, Instant startsAt, Instant endsAt, String coverUrl, String coverPublicId) {
        if (!endsAt.isAfter(startsAt)) throw new IllegalArgumentException("A data de término deve ser posterior ao início.");
        this.title = title.trim(); this.description = description.trim(); this.venue = venue.trim(); this.address = address.trim();
        this.city = city.trim(); this.state = state.trim().toUpperCase(); this.timezone = timezone;
        this.startsAt = startsAt; this.endsAt = endsAt; this.coverUrl = coverUrl; this.coverPublicId = coverPublicId;
        this.updatedAt = Instant.now();
    }
    void publish() { this.status = EventStatus.PUBLISHED; this.updatedAt = Instant.now(); }
    void cancel() { this.status = EventStatus.CANCELLED; this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getOrganizerId() { return organizerId; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getTimezone() { return timezone; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public EventStatus getStatus() { return status; }
    public String getCoverUrl() { return coverUrl; }
    public String getCoverPublicId() { return coverPublicId; }
    public Instant getCreatedAt() { return createdAt; }
}

