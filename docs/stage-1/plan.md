# Stage 1 Plan

**Status:** Planning (started 2026-11-21).

Implementation details for this stage go in a separate `implementation-plan.md`.

## Goals

1. Work with two hardcoded event types: `breaking_news` and `natural_disaster`.
2. Define the API contracts, endpoints, database schema and domain models for:
   - creating a new event of one of those two types,
   - creating, editing, deleting and listing alerts for a user,
   - users, as a resource of their own.

## Scope

**In scope:** event creation, user management, alert management, HTTP Basic authentication with the `ADMIN` and `USER` roles (see [auth-implementation-plan.md](auth-implementation-plan.md)), and the `users`, `events` and `alerts` tables.

**Out of scope for this stage:**
- Matching events to alerts.
- Sending notifications by email or Slack.
- The queue.
- Changing a user's role through the API.
- The Angular frontend.

## 1. Event types

There are exactly two event types. They are fixed, so there is no way to add, edit or remove one through the API.

| Value | Meaning |
|---|---|
| `breaking_news` | Breaking news |
| `natural_disaster` | Natural disaster |

The same value is used in requests, in responses and in the database. A request with any other value is rejected.

## 2. API contracts

All endpoints follow the REST API Conventions in `CLAUDE.md`. Users, alerts and events are each their own resource.

### Authentication

- Every request needs HTTP Basic credentials (email and password), except sign-up (`POST /api/v1/users`).
- Missing or wrong credentials return `401`. A role or ownership check failing returns `403`.
- A `USER` can only reach their own user record and their own alerts.
- An `ADMIN` can reach everything, and is the only role that can list users and create events.
- The full access table per endpoint is in section 3 of [auth-implementation-plan.md](auth-implementation-plan.md).

### Endpoints

**Users**

| Method | Path | Purpose | Success |
|---|---|---|---|
| `POST` | `/api/v1/users` | Sign up (public, always creates a `USER`) | `201 Created` |
| `GET` | `/api/v1/users` | List users, paginated | `200 OK` |
| `GET` | `/api/v1/users/me` | Read the logged-in user (used by clients after login) | `200 OK` |
| `GET` | `/api/v1/users/{userId}` | Read a user | `200 OK` |
| `PUT` | `/api/v1/users/{userId}` | Edit a user (full replace) | `200 OK` |
| `DELETE` | `/api/v1/users/{userId}` | Delete a user and their alerts | `204 No Content` |

**Alerts**

| Method | Path | Purpose | Success |
|---|---|---|---|
| `POST` | `/api/v1/alerts` | Create an alert for a user | `201 Created` |
| `GET` | `/api/v1/alerts?userId={userId}` | List a user's alerts, paginated | `200 OK` |
| `GET` | `/api/v1/alerts/{alertId}/users/{userId}` | Read a user's alert | `200 OK` |
| `PUT` | `/api/v1/alerts/{alertId}/users/{userId}` | Edit a user's alert (full replace) | `200 OK` |
| `DELETE` | `/api/v1/alerts/{alertId}/users/{userId}` | Delete a user's alert | `204 No Content` |

**Events**

| Method | Path | Purpose | Success |
|---|---|---|---|
| `POST` | `/api/v1/events` | Create an event | `201 Created` |
| `GET` | `/api/v1/events/{eventId}` | Read an event | `200 OK` |

### Users

Request body for `POST` and `PUT` (`UserRequest`):

```json
{
  "email": "jane.doe@example.com",
  "name": "Jane Doe",
  "password": "correct-horse-battery"
}
```

| Field | Type | Rule |
|---|---|---|
| `email` | string | Required. A valid email address, at most 320 characters. Unique across users, ignoring case. It is also the login name. |
| `name` | string | Required, 1 to 100 characters. |
| `password` | string | Required on `POST` and `PUT`, 8 to 72 characters and at most 72 bytes in UTF-8 (the BCrypt limit). Stored only as a hash. |

Response (`UserResponse`). The password and its hash are never returned.

```json
{
  "id": "0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4",
  "email": "jane.doe@example.com",
  "name": "Jane Doe",
  "role": "USER",
  "createdAt": "2026-11-21T15:00:00Z",
  "updatedAt": "2026-11-21T15:00:00Z"
}
```

Per operation:
- **Create:** `201 Created`, `Location: /api/v1/users/{id}`, body `UserResponse`.
- **List:** `200 OK`, a page of `UserResponse` (see Pagination).
- **Read:** `200 OK`, body `UserResponse`.
- **Edit:** `200 OK`, body `UserResponse`.
- **Delete:** `204 No Content`. The user's alerts are deleted with them.

Errors:
- `400`: invalid body, or invalid `page` or `size`.
- `401`: missing or wrong credentials.
- `403`: a `USER` listing users or reaching another user's record.
- `404`: the user does not exist.

### Alerts

Request body for `POST` and `PUT` (`AlertRequest`). On `PUT`, the user comes from the path, so `userId` is not sent in the body.

```json
{
  "userId": "0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4",
  "name": "Earthquakes",
  "eventType": "natural_disaster",
  "channel": "email",
  "enabled": true
}
```

| Field | Type | Rule |
|---|---|---|
| `userId` | UUID | Required on `POST`. The user who owns the alert. |
| `name` | string | Required, 1 to 100 characters. The user's label for the alert. |
| `eventType` | string | Required. `breaking_news` or `natural_disaster`. The alert's only condition. |
| `channel` | string | Required. `email` or `slack`. |
| `enabled` | boolean | Optional on `POST`, defaults to `true`. Required on `PUT`, because `PUT` replaces the whole alert. |

Response (`AlertResponse`):

```json
{
  "id": "8d2e4b7a-1c3f-4e5a-9b6d-0f1e2d3c4b5a",
  "userId": "0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4",
  "name": "Earthquakes",
  "eventType": "natural_disaster",
  "channel": "email",
  "enabled": true,
  "createdAt": "2026-11-21T15:00:00Z",
  "updatedAt": "2026-11-21T15:00:00Z"
}
```

Per operation:
- **Create:** `201 Created`, `Location: /api/v1/alerts/{id}/users/{userId}`, body `AlertResponse`.
- **List:** `200 OK`, a page of the user's alerts, oldest first. `userId` is required.
- **Read:** `200 OK`, body `AlertResponse`.
- **Edit:** `200 OK`, body `AlertResponse`. Enabling and disabling an alert is an edit.
- **Delete:** `204 No Content`.

Errors:
- `400`: invalid body, missing `userId` on list, or invalid `page` or `size`.
- `401`: missing or wrong credentials.
- `403`: a `USER` sending another user's `userId`.
- `404`: the user does not exist, or the alert does not exist or belongs to another user. Both cases return the same response, so the API does not reveal other users' alerts.

### Events

Request body for `POST` (`CreateEventRequest`):

```json
{
  "type": "natural_disaster",
  "title": "M6.8 earthquake off the coast",
  "source": "usgs",
  "occurredAt": "2026-11-21T14:52:00Z",
  "payload": { "magnitude": 6.8, "region": "Pacific" }
}
```

| Field | Type | Rule |
|---|---|---|
| `type` | string | Required. `breaking_news` or `natural_disaster`. |
| `title` | string | Required, 1 to 200 characters. |
| `source` | string | Required, 1 to 100 characters. Who reported the event. |
| `occurredAt` | timestamp | Required. When the event happened. |
| `payload` | object | Optional. Type-specific details, stored as sent. |

Response (`EventResponse`):

```json
{
  "id": "3f6c1a52-9a1e-4c0e-9a7e-2a1b5f0c9d11",
  "type": "natural_disaster",
  "title": "M6.8 earthquake off the coast",
  "source": "usgs",
  "occurredAt": "2026-11-21T14:52:00Z",
  "payload": { "magnitude": 6.8, "region": "Pacific" },
  "receivedAt": "2026-11-21T14:53:10Z"
}
```

Per operation:
- **Create:** `201 Created`, `Location: /api/v1/events/{id}`, body `EventResponse`. The event is only stored. Nothing is matched or sent.
- **Read:** `200 OK`, body `EventResponse`.

Errors:
- `400`: missing or invalid field, unknown `type`, or malformed JSON.
- `401`: missing or wrong credentials.
- `403`: a `USER` creating an event.
- `404`: the event does not exist (read only).

### Pagination

List endpoints take `page` (zero-based, default `0`) and `size` (default `20`, maximum `100`) query parameters, and return:

```json
{
  "content": [ ],
  "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
}
```

### Error format

`400` and `404` errors use Spring's standard Problem Details response:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "User 0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4 not found",
  "instance": "/api/v1/users/0b9f1d2e-3c4a-4b5c-8d6e-7f8091a2b3c4"
}
```

## 3. Database schema

PostgreSQL.

**`users`**

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | Primary key |
| `email` | `VARCHAR(320)` | Not null, unique, stored in lower case |
| `name` | `VARCHAR(100)` | Not null |
| `password_hash` | `VARCHAR(100)` | Not null |
| `role` | `VARCHAR(16)` | Not null, `ADMIN` or `USER` |
| `created_at` | `TIMESTAMPTZ` | Not null |
| `updated_at` | `TIMESTAMPTZ` | Not null |

**`alerts`**

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | Primary key |
| `user_id` | `UUID` | Not null, foreign key to `users.id`, deleted with the user |
| `name` | `VARCHAR(100)` | Not null |
| `event_type` | `VARCHAR(32)` | Not null, `breaking_news` or `natural_disaster` |
| `channel` | `VARCHAR(16)` | Not null, `email` or `slack` |
| `enabled` | `BOOLEAN` | Not null, default `true` |
| `created_at` | `TIMESTAMPTZ` | Not null |
| `updated_at` | `TIMESTAMPTZ` | Not null |

Index on `alerts.user_id`, for listing a user's alerts.

**`events`**

| Column | Type | Constraints |
|---|---|---|
| `id` | `UUID` | Primary key |
| `type` | `VARCHAR(32)` | Not null, `breaking_news` or `natural_disaster` |
| `title` | `VARCHAR(200)` | Not null |
| `source` | `VARCHAR(100)` | Not null |
| `occurred_at` | `TIMESTAMPTZ` | Not null |
| `payload` | `JSONB` | Nullable |
| `received_at` | `TIMESTAMPTZ` | Not null |

## 4. Domain model

| Concept | Attributes | Relationships |
|---|---|---|
| **User** | id, email, name, passwordHash, role, createdAt, updatedAt | Has zero or more Alerts |
| **Role** | `ADMIN`, `USER` | Each User has exactly one |
| **Alert** | id, name, eventType, channel, enabled, createdAt, updatedAt | Belongs to exactly one User |
| **Event** | id, type, title, source, occurredAt, payload, receivedAt | None in this stage |
| **EventType** | `breaking_news`, `natural_disaster` | Used by Alert and Event |
| **Channel** | `email`, `slack` | Used by Alert |

Rules:
- A user's email is unique, ignoring case.
- An alert always belongs to one user. Deleting the user deletes their alerts.
- An alert has exactly one condition: its event type.
- A `USER` can only read, edit or delete their own user record and their own alerts. An `ADMIN` can reach every user and alert.
- Sign-up always creates a `USER`. The first `ADMIN` is created at startup from `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
