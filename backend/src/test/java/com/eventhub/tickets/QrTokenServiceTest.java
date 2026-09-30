package com.eventhub.tickets;

import com.eventhub.shared.ApiException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrTokenServiceTest {
    private final QrTokenService tokens = new QrTokenService("a-secure-test-secret-with-more-than-32-characters");
    @Test void signsAndValidatesTicketWithoutPersonalData() {
        var id = UUID.randomUUID(); var token = tokens.issue(id);
        assertThat(tokens.verify(token)).isEqualTo(id);
        assertThat(token).doesNotContain("@");
    }
    @Test void rejectsTamperedToken() {
        var token = tokens.issue(UUID.randomUUID());
        assertThatThrownBy(() -> tokens.verify(token + "x")).isInstanceOf(ApiException.class);
    }
}

