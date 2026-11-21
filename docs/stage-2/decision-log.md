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
