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

CREATE INDEX idx_payments_status ON payments (status);
