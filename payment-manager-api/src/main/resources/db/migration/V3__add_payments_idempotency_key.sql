ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE payments ADD COLUMN request_hash VARCHAR(64);
ALTER TABLE payments ADD CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key);
ALTER TABLE payments ADD CONSTRAINT ck_payments_idempotency_key_has_request_hash
    CHECK ((idempotency_key IS NULL) = (request_hash IS NULL));
