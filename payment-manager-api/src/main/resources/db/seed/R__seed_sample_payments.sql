-- Sample payments from the challenge (examples/sample-transactions.json), one per final status.
-- Only loaded by the local profile; ON CONFLICT keeps reruns from touching existing rows.
INSERT INTO payments (id, merchant_id, amount, currency, status, created_at)
VALUES ('pay_001', 'COMMERCE_001', 15000.00, 'ARS', 'APPROVED', '2026-10-08T15:00:00Z'),
       ('pay_002', 'COMMERCE_001', 32500.00, 'ARS', 'REJECTED', '2026-10-08T15:05:00Z'),
       ('pay_003', 'COMMERCE_002', 8200.00, 'ARS', 'REFUNDED', '2026-10-08T15:10:00Z')
ON CONFLICT (id) DO NOTHING;
