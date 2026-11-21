# Decision Log

Each decision has an ID, a status (**Confirmed** or **Provisional**), the options considered, and the consequences. Contradictions are logged as new entries that reference the old one. Entries are not deleted.

Note: CLAUDE.md refers to D-002 (no application code until implementation is approved), but no D-001 or D-002 entry exists in the repo. Numbering starts at D-003 to avoid collisions. Supply the existing entries if they exist elsewhere.

---

## D-003: Alert condition is the event category only

- **Status:** Confirmed (2026-11-21)
- **Decision:** An alert has exactly one condition, an event category (news, market movement, natural disaster). No typed fields.
- **Options considered:**
  - Structured conditions (category plus typed fields, e.g. symbol and percent move). Not chosen.
  - Free-text or keyword matching. Not chosen.
  - Category only. Chosen, as simple as possible.
- **Consequences:**
  - The alert schema holds one category value.
  - Matching is a category equality check.
  - FR-02 is narrowed accordingly. The "attribute criteria" wording proposed in chat on 2026-11-21 is withdrawn (not logged as a decision).
  - Richer conditions can be added later as a new decision that references this one.

## D-004: Notification delivery is asynchronous through a queue

- **Status:** Confirmed (2026-11-21)
- **Decision:** Notification dispatch runs asynchronously through a queue, not inside the request that creates the event.
- **Options considered:**
  - Synchronous dispatch within the event request. Not chosen.
  - Asynchronous through a queue. Chosen.
  - Scheduler polling a table of pending notifications. Not chosen.
- **Consequences:**
  - Retries (NFR-01) are implemented through the queue.
  - The queue technology is not chosen yet (open question).
  - Idempotency (NFR-03) must hold across queue redelivery.

## D-005: Only event ingestion in this phase; no automatic detection

- **Status:** Confirmed (2026-11-21)
- **Decision:** FR-03 is an authenticated endpoint that accepts events. The system does not detect events automatically in this phase.
- **Options considered:**
  - Automatic detection from news, market or disaster sources. Deferred.
  - Endpoint ingestion. Chosen.
- **Consequences:**
  - Event sources push events through the endpoint.
  - Automatic detection requires a new decision when it is planned.

## D-006: Requirements baseline

- **Status:** Provisional (2026-11-21)
- **Decision:** The requirements in [plan.md](plan.md) form the baseline. Items marked "Proposed" there were not confirmed by the user: FR-04, FR-08, FR-11 and NFR-03 to NFR-07. FR-06 and FR-07 are interpreted as a channel interface plus a test double, with no real provider.
- **Options considered:** The user's original list alone, or the original list plus proposed additions. The proposed additions were offered and are not yet accepted.
- **Consequences:**
  - Confirming or dropping each proposed item will update its status here, as a new entry if it contradicts this one.
  - Until then, the proposed items are not implementation requirements.
