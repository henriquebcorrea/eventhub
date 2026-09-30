package com.eventhub.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationOutboxRepository outbox;
    private final EmailSender sender;
    private final String frontendUrl;
    NotificationService(NotificationOutboxRepository outbox, EmailSender sender, @Value("${app.frontend-url}") String frontendUrl) { this.outbox = outbox; this.sender = sender; this.frontendUrl = frontendUrl; }

    @Transactional
    public void enqueueTicket(String recipient, String participantName, String eventTitle, UUID ticketId, String publicCode) {
        var url = frontendUrl + "/ingressos/" + ticketId;
        var html = """
            <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#111827">
              <h1 style="font-size:24px">Seu ingresso está confirmado</h1>
              <p>Olá, %s. Sua inscrição em <strong>%s</strong> foi confirmada.</p>
              <p>Código do ingresso: <strong>%s</strong></p>
              <p><a href="%s" style="display:inline-block;background:#ff5d4a;color:white;padding:12px 18px;border-radius:8px;text-decoration:none">Abrir ingresso</a></p>
            </div>
            """.formatted(escape(participantName), escape(eventTitle), publicCode, url);
        outbox.save(new NotificationOutbox(recipient, "Seu ingresso para " + eventTitle, html));
    }

    @Scheduled(fixedDelayString = "${app.outbox-delay-ms:10000}")
    @Transactional
    public void deliverPending() {
        var messages = outbox.findByStatusAndNextAttemptAtBeforeOrderByCreatedAt(
                NotificationOutbox.Status.PENDING, Instant.now(), PageRequest.of(0, 10));
        messages.forEach(message -> {
            try { sender.send(message.getId(), message.getRecipient(), message.getSubject(), message.getHtmlBody()); message.sent(); }
            catch (Exception exception) { message.failed(exception); }
        });
    }
    private String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}

