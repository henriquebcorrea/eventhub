package com.eventhub.tickets;

import com.eventhub.events.EventService;
import com.eventhub.shared.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final QrTokenService qrTokens;
    private final EventService events;
    TicketService(TicketRepository tickets, QrTokenService qrTokens, EventService events) { this.tickets = tickets; this.qrTokens = qrTokens; this.events = events; }

    @Transactional
    public Ticket create(UUID registrationId, UUID eventId, UUID participantId) {
        return tickets.save(new Ticket(registrationId, eventId, participantId, "EVT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()));
    }

    @Transactional
    public void cancelByRegistration(UUID registrationId) {
        tickets.findByRegistrationId(registrationId).ifPresent(Ticket::cancel);
    }

    @Transactional
    public void lockByRegistration(UUID registrationId) {
        tickets.findByRegistrationId(registrationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido."));
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

    public String qrPayload(Ticket ticket) { return qrTokens.issue(ticket.getId()); }

    private TicketView view(Ticket ticket) {
        var event = events.require(ticket.getEventId());
        return new TicketView(ticket.getId(), ticket.getRegistrationId(), ticket.getPublicCode(), ticket.getStatus(), ticket.getIssuedAt(), qrPayload(ticket),
                event.getId(), event.getSlug(), event.getTitle(), event.getVenue(), event.getCity(), event.getState(), event.getStartsAt(), event.getCoverUrl(), event.getStatus());
    }

    public record TicketView(UUID id, UUID registrationId, String publicCode, TicketStatus status, java.time.Instant issuedAt,
                             String qrPayload, UUID eventId, String eventSlug, String eventTitle, String venue,
                             String city, String state, java.time.Instant startsAt, String coverUrl, com.eventhub.events.EventStatus eventStatus) {}
}

