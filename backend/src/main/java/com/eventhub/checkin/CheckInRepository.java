package com.eventhub.checkin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    Optional<CheckIn> findByTicketId(UUID ticketId);
    long countByEventId(UUID eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO check_ins(id, ticket_id, event_id, scanned_by, checked_in_at) VALUES (:id, :ticketId, :eventId, :scannedBy, :checkedAt) ON CONFLICT (ticket_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("ticketId") UUID ticketId, @Param("eventId") UUID eventId,
                       @Param("scannedBy") UUID scannedBy, @Param("checkedAt") Instant checkedAt);
}

