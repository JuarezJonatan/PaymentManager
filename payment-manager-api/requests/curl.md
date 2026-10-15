# Ejemplos con curl

```bash
BASE_URL=http://localhost:8080        # con el stack de Docker: http://localhost:3000/api
```

## Crear un pago

```bash
# Aprobado (con "REJECTED" se crea rechazado)
curl -X POST "$BASE_URL/payments" \
  -H "Content-Type: application/json" \
  -d '{"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "APPROVED"}'

# Datos inválidos → 400 con el error de cada campo
curl -X POST "$BASE_URL/payments" \
  -H "Content-Type: application/json" \
  -d '{"merchantId": "", "amount": -10, "currency": "ARS", "simulationResult": "APPROVED"}'
```

## Consultar pagos

```bash
curl "$BASE_URL/payments?page=0&size=20"
curl "$BASE_URL/payments?status=APPROVED"
curl "$BASE_URL/payments/pay_001"                # pago de ejemplo cargado en local
curl "$BASE_URL/payments/pay_unknown"          # 404
```

## Devolver un pago

```bash
# Crea un pago aprobado y guarda su id (requiere jq)
ID=$(curl -s -X POST "$BASE_URL/payments" -H "Content-Type: application/json" \
  -d '{"merchantId": "COMMERCE_001", "amount": 15000, "currency": "ARS", "simulationResult": "APPROVED"}' | jq -r .id)

curl -X POST "$BASE_URL/payments/$ID/refund"       # 200: pasa a REFUNDED
curl -X POST "$BASE_URL/payments/$ID/refund"       # 409: ya fue devuelto
curl -X POST "$BASE_URL/payments/pay_002/refund"   # 409: pago de ejemplo REJECTED
```

## Idempotencia

```bash
KEY=$(uuidgen)
BODY='{"merchantId": "COMMERCE_002", "amount": 8200, "currency": "ARS", "simulationResult": "APPROVED"}'

# La primera vez crea el pago; el reintento con la misma key devuelve el mismo pago
curl -X POST "$BASE_URL/payments" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" -d "$BODY"
curl -X POST "$BASE_URL/payments" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" -d "$BODY"

# La misma key con otros datos → 422
curl -X POST "$BASE_URL/payments" -H "Content-Type: application/json" -H "Idempotency-Key: $KEY" \
  -d '{"merchantId": "COMMERCE_002", "amount": 9000, "currency": "ARS", "simulationResult": "APPROVED"}'
```
