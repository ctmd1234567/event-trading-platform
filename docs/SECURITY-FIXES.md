# Security and Consistency Changes

Baseline: the original repository at commit `cd293f3`, migrated locally to Java 21 and Spring Boot 3.5.16.
MyBatis-Plus 3.5.17, Hutool 5.8.47, and the existing Postman workspace definitions were retained.

## Review Findings and Changes

1. Thread-local identity leakage: `TokenFilter` initializes and clears request identity in a `finally` block, so an invalid token cannot inherit the previous request's user.
2. Anonymous write endpoints: Spring Security distinguishes public reads from administrative operations by method and path; administrator IDs come from server-side configuration.
3. Path traversal: the server generates file names and validates ownership, normalized paths, symbolic links, and image contents; deletion via `GET` is prohibited.
4. Cache correctness: cold reads fall back to the database, lock ownership is verified, logical expiration is bounded, and invalidation happens after commit.
5. Partial order commits: a database transaction reserves stock and persists the request plus Outbox event; the consumer transaction creates the order; a database uniqueness constraint enforces business idempotency.
6. Redis/message-broker dual writes: Redis is no longer the authoritative inventory source. A durable Outbox, broker confirms, retries, idempotent consumption, and owner-scoped status queries provide recoverability.
7. Flash-sale timing: the conditional stock update validates start time, end time, and voucher status in the database.
8. Legacy stream, verification code, and logout behavior: the unused stream path was removed; verification-code delivery is configurable, rate-limited, and single-use; logout revokes the token.

Platform administrators can operate all shops. This is not a tenant-isolated merchant platform.
Redis caching and sessions plus RabbitMQ remain runtime dependencies; the project does not claim every production protection by default.
Payments, refunds, and an operations console are not implemented.

## Database Upgrade

Run only `security-upgrade.sql` for an existing database. The application does not automatically run migrations, clear old Redis data, or change local service configuration.
Back up the database, stop the old application, and review historical orders, inventory, and queue backlogs before upgrading.
Initialize a new environment with `event_trading.sql` followed by `security-upgrade.sql`.
MySQL DDL is not transactional across the entire script. If a step fails, inspect completed statements before applying the remainder.

## Regression Verification

- `mvn test` and `mvn package` do not connect to personal infrastructure by default.
- `mvn -Pinfrastructure verify` creates isolated MySQL, Redis, and RabbitMQ test containers.
- Data-writing preparation tests use the `manual` tag and are excluded by default.

Implementation references:

- https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
- https://www.rabbitmq.com/docs/reliability
- https://docs.spring.io/spring-amqp/reference/amqp/template.html
- https://docs.spring.io/spring-boot/reference/testing/testcontainers.html

Publisher and consumer acknowledgements do not provide cross-system exactly-once delivery. This project uses durable records and idempotent processing to make retries recoverable.
