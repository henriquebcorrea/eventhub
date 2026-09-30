package com.eventhub.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findBySlugAndStatus(String slug, EventStatus status);
    boolean existsBySlug(String slug);
    List<Event> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    @Query("""
        select e from Event e where e.status = com.eventhub.events.EventStatus.PUBLISHED
        and (:query is null or lower(e.title) like lower(concat('%', :query, '%')) or lower(e.description) like lower(concat('%', :query, '%')))
        and (:city is null or lower(e.city) = lower(:city))
        and (:fromDate is null or e.startsAt >= :fromDate)
        order by e.startsAt asc
        """)
    Page<Event> searchPublished(@Param("query") String query, @Param("city") String city,
                                @Param("fromDate") Instant fromDate, Pageable pageable);
}

