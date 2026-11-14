ALTER TABLE client ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Accounts that existed before verification was introduced count as verified
UPDATE client SET email_verified_at = now();

-- Single-use links sent by email; only a SHA-256 hash of the token is stored
CREATE TABLE email_verification_token (
  id BIGSERIAL PRIMARY KEY,
  client_id BIGINT NOT NULL REFERENCES client(id),
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX email_verification_token_client_idx ON email_verification_token(client_id);
