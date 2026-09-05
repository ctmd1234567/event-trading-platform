# Domain Model

## Purpose

This document defines the target business model for the event trading platform. It is the source of truth for module ownership, aggregate boundaries, terminology, and business invariants. Database migrations and application code must follow this model unless an Architecture Decision Record explicitly changes it.

## Architecture Style

The application remains a modular monolith. Modules communicate through application services and domain events while sharing one MySQL database. Redis is an acceleration and traffic-control layer, not a transactional source of truth. RabbitMQ provides asynchronous delivery, and the Transactional Outbox guarantees that business changes and outgoing events are committed together.

Each business module uses the following internal structure when complexity requires it:

```text
module/
├─ api/             HTTP controllers and request/response models
├─ application/     use cases and transaction boundaries
├─ domain/          aggregates, value objects, policies, and repository ports
└─ infrastructure/  MyBatis repositories, external clients, and messaging adapters
```

Do not create empty layers or interfaces only to satisfy this layout.

## Bounded Contexts

| Context | Owns | Responsibilities |
|---|---|---|
| Identity | User, Role, AccessToken | Authentication, authorization, token revocation |
| Event Catalog | Event, EventSession, TicketTier | Event authoring, publication, sales windows, public catalog |
| Inventory | Inventory, InventoryReservation | Atomic reservation, confirmation, release, reconciliation |
| Ordering | Order, OrderItem | Idempotent order creation, pricing snapshot, order lifecycle |
| Payment | Payment, Refund | Payment attempts, signed callbacks, cancellation, refunds |
| Messaging | OutboxEvent, InboxMessage | Durable event publication and consumer deduplication |
| Notification | NotificationEvent, SubscriptionCursor | SSE delivery, reconnect, replay, duplicate suppression |
| Operations | AuditLog, ReconciliationRun | Failure inspection, retry, redrive, reconciliation, operator audit |

## Core Aggregates

### Event

An event is the administrative root for one sellable event.

Key fields: `id`, `title`, `description`, `venue`, `status`, `createdBy`, `createdAt`, `updatedAt`.

States: `DRAFT`, `PUBLISHED`, `OFF_SALE`, `ENDED`.

Rules:

- Only draft events may change essential descriptive fields freely.
- An event can be published only when it has at least one valid session and sellable ticket tier.
- Taking an event off sale prevents new reservations but does not invalidate existing orders.

### EventSession

An event session represents one scheduled occurrence.

Key fields: `id`, `eventId`, `name`, `startsAt`, `endsAt`, `salesStartAt`, `salesEndAt`, `status`.

States: `DRAFT`, `ON_SALE`, `OFF_SALE`, `ENDED`.

Rules:

- `startsAt` must be earlier than `endsAt`.
- `salesStartAt` must be earlier than `salesEndAt` and no later than `startsAt`.
- A session cannot be put on sale unless its parent event is published.

### TicketTier

A ticket tier is a sellable product within one event session.

Key fields: `id`, `sessionId`, `name`, `unitPrice`, `currency`, `purchaseLimitPerUser`, `status`.

States: `DRAFT`, `ON_SALE`, `OFF_SALE`, `SOLD_OUT`.

Rules:

- Money is stored as an integer in the smallest currency unit.
- Price and currency are copied into the order item at reservation time.
- A price change never changes an existing order.

### Inventory

Inventory is the MySQL source of truth for one ticket tier.

Key fields: `ticketTierId`, `totalQuantity`, `reservedQuantity`, `soldQuantity`, `version`.

Invariant:

```text
0 <= reservedQuantity
0 <= soldQuantity
reservedQuantity + soldQuantity <= totalQuantity
availableQuantity = totalQuantity - reservedQuantity - soldQuantity
```

Redis may cache availability or reject obvious overload, but a Redis result must never create or restore authoritative inventory.

### InventoryReservation

One reservation binds inventory to one order item.

Key fields: `id`, `orderId`, `orderItemId`, `ticketTierId`, `quantity`, `status`, `expiresAt`.

States: `RESERVED`, `CONFIRMED`, `RELEASED`.

Rules:

- There is at most one reservation per order item.
- Successful payment changes reserved inventory to confirmed inventory.
- Cancellation, timeout, or an allowed refund releases inventory exactly once.

### Order

An order is the consistency boundary for the customer purchase lifecycle.

Key fields: `id`, `orderNumber`, `userId`, `eventId`, `sessionId`, `status`, `totalAmount`, `currency`, `idempotencyKey`, `expiresAt`, `paidAt`, `canceledAt`, `refundedAt`, `version`, `createdAt`, `updatedAt`.

Rules:

- `(userId, idempotencyKey)` is unique.
- A repeated create request with the same key and payload returns the original order.
- Reusing a key with a different payload is rejected.
- Order prices are immutable snapshots.
- State changes use conditional updates or row locking so competing operations have one winner.

### Payment

A payment records one payment attempt for an order.

Key fields: `id`, `paymentNumber`, `orderId`, `provider`, `providerTransactionId`, `amount`, `currency`, `status`, `callbackPayloadHash`, `createdAt`, `updatedAt`, `succeededAt`.

Rules:

- `paymentNumber` is globally unique.
- `(provider, providerTransactionId)` is unique when a provider transaction exists.
- Amount and currency must match the order snapshot.
- Duplicate callbacks return the committed result without repeating effects.

### Refund

A refund records one full or partial reversal of a successful payment.

Key fields: `id`, `refundNumber`, `orderId`, `paymentId`, `amount`, `reason`, `status`, `idempotencyKey`, `providerRefundId`, `requestedBy`, `createdAt`, `updatedAt`, `succeededAt`.

Rules:

- Total successful refund amount cannot exceed the successful payment amount.
- `(paymentId, idempotencyKey)` is unique.
- Inventory release is driven by a durable refund-success event and is idempotent.

## Supporting Records

### OutboxEvent

Required fields: `id`, `aggregateType`, `aggregateId`, `eventType`, `eventVersion`, `payload`, `traceId`, `status`, `attemptCount`, `nextAttemptAt`, `leasedUntil`, `lastError`, `createdAt`, `publishedAt`.

### InboxMessage

Required fields: `consumerName`, `messageId`, `eventType`, `payloadHash`, `status`, `receivedAt`, `processedAt`, `lastError`.

`(consumerName, messageId)` is unique and is the consumer idempotency boundary.

### AuditLog

Every sensitive administrative action records the actor, action, target, request trace, before/after summary, result, IP address, and timestamp. Audit records are append-only.

## Initial Domain Events

- `EventPublished`
- `EventTakenOffSale`
- `InventoryReserved`
- `InventoryReservationConfirmed`
- `InventoryReservationReleased`
- `OrderCreated`
- `OrderPaid`
- `OrderCanceled`
- `OrderExpired`
- `PaymentSucceeded`
- `PaymentFailed`
- `RefundRequested`
- `RefundSucceeded`
- `RefundFailed`
- `OrderRefunded`

Event names describe completed facts. Payloads carry identifiers and immutable facts, not database entities.

## Cross-Module Rules

1. MySQL is authoritative for orders, payments, refunds, and inventory.
2. Every externally retried command has an idempotency boundary.
3. Every asynchronous consumer assumes at-least-once delivery.
4. Business state and its outgoing event are committed in one transaction.
5. A module does not update another module's tables outside an explicitly documented transaction use case.
6. Money comparisons include amount and currency.
7. Timestamps are persisted in UTC and rendered in the client's time zone.
8. IDs are opaque to clients; business numbers are separate from primary keys.

## Legacy Migration Map

| Legacy model | Target model | Removal condition |
|---|---|---|
| `Voucher` | `TicketTier` | Public catalog and order creation use ticket tiers |
| `SeckillVoucher` | `Inventory` | Reservation and release are implemented and reconciled |
| `VoucherOrder` | `Order`, `OrderItem`, `InventoryReservation` | The new state machine passes concurrency and migration tests |
| Shop review features | Removed | The event catalog provides all required demonstration data |
| Follow, feed, and sign-in features | Removed | No target API, test, or documentation depends on them |

Legacy tables and code are removed only after the replacement vertical slice is operational. They must not receive new features.
