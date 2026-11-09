CREATE TABLE client (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE payment_method (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT NOT NULL REFERENCES client(id),
  type VARCHAR(20) NOT NULL,          -- CARD / BANK_ACCOUNT
  card_last4 VARCHAR(4),
  card_holder VARCHAR(255),
  card_expiry VARCHAR(5),
  iban VARCHAR(34),
  status VARCHAR(20) NOT NULL,        -- ACTIVE / REPLACED
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE change_request (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT NOT NULL REFERENCES client(id),
  current_method_id BIGINT REFERENCES payment_method(id),
  new_type VARCHAR(20) NOT NULL,
  new_card_last4 VARCHAR(4),
  new_card_holder VARCHAR(255),
  new_card_expiry VARCHAR(5),
  new_iban VARCHAR(34),
  status VARCHAR(20) NOT NULL,        -- PENDING / APPROVED / REJECTED
  reviewer VARCHAR(255),
  review_comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  reviewed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX one_pending_per_client
  ON change_request(client_id) WHERE status = 'PENDING';