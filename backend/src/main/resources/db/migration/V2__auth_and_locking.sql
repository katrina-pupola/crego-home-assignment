CREATE TABLE bo_user (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE payment_method ADD COLUMN card_brand VARCHAR(20);

ALTER TABLE change_request ADD COLUMN new_card_brand VARCHAR(20);
ALTER TABLE change_request ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Reviewer becomes a link to a staff account instead of free text
ALTER TABLE change_request DROP COLUMN reviewer;
ALTER TABLE change_request ADD COLUMN reviewer_id BIGINT REFERENCES bo_user(id);

CREATE UNIQUE INDEX one_active_method_per_client
  ON payment_method(client_id) WHERE status = 'ACTIVE';

-- Fictional seed accounts (passwords: admin123, demo123)
INSERT INTO bo_user (email, full_name, password_hash)
VALUES ('admin@crego.test', 'Crego Admin', '$2a$10$ASBK/qpvxPpU9YT10kRiSu9tuT5.IxWcnSlNWCjiNWYmlfSCTrKHO');

INSERT INTO client (email, full_name, password_hash)
VALUES ('demo@crego.test', 'Anna Ozola', '$2a$10$TDOnXnLM/GItcq7s3IhmTe3u4Ox.TrVQZR0Nsk/HYUuBCcaxS0PHG');
