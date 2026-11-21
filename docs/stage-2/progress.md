# Stage 2 Progress

**Status:** Implemented and reviewed (2026-11-21). The review verdict was APPROVED WITH WARNINGS, and its required actions are fixed. Nothing is committed yet.

## Completed

| Step | Result |
|---|---|
| 1. Schema | Changesets `006-create-event-outbox`, `007-create-notification-deliveries` and `008-alerts-matching-index`. Entities `OutboxRecord` and `NotificationDelivery`, with enums `OutboxStatus` and `DeliveryStatus`. |
| 2. Outbox write | `EventService.create` saves the event and a `PENDING` `event_outbox` record in one transaction. |
| 3. Outbox processing | `OutboxRepository.claimNext()` (`FOR UPDATE SKIP LOCKED`), `OutboxProcessor`, `OutboxWorker`. `AlertRepository.findByEventTypeAndEnabledTrue` loads the owners in the same query. Jobs are inserted with `ON CONFLICT (event_id, alert_id) DO NOTHING`. |
| 4. Senders | The `NotificationSender` interface, `NotificationMessage`, and the logging-only `EmailNotificationSender` and `SlackNotificationSender`. `EventType` gained a readable label. |
| 5. Delivery processing | `NotificationDeliveryRepository.claimNext()`, `DeliveryProcessor` and `DeliveryWorker`. A failure marks the job `FAILED` with the error, in a second transaction. There are no retries. |
| 6. Wrap-up | `NotificationProperties` and `NotificationConfiguration` (scheduling can be switched off). `application.yml` settings. `CLAUDE.md` updated: the queue question is resolved, and a "Notifications" section is added. |

## Verification

- `mvn clean verify`: 167 tests, all passing.
  - Surefire: 128 tests. The new ones are `OutboxProcessorTest` (4), `DeliveryProcessorTest` (4), `NotificationMessageTest` (2), `NotificationWorkersTest` (3), and the outbox test in `EventServiceTest`.
  - Failsafe: 39 tests.
    - `OutboxProcessingIT` (4): the outbox write; jobs only for enabled alerts of the matching type; no duplicates when a record is processed twice; an event with no matching alerts.
    - `OutboxConcurrencyIT` (1): 4 threads process 40 records, and each record is processed exactly once.
    - `DeliveryProcessingIT` (1): an email job is `SENT`, and a Slack job whose sender throws is `FAILED` with the error, without stopping the email job.
    - `OutboxFailureIT` (1): a test-only database trigger makes the second delivery insert fail. The first insert is rolled back, and the record is `FAILED` with the database error.
    - `DeliveryConcurrencyIT` (1): 4 threads send 40 jobs. Each job is `SENT`, and the (spied) email sender ran exactly 40 times.
    - `SchemaConstraintsIT`: 12 tests, including the new tables and 8 migrations.
- **TDD:** each step's tests were written and run first. The RED evidence was a failing schema test (missing tables) and compile errors for the missing classes.
- **Live check:** run with the scheduled workers on, against an isolated database. One `natural_disaster` event with three alerts produced exactly two log lines:

  ```text
  Notification [email] to jane@example.com — alert "Quakes": Natural disaster: M6.8 earthquake (source: usgs)
  Notification [slack] to kim@example.com — alert "Quakes on Slack": Natural disaster: M6.8 earthquake (source: usgs)
  ```

  The `breaking_news` alert was skipped, the outbox record is `PROCESSED`, and both deliveries are `SENT`.

## Review

`review-agent` returned APPROVED WITH WARNINGS, with no critical or high findings.

- **Required actions, both fixed:**
  - an integration test for an outbox failure halfway through, with a real rollback (`OutboxFailureIT`);
  - a concurrent delivery test (`DeliveryConcurrencyIT`).
- **Recommendations applied:**
  - Both processors' Javadoc and `plan.md` now note the short window after a failure in which another worker can try once more before the record is `FAILED`.
  - The poll intervals are defined only in `application.yml` and the `@Scheduled` placeholders; `NotificationProperties` holds just the batch size.
  - `findByEventTypeAndEnabledTrue` now returns alerts oldest first, so the processing order is predictable.
- **Noted for later:** real senders must not be called while the row is locked without a short timeout.
- **TDD compliance:** "unverified" by the review, because there is no per-step git history.

## Deviations from the plan

- `NotificationSender.send` takes a `NotificationMessage` instead of `(delivery, event)`, so senders need no database access. The plan and diagram are updated.
- The scheduling switch is a nested configuration inside `NotificationConfiguration`, not a separate `SchedulingConfiguration` class.
- `NotificationProperties` holds only the batch size. The poll intervals are read by the `@Scheduled` placeholders.

## Known limitations

- **No retries:** a failed record or job stays `FAILED`. Retries (NFR-01) are deferred.
- **No real delivery:** the email and Slack senders only log.
- **At-least-once:** a crash after sending but before the commit sends again.
- **No cleanup:** the `event_outbox` and `notification_deliveries` tables grow without limit.
- **No visibility:** there is no API or page for notification activity (FR-10).
- **Architecture docs:** `docs/architecture/` is not updated, because changes to `system-overview.puml` are on hold.
