package com.eventhub.tickets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketRepository extends JpaRepository<Ticket, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ticket> findByRegistrationId(UUID registrationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findLockedById(@Param("id") UUID id);
    List<Ticket> findByParticipantIdOrderByIssuedAtDesc(UUID participantId);
}

