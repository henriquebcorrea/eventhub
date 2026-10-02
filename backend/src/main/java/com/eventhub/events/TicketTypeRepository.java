package com.eventhub.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    Optional<TicketType> findFirstByEventId(UUID eventId);
    List<TicketType> findByEventIdOrderByNameAsc(UUID eventId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketType t where t.id = :id")
    Optional<TicketType> findLockedById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket_types SET confirmed_count = confirmed_count + :quantity WHERE id = :id AND confirmed_count + :quantity <= capacity", nativeQuery = true)
    int reserve(@Param("id") UUID id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket_types SET confirmed_count = confirmed_count - :quantity WHERE id = :id AND confirmed_count >= :quantity", nativeQuery = true)
    int release(@Param("id") UUID id, @Param("quantity") int quantity);

    @Query(value = "SELECT COALESCE(MAX(n.names), 0) FROM (SELECT COUNT(*) names FROM waitlist_attendees a JOIN waitlist_requests w ON w.id = a.request_id WHERE w.ticket_type_id = :id AND w.status = 'WAITING' GROUP BY w.id) n", nativeQuery = true)
    int largestPendingGroup(@Param("id") UUID id);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM tickets WHERE ticket_type_id = :id UNION ALL SELECT 1 FROM waitlist_requests WHERE ticket_type_id = :id)", nativeQuery = true)
    boolean hasHistory(@Param("id") UUID id);
    @Query(value = "SELECT COUNT(*) FROM waitlist_requests WHERE ticket_type_id = :id AND status = 'WAITING'", nativeQuery = true)
    long waitingCount(@Param("id") UUID id);
}

