package com.eventhub.tickets;

import com.eventhub.events.EventService;
import com.eventhub.shared.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final QrTokenService qrTokens;
    private final EventService events;
    private final EntityManager entityManager;
    TicketService(TicketRepository tickets, QrTokenService qrTokens, EventService events, EntityManager entityManager) { this.tickets = tickets; this.qrTokens = qrTokens; this.events = events; this.entityManager = entityManager; }

    @Transactional
    public Ticket create(UUID registrationId, UUID ticketTypeId, UUID eventId, UUID participantId, String attendeeName) {
        return tickets.save(new Ticket(registrationId, ticketTypeId, eventId, participantId, attendeeName, "EVT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()));
    }

    @Transactional
    public void cancelByRegistration(UUID registrationId) {
        tickets.findByRegistrationId(registrationId).forEach(Ticket::cancel);
    }

    @Transactional
    public void lockByRegistration(UUID registrationId) {
        if (tickets.findByRegistrationId(registrationId).isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido.");
    }

    @Transactional(readOnly = true)
    public List<Ticket> byRegistration(UUID registrationId) { return tickets.findByRegistrationId(registrationId); }

    @Transactional(readOnly = true)
    public long activeCount(UUID typeId, UUID ownerId) { return tickets.countByTicketTypeIdAndParticipantIdAndStatus(typeId, ownerId, TicketStatus.ACTIVE); }

    @Transactional(readOnly = true)
    public boolean hasCheckIn(UUID id) { return tickets.hasCheckIn(id); }

    @Transactional(readOnly = true)
    public long countIssuedBetween(UUID eventId, java.time.Instant from, java.time.Instant to) { return tickets.countIssuedBetween(eventId, from, to); }

    @Transactional
    public void cancel(Ticket ticket) { ticket.cancel(); tickets.saveAndFlush(ticket); }

    @Transactional
    public Ticket lockOwned(UUID id, UUID ownerId) {
        var ticket = tickets.findLockedById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido."));
        entityManager.refresh(ticket);
        if (!ticket.getParticipantId().equals(ownerId)) throw new ApiException(HttpStatus.FORBIDDEN, "NOT_TICKET_OWNER", "Este ingresso pertence a outro participante.");
        return ticket;
    }

    @Transactional
    public TicketView rename(UUID id, UUID ownerId, String name) {
        var ticket = lockOwned(id, ownerId);
        var event = events.require(ticket.getEventId());
        if (ticket.getStatus() != TicketStatus.ACTIVE || !event.getStartsAt().isAfter(java.time.Instant.now()) || hasCheckIn(id)) throw new ApiException(HttpStatus.CONFLICT, "TICKET_NOT_EDITABLE", "Este ingresso não permite alterar o nome.");
        if (name == null || name.isBlank() || name.strip().length() > 120) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ATTENDEE_NAME", "Informe um nome de até 120 caracteres.");
        ticket.rename(name.strip());
        return view(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket require(UUID id) {
        return tickets.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido."));
    }

    @Transactional(readOnly = true)
    public Ticket requireOwned(UUID id, UUID participantId) {
        var ticket = require(id);
        if (!ticket.getParticipantId().equals(participantId)) throw new ApiException(HttpStatus.FORBIDDEN, "NOT_TICKET_OWNER", "Este ingresso pertence a outro participante.");
        return ticket;
    }

    @Transactional
    public Ticket fromQr(String token) {
        return tickets.findLockedById(qrTokens.verify(token))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido."));
    }

    @Transactional(readOnly = true)
    public TicketView detail(UUID ticketId, UUID participantId) { return view(requireOwned(ticketId, participantId)); }

    @Transactional(readOnly = true)
    public List<TicketView> mine(UUID participantId) { return tickets.findByParticipantIdOrderByIssuedAtDesc(participantId).stream().map(this::view).toList(); }
    @Transactional(readOnly = true)
    public Page<Ticket> forEvent(UUID eventId, Pageable pageable) { return tickets.findByEventIdOrderByIssuedAtDesc(eventId, pageable); }

    public String qrPayload(Ticket ticket) { return qrTokens.issue(ticket.getId()); }

    private TicketView view(Ticket ticket) {
        var event = events.require(ticket.getEventId());
        var typeName = events.ticketTypes(event.getId()).stream().filter(t -> t.getId().equals(ticket.getTicketTypeId())).findFirst().map(com.eventhub.events.TicketType::getName).orElse("Ingresso geral");
        return new TicketView(ticket.getId(), ticket.getRegistrationId(), ticket.getPublicCode(), ticket.getStatus(), ticket.getIssuedAt(), qrPayload(ticket),
                event.getId(), event.getSlug(), event.getTitle(), event.getVenue(), event.getCity(), event.getState(), event.getStartsAt(), event.getCoverUrl(), event.getStatus(), ticket.getTicketTypeId(), typeName, ticket.getAttendeeName(), event.getDescription().toLowerCase(java.util.Locale.ROOT).contains("evento fictício para demonstração"));
    }

    public record TicketView(UUID id, UUID registrationId, String publicCode, TicketStatus status, java.time.Instant issuedAt,
                             String qrPayload, UUID eventId, String eventSlug, String eventTitle, String venue,
                             String city, String state, java.time.Instant startsAt, String coverUrl, com.eventhub.events.EventStatus eventStatus,
                             UUID ticketTypeId, String ticketTypeName, String attendeeName, boolean demo) {}
}

