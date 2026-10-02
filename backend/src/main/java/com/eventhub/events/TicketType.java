package com.eventhub.events;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "ticket_types")
public class TicketType {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private int capacity;
    @Column(name = "confirmed_count", nullable = false) private int confirmedCount;
    @Column(name = "price_cents", nullable = false) private int priceCents;

    protected TicketType() {}
    TicketType(UUID eventId, String name, int capacity) {
        this.id = UUID.randomUUID(); this.eventId = eventId; this.name = name;
        this.capacity = capacity; this.confirmedCount = 0; this.priceCents = 0;
    }
    TicketType(UUID eventId, int capacity) { this(eventId, "Ingresso geral", capacity); }
    void changeCapacity(int capacity) { this.capacity = capacity; }
    void rename(String name) { this.name = name; }
    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getConfirmedCount() { return confirmedCount; }
    public int getAvailable() { return Math.max(0, capacity - confirmedCount); }
}

