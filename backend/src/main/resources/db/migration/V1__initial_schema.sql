CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    organizer_id UUID NOT NULL REFERENCES users(id),
    slug VARCHAR(180) NOT NULL UNIQUE,
    title VARCHAR(140) NOT NULL,
    description TEXT NOT NULL,
    venue VARCHAR(160) NOT NULL,
    address VARCHAR(220) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state CHAR(2) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    cover_url VARCHAR(600),
    cover_public_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_catalog ON events(status, starts_at);
CREATE INDEX idx_events_organizer ON events(organizer_id, created_at DESC);

CREATE TABLE ticket_types (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    confirmed_count INTEGER NOT NULL DEFAULT 0 CHECK (confirmed_count >= 0 AND confirmed_count <= capacity),
    price_cents INTEGER NOT NULL DEFAULT 0 CHECK (price_cents = 0),
    CONSTRAINT uk_ticket_type_event_name UNIQUE(event_id, name)
);

CREATE TABLE registrations (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    participant_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT uk_registration_event_participant UNIQUE(event_id, participant_id)
);
CREATE INDEX idx_registrations_event ON registrations(event_id, created_at DESC);
CREATE INDEX idx_registrations_participant ON registrations(participant_id, created_at DESC);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    registration_id UUID NOT NULL REFERENCES registrations(id),
    event_id UUID NOT NULL REFERENCES events(id),
    participant_id UUID NOT NULL REFERENCES users(id),
    public_code VARCHAR(32) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ticket_registration UNIQUE(registration_id)
);
CREATE INDEX idx_tickets_participant ON tickets(participant_id, issued_at DESC);

CREATE TABLE check_ins (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    event_id UUID NOT NULL REFERENCES events(id),
    scanned_by UUID NOT NULL REFERENCES users(id),
    checked_in_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_check_in_ticket UNIQUE(ticket_id)
);
CREATE INDEX idx_check_ins_event ON check_ins(event_id, checked_in_at DESC);

CREATE TABLE notification_outbox (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(220) NOT NULL,
    html_body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_outbox_pending ON notification_outbox(status, next_attempt_at);

