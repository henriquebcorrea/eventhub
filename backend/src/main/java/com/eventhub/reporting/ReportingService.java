package com.eventhub.reporting;

import com.eventhub.checkin.CheckInService;
import com.eventhub.events.EventService;
import com.eventhub.registrations.RegistrationRepository;
import com.eventhub.registrations.RegistrationStatus;
import com.eventhub.tickets.TicketService;
import com.eventhub.tickets.TicketStatus;
import com.eventhub.users.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportingService {
    private final EventService events;
    private final RegistrationRepository registrations;
    private final UserService users;
    private final CheckInService checkIns;
    private final TicketService tickets;
    ReportingService(EventService events, RegistrationRepository registrations, UserService users, CheckInService checkIns, TicketService tickets) {
        this.events = events; this.registrations = registrations; this.users = users; this.checkIns = checkIns; this.tickets = tickets;
    }

    @Transactional(readOnly = true)
    public Metrics metrics(UUID eventId, UUID organizerId) {
        events.requireOwned(eventId, organizerId);
        var types = events.ticketTypes(eventId);
        var confirmed = types.stream().mapToInt(t -> t.getConfirmedCount()).sum();
        var capacity = types.stream().mapToInt(t -> t.getCapacity()).sum();
        var checkedIn = checkIns.countForEvent(eventId);
        var now = Instant.now();
        var daily = java.util.stream.IntStream.rangeClosed(0, 6).mapToObj(offset -> {
            var from = now.minus(6L - offset, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            return new DailyPoint(from, tickets.countIssuedBetween(eventId, from, from.plus(1, ChronoUnit.DAYS)));
        }).toList();
        var perType = types.stream().map(t -> new TypeMetrics(t.getId(), t.getName(), t.getConfirmedCount(), t.getCapacity(), checkIns.countForType(t.getId()))).toList();
        return new Metrics(confirmed, capacity, checkedIn, confirmed == 0 ? 0 : Math.round((checkedIn * 1000.0 / confirmed)) / 10.0, daily, perType);
    }

    @Transactional(readOnly = true)
    public EventService.PageView<Attendee> attendees(UUID eventId, UUID organizerId, int page, int size) {
        events.requireOwned(eventId, organizerId);
        var result = tickets.forEvent(eventId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50)));
        var userMap = users.findAllById(result.getContent().stream().map(t -> t.getParticipantId()).toList()).stream().collect(Collectors.toMap(u -> u.getId(), Function.identity()));
        var typeMap = events.ticketTypes(eventId).stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getName()));
        var content = result.getContent().stream().map(t -> {
            var u = userMap.get(t.getParticipantId());
            return new Attendee(t.getRegistrationId(), t.getAttendeeName(), u == null ? "" : u.getEmail(), t.getStatus() == TicketStatus.ACTIVE ? RegistrationStatus.CONFIRMED : RegistrationStatus.CANCELLED, t.getIssuedAt(), t.getId(), typeMap.getOrDefault(t.getTicketTypeId(), "Ingresso geral"));
        }).toList();
        return new EventService.PageView<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public record Metrics(long confirmed, long capacity, long checkedIn, double checkInRate, List<DailyPoint> registrationsByDay, List<TypeMetrics> byType) {}
    public record TypeMetrics(UUID ticketTypeId, String name, long confirmed, long capacity, long checkedIn) {}
    public record DailyPoint(Instant day, long count) {}
    public record Attendee(UUID registrationId, String name, String email, RegistrationStatus status, Instant registeredAt, UUID ticketId, String ticketTypeName) {}
}

