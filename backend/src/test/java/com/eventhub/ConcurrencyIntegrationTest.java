package com.eventhub;

import com.eventhub.checkin.CheckInService;
import com.eventhub.events.EventService;
import com.eventhub.registrations.RegistrationService;
import com.eventhub.tickets.TicketService;
import com.eventhub.tickets.TicketStatus;
import com.eventhub.shared.ApiException;
import com.eventhub.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.demo-seed=false", "app.jwt.allow-ephemeral=true", "app.outbox-delay-ms=3600000", "app.qr-secret=test-secret-with-at-least-thirty-two-characters"})
@Testcontainers(disabledWithoutDocker = true)
class ConcurrencyIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired UserService users; @Autowired EventService events; @Autowired RegistrationService registrations; @Autowired CheckInService checkIns; @Autowired TicketService tickets;
    UUID organizerId, eventId; String suffix;
    @BeforeEach void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        organizerId = users.create("Organizador", "org-" + suffix + "@test.dev", "hash", true).getId();
        var start = Instant.now().plusSeconds(86400);
        var event = events.create(organizerId, new EventService.EventData("Evento " + suffix, "Descrição suficientemente longa para validar o evento concorrente.", "Arena", "Rua de Teste, 1", "São Paulo", "SP", "America/Sao_Paulo", start, start.plusSeconds(7200), 1, null, null));
        eventId = event.id(); events.publish(eventId, organizerId);
    }

    @Test void onlyOneParticipantGetsTheLastTicket() throws Exception {
        var participants = java.util.stream.IntStream.range(0, 20)
                .mapToObj(index -> users.create("Participante " + index, "p" + index + "-" + suffix + "@test.dev", "hash", false).getId())
                .toList();
        var barrier = new CyclicBarrier(participants.size());
        var pool = Executors.newFixedThreadPool(participants.size());
        try {
            var attempts = participants.stream().map(id -> pool.submit(() -> attempt(id, barrier))).toList();
            var results = attempts.stream().map(future -> {
                try { return future.get(30, TimeUnit.SECONDS); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            }).toList();
            assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
            assertThat(results.stream().filter(value -> !value).count()).isEqualTo(19);
        } finally { pool.shutdownNow(); }
    }

    @Test void catalogSearchHandlesAbsentAndPresentFilters() {
        assertThat(events.search(null, null, null, 0, 50).totalElements()).isGreaterThanOrEqualTo(1);
        var matching = events.search("Evento " + suffix, "sÃO pAULO", null, 0, 50);
        assertThat(matching.totalElements()).isEqualTo(1);
        assertThat(matching.content().get(0).id()).isEqualTo(eventId);
        assertThat(events.search("Evento " + suffix, null, Instant.now().plusSeconds(172800), 0, 50).totalElements()).isZero();
    }

    @Test void oneAccountCannotExceedFourTicketsUnderConcurrency() throws Exception {
        var event = createEvent(10, null);
        var participant = users.create("Grupo", "group-" + suffix + "@test.dev", "hash", false).getId();
        var typeId = event.ticketTypeId();
        var barrier = new CyclicBarrier(5);
        var pool = Executors.newFixedThreadPool(5);
        try {
            var attempts = java.util.stream.IntStream.range(0, 5).mapToObj(index -> pool.submit(() -> {
                try { barrier.await(5, TimeUnit.SECONDS); registrations.register(event.id(), participant, typeId, List.of("Pessoa " + index)); return true; }
                catch (ApiException expected) { return false; }
            })).toList();
            assertThat(attempts.stream().filter(f -> {
                try { return f.get(30, TimeUnit.SECONDS); } catch (Exception e) { throw new RuntimeException(e); }
            }).count()).isEqualTo(4);
            assertThat(tickets.mine(participant).stream().filter(t -> t.eventId().equals(event.id()) && t.status() == TicketStatus.ACTIVE).count()).isEqualTo(4);
        } finally { pool.shutdownNow(); }
    }

    @Test void fifoBlocksSmallerGroupsUntilFrontGroupFits() {
        var event = createEvent(4, null);
        var holder = users.create("Titular", "holder-" + suffix + "@test.dev", "hash", false).getId();
        var group = users.create("Grupo", "fifo-group-" + suffix + "@test.dev", "hash", false).getId();
        var small = users.create("Individual", "fifo-small-" + suffix + "@test.dev", "hash", false).getId();
        var booked = registrations.register(event.id(), holder, event.ticketTypeId(), List.of("A", "B", "C", "D"));
        var first = registrations.joinWaitlist(event.id(), group, event.ticketTypeId(), List.of("E", "F", "G"));
        var second = registrations.joinWaitlist(event.id(), small, event.ticketTypeId(), List.of("H"));
        registrations.cancelTicket(booked.tickets().get(0).id(), holder);
        assertThat(registrations.myWaitlist(group)).extracting(RegistrationService.WaitlistView::id).contains(first.id());
        assertThat(registrations.myWaitlist(small)).extracting(RegistrationService.WaitlistView::id).contains(second.id());
        registrations.cancelTicket(booked.tickets().get(1).id(), holder);
        registrations.cancelTicket(booked.tickets().get(2).id(), holder);
        assertThat(registrations.myWaitlist(group)).isEmpty();
        assertThat(tickets.mine(group).stream().filter(t -> t.eventId().equals(event.id()) && t.status() == TicketStatus.ACTIVE).count()).isEqualTo(3);
        assertThat(registrations.myWaitlist(small)).hasSize(1);
        registrations.cancelTicket(booked.tickets().get(3).id(), holder);
        assertThat(registrations.myWaitlist(small)).isEmpty();
    }

    @Test void capacityIncreasePromotesAndDifferentTypesAreIndependent() {
        var event = createEvent(1, List.of(new EventService.TicketTypeData(null, "Pista", 1), new EventService.TicketTypeData(null, "Arquibancada", 2)));
        var pista = event.ticketTypes().stream().filter(t -> t.name().equals("Pista")).findFirst().orElseThrow();
        var arquibancada = event.ticketTypes().stream().filter(t -> t.name().equals("Arquibancada")).findFirst().orElseThrow();
        var holder = users.create("Titular", "cap-holder-" + suffix + "@test.dev", "hash", false).getId();
        var waiting = users.create("Espera", "cap-wait-" + suffix + "@test.dev", "hash", false).getId();
        registrations.register(event.id(), holder, pista.id(), List.of("Titular"));
        registrations.register(event.id(), holder, arquibancada.id(), List.of("Acompanhante"));
        registrations.joinWaitlist(event.id(), waiting, pista.id(), List.of("Espera"));
        events.update(event.id(), organizerId, new EventService.EventData(event.title(), event.description(), event.venue(), event.address(), event.city(), event.state(), event.timezone(), event.startsAt(), event.endsAt(), 4, event.coverUrl(), event.coverPublicId(), List.of(new EventService.TicketTypeData(pista.id(), "Pista", 2), new EventService.TicketTypeData(arquibancada.id(), "Arquibancada", 2))));
        assertThat(registrations.myWaitlist(waiting)).isEmpty();
        assertThat(tickets.mine(waiting)).hasSize(1);
        assertThat(tickets.mine(holder).stream().filter(t -> t.eventId().equals(event.id())).count()).isEqualTo(2);
    }

    @Test void capacityIncreaseStopsAfterPromotingWholeGroupsThatFit() {
        var event = createEvent(4, null);
        var holder = users.create("Titular", "batch-holder-" + suffix + "@test.dev", "hash", false).getId();
        registrations.register(event.id(), holder, event.ticketTypeId(), List.of("A", "B", "C", "D"));
        var queued = java.util.stream.IntStream.range(0, 3).mapToObj(index -> {
            var participant = users.create("Fila " + index, "batch-" + index + "-" + suffix + "@test.dev", "hash", false).getId();
            registrations.joinWaitlist(event.id(), participant, event.ticketTypeId(), List.of("Pessoa A", "Pessoa B"));
            return participant;
        }).toList();
        events.update(event.id(), organizerId, new EventService.EventData(event.title(), event.description(), event.venue(), event.address(), event.city(), event.state(), event.timezone(), event.startsAt(), event.endsAt(), 8, event.coverUrl(), event.coverPublicId()));
        assertThat(tickets.mine(queued.get(0))).hasSize(2);
        assertThat(tickets.mine(queued.get(1))).hasSize(2);
        assertThat(tickets.mine(queued.get(2))).isEmpty();
        assertThat(registrations.myWaitlist(queued.get(2))).hasSize(1);
    }

    private EventService.EventView createEvent(int capacity, List<EventService.TicketTypeData> types) {
        var start = Instant.now().plusSeconds(172800);
        var created = events.create(organizerId, new EventService.EventData("Teste " + UUID.randomUUID(), "Descrição suficientemente longa para testar ingressos em grupo e lista de espera.", "Arena", "Rua de Teste, 1", "São Paulo", "SP", "America/Sao_Paulo", start, start.plusSeconds(7200), capacity, null, null, types));
        return events.publish(created.id(), organizerId);
    }

    @Test void onlyOneOfTwoSimultaneousCheckInsIsAccepted() throws Exception {
        var participant = users.create("Participante", "checkin-" + suffix + "@test.dev", "hash", false).getId();
        var ticket = registrations.register(eventId, participant).ticket();
        var barrier = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        var one = pool.submit(() -> attemptCheckIn(ticket.qrPayload(), barrier));
        var two = pool.submit(() -> attemptCheckIn(ticket.qrPayload(), barrier));

        assertThat(java.util.List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
        pool.shutdownNow();
    }

    private boolean attempt(UUID participant, CyclicBarrier barrier) {
        try { barrier.await(5, TimeUnit.SECONDS); registrations.register(eventId, participant); return true; }
        catch (ApiException e) { return false; } catch (Exception e) { throw new RuntimeException(e); }
    }


    private boolean attemptCheckIn(String qrToken, CyclicBarrier barrier) {
        try { barrier.await(5, TimeUnit.SECONDS); checkIns.checkIn(eventId, qrToken, organizerId); return true; }
        catch (ApiException e) { return e.status() != org.springframework.http.HttpStatus.CONFLICT ? fail(e) : false; }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private boolean fail(ApiException exception) { throw exception; }
}

