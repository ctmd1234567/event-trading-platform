# API Contract

## Conventions

- Base path: `/api/v1`
- Media type: `application/json`
- Authentication: `Authorization: Bearer <token>`
- Request trace: `X-Request-Id`; the server creates one when absent.
- Command idempotency: `Idempotency-Key`
- Timestamps: ISO-8601 UTC, for example `2026-09-05T08:00:00Z`
- Money: integer smallest unit and ISO currency, for example `{"amount":4750,"currency":"CNY"}`

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "traceId": "01K4D3..."
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "code": "ORDER_INVALID_STATE",
    "message": "The order cannot be canceled in its current state."
  },
  "traceId": "01K4D3..."
}
```

Messages are diagnostic English text and may change. Clients branch only on `code`.

## HTTP Status Policy

| Status | Meaning |
|---|---|
| `200` | Successful query or idempotent replay |
| `201` | Resource created |
| `202` | Asynchronous command durably accepted |
| `400` | Invalid syntax or field validation |
| `401` | Missing or invalid authentication |
| `403` | Actor lacks permission |
| `404` | Resource absent or not visible to the actor |
| `409` | Business conflict, invalid state, or reused idempotency key |
| `422` | Valid request violates a domain rule |
| `429` | Rate limit or overload rejection |
| `503` | Required dependency unavailable and command not accepted |

## Idempotency

These commands require `Idempotency-Key`:

- Create order
- Create payment attempt
- Cancel order
- Request refund
- Operator retry or redrive

The server stores actor, endpoint, key, normalized request hash, response status, and resource ID.

- Same actor, endpoint, key, and payload: return the original result.
- Same actor, endpoint, and key with a different payload: return `409 IDEMPOTENCY_KEY_REUSED`.
- A timed-out client retries with the same key.
- Keys are opaque ASCII strings between 16 and 128 characters.

Provider callbacks use provider transaction and callback-event identifiers instead of this header.

## Public Catalog

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/events` | List published events |
| `GET` | `/api/v1/events/{eventId}` | Get event details and sessions |
| `GET` | `/api/v1/sessions/{sessionId}/ticket-tiers` | List ticket tiers and availability |

Availability is informational; a query never reserves inventory.

## Administrative Event API

All endpoints require `ADMIN` and create audit records.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/admin/events` | Create a draft event |
| `PUT` | `/api/v1/admin/events/{eventId}` | Update a draft event |
| `POST` | `/api/v1/admin/events/{eventId}/sessions` | Add a session |
| `POST` | `/api/v1/admin/sessions/{sessionId}/ticket-tiers` | Add a ticket tier and inventory |
| `POST` | `/api/v1/admin/events/{eventId}/publish` | Publish an event |
| `POST` | `/api/v1/admin/events/{eventId}/take-off-sale` | Stop new sales |
| `POST` | `/api/v1/admin/events/{eventId}/resume-sale` | Resume sales |

State changes are explicit commands, not unrestricted status updates.

## Order API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/orders` | Reserve inventory and create an order |
| `GET` | `/api/v1/orders/{orderId}` | Get an owned order |
| `GET` | `/api/v1/orders` | List the current user's orders |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Cancel an unpaid order |

Create-order request:

```json
{
  "ticketTierId": 10001,
  "quantity": 1
}
```

The response contains the immutable price snapshot, state, payment deadline, and order number. It never promises payment success.

## Payment and Refund API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/orders/{orderId}/payments` | Create a payment attempt |
| `POST` | `/api/v1/payment-callbacks/{provider}` | Receive a signed callback |
| `POST` | `/api/v1/orders/{orderId}/refunds` | Request an allowed refund |
| `GET` | `/api/v1/refunds/{refundId}` | Get refund status |

Provider callbacks use provider-specific signatures and never use user bearer tokens.

## Notification API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/notifications/stream` | Open an authenticated SSE stream |
| `GET` | `/api/v1/notifications` | Replay events after a cursor |

SSE events include `id`, `type`, `occurredAt`, `aggregateId`, and versioned payload. Reconnecting clients send `Last-Event-ID`; duplicate IDs are safe.

## Operations API

All endpoints require `ADMIN` and append an audit record.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/admin/operations/outbox` | Query pending or failed events |
| `POST` | `/api/v1/admin/operations/outbox/{eventId}/retry` | Retry an event |
| `GET` | `/api/v1/admin/operations/dead-letters` | Query dead-letter metadata |
| `POST` | `/api/v1/admin/operations/dead-letters/{messageId}/redrive` | Redrive a message |
| `POST` | `/api/v1/admin/operations/reconciliation-runs` | Start reconciliation |
| `GET` | `/api/v1/admin/operations/reconciliation-runs/{runId}` | Read results |

## Stable Error Codes

Common:

- `VALIDATION_FAILED`
- `AUTHENTICATION_REQUIRED`
- `ACCESS_DENIED`
- `RESOURCE_NOT_FOUND`
- `RATE_LIMITED`
- `DEPENDENCY_UNAVAILABLE`
- `IDEMPOTENCY_KEY_REQUIRED`
- `IDEMPOTENCY_KEY_REUSED`

Catalog:

- `EVENT_INVALID_STATE`
- `EVENT_NOT_SELLABLE`
- `SESSION_INVALID_TIME_RANGE`
- `SESSION_NOT_ON_SALE`
- `TICKET_TIER_NOT_ON_SALE`

Orders and inventory:

- `INVENTORY_SOLD_OUT`
- `PURCHASE_LIMIT_EXCEEDED`
- `ORDER_INVALID_STATE`
- `ORDER_PAYMENT_EXPIRED`
- `ORDER_ALREADY_PAID`

Payment and refund:

- `PAYMENT_AMOUNT_MISMATCH`
- `PAYMENT_SIGNATURE_INVALID`
- `PAYMENT_CALLBACK_CONFLICT`
- `REFUND_NOT_ALLOWED`
- `REFUND_AMOUNT_EXCEEDED`
- `REFUND_INVALID_STATE`

## Authorization and Ownership

- Users access only their own orders, payments, refunds, and notifications.
- Another user's resource returns `404` to avoid disclosing its existence.
- Administrators use dedicated `/admin` endpoints.
- Every administrative mutation records actor ID, target ID, trace ID, result, and before/after summary.

## Versioning

Breaking HTTP changes require a new API version. Additive response fields and new error codes are backward compatible. Domain events carry an independent `eventVersion`; unsupported major versions enter an observable failure path.
