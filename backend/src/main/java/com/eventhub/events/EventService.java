package com.eventhub.events;

import com.eventhub.shared.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EventService {
    private final EventRepository events;
    private final TicketTypeRepository ticketTypes;
    private final ApplicationEventPublisher publisher;
    EventService(EventRepository events, TicketTypeRepository ticketTypes, ApplicationEventPublisher publisher) { this.events = events; this.ticketTypes = ticketTypes; this.publisher = publisher; }

    @Transactional
    public EventView create(UUID organizerId, EventData data) {
        validateData(data);
        var event = events.save(new Event(organizerId, uniqueSlug(data.title()), data.title(), data.description(), data.venue(), data.address(),
                data.city(), data.state(), data.timezone(), data.startsAt(), data.endsAt(), data.coverUrl(), data.coverPublicId()));
        var definitions = definitions(data);
        for (var definition : definitions) ticketTypes.save(new TicketType(event.getId(), definition.name(), definition.capacity()));
        return view(event);
    }

    @Transactional
    public EventView update(UUID eventId, UUID organizerId, EventData data) {
        validateData(data);
        var event = requireOwned(eventId, organizerId);
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_EDITABLE", "Este evento não pode mais ser editado.");
        if (!event.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_ALREADY_STARTED", "Este evento já começou.");
        event.update(data.title(), data.description(), data.venue(), data.address(), data.city(), data.state(), data.timezone(), data.startsAt(), data.endsAt(), data.coverUrl(), data.coverPublicId());
        if (data.ticketTypes() == null) {
            var first = lockTicketType(eventId, requireTicketType(eventId).getId());
            changeType(first, first.getName(), data.capacity());
        } else {
            var definitions = definitions(data);
            var existing = ticketTypes.findByEventIdOrderByNameAsc(eventId);
            for (var definition : definitions) {
                if (definition.id() == null) ticketTypes.save(new TicketType(eventId, definition.name(), definition.capacity()));
                else changeType(lockTicketType(eventId, definition.id()), definition.name(), definition.capacity());
            }
            var retained = definitions.stream().map(TicketTypeData::id).filter(java.util.Objects::nonNull).toList();
            for (var old : existing) if (!retained.contains(old.getId())) {
                if (ticketTypes.hasHistory(old.getId())) throw new ApiException(HttpStatus.CONFLICT, "TICKET_TYPE_HAS_HISTORY", "Um tipo com ingressos ou fila não pode ser removido.");
                ticketTypes.delete(old);
            }
        }
        ticketTypes.flush();
        return view(event);
    }

    @Transactional
    public EventView publish(UUID eventId, UUID organizerId) {
        var event = requireOwned(eventId, organizerId);
        if (event.getStatus() != EventStatus.DRAFT) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_DRAFT", "Somente eventos em rascunho podem ser publicados.");
        if (!event.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_DATE_IN_PAST", "A data do evento deve estar no futuro.");
        event.publish();
        return view(event);
    }

    @Transactional
    public EventView cancel(UUID eventId, UUID organizerId) {
        var event = requireOwned(eventId, organizerId);
        event.cancel();
        return view(event);
    }

    @Transactional(readOnly = true)
    public EventView getPublished(String slug) {
        var event = events.findBySlugAndStatus(slug, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Evento não encontrado."));
        return view(event);
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
    public TicketType lockTicketType(UUID eventId, UUID typeId) {
        var type = ticketTypes.findLockedById(typeId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TICKET_TYPE_NOT_FOUND", "Tipo de ingresso não encontrado."));
        if (!type.getEventId().equals(eventId)) throw new ApiException(HttpStatus.NOT_FOUND, "TICKET_TYPE_NOT_FOUND", "Tipo de ingresso não encontrado.");
        return type;
    }

    @Transactional(readOnly = true)
    public List<TicketType> ticketTypes(UUID eventId) { return ticketTypes.findByEventIdOrderByNameAsc(eventId); }

    @Transactional
    public boolean reserve(UUID ticketTypeId, int quantity) { return ticketTypes.reserve(ticketTypeId, quantity) == 1; }
    @Transactional
    public void release(UUID ticketTypeId, int quantity) { ticketTypes.release(ticketTypeId, quantity); }

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
        return new PageView<>(result.getContent().stream().map(this::view).toList(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<EventView> organizerEvents(UUID organizerId) {
        return events.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream().map(this::view).toList();
    }

    private void validateData(EventData data) {
        if (!data.endsAt().isAfter(data.startsAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_DATES", "O término deve ser posterior ao início.");
        definitions(data);
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
    private List<TicketTypeData> definitions(EventData data) {
        var values = data.ticketTypes() == null ? List.of(new TicketTypeData(null, "Ingresso geral", data.capacity())) : data.ticketTypes();
        if (values.isEmpty() || values.size() > 10) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TICKET_TYPES", "Informe entre 1 e 10 tipos de ingresso.");
        var names = new java.util.HashSet<String>();
        var ids = new java.util.HashSet<UUID>();
        for (var type : values) {
            if (type.name() == null || type.name().isBlank() || type.name().length() > 100 || !names.add(type.name().strip().toLowerCase(Locale.ROOT))) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TICKET_TYPES", "Nomes de ingresso devem ser únicos e ter até 100 caracteres.");
            if (type.capacity() < 1 || type.capacity() > 100_000 || (type.id() != null && !ids.add(type.id()))) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TICKET_TYPES", "Capacidade ou identificador de ingresso inválido.");
        }
        return values;
    }

    private void changeType(TicketType type, String name, int capacity) {
        if (capacity < type.getConfirmedCount() || capacity < ticketTypes.largestPendingGroup(type.getId())) throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_BELOW_REGISTRATIONS", "A capacidade não pode impedir ingressos ativos ou grupos na fila.");
        var increased = capacity > type.getCapacity();
        type.changeCapacity(capacity); type.rename(name.strip());
        ticketTypes.saveAndFlush(type);
        if (increased) publisher.publishEvent(new CapacityIncreased(type.getEventId(), type.getId()));
    }

    private EventView view(Event e) {
        var types = ticketTypes.findByEventIdOrderByNameAsc(e.getId());
        var primary = types.get(0);
        var capacity = types.stream().mapToInt(TicketType::getCapacity).sum();
        var confirmed = types.stream().mapToInt(TicketType::getConfirmedCount).sum();
        return new EventView(e.getId(), e.getOrganizerId(), e.getSlug(), e.getTitle(), e.getDescription(), e.getVenue(), e.getAddress(), e.getCity(), e.getState(), e.getTimezone(), e.getStartsAt(), e.getEndsAt(), e.getStatus(), e.getCoverUrl(), e.getCoverPublicId(), primary.getId(), capacity, confirmed, capacity - confirmed, types.stream().map(t -> new TicketTypeView(t.getId(), t.getName(), t.getCapacity(), t.getConfirmedCount(), t.getAvailable(), ticketTypes.waitingCount(t.getId()))).toList());
    }

    public record EventData(String title, String description, String venue, String address, String city, String state,
                            String timezone, Instant startsAt, Instant endsAt, int capacity, String coverUrl, String coverPublicId, List<TicketTypeData> ticketTypes) {
        public EventData(String title, String description, String venue, String address, String city, String state, String timezone, Instant startsAt, Instant endsAt, int capacity, String coverUrl, String coverPublicId) { this(title, description, venue, address, city, state, timezone, startsAt, endsAt, capacity, coverUrl, coverPublicId, null); }
    }
    public record TicketTypeData(UUID id, String name, int capacity) {}
    public record TicketTypeView(UUID id, String name, int capacity, int confirmedCount, int available, long waitingCount) {}
    public record CapacityIncreased(UUID eventId, UUID ticketTypeId) {}
    public record EventView(UUID id, UUID organizerId, String slug, String title, String description, String venue, String address,
                            String city, String state, String timezone, Instant startsAt, Instant endsAt, EventStatus status,
                            String coverUrl, String coverPublicId, UUID ticketTypeId, int capacity, int confirmedCount, int available, List<TicketTypeView> ticketTypes) {}
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}


