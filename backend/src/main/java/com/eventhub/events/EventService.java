package com.eventhub.events;

import com.eventhub.shared.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository events;
    private final TicketTypeRepository ticketTypes;
    EventService(EventRepository events, TicketTypeRepository ticketTypes) { this.events = events; this.ticketTypes = ticketTypes; }

    @Transactional
    public EventView create(UUID organizerId, EventData data) {
        validateData(data);
        var event = events.save(new Event(organizerId, uniqueSlug(data.title()), data.title(), data.description(), data.venue(), data.address(),
                data.city(), data.state(), data.timezone(), data.startsAt(), data.endsAt(), data.coverUrl(), data.coverPublicId()));
        var type = ticketTypes.save(new TicketType(event.getId(), data.capacity()));
        return view(event, type);
    }

    @Transactional
    public EventView update(UUID eventId, UUID organizerId, EventData data) {
        validateData(data);
        var event = requireOwned(eventId, organizerId);
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_EDITABLE", "Este evento não pode mais ser editado.");
        var type = requireTicketType(eventId);
        event.update(data.title(), data.description(), data.venue(), data.address(), data.city(), data.state(), data.timezone(), data.startsAt(), data.endsAt(), data.coverUrl(), data.coverPublicId());
        if (ticketTypes.changeCapacity(type.getId(), data.capacity()) != 1) throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_BELOW_REGISTRATIONS", "A capacidade não pode ser menor que o número de inscritos.");
        return view(require(eventId), requireTicketType(eventId));
    }

    @Transactional
    public EventView publish(UUID eventId, UUID organizerId) {
        var event = requireOwned(eventId, organizerId);
        if (event.getStatus() != EventStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_DRAFT", "Somente eventos em rascunho podem ser publicados.");
        if (!event.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_DATE_IN_PAST", "A data do evento deve estar no futuro.");
        event.publish();
        return view(event, requireTicketType(eventId));
    }

    @Transactional
    public EventView cancel(UUID eventId, UUID organizerId) {
        var event = requireOwned(eventId, organizerId);
        event.cancel();
        return view(event, requireTicketType(eventId));
    }

    @Transactional(readOnly = true)
    public EventView getPublished(String slug) {
        var event = events.findBySlugAndStatus(slug, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Evento não encontrado."));
        return view(event, requireTicketType(event.getId()));
    }

    @Transactional(readOnly = true)
    public Event require(UUID eventId) {
        return events.findById(eventId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Evento não encontrado."));
    }

    @Transactional(readOnly = true)
    public Event requireOwned(UUID eventId, UUID organizerId) {
        var event = require(eventId);
        if (!event.getOrganizerId().equals(organizerId)) throw new ApiException(HttpStatus.FORBIDDEN, "NOT_EVENT_OWNER", "Você não administra este evento.");
        return event;
    }

    @Transactional(readOnly = true)
    public TicketType requireTicketType(UUID eventId) {
        return ticketTypes.findFirstByEventId(eventId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TICKET_TYPE_NOT_FOUND", "Tipo de ingresso não encontrado."));
    }

    @Transactional
    public boolean reserve(UUID ticketTypeId) { return ticketTypes.reserve(ticketTypeId) == 1; }
    @Transactional
    public void release(UUID ticketTypeId) { ticketTypes.release(ticketTypeId); }

    @Transactional(readOnly = true)
    public PageView<EventView> search(String query, String city, Instant from, int page, int size) {
        Specification<Event> filter = (root, criteria, cb) -> {
            var predicate = cb.equal(root.get("status"), EventStatus.PUBLISHED);
            var searchText = blankToNull(query);
            if (searchText != null) {
                var pattern = "%" + searchText.toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            var searchCity = blankToNull(city);
            if (searchCity != null) {
                predicate = cb.and(predicate, cb.equal(cb.lower(root.get("city")), searchCity.toLowerCase(Locale.ROOT)));
            }
            if (from != null) predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.<Instant>get("startsAt"), from));
            return predicate;
        };
        var result = events.findAll(filter, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("startsAt").ascending()));
        return new PageView<>(result.getContent().stream().map(e -> view(e, requireTicketType(e.getId()))).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<EventView> organizerEvents(UUID organizerId) {
        return events.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream().map(e -> view(e, requireTicketType(e.getId()))).toList();
    }

    private void validateData(EventData data) {
        if (!data.endsAt().isAfter(data.startsAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_DATES", "O término deve ser posterior ao início.");
        if (data.capacity() < 1 || data.capacity() > 100_000) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CAPACITY", "A capacidade deve ficar entre 1 e 100.000.");
        try { java.time.ZoneId.of(data.timezone()); } catch (Exception e) { throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Informe um fuso horário IANA válido."); }
    }

    private String uniqueSlug(String title) {
        var base = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "evento";
        var slug = base;
        if (events.existsBySlug(slug)) slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        return slug;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private EventView view(Event e, TicketType t) { return new EventView(e.getId(), e.getOrganizerId(), e.getSlug(), e.getTitle(), e.getDescription(), e.getVenue(), e.getAddress(), e.getCity(), e.getState(), e.getTimezone(), e.getStartsAt(), e.getEndsAt(), e.getStatus(), e.getCoverUrl(), e.getCoverPublicId(), t.getId(), t.getCapacity(), t.getConfirmedCount(), t.getAvailable()); }

    public record EventData(String title, String description, String venue, String address, String city, String state,
                            String timezone, Instant startsAt, Instant endsAt, int capacity, String coverUrl, String coverPublicId) {}
    public record EventView(UUID id, UUID organizerId, String slug, String title, String description, String venue, String address,
                            String city, String state, String timezone, Instant startsAt, Instant endsAt, EventStatus status,
                            String coverUrl, String coverPublicId, UUID ticketTypeId, int capacity, int confirmedCount, int available) {}
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}


