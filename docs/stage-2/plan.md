# Stage 2 Plan: Notification Service

**Status:** Planning (started 2026-11-21).

Implementation details for this stage are in [implementation-plan.md](implementation-plan.md).

## Goal

When a new event is registered, find the alerts that match it and notify each alert's owner through the alert's channel: email or Slack.

## Scope

**In scope:**
- Recording each new event in an outbox, in the same transaction as the event.
- An outbox worker that turns each new event into notification delivery jobs, one per matching alert.
- A delivery worker that hands each job to its channel's sender.
- An email sender and a Slack sender that only log the notification. There is one sender per channel, so real delivery can replace them later without other changes.

**Out of scope for this stage:**
- Real email and Slack delivery (SMTP, the Slack API). No message leaves the application.
- An API or frontend page for notification activity (FR-10).
- Cleaning up old outbox records and delivery jobs.

## 1. Flow

```text
POST /api/v1/events
  └─ one transaction: save event + save outbox record (PENDING)

OutboxWorker (runs every few seconds)
  1. finds pending outbox records
  2. claims them safely (a row lock that other workers skip)
  3. loads the event
  4. finds matching alerts: same event type, enabled
  5. creates one notification delivery job per alert (PENDING)
  6. marks the outbox record PROCESSED
  └─ steps 2 to 6 run in one transaction per record

DeliveryWorker (runs every few seconds)
  1. finds pending delivery jobs
  2. claims them safely
  3. passes the job to its channel's sender (email or Slack); in this stage the sender only logs it
  4. marks the job SENT, or FAILED with the error
```

## 2. Rules

- **Outbox:** an event and its outbox record are saved together or not at all. No event is ever lost between being saved and being processed.
- **Matching:** an alert matches an event when the alert's event type equals the event's type and the alert is enabled. Disabled alerts get no notification.
- **Idempotency:** one event and one alert produce at most one delivery job, even if an outbox record is processed twice (NFR-03).
- **Safe claiming:** several worker threads, or several application instances, can run at once. A record or job is processed by only one of them at a time.
- **Failures:** there are no retries. When processing an outbox record or a delivery job fails, it is marked FAILED and keeps the error. One broken record does not block the others. Retries (NFR-01) are deferred to a later stage.
- **Delivery guarantee:** at least once. In rare cases, such as a crash right after sending but before the job is marked SENT, a notification can be sent twice. There is also a short window after a failure: before the job is marked FAILED, another worker can try it once more.
- **Recipient:** the alert owner, identified by their email address, for both channels.
- **Sending in this stage:** the email and Slack senders write one log line per notification (channel, recipient, alert, event) and report success.

## 3. Database schema

**`event_outbox`**

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | Primary key |
| `event_id` | `UUID` | Not null, foreign key to `events.id` |
| `status` | `VARCHAR(16)` | Not null, `PENDING`, `PROCESSED` or `FAILED` |
| `last_error` | `VARCHAR(1000)` | Nullable |
| `created_at` | `TIMESTAMPTZ` | Not null |
| `processed_at` | `TIMESTAMPTZ` | Nullable |

Partial index on `created_at` where `status = 'PENDING'`, for the worker's query.

**`notification_deliveries`**

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | Primary key |
| `event_id` | `UUID` | Not null, foreign key to `events.id` |
| `alert_id` | `UUID` | Not null, foreign key to `alerts.id`, deleted with the alert |
| `user_id` | `UUID` | Not null, foreign key to `users.id`, deleted with the user |
| `channel` | `VARCHAR(16)` | Not null, `email` or `slack` |
| `recipient` | `VARCHAR(320)` | Not null. The alert owner's email address when the job was created. |
| `status` | `VARCHAR(16)` | Not null, `PENDING`, `SENT` or `FAILED` |
| `last_error` | `VARCHAR(1000)` | Nullable |
| `created_at` | `TIMESTAMPTZ` | Not null |
| `sent_at` | `TIMESTAMPTZ` | Nullable |

Constraints and indexes:
- Unique on `(event_id, alert_id)`. This enforces one delivery per event and alert.
- Partial index on `created_at` where `status = 'PENDING'`.
- Index on `alerts (event_type) WHERE enabled`, for matching.

## 4. Domain model

| Concept | Attributes | Relationships |
|---|---|---|
| **OutboxRecord** | id, status, lastError, createdAt, processedAt | Refers to one Event |
| **OutboxStatus** | `PENDING`, `PROCESSED`, `FAILED` | |
| **NotificationDelivery** | id, channel, recipient, status, lastError, createdAt, sentAt | Refers to one Event, one Alert and one User |
| **DeliveryStatus** | `PENDING`, `SENT`, `FAILED` | |

## 5. API

No new endpoints. `POST /api/v1/events` keeps its contract; it now also writes the outbox record. Notifications are sent in the background, so the event request does not wait for them.
