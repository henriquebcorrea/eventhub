package com.eventhub;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class MigrationV1IntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test void preservesExistingTicketAndAllowsGroupTicketsAfterMigration() throws Exception {
        var dataSource = postgres.getJdbcUrl();
        Flyway.configure().dataSource(dataSource, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").target(MigrationVersion.fromVersion("1")).load().migrate();
        var organizer = UUID.randomUUID(); var participant = UUID.randomUUID(); var event = UUID.randomUUID();
        var type = UUID.randomUUID(); var registration = UUID.randomUUID(); var ticket = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(dataSource, postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users(id,name,email,password_hash) VALUES ('" + organizer + "','Organizador','org@test.dev','hash'),('" + participant + "','Participante','user@test.dev','hash')");
            statement.execute("INSERT INTO events(id,organizer_id,slug,title,description,venue,address,city,state,timezone,starts_at,ends_at,status) VALUES ('" + event + "','" + organizer + "','antigo','Evento antigo','Descrição antiga','Arena','Rua 1','São Paulo','SP','America/Sao_Paulo',now() + interval '30 days',now() + interval '31 days','PUBLISHED')");
            statement.execute("INSERT INTO ticket_types(id,event_id,name,capacity,confirmed_count,price_cents) VALUES ('" + type + "','" + event + "','Ingresso geral',10,1,0)");
            statement.execute("INSERT INTO registrations(id,event_id,ticket_type_id,participant_id,status) VALUES ('" + registration + "','" + event + "','" + type + "','" + participant + "','CONFIRMED')");
            statement.execute("INSERT INTO tickets(id,registration_id,event_id,participant_id,public_code,status) VALUES ('" + ticket + "','" + registration + "','" + event + "','" + participant + "','EVT-OLD-123','ACTIVE')");
        }
        Flyway.configure().dataSource(dataSource, postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
        try (var connection = DriverManager.getConnection(dataSource, postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT public_code, attendee_name, ticket_type_id FROM tickets WHERE id = '" + ticket + "'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("EVT-OLD-123");
                assertThat(result.getString(2)).isEqualTo("Participante");
                assertThat(result.getObject(3, UUID.class)).isEqualTo(type);
            }
            statement.execute("INSERT INTO tickets(id,registration_id,ticket_type_id,event_id,participant_id,attendee_name,public_code,status) VALUES ('" + UUID.randomUUID() + "','" + registration + "','" + type + "','" + event + "','" + participant + "','Acompanhante','EVT-NEW-123','ACTIVE')");
            statement.execute("INSERT INTO registrations(id,event_id,ticket_type_id,participant_id,status) VALUES ('" + UUID.randomUUID() + "','" + event + "','" + type + "','" + participant + "','CONFIRMED')");
        }
    }
}
