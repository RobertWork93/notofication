---
Date: 2026-11-21 16:09

Prompt:
Help me identifying the functional and non functional requirements for this project.
Currently i have the following list:

1. Functional requirements
ID    Requirement
FR-01    Users can create, edit, enable, disable, and delete alerts.
FR-02    Users can configure alert conditions based on events.
FR-03    The system detects relevant events (news, market movements, natural disasters). This must be currently an endpoitn which create a new event/
FR-05    The system sends notifications to the useres where alert conditions are met.
FR-06    The system supports email notifications. (only abstract implementation)
FR-07    The system supports Slack notifications. (only abstract imple,mentation)
FR-09    Administrators can view and manage users and alerts.
FR-10    Administrators can monitor notification activity.


2. Non-functional requirements
NFR-01    The system must handle temporary failures and retry notifications.
NFR-02    The architecture must be modular and extensible.

Answer:
The list is a good start. `docs/plan.md` and `docs/decision-log.md` (referenced in CLAUDE.md) do not exist yet, so this review is not checked against them.

**Problems with the current list**
1. **FR-03 mixes two things.** "The system detects events" and "an endpoint creates an event" are different. I treat FR-03 as event ingestion through an endpoint for now. Automatic detection is a later requirement.
2. **FR-02 is too vague** to design against. It needs a condition model.
3. **FR-06 and FR-07 ("abstract only")** are ambiguous. They need a definition: a channel interface plus a test double, with no real provider integration.
4. **FR-09 and FR-10 assume roles,** but no requirement covers authentication or authorization.
5. **Numbering gaps:** FR-04 and FR-08 are missing. Check whether they were dropped on purpose.
6. **Only two NFRs.** Security, timeliness, idempotency, observability and scale are not covered.

**Revised functional requirements** (new items marked *new*)

| ID | Requirement |
|---|---|
| FR-01 | Users create, edit, enable, disable and delete their own alerts. |
| FR-02 | Each alert has one or more conditions that match events by category (news, market, natural disaster) and by attribute criteria. The exact model is an open question. |
| FR-03 | An authenticated endpoint accepts a new event (type, category, timestamp, source, payload), validates it and stores it. Automatic detection is out of scope for now. |
| FR-04 *(new)* | When an event is stored, the system evaluates enabled alerts and creates one notification per matching alert. |
| FR-05 | The system sends notifications to the users' registered destinations for matching alerts, and records each one. |
| FR-06 | Email is supported through a channel interface, with a test double. No real provider is wired in. |
| FR-07 | Slack is supported through the same channel interface, with a test double. No real integration. |
| FR-08 *(new)* | Users register and manage notification destinations (email address, Slack target) and choose which channel each alert uses. |
| FR-09 | Admins can list, view, disable and delete users and alerts. |
| FR-10 | Admins can view notification activity (status, attempts, last error, timestamps), filterable by user, alert, channel and status. |
| FR-11 *(new)* | Users authenticate. Admins are identified by role. Users can only access their own alerts and destinations. |

**Revised non-functional requirements**

| ID | Requirement |
|---|---|
| NFR-01 | Transient failures (timeouts, 5xx, rate limits) are retried with a bounded number of attempts and backoff. Permanent failures (e.g. invalid destination) are not retried and are marked failed. Exhausted retries end in a visible failed state (FR-10). The exact numbers are deferred. |
| NFR-02 | Adding a new channel requires only a new adapter implementing the channel interface. Core matching and dispatch do not change. Verifiable with a test channel. |
| NFR-03 *(new)* | Idempotency: one event and one alert produce at most one notification, even when an event is re-submitted or a retry happens. |
| NFR-04 *(new)* | Security: credentials (Slack tokens, provider keys) are kept out of source control. Personal data (emails) is access-controlled. |
| NFR-05 *(new)* | Observability: structured logs and counters for sent, failed and retried notifications. Admin actions are logged. |
| NFR-06 *(new)* | Testability: event matching and each channel can be tested without external services. |
| NFR-07 *(new)* | Timeliness and scale: target latency from event to notification and expected event volume. The user must supply the numbers. |

**Open questions** (only the ones that change architecture or the data model, per CLAUDE.md)
1. **Condition model:** are alert conditions structured (category plus typed fields, e.g. symbol and percent move) or free-text/keyword? This drives the alert schema.
2. **Delivery model:** is dispatch synchronous, or asynchronous through a queue or scheduler? Retries (NFR-01) depend on this.
3. **Scope:** confirm that only ingestion is in scope for FR-03, with no automatic detection in this phase.

Deferred, since they don't change the architecture yet: retry counts and backoff values, latency targets, expected volume, data retention, admin role granularity.

If you want, I can create `docs/plan.md` and `docs/decision-log.md` from this, with the requirements as the first decisions. Both are marked Provisional until you answer the questions above.
---
---
Date: 2026-11-21 16:16

Prompt:
1) Currently keep as simple as possible so the only condition is the category, no typed fields are included!
2) Delibery model must be async through a queue
3) Confirmed!@ No automatic detection in this phase!

Answer:
Recorded three decisions:
- D-003: alert condition is the event category only (Confirmed).
- D-004: delivery is asynchronous through a queue (Confirmed).
- D-005: only event ingestion this phase, no automatic detection (Confirmed).

Created `docs/plan.md` (product brief verbatim, requirements baseline) and `docs/decision-log.md`. FR-02 narrowed to category-only. Proposed items FR-04, FR-08, FR-11 and NFR-03 to NFR-07 are logged as Provisional in D-006 until you confirm them.

Still open: queue technology, retry counts and backoff, latency and volume targets, data retention, admin role granularity.

Note: CLAUDE.md refers to D-002 (no code until implementation is approved), but no D-001 or D-002 entry exists in the repo. Numbering starts at D-003 to avoid collisions.
---
---
Date: 2026-11-21 16:20

Prompt:
<pasted_content id="801c">
Based on the functional requirements, create a high-level system architecture diagram using PlantUML.

The known workflow is:

1. The system detects a new event.
2. The event is saved to the database.
3. New events are processed asynchronously.
4. The system identifies users whose alert configurations match the event.
5. Notifications are sent to the relevant users through their configured channels (email or Slack).

Keep the architecture high-level. Show the main components and how they interact. Do not introduce unnecessary complexity or assume technologies that haven't been decided.
</pasted_content id="801c">

Answer:
Created `docs/architecture/system-overview.puml`, a high-level component diagram.

PlantUML is not installed locally, so the file is not render-checked.
---
---
Date: 2026-11-21 16:35

Prompt:
update claude.md with the tech stack

Answer:
Updated the `## Stack` section of `CLAUDE.md` with the proposed tech stack: Java 21, Spring Boot modular monolith, Angular with TypeScript and Angular Material, PostgreSQL, Spring Data JPA / Hibernate, Liquibase, Spring Security, JUnit 5 and Mockito, Docker and Docker Compose. Build tool (Maven or Gradle) and queue technology are listed as open. The stack is marked as proposed and not yet implemented.

The "Current state" section still says no language is assumed and "Current phase: planning" still applies. Those were not changed.
---
