package com.eventhub.auth;

import com.eventhub.users.UserAccount;
import com.eventhub.users.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void refreshTokenIsRotatedAndThePreviousTokenIsRevoked() {
        var users = mock(UserService.class);
        var repository = mock(RefreshTokenRepository.class);
        var passwords = mock(PasswordEncoder.class);
        var encoder = mock(JwtEncoder.class);
        var user = new UserAccount("Pessoa", "pessoa@test.dev", "hash", false);
        var current = new RefreshToken(user.getId(), "hash-atual", Instant.now().plusSeconds(3600));

        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(current));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.require(user.getId())).thenReturn(user);
        when(encoder.encode(any())).thenReturn(Jwt.withTokenValue("access-token")
                .header("alg", "RS256").subject(user.getId().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build());
        var service = new AuthService(users, repository, passwords, encoder, "https://api.eventhub.test", 15, 30);

        var result = service.refresh("refresh-token-atual");

        var replacement = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(replacement.capture());
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo("refresh-token-atual");
        assertThat(current.isRevoked()).isTrue();
        assertThat(current.getReplacedBy()).isEqualTo(replacement.getValue().getId());
    }

    @Test
    void logoutRevokesTheCurrentRefreshToken() {
        var users = mock(UserService.class);
        var repository = mock(RefreshTokenRepository.class);
        var current = new RefreshToken(java.util.UUID.randomUUID(), "hash-atual", Instant.now().plusSeconds(3600));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(current));
        var service = new AuthService(users, repository, mock(PasswordEncoder.class), mock(JwtEncoder.class), "issuer", 15, 30);

        service.logout("refresh-token-atual");

        assertThat(current.isRevoked()).isTrue();
    }
}
