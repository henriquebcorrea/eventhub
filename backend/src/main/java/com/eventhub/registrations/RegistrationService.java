package com.eventhub.registrations;

import com.eventhub.events.EventService;
import com.eventhub.events.EventStatus;
import com.eventhub.notifications.NotificationService;
import com.eventhub.shared.ApiException;
import com.eventhub.tickets.Ticket;
import com.eventhub.tickets.TicketService;
import com.eventhub.tickets.TicketStatus;
import com.eventhub.users.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final WaitlistRepository waitlist;
    private final EventService events;
    private final TicketService tickets;
    private final UserService users;
    private final NotificationService notifications;
    private final EntityManager entityManager;

    RegistrationService(RegistrationRepository registrations, WaitlistRepository waitlist, EventService events,
                        TicketService tickets, UserService users, NotificationService notifications, EntityManager entityManager) {
        this.registrations = registrations; this.waitlist = waitlist; this.events = events;
        this.tickets = tickets; this.users = users; this.notifications = notifications; this.entityManager = entityManager;
    }

    @Transactional
    public RegistrationResult register(UUID eventId, UUID participantId) {
        return register(eventId, participantId, null, List.of(users.require(participantId).getName()));
    }

    @Transactional
    public RegistrationResult register(UUID eventId, UUID participantId, UUID typeId, List<String> attendeeNames) {
        var names = validateNames(attendeeNames);
        var selectedId = typeId == null ? events.requireTicketType(eventId).getId() : typeId;
        var type = events.lockTicketType(eventId, selectedId);
        validateOpen(eventId, participantId);
        enforceAccountLimit(selectedId, participantId, names.size());
        if (waitlist.countByTicketTypeIdAndStatus(selectedId, WaitlistStatus.WAITING) > 0)
            throw new ApiException(HttpStatus.CONFLICT, "WAITLIST_ACTIVE", "Este tipo possui lista de espera. Entre na fila para preservar a ordem.");
        if (type.getAvailable() < names.size() || !events.reserve(selectedId, names.size()))
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_SOLD_OUT", "Não há vagas suficientes para o grupo. Você pode entrar na lista de espera.");
        return createConfirmed(eventId, selectedId, participantId, names);
    }

    @Transactional
    public WaitlistView joinWaitlist(UUID eventId, UUID participantId, UUID typeId, List<String> attendeeNames) {
        var names = validateNames(attendeeNames);
        var type = events.lockTicketType(eventId, typeId);
        validateOpen(eventId, participantId);
        if (names.size() > type.getCapacity()) throw new ApiException(HttpStatus.CONFLICT, "GROUP_EXCEEDS_CAPACITY", "O grupo é maior que a capacidade deste tipo.");
        enforceAccountLimit(typeId, participantId, names.size());
        if (waitlist.countByTicketTypeIdAndStatus(typeId, WaitlistStatus.WAITING) == 0 && type.getAvailable() >= names.size())
            throw new ApiException(HttpStatus.CONFLICT, "TICKETS_AVAILABLE", "Há vagas disponíveis. Faça a inscrição diretamente.");
        var request = waitlist.saveAndFlush(new WaitlistRequest(eventId, typeId, participantId, names));
        return view(request);
    }

    @Transactional(readOnly = true)
    public List<WaitlistView> myWaitlist(UUID participantId) {
        return waitlist.findByParticipantIdAndStatusOrderByIdAsc(participantId, WaitlistStatus.WAITING).stream().map(this::view).toList();
    }

    @Transactional
    public void leaveWaitlist(Long requestId, UUID participantId) {
        var existing = waitlist.findByIdAndParticipantId(requestId, participantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WAITLIST_NOT_FOUND", "Pedido não encontrado."));
        events.lockTicketType(existing.getEventId(), existing.getTicketTypeId());
        var request = waitlist.findByIdAndParticipantId(requestId, participantId).orElseThrow();
        entityManager.refresh(request);
        if (request.getStatus() != WaitlistStatus.WAITING)
            throw new ApiException(HttpStatus.CONFLICT, "WAITLIST_RESOLVED", "Este pedido já foi resolvido.");
        request.cancel();
        waitlist.saveAndFlush(request);
        promote(existing.getEventId(), existing.getTicketTypeId());
    }

    @Transactional
    public void cancelTicket(UUID ticketId, UUID participantId) {
        var initial = tickets.requireOwned(ticketId, participantId);
        events.lockTicketType(initial.getEventId(), initial.getTicketTypeId());
        var ticket = tickets.lockOwned(ticketId, participantId);
        cancelTickets(List.of(ticket), participantId);
    }

    @Transactional
    public void cancel(UUID registrationId, UUID participantId) {
        var registration = registrations.findByIdAndParticipantId(registrationId, participantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REGISTRATION_NOT_FOUND", "Inscrição não encontrada."));
        events.lockTicketType(registration.getEventId(), registration.getTicketTypeId());
        var active = tickets.byRegistration(registrationId).stream().filter(t -> t.getStatus() == TicketStatus.ACTIVE).map(t -> tickets.lockOwned(t.getId(), participantId)).toList();
        if (active.isEmpty()) return;
        cancelTickets(active, participantId);
    }

    private void cancelTickets(List<Ticket> selected, UUID participantId) {
        var first = selected.get(0);
        var event = events.require(first.getEventId());
        if (!event.getStartsAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_ALREADY_STARTED", "Não é possível cancelar após o início do evento.");
        for (var ticket : selected) {
            if (!ticket.getParticipantId().equals(participantId)) throw new ApiException(HttpStatus.FORBIDDEN, "NOT_TICKET_OWNER", "Este ingresso pertence a outro participante.");
            if (ticket.getStatus() != TicketStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT, "TICKET_CANCELLED", "Este ingresso já foi cancelado.");
            if (tickets.hasCheckIn(ticket.getId())) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CHECKED_IN", "Não é possível cancelar após o check-in.");
        }
        for (var ticket : selected) tickets.cancel(ticket);
        var remaining = tickets.byRegistration(first.getRegistrationId()).stream().anyMatch(t -> t.getStatus() == TicketStatus.ACTIVE);
        if (!remaining) {
            var registration = registrations.findById(first.getRegistrationId()).orElseThrow();
            registration.cancel();
            registrations.saveAndFlush(registration);
        }
        events.release(first.getTicketTypeId(), selected.size());
        promote(first.getEventId(), first.getTicketTypeId());
    }

    @EventListener
    @Transactional
    public void onCapacityIncreased(EventService.CapacityIncreased event) {
        events.lockTicketType(event.eventId(), event.ticketTypeId());
        promote(event.eventId(), event.ticketTypeId());
    }

    private void promote(UUID eventId, UUID typeId) {
        while (true) {
            var next = waitlist.findFirstByTicketTypeIdAndStatusOrderByIdAsc(typeId, WaitlistStatus.WAITING);
            if (next.isEmpty()) return;
            var request = next.get();
            var event = events.require(eventId);
            if (event.getStatus() != EventStatus.PUBLISHED || !event.getStartsAt().isAfter(Instant.now())) return;
            var type = events.lockTicketType(eventId, typeId);
            var names = request.getAttendeeNames();
            if (type.getAvailable() < names.size()) return;
            request.promote();
            waitlist.saveAndFlush(request);
            if (!events.reserve(typeId, names.size())) throw new IllegalStateException("A reserva da promoção falhou sob bloqueio.");
            createConfirmed(eventId, typeId, request.getParticipantId(), names);
        }
    }

    private RegistrationResult createConfirmed(UUID eventId, UUID typeId, UUID participantId, List<String> names) {
        var registration = registrations.saveAndFlush(new Registration(eventId, typeId, participantId));
        var event = events.require(eventId);
        var user = users.require(participantId);
        var created = names.stream().map(name -> tickets.create(registration.getId(), typeId, eventId, participantId, name)).toList();
        for (var ticket : created)
            notifications.enqueueTicket(user.getEmail(), user.getName(), event.getTitle(), ticket.getId(), ticket.getPublicCode());
        var views = created.stream().map(t -> tickets.detail(t.getId(), participantId)).toList();
        return new RegistrationResult(registration.getId(), registration.getStatus(), registration.getCreatedAt(), views.get(0), views);
    }

    private void validateOpen(UUID eventId, UUID participantId) {
        var event = events.require(eventId);
        if (event.getStatus() != EventStatus.PUBLISHED || !event.getStartsAt().isAfter(Instant.now()))
            throw new ApiException(HttpStatus.CONFLICT, "EVENT_NOT_OPEN", "Este evento não está aceitando inscrições.");
        if (event.getOrganizerId().equals(participantId))
            throw new ApiException(HttpStatus.CONFLICT, "ORGANIZER_CANNOT_REGISTER", "O organizador não pode se inscrever no próprio evento.");
    }

    private void enforceAccountLimit(UUID typeId, UUID participantId, int requested) {
        if (tickets.activeCount(typeId, participantId) + waitlist.pendingQuantity(typeId, participantId) + requested > 4)
            throw new ApiException(HttpStatus.CONFLICT, "TICKET_LIMIT_REACHED", "O limite é de quatro ingressos por conta e tipo, incluindo a fila.");
    }

    private List<String> validateNames(List<String> names) {
        if (names == null || names.isEmpty() || names.size() > 4 || names.stream().anyMatch(n -> n == null || n.isBlank() || n.strip().length() > 120))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ATTENDEE_NAMES", "Informe de um a quatro nomes, com até 120 caracteres cada.");
        return names.stream().map(String::strip).toList();
    }

    private WaitlistView view(WaitlistRequest request) {
        var event = events.require(request.getEventId());
        var typeName = events.ticketTypes(request.getEventId()).stream().filter(t -> t.getId().equals(request.getTicketTypeId())).findFirst().map(com.eventhub.events.TicketType::getName).orElse("Ingresso geral");
        return new WaitlistView(request.getId(), request.getEventId(), request.getTicketTypeId(), request.getStatus(),
                request.getAttendeeNames(), request.getCreatedAt(), request.getStatus() == WaitlistStatus.WAITING
                        ? waitlist.position(request.getTicketTypeId(), request.getId()) : 0, event.getTitle(), event.getSlug(), typeName);
    }

    @Transactional(readOnly = true)
    public List<Registration> mine(UUID participantId) { return registrations.findByParticipantIdOrderByCreatedAtDesc(participantId); }

    public record RegistrationResult(UUID registrationId, RegistrationStatus status, Instant registeredAt,
                                     TicketService.TicketView ticket, List<TicketService.TicketView> tickets) {}
    public record WaitlistView(Long id, UUID eventId, UUID ticketTypeId, WaitlistStatus status,
                               List<String> attendeeNames, Instant createdAt, long position, String eventTitle, String eventSlug, String ticketTypeName) {}
}
