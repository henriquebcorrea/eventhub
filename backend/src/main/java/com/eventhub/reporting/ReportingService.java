package com.eventhub.reporting;

import com.eventhub.checkin.CheckInService;
import com.eventhub.events.EventService;
import com.eventhub.registrations.RegistrationRepository;
import com.eventhub.registrations.RegistrationStatus;
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
    ReportingService(EventService events, RegistrationRepository registrations, UserService users, CheckInService checkIns) {
        this.events = events; this.registrations = registrations; this.users = users; this.checkIns = checkIns;
    }

    @Transactional(readOnly = true)
    public Metrics metrics(UUID eventId, UUID organizerId) {
        events.requireOwned(eventId, organizerId);
        var type = events.requireTicketType(eventId);
        var checkedIn = checkIns.countForEvent(eventId);
        var now = Instant.now();
        var daily = java.util.stream.IntStream.rangeClosed(0, 6).mapToObj(offset -> {
            var from = now.minus(6L - offset, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            return new DailyPoint(from, registrations.countByEventIdAndStatusAndCreatedAtBetween(eventId, RegistrationStatus.CONFIRMED, from, from.plus(1, ChronoUnit.DAYS)));
        }).toList();
        return new Metrics(type.getConfirmedCount(), type.getCapacity(), checkedIn, type.getConfirmedCount() == 0 ? 0 : Math.round((checkedIn * 1000.0 / type.getConfirmedCount())) / 10.0, daily);
    }

    @Transactional(readOnly = true)
    public EventService.PageView<Attendee> attendees(UUID eventId, UUID organizerId, int page, int size) {
        events.requireOwned(eventId, organizerId);
        var result = registrations.findByEventIdOrderByCreatedAtDesc(eventId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50)));
        var userMap = users.findAllById(result.getContent().stream().map(r -> r.getParticipantId()).toList()).stream().collect(Collectors.toMap(u -> u.getId(), Function.identity()));
        var content = result.getContent().stream().map(r -> {
            var u = userMap.get(r.getParticipantId());
            return new Attendee(r.getId(), u == null ? "Participante" : u.getName(), u == null ? "" : u.getEmail(), r.getStatus(), r.getCreatedAt());
        }).toList();
        return new EventService.PageView<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public record Metrics(long confirmed, long capacity, long checkedIn, double checkInRate, List<DailyPoint> registrationsByDay) {}
    public record DailyPoint(Instant day, long count) {}
    public record Attendee(UUID registrationId, String name, String email, RegistrationStatus status, Instant registeredAt) {}
}

