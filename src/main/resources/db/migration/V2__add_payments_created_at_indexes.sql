-- Supports the default listing order (newest first) with and without the status filter.
CREATE INDEX idx_payments_created_at ON payments (created_at DESC, id DESC);

DROP INDEX idx_payments_status;
CREATE INDEX idx_payments_status_created_at ON payments (status, created_at DESC, id DESC);
