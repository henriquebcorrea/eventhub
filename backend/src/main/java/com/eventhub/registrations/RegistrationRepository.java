package com.eventhub.registrations;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    boolean existsByEventIdAndParticipantId(UUID eventId, UUID participantId);
    Optional<Registration> findByIdAndParticipantId(UUID id, UUID participantId);
    List<Registration> findByParticipantIdOrderByCreatedAtDesc(UUID participantId);
    Page<Registration> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
    long countByEventIdAndStatus(UUID eventId, RegistrationStatus status);
    long countByEventIdAndStatusAndCreatedAtBetween(UUID eventId, RegistrationStatus status, Instant from, Instant to);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM check_ins c JOIN tickets t ON t.id = c.ticket_id WHERE t.registration_id = :registrationId)", nativeQuery = true)
    boolean hasCheckIn(@Param("registrationId") UUID registrationId);
}

