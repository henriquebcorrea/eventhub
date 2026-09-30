package com.eventhub.users;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 255) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<UserRole> roles = new HashSet<>();
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String name, String email, String passwordHash, boolean organizer) {
        this.id = UUID.randomUUID();
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.roles.add(UserRole.USER);
        if (organizer) this.roles.add(UserRole.ORGANIZER);
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Set<UserRole> getRoles() { return Set.copyOf(roles); }
    public Instant getCreatedAt() { return createdAt; }
    public void addRole(UserRole role) { roles.add(role); }
}

