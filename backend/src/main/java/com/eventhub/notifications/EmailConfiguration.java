package com.eventhub.notifications;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Configuration
class EmailConfiguration {
    @Bean
    EmailSender emailSender(@Value("${app.resend.api-key:}") String apiKey, @Value("${app.resend.from}") String from) {
        if (apiKey.isBlank()) {
            var log = LoggerFactory.getLogger("EventHubEmailSandbox");
            return (idempotencyKey, recipient, subject, html) -> log.info("E-mail sandbox id={} (envio externo desativado)", idempotencyKey);
        }
        var client = RestClient.builder().baseUrl("https://api.resend.com").defaultHeader("Authorization", "Bearer " + apiKey).build();
        return (idempotencyKey, recipient, subject, html) -> client.post().uri("/emails")
                .header("Idempotency-Key", "eventhub-outbox/" + idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("from", from, "to", List.of(recipient), "subject", subject, "html", html)).retrieve().toBodilessEntity();
    }
}

