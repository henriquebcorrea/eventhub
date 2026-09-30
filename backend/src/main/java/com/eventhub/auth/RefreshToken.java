package com.eventhub.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshToken {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "replaced_by") private UUID replacedBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected RefreshToken() {}
    RefreshToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }
    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    boolean isRevoked() { return revokedAt != null; }
    boolean isExpired(Instant now) { return !expiresAt.isAfter(now); }
    UUID getReplacedBy() { return replacedBy; }
    void revoke(UUID replacement) { this.revokedAt = Instant.now(); this.replacedBy = replacement; }
    void revoke() { this.revokedAt = Instant.now(); }
}

