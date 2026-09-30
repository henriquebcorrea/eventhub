package com.eventhub.media;

import com.eventhub.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final String cloudName, apiKey, apiSecret;
    public MediaController(@Value("${app.cloudinary.cloud-name:}") String cloudName,
                           @Value("${app.cloudinary.api-key:}") String apiKey,
                           @Value("${app.cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName; this.apiKey = apiKey; this.apiSecret = apiSecret;
    }
    @PostMapping("/cloudinary-signature")
    @PreAuthorize("hasRole('ORGANIZER')")
    Signature signature() {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MEDIA_NOT_CONFIGURED", "O upload de imagens ainda não foi configurado.");
        var timestamp = Instant.now().getEpochSecond();
        var folder = "eventhub/covers";
        var allowedFormats = "jpg,jpeg,png,webp";
        try {
            var value = "allowed_formats=" + allowedFormats + "&folder=" + folder + "&timestamp=" + timestamp + apiSecret;
            var signature = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8)));
            return new Signature(cloudName, apiKey, timestamp, folder, allowedFormats, signature);
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    record Signature(String cloudName, String apiKey, long timestamp, String folder, String allowedFormats, String signature) {}
}

