package com.eventhub.media;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MediaControllerTest {
    @Test
    void signatureRestrictsTheAllowedImageFormats() throws Exception {
        var controller = new MediaController("demo", "api-key", "api-secret");

        var signed = controller.signature();

        var payload = "allowed_formats=" + signed.allowedFormats() + "&folder=" + signed.folder()
                + "&timestamp=" + signed.timestamp() + "api-secret";
        var expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        assertThat(signed.allowedFormats()).isEqualTo("jpg,jpeg,png,webp");
        assertThat(signed.signature()).isEqualTo(expected);
    }
}
