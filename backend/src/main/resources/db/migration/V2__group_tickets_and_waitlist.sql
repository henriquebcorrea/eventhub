ALTER TABLE registrations DROP CONSTRAINT uk_registration_event_participant;
ALTER TABLE tickets DROP CONSTRAINT uk_ticket_registration;
ALTER TABLE tickets ADD COLUMN ticket_type_id UUID;
UPDATE tickets t SET ticket_type_id = r.ticket_type_id FROM registrations r WHERE t.registration_id = r.id;
ALTER TABLE tickets ALTER COLUMN ticket_type_id SET NOT NULL;
ALTER TABLE tickets ADD CONSTRAINT fk_tickets_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id);
ALTER TABLE tickets ADD COLUMN attendee_name VARCHAR(120);
UPDATE tickets t SET attendee_name = u.name FROM users u WHERE t.participant_id = u.id;
ALTER TABLE tickets ALTER COLUMN attendee_name SET NOT NULL;
CREATE INDEX idx_tickets_type_account ON tickets(ticket_type_id, participant_id, status);

CREATE TABLE waitlist_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_type_id UUID NOT NULL REFERENCES ticket_types(id),
    event_id UUID NOT NULL REFERENCES events(id),
    participant_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('WAITING', 'PROMOTED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_waitlist_fifo ON waitlist_requests(ticket_type_id, status, id);
CREATE INDEX idx_waitlist_owner ON waitlist_requests(participant_id, status);
CREATE TABLE waitlist_attendees (
    request_id BIGINT NOT NULL REFERENCES waitlist_requests(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position BETWEEN 0 AND 3),
    attendee_name VARCHAR(120) NOT NULL,
    PRIMARY KEY (request_id, position)
);
