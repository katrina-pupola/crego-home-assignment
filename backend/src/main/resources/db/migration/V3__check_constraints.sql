-- Allowed enum values, matching the Java enums
ALTER TABLE payment_method
  ADD CONSTRAINT payment_method_type_check
    CHECK (type IN ('CARD', 'BANK_ACCOUNT')),
  ADD CONSTRAINT payment_method_status_check
    CHECK (status IN ('ACTIVE', 'REPLACED')),
  ADD CONSTRAINT payment_method_card_brand_check
    CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'OTHER'));

ALTER TABLE change_request
  ADD CONSTRAINT change_request_new_type_check
    CHECK (new_type IN ('CARD', 'BANK_ACCOUNT')),
  ADD CONSTRAINT change_request_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
  ADD CONSTRAINT change_request_new_card_brand_check
    CHECK (new_card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'OTHER'));

-- A card has only card fields, a bank account has only an IBAN
ALTER TABLE payment_method
  ADD CONSTRAINT payment_method_fields_match_type CHECK (
    (type = 'CARD'
      AND card_last4 IS NOT NULL AND card_brand IS NOT NULL
      AND card_holder IS NOT NULL AND card_expiry IS NOT NULL
      AND iban IS NULL)
    OR
    (type = 'BANK_ACCOUNT'
      AND iban IS NOT NULL
      AND card_last4 IS NULL AND card_brand IS NULL
      AND card_holder IS NULL AND card_expiry IS NULL)
  );

ALTER TABLE change_request
  ADD CONSTRAINT change_request_fields_match_type CHECK (
    (new_type = 'CARD'
      AND new_card_last4 IS NOT NULL AND new_card_brand IS NOT NULL
      AND new_card_holder IS NOT NULL AND new_card_expiry IS NOT NULL
      AND new_iban IS NULL)
    OR
    (new_type = 'BANK_ACCOUNT'
      AND new_iban IS NOT NULL
      AND new_card_last4 IS NULL AND new_card_brand IS NULL
      AND new_card_holder IS NULL AND new_card_expiry IS NULL)
  );
