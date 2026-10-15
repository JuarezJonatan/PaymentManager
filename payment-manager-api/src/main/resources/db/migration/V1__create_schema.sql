CREATE TABLE payments (
    id          VARCHAR(40)              NOT NULL,
    merchant_id VARCHAR(100)             NOT NULL,
    amount      NUMERIC(19, 2)           NOT NULL,
    currency    VARCHAR(3)               NOT NULL,
    status      VARCHAR(20)              NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_status CHECK (status IN ('APPROVED', 'REJECTED', 'REFUNDED'))
);

-- Support the default listing order (newest first) with and without the status filter.
CREATE INDEX idx_payments_created_at ON payments (created_at DESC, id DESC);
CREATE INDEX idx_payments_status_created_at ON payments (status, created_at DESC, id DESC);

CREATE TABLE payment_idempotency_keys (
    idempotency_key VARCHAR(100)             NOT NULL,
    request_hash    VARCHAR(64)              NOT NULL,
    payment_id      VARCHAR(40)              NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_payment_idempotency_keys PRIMARY KEY (idempotency_key),
    CONSTRAINT uk_payment_idempotency_keys_payment UNIQUE (payment_id),
    CONSTRAINT fk_payment_idempotency_keys_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);
