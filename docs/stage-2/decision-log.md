# Stage 2 Decision Log

Each decision has an ID, a status (**Confirmed** or **Provisional**), the options considered, and the consequences. Numbering continues from stage 0 (D-007 was the last entry).

---

## D-010: Transactional outbox and a database-backed delivery queue

- **Status:** Confirmed (2026-11-21). The outbox approach was proposed by the user, and the plan was approved and implemented.
- **Decision:**
  - Saving an event also saves an outbox record in the same transaction.
  - An OutboxWorker claims pending records with `FOR UPDATE SKIP LOCKED`, finds the matching alerts, creates one notification delivery job per alert, and marks the record processed.
  - A DeliveryWorker hands the jobs to the email or Slack sender. There are no retries: a failed record or job is marked FAILED with its error. In stage 2 both senders only log the notification, as the user decided; there is no real email or Slack delivery and no new dependencies.
  - The `event_outbox` and `notification_deliveries` tables in PostgreSQL serve as the queue.
- **Options considered:**
  - Notify synchronously inside the event request. Rejected by D-004.
  - A message broker such as RabbitMQ or Kafka. Not chosen: it adds infrastructure, and the dual-write problem remains without an outbox anyway.
  - Outbox plus database-backed jobs. Chosen.
- **Consequences:**
  - Resolves the open question "queue technology" from D-004 for the MVP. A broker can be added later behind the outbox without changing event ingestion.
  - Delivery is at least once. Jobs are idempotent per `(event_id, alert_id)`.
  - Several application instances can run the workers safely.

## D-011: Modular monolith with three Maven modules

- **Status:** Confirmed (2026-11-21). The modules were named by the user; the structure was shown first and approved.
- **Decision:** the backend is one deployable application built from three Maven modules under a parent pom.
  - `event`: events and the `EventCreated` application event.
  - `notification`: the outbox, delivery jobs, workers and senders.
  - `app`: users, alerts, security, configuration, Liquibase, and the runnable entry point.
  - **Dependencies:** they point one way: app → notification → event.
  - **Public API:** each module exposes its contract in an `api` package.
  - **Ports:** a module that needs data from a higher module defines a port, which the higher module implements (`AlertLookup`).
  - **Outbox:** the outbox is written by a synchronous `@EventListener` on `EventCreated`, inside the event's transaction.
  - **Errors:** `NotFoundException` is replaced by Spring's `ResponseStatusException`, so no shared module is needed.
- **Options considered:**
  - Packages in one Maven module, with boundaries checked by Spring Modulith tests. Not chosen: Maven modules make a wrong-way dependency a compile error.
  - A fourth `common` module for shared types. Not chosen: each shared type has a clear owner (`EventType` belongs to `event`, `Channel` to `notification`), and the only other shared class was replaced by `ResponseStatusException`.
  - `app` holding alerts with the notification module depending on `app`. Not possible: it would create a cycle.
- **Consequences:**
  - **Migrations:** the Liquibase changelogs stay at the same classpath path in `app`, so existing databases are not re-migrated.
  - **Tests:** unit tests live in their module. Web MVC and integration tests live in `app`, the only module with a Spring Boot configuration.
  - **Running:** `mvn spring-boot:run` from the root runs only `app`, with the root as its working directory.
