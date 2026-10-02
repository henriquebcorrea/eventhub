package com.eventhub.checkin;

import com.eventhub.events.EventService;
import com.eventhub.events.EventStatus;
import com.eventhub.shared.ApiException;
import com.eventhub.tickets.TicketStatus;
import com.eventhub.tickets.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class CheckInService {
    private final CheckInRepository checkIns;
    private final TicketService tickets;
    private final EventService events;
    CheckInService(CheckInRepository checkIns, TicketService tickets, EventService events) { this.checkIns = checkIns; this.tickets = tickets; this.events = events; }

    @Transactional
    public Result checkIn(UUID eventId, String qrToken, UUID organizerId) {
        var event = events.requireOwned(eventId, organizerId);
        if (event.getStatus() != EventStatus.PUBLISHED) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_OPEN", "Este evento não está aberto para check-in.");
        var ticket = tickets.fromQr(qrToken);
        if (!ticket.getEventId().equals(eventId) || ticket.getStatus() != TicketStatus.ACTIVE) throw new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido para este evento.");
        var now = Instant.now();
        if (checkIns.insertIfAbsent(UUID.randomUUID(), ticket.getId(), eventId, organizerId, now) == 0) {
            var previous = checkIns.findByTicketId(ticket.getId()).orElseThrow();
            var localTime = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                    .withZone(ZoneId.of(event.getTimezone())).format(previous.getCheckedInAt());
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CHECKED_IN", "Ingresso já utilizado — check-in realizado em " + localTime + ".");
        }
        var typeName = events.ticketTypes(eventId).stream().filter(t -> t.getId().equals(ticket.getTicketTypeId())).findFirst().map(com.eventhub.events.TicketType::getName).orElse("Ingresso geral");
        return new Result("CHECKED_IN", "Ingresso válido — entrada autorizada", ticket.getPublicCode(), now, ticket.getAttendeeName(), typeName);
    }

    @Transactional(readOnly = true)
    public long countForEvent(UUID eventId) { return checkIns.countByEventId(eventId); }
    @Transactional(readOnly = true)
    public long countForType(UUID typeId) { return checkIns.countByTicketType(typeId); }

    public record Result(String status, String message, String publicCode, Instant checkedInAt, String attendeeName, String ticketTypeName) {}
}


