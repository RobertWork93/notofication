# Notification App: Plan

## Product brief (verbatim)

> We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later.

## Status

Planning phase. No application code is written until implementation is approved (see CLAUDE.md). Decisions are in [decision-log.md](decision-log.md).

## Functional requirements

| ID | Requirement | Status / decision |
|---|---|---|
| FR-01 | Users create, edit, enable, disable and delete their own alerts. | Original |
| FR-02 | Each alert has exactly one condition: an event category (news, market movement, natural disaster). No typed fields. | Confirmed (D-003) |
| FR-03 | An authenticated endpoint accepts a new event (type, category, timestamp, source, payload), validates it and stores it. | Original, scope confirmed (D-005) |
| FR-04 | When an event is stored, the system evaluates enabled alerts and creates one notification per matching alert. | Proposed (D-006) |
| FR-05 | The system sends notifications to users' registered destinations for matching alerts, and records each one. Dispatch is asynchronous through a queue. | Original, delivery confirmed (D-004) |
| FR-06 | Email is supported through a channel interface, with a test double. No real provider is wired in. | Original, interpreted (D-006) |
| FR-07 | Slack is supported through the same channel interface, with a test double. No real integration. | Original, interpreted (D-006) |
| FR-08 | Users register and manage notification destinations (email address, Slack target) and choose which channel each alert uses. | Proposed (D-006) |
| FR-09 | Admins can list, view, disable and delete users and alerts. | Original |
| FR-10 | Admins can view notification activity (status, attempts, last error, timestamps), filterable by user, alert, channel and status. | Original |
| FR-11 | Users authenticate. Admins are identified by role. Users can only access their own alerts and destinations. | Proposed (D-006) |

## Non-functional requirements

| ID | Requirement | Status / decision |
|---|---|---|
| NFR-01 | Transient failures are retried with bounded attempts and backoff, through the queue. Permanent failures are not retried and are marked failed. Exhausted retries end in a visible failed state (FR-10). Exact numbers are open. | Original, delivery confirmed (D-004) |
| NFR-02 | Adding a channel requires only a new adapter implementing the channel interface. Core matching and dispatch do not change. | Original |
| NFR-03 | Idempotency: one event and one alert produce at most one notification, even on re-submission or retry. | Proposed (D-006) |
| NFR-04 | Security: credentials stay out of source control. Personal data (emails) is access-controlled. | Proposed (D-006) |
| NFR-05 | Observability: structured logs and counters for sent, failed and retried notifications. Admin actions are logged. | Proposed (D-006) |
| NFR-06 | Testability: event matching and each channel can be tested without external services. | Proposed (D-006) |
| NFR-07 | Timeliness and scale: target latency and expected event volume. Values not yet supplied. | Proposed (D-006) |

## Open questions

- Queue technology (not chosen).
- Retry counts and backoff values (NFR-01).
- Latency target and expected event volume (NFR-07).
- Data retention for events and notifications.
- Admin role granularity (FR-09, FR-11).
- Stack not chosen (see CLAUDE.md).
