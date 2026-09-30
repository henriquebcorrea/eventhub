package com.eventhub.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    Optional<TicketType> findFirstByEventId(UUID eventId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket_types SET confirmed_count = confirmed_count + 1 WHERE id = :id AND confirmed_count < capacity", nativeQuery = true)
    int reserve(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket_types SET confirmed_count = confirmed_count - 1 WHERE id = :id AND confirmed_count > 0", nativeQuery = true)
    int release(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket_types SET capacity = :capacity WHERE id = :id AND confirmed_count <= :capacity", nativeQuery = true)
    int changeCapacity(@Param("id") UUID id, @Param("capacity") int capacity);
}

