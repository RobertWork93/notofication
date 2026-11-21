# Stage 2 Implementation Plan: Notification Service

**Status:** Implemented (2026-11-21). See [progress.md](progress.md).

This plan turns [plan.md](plan.md) into implementation work. Every rule in `CLAUDE.md` still applies:
- services return entities, not DTOs;
- error handling stays minimal;
- the Testing Conventions and the TDD workflow.

## 1. Assessment of the proposed design

The proposed design is to write an outbox record together with the event, and have an OutboxWorker turn it into delivery jobs. It is sound and fits the project:

- **No lost events:** the event and its outbox record commit in one transaction. That fixes the classic dual-write problem, where an event is saved but the "send notifications" step never happens.
- **No new infrastructure:** decision D-004 asked for asynchronous delivery through a queue, and left the technology open. The `event_outbox` table plus the delivery job table act as a database-backed queue, so no broker such as RabbitMQ or Kafka is needed for the MVP (see D-010).
- **Additions this plan makes:**
  - claiming with `SELECT … FOR UPDATE SKIP LOCKED`;
  - idempotent job creation through a unique constraint;
  - a second worker that hands the deliveries to the channel senders.
- **No new dependencies:** the email and Slack senders only log in this stage, so the plan adds no dependency and no new infrastructure.

## 2. How "claims them safely" works

Each record is processed in its own transaction:

```sql
SELECT * FROM event_outbox
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 1
FOR UPDATE SKIP LOCKED
```

- **Locking:** the row lock is the claim. Other workers skip locked rows, so two workers never process the same record at once. This holds with several threads or several application instances, so no extra locking library is needed.
- **Steps 3 to 6:** loading the event, matching the alerts, creating the jobs and marking the record `PROCESSED` all happen in the same transaction. They commit together.
- **Crashes:** if the worker crashes, the transaction rolls back, the lock is released, and the record is picked up again. No lease or timeout bookkeeping is needed.
- **Failures:** when processing throws, the transaction rolls back. A second, short transaction (`REQUIRES_NEW`) then marks the record `FAILED` and stores `last_error`. There is no retry.
- **Duplicates:** delivery jobs are inserted with `ON CONFLICT (event_id, alert_id) DO NOTHING`. If a record is processed twice, no duplicate job appears.

The DeliveryWorker uses the same pattern. It claims a job, calls the channel's sender, and marks the job `SENT` in one transaction. Today the senders only log, so this is instant. When real senders replace them, the send runs while the row is locked, which gives the at-least-once guarantee stated in `plan.md`. A real sender will need a timeout, so the lock stays short.

## 3. Code changes

| Package | Change |
|---|---|
| `model` | `OutboxRecord` and `OutboxStatus`. `NotificationDelivery` and `DeliveryStatus`. |
| `repository` | `OutboxRepository` and `NotificationDeliveryRepository`. Each has a native claim query (`FOR UPDATE SKIP LOCKED`), and the delivery repository has a native idempotent insert. `AlertRepository` gains `findByEventTypeAndEnabledTrue(EventType)`. |
| `service` | `EventService.create` also saves an `OutboxRecord` in the same transaction. New classes: `OutboxProcessor` (claim, match, create jobs, mark processed, record failure), and `DeliveryProcessor` (claim, call the sender, mark sent, record failure). |
| `service` (channels) | The `NotificationSender` interface (`channel()`, `send(NotificationMessage)`). `NotificationMessage` carries the channel, recipient, alert name and event details, so senders need no database access. Two implementations, `EmailNotificationSender` and `SlackNotificationSender`, each with only a `log.info(...)` of the channel, recipient, alert and event title. `DeliveryProcessor` picks the sender by the job's channel. Adding a channel, or replacing a logging sender with a real one, means writing one class (NFR-02). |
| `service` (workers) | `OutboxWorker` and `DeliveryWorker`: `@Scheduled(fixedDelay)` components that call their processor in a loop until no record is left or the batch size is reached. |
| `configuration` | `NotificationConfiguration`, which binds `NotificationProperties` and has a nested `@EnableScheduling` configuration. Scheduling is switched off with `app.notifications.scheduling.enabled=false`, so tests drive the workers directly. `NotificationProperties` (`@ConfigurationProperties("app.notifications")`) holds the batch size. The workers read the poll intervals straight from `app.notifications.*-poll-interval-ms`. |
| `application.yml` | `app.notifications.*`, with the defaults below. |

No changes to `pom.xml` or `compose.yaml`.

### Settings (defaults)

| Setting | Default |
|---|---|
| Outbox poll interval | 2 s |
| Delivery poll interval | 2 s |
| Batch size per run | 50 |

### Log line

Both senders write one line in the same shape:

```text
Notification [email] to jane.doe@example.com — alert "Earthquakes": Natural disaster: M6.8 earthquake (source: usgs)
```

## 4. Database changes

| Changeset | Content |
|---|---|
| `006-create-event-outbox.sql` | The `event_outbox` table and its partial `PENDING` index. |
| `007-create-notification-deliveries.sql` | The `notification_deliveries` table, the `(event_id, alert_id)` unique constraint, its partial index, and the cascading foreign keys. |
| `008-alerts-matching-index.sql` | `CREATE INDEX … ON alerts (event_type) WHERE enabled`. |

## 5. Testing

Follows the Testing Conventions in `CLAUDE.md`, test-first. The logging senders are not unit-tested on their own, because they contain no logic.

| Level | Location | What it covers |
|---|---|---|
| Unit | `unit/service/` | `OutboxProcessor`: no record means nothing to do; matching alerts become jobs; disabled and other-type alerts are ignored; the record is marked processed. `DeliveryProcessor`: picks the sender for the job's channel; success marks it `SENT`; a failure marks it `FAILED` with the error. `EventService` saves the outbox record. |
| Integration | `integration/service/` | `OutboxProcessingIT`: creating an event writes the outbox record in the same transaction; processing creates jobs only for enabled alerts of the matching type; processing twice creates no duplicates. `OutboxConcurrencyIT`: several threads process many records at once, and each record is processed exactly once. `DeliveryProcessingIT`: checks the move to `SENT`, and uses a test sender that throws, to check that the job becomes `FAILED` with the error and that other jobs are still sent. |
| Integration | `integration/repository/` | `SchemaConstraintsIT`: the new `CHECK` constraints, the unique `(event_id, alert_id)`, the cascade when an alert or user is deleted, and the migration count of 8. |

`AbstractIntegrationTest` sets `app.notifications.scheduling.enabled=false`, so background workers never interfere with a test.

## 6. Work breakdown

Each step ends with `mvn verify` passing.

1. **Schema:** changesets 006 to 008, the entities and enums, and the `SchemaConstraintsIT` updates.
2. **Outbox write:** `EventService.create` saves the outbox record in the same transaction.
3. **Outbox processing:** the claim query, `OutboxProcessor`, `OutboxWorker`, and scheduling with its switch.
4. **Senders:** the `NotificationSender` interface, and the logging `EmailNotificationSender` and `SlackNotificationSender`.
5. **Delivery processing:** `DeliveryProcessor` and `DeliveryWorker`, with failures marked `FAILED`.
6. **Wrap-up:**
   - `review-agent` review;
   - `CLAUDE.md` updates (architecture, the queue question resolved, settings);
   - `progress.md`;
   - a live check: create an event as admin and see one notification log line per matching enabled alert.

## 7. Approvals

None needed. The plan adds no dependencies or infrastructure.

## 8. Risks

| Risk | Mitigation |
|---|---|
| No real notification is sent yet. | Intended for this stage. A real sender replaces a logging one without other changes. |
| At-least-once delivery can repeat a send after a crash. | Accepted and documented in `plan.md`. The unique `(event_id, alert_id)` prevents duplicate jobs. |
| The outbox and delivery tables grow without limit. | Accepted for the MVP. A cleanup job is out of scope. |
| A temporary failure is not retried, so the notification is lost. | Accepted for the MVP. The job stays `FAILED` with its error, so it can be found. Retries (NFR-01) can be added later as their own stage. |
| Polling adds database load every 2 s. | The partial indexes make an empty poll cheap. The intervals are configurable. |
