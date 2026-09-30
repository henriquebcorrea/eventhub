package com.eventhub.registrations;

import com.eventhub.events.EventService;
import com.eventhub.events.EventStatus;
import com.eventhub.notifications.NotificationService;
import com.eventhub.shared.ApiException;
import com.eventhub.tickets.TicketService;
import com.eventhub.users.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final EventService events;
    private final TicketService tickets;
    private final UserService users;
    private final NotificationService notifications;
    RegistrationService(RegistrationRepository registrations, EventService events, TicketService tickets, UserService users, NotificationService notifications) {
        this.registrations = registrations; this.events = events; this.tickets = tickets; this.users = users; this.notifications = notifications;
    }

    @Transactional
    public RegistrationResult register(UUID eventId, UUID participantId) {
        var event = events.require(eventId);
        if (event.getStatus() != EventStatus.PUBLISHED || !event.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_OPEN", "Este evento não está aceitando inscrições.");
        if (event.getOrganizerId().equals(participantId)) throw new ApiException(HttpStatus.CONFLICT, "ORGANIZER_CANNOT_REGISTER", "O organizador não pode se inscrever no próprio evento.");
        if (registrations.existsByEventIdAndParticipantId(eventId, participantId)) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "Você já se inscreveu neste evento.");
        var type = events.requireTicketType(eventId);
        if (!events.reserve(type.getId())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_SOLD_OUT", "Os ingressos deste evento estão esgotados.");
        Registration registration;
        try {
            registration = registrations.saveAndFlush(new Registration(eventId, type.getId(), participantId));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED", "Você já se inscreveu neste evento.");
        }
        var ticket = tickets.create(registration.getId(), eventId, participantId);
        var user = users.require(participantId);
        notifications.enqueueTicket(user.getEmail(), user.getName(), event.getTitle(), ticket.getId(), ticket.getPublicCode());
        return new RegistrationResult(registration.getId(), registration.getStatus(), registration.getCreatedAt(), tickets.detail(ticket.getId(), participantId));
    }

    @Transactional
    public void cancel(UUID registrationId, UUID participantId) {
        var registration = registrations.findByIdAndParticipantId(registrationId, participantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REGISTRATION_NOT_FOUND", "Inscrição não encontrada."));
        if (registration.getStatus() == RegistrationStatus.CANCELLED) return;
        tickets.lockByRegistration(registrationId);
        if (registrations.hasCheckIn(registrationId)) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CHECKED_IN", "Não é possível cancelar após o check-in.");
        var event = events.require(registration.getEventId());
        if (!event.getStartsAt().isAfter(Instant.now())) throw new ApiException(HttpStatus.CONFLICT, "EVENT_ALREADY_STARTED", "Não é possível cancelar após o início do evento.");
        registration.cancel(); tickets.cancelByRegistration(registrationId); events.release(registration.getTicketTypeId());
    }

    @Transactional(readOnly = true)
    public List<Registration> mine(UUID participantId) { return registrations.findByParticipantIdOrderByCreatedAtDesc(participantId); }

    public record RegistrationResult(UUID registrationId, RegistrationStatus status, Instant registeredAt, TicketService.TicketView ticket) {}
}

