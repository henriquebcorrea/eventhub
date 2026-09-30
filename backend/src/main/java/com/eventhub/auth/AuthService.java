package com.eventhub.auth;

import com.eventhub.shared.ApiException;
import com.eventhub.users.UserAccount;
import com.eventhub.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserService users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final SecureRandom random = new SecureRandom();
    private final String issuer;
    private final Duration accessDuration;
    private final Duration refreshDuration;

    AuthService(UserService users, RefreshTokenRepository refreshTokens, PasswordEncoder passwords, JwtEncoder jwtEncoder,
                @Value("${app.jwt.issuer}") String issuer,
                @Value("${app.jwt.access-minutes:15}") long accessMinutes,
                @Value("${app.jwt.refresh-days:30}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessDuration = Duration.ofMinutes(accessMinutes);
        this.refreshDuration = Duration.ofDays(refreshDays);
    }

    @Transactional
    public AuthTokens register(String name, String email, String password, boolean organizer) {
        var user = users.create(name, email, passwords.encode(password), organizer);
        return issue(user);
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        var user = users.requireByEmail(email);
        if (!passwords.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
        }
        return issue(user);
    }

    @Transactional
    public AuthTokens refresh(String rawToken) {
        var current = refreshTokens.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked() && !token.isExpired(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Sessão inválida ou expirada."));
        var user = users.require(current.getUserId());
        var nextRaw = randomToken();
        var next = refreshTokens.save(new RefreshToken(user.getId(), hash(nextRaw), Instant.now().plus(refreshDuration)));
        current.revoke(next.getId());
        return new AuthTokens(accessToken(user), nextRaw, accessDuration.toSeconds(), user.getId(), user.getName(), user.getRoles().stream().map(Enum::name).toList());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken))
                .filter(token -> !token.isRevoked() && !token.isExpired(Instant.now()))
                .ifPresent(RefreshToken::revoke);
    }

    private AuthTokens issue(UserAccount user) {
        var rawRefresh = randomToken();
        refreshTokens.save(new RefreshToken(user.getId(), hash(rawRefresh), Instant.now().plus(refreshDuration)));
        return new AuthTokens(accessToken(user), rawRefresh, accessDuration.toSeconds(), user.getId(), user.getName(), user.getRoles().stream().map(Enum::name).toList());
    }

    private String accessToken(UserAccount user) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer).issuedAt(now).expiresAt(now.plus(accessDuration))
                .subject(user.getId().toString()).audience(java.util.List.of("eventhub-api"))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .id(UUID.randomUUID().toString()).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    private String randomToken() {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record AuthTokens(String accessToken, String refreshToken, long expiresIn, UUID userId, String name, java.util.List<String> roles) {}
}

