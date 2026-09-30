package com.eventhub.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    Optional<Event> findBySlugAndStatus(String slug, EventStatus status);
    boolean existsBySlug(String slug);
    List<Event> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
}


