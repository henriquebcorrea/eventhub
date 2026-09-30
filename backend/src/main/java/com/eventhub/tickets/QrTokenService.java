package com.eventhub.tickets;

import com.eventhub.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Component
class QrTokenService {
    private final byte[] secret;
    QrTokenService(@Value("${app.qr-secret}") String secret) {
        if (secret.length() < 32) throw new IllegalStateException("app.qr-secret deve ter pelo menos 32 caracteres");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String issue(UUID ticketId) {
        var payload = "v1." + ticketId;
        return payload + "." + sign(payload);
    }

    UUID verify(String token) {
        try {
            var parts = token.split("\\.");
            if (parts.length != 3 || !"v1".equals(parts[0])) throw invalid();
            var payload = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII), parts[2].getBytes(StandardCharsets.US_ASCII))) throw invalid();
            return UUID.fromString(parts[1]);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String sign(String value) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private ApiException invalid() { return new ApiException(HttpStatus.NOT_FOUND, "INVALID_TICKET", "Ingresso inválido."); }
}

