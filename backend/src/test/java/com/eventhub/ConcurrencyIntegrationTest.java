package com.eventhub;

import com.eventhub.checkin.CheckInService;
import com.eventhub.events.EventService;
import com.eventhub.registrations.RegistrationService;
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
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.demo-seed=false", "app.jwt.allow-ephemeral=true", "app.outbox-delay-ms=3600000", "app.qr-secret=test-secret-with-at-least-thirty-two-characters"})
@Testcontainers(disabledWithoutDocker = true)
class ConcurrencyIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl); registry.add("spring.datasource.username", postgres::getUsername); registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired UserService users; @Autowired EventService events; @Autowired RegistrationService registrations; @Autowired CheckInService checkIns;
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

