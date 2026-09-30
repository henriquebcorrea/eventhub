package com.eventhub.bootstrap;

import com.eventhub.events.EventService;
import com.eventhub.users.UserService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
class DemoDataInitializer implements ApplicationRunner {
    private final UserService users;
    private final EventService events;
    private final PasswordEncoder passwords;
    private final boolean enabled;

    DemoDataInitializer(UserService users, EventService events, PasswordEncoder passwords,
                        @Value("${app.demo-seed:true}") boolean enabled) {
        this.users = users;
        this.events = events;
        this.passwords = passwords;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        try {
            var organizer = users.create("Marina Costa", "organizador@eventhub.dev", passwords.encode("Demo@123"), true);
            users.create("Lucas Almeida", "participante@eventhub.dev", passwords.encode("Demo@123"), false);
            var zone = ZoneId.of("America/Sao_Paulo");
            var starts = ZonedDateTime.now(zone).plusDays(48).withHour(19).withMinute(0).withSecond(0).withNano(0);
            var event = events.create(organizer.getId(), new EventService.EventData(
                    "Festival de Rock 2026",
                    "Uma noite para celebrar grandes bandas independentes, novos sons e a energia de quem vive música ao vivo.",
                    "Arena Beira-Mar",
                    "Av. Jornalista Rubens de Arruda Ramos, 1200",
                    "Florianópolis", "SC", zone.getId(), starts.toInstant(), starts.plusHours(6).toInstant(), 500,
                    "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1600&q=85", null));
            events.publish(event.id(), organizer.getId());
        } catch (Exception ignored) {
            LoggerFactory.getLogger(DemoDataInitializer.class).debug("Dados de demonstração já existem ou não puderam ser criados.");
        }
    }
}
