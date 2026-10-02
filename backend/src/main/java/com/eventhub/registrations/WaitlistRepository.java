package com.eventhub.registrations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WaitlistRepository extends JpaRepository<WaitlistRequest, Long> {
    Optional<WaitlistRequest> findFirstByTicketTypeIdAndStatusOrderByIdAsc(UUID ticketTypeId, WaitlistStatus status);
    Optional<WaitlistRequest> findByIdAndParticipantId(Long id, UUID participantId);
    List<WaitlistRequest> findByParticipantIdAndStatusOrderByIdAsc(UUID participantId, WaitlistStatus status);
    long countByTicketTypeIdAndStatus(UUID ticketTypeId, WaitlistStatus status);
    @Query(value = "SELECT COUNT(*) FROM waitlist_attendees a JOIN waitlist_requests w ON w.id = a.request_id WHERE w.ticket_type_id = :typeId AND w.participant_id = :ownerId AND w.status = 'WAITING'", nativeQuery = true)
    long pendingQuantity(@Param("typeId") UUID typeId, @Param("ownerId") UUID ownerId);
    @Query(value = "SELECT COUNT(*) FROM waitlist_requests WHERE ticket_type_id = :typeId AND status = 'WAITING' AND id <= :id", nativeQuery = true)
    long position(@Param("typeId") UUID typeId, @Param("id") Long id);
}
