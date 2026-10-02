package com.eventhub.tickets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketRepository extends JpaRepository<Ticket, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Ticket> findByRegistrationId(UUID registrationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findLockedById(@Param("id") UUID id);
    List<Ticket> findByParticipantIdOrderByIssuedAtDesc(UUID participantId);
    Page<Ticket> findByEventIdOrderByIssuedAtDesc(UUID eventId, Pageable pageable);
    long countByTicketTypeIdAndParticipantIdAndStatus(UUID ticketTypeId, UUID participantId, TicketStatus status);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM check_ins WHERE ticket_id = :id)", nativeQuery = true)
    boolean hasCheckIn(@Param("id") UUID id);
    @Query(value = "SELECT COUNT(*) FROM tickets WHERE event_id = :eventId AND status = 'ACTIVE' AND issued_at >= :from AND issued_at < :to", nativeQuery = true)
    long countIssuedBetween(@Param("eventId") UUID eventId, @Param("from") java.time.Instant from, @Param("to") java.time.Instant to);
}

