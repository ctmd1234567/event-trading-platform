# Project Requirements: High-Concurrency Real-Time Event Trading Platform

## Objective

> Build a high-concurrency event trading platform in which MySQL is the transactional source of truth, Redis supports caching and traffic control, and RabbitMQ plus a Transactional Outbox provides reliable asynchronous messaging. The complete target domain covers events, sessions, products or ticket tiers, inventory, orders, payments, refunds, and real-time notifications, with demonstrable concurrency correctness, idempotency, eventual consistency, recovery, observability, and reproducible load testing.

## Requirements

1. Implement a complete business path from event and session through inventory, ordering, payment, cancellation or refund, and notification; the result must be more than CRUD.
2. Define explicit order and payment state machines that handle duplicate payments, refunds, callbacks, timeouts, and exceptional states.
3. Keep inventory concurrency-safe: no overselling, no negative stock, and no duplicate reservation by one user.
4. Make every critical API and asynchronous consumer idempotent, including ordering, payment callbacks, refunds, and RabbitMQ consumers.
5. Use MySQL as the final transactional source of truth with appropriate unique constraints, transactions, indexes, and data constraints.
6. Use Redis for hot-data caching, distributed rate limiting, temporary state, or other justified concurrency support, while handling cache penetration, hot-key expiration, timeouts, and degraded operation.
7. Use RabbitMQ for asynchronous load leveling and decoupling, with Publisher Confirm, Consumer ACK, bounded retries, a dead-letter queue, backlog handling, and redrive support.
8. Use a Transactional Outbox so business data and pending events are stored in the same database transaction.
9. Design RabbitMQ consumers for at-least-once delivery so duplicate messages cannot create duplicate business results.
10. Design compensation and reconciliation for inventory, orders, payments, and refunds.
11. Provide scheduled compensation and operator entry points for querying, retrying, and redriving failures.
12. Support real-time status delivery through WebSocket or SSE, including reconnect, replay, duplicate handling, and ordering concerns.
13. Provide distributed rate limiting and overload protection by user, endpoint, event, or global traffic.
14. Provide timeouts, circuit breaking, isolation, backpressure, and graceful shutdown to prevent cascading failures.
15. Match database indexes to actual query patterns, verify core queries with `EXPLAIN ANALYZE`, and preserve before-and-after evidence for at least one real SQL optimization.
16. Provide USER and ADMIN authorization, keep secrets out of source code, and audit sensitive administrative operations.
17. Make complete business paths traceable with fields such as `traceId`, `userId`, `orderId`, and `messageId`.
18. Expose HTTP QPS, P50/P95/P99 latency, error and rejection rates, JVM health, connection pools, Redis latency, RabbitMQ backlog, and consumer throughput.
19. Alert on significant queue backlogs, dead-letter growth, database failures, excessive API errors, and Redis failures.
20. Use Flyway or Liquibase for versioned database migration instead of manual schema changes.
21. Demonstrate at least one tested database backup and recovery procedure.
22. Design upgrades for rollback-safe application and database version combinations.
23. Provide a reproducible Docker Compose environment for MySQL, Redis, RabbitMQ, monitoring components, and the application.
24. Include unit, integration, and end-to-end tests instead of relying entirely on manual Postman testing.
25. Test core concurrency correctness; for example, stock of 100 must produce exactly 100 valid outcomes under heavy concurrency, with no overselling or duplicate orders.
26. Cover duplicate requests, messages and callbacks, out-of-order delivery, service restarts, and network timeouts.
27. Inject failures into Redis, MySQL, RabbitMQ, and consumers, then verify protection and recovery.
28. Demonstrate observable RabbitMQ backlog growth under producer pressure and recovery after consumer capacity returns.
29. Provide reproducible performance tests with scripts, environment details, data, and parameters.
30. Cover normal load, capacity stress, sudden bursts, and long-running stability.
31. Run the final stability test for about 60 minutes and observe memory, garbage collection, thread count, connection pools, Redis, and RabbitMQ for degradation or leaks.
32. Define every performance number precisely as requests, concurrent users, or QPS, together with hardware, data size, duration, and traffic model.
33. Record QPS, P50, P95, P99, errors, business rejections, CPU, memory, database connections, and RabbitMQ backlog for each performance run.
34. Document architecture, core workflows, order and payment state machines, key sequences, database design, reliable messaging, recovery, and test methods.
35. Publish only measured performance data that can be traced to its environment, scripts, and raw results.
36. Explain why each major technology is used, alternatives considered, costs, failure modes, and replacement conditions.
37. Keep the architecture focused; do not add Kafka, microservices, Kubernetes, Seata, Elasticsearch, or other components only to increase the technology count.
38. If the prototype came from a course sample, genuinely refactor the domain and core workflow and remove unrelated shop-review, follow, and check-in features over time.

## Minimum Acceptance Principle

> Business behavior remains correct under normal and concurrent load; duplicate requests and messages do not repeat effects; failures in Redis, MySQL, RabbitMQ, or consumers can be contained and recovered; and every reliability or performance claim is supported by measured evidence.

## Completion Standard

The finished project should be suitable as the primary project in a Java backend internship or graduate interview. Its domain model, transaction consistency, concurrency controls, reliable messaging, recovery strategy, performance evidence, and engineering tradeoffs should support a focused 20-to-30-minute technical discussion.
