# Stage 1 Progress

**Status:** Completed (2026-11-21). Implemented and reviewed, then simplified to an MVP, with HTTP Basic authentication and the Angular frontend added. The custom error handling was removed on request. Committed as "Create frontend and backend MVP with basic auth". The open issues below carry over to later stages. Stage 2 starts the notification service.

## Completed

| Phase | Result |
|---|---|
| 0. Infrastructure | JPA, PostgreSQL driver, Liquibase, `spring-boot-docker-compose` and Testcontainers added. `compose.yaml` runs PostgreSQL 18. Formatted-SQL changesets create `users`, `alerts` and `events`. Integration tests run against Testcontainers PostgreSQL. |
| 1. Shared building blocks | `EventType` and `Channel` enums with JSON and database mapping, `NotFoundException` (`404`), Spring's built-in Problem Details, `Clock` bean, validated `page` and `size` parameters. |
| 2. Users | `POST`, `GET` (list), `GET`, `PUT` and `DELETE` on `/api/v1/users`. |
| 3. Alerts | `POST /api/v1/alerts`, `GET /api/v1/alerts?userId=`, and `GET`, `PUT`, `DELETE` on `/api/v1/alerts/{alertId}/users/{userId}`. |
| 4. Events | `POST /api/v1/events` and `GET /api/v1/events/{eventId}`. |
| 5. Wrap-up | `review-agent` review, fixes, MVP simplification, `CLAUDE.md` updated. This file. |
| 6. Authentication ([auth-implementation-plan.md](auth-implementation-plan.md)) | HTTP Basic, stateless, with `ADMIN` and `USER` roles. Changeset `005` adds `password_hash` and `role`. Public sign-up creates a `USER`. The startup admin comes from `ADMIN_EMAIL`/`ADMIN_PASSWORD`. URL rules cover user listing and event creation (admin only). `@PreAuthorize` handles the owner checks on users and alerts. Passwords are BCrypt hashes and are never returned or logged. |
| 7. Frontend | Angular 21.2 app in `frontend/` with Angular Material, following the Angular guidelines in `CLAUDE.md`. Sign-up, login and logout over HTTP Basic, with the session in `sessionStorage`. Users manage their alerts (list, create, edit, enable or disable, delete). Admins list and delete users, manage any user's alerts, and create events. Everyone can view an event by id and edit their own account. The backend gained `GET /api/v1/users/me`, test-first, so the frontend can learn the logged-in user. |

## Verification

- `mvn clean verify`: 140 tests, all passing.
  - Surefire (`mvn test`): 114 tests.
    - 48 unit tests in `unit/`.
    - 42 Web MVC tests in `web/controller/`.
    - 24 access-rule tests in `web/security/`, including `/users/me`, and that browser scripts get `401` without the `WWW-Authenticate` header that would open the browser's login popup.
  - Failsafe (`mvn verify`): 26 integration tests in `integration/`, over real HTTP against Testcontainers PostgreSQL, including `AuthenticationApiIT`.
- **Frontend:** `npm test -- --watch=false` runs 30 Vitest tests (auth service, interceptor, guards, alert service, JSON validator, error messages). `npm run build` passes without warnings.
- **Frontend browser check:** run against an isolated backend and database. It covered:
  - sign-up with automatic login;
  - creating an alert;
  - turning the alert off (confirmed in the database);
  - logout and admin login;
  - the users list and an admin viewing another user's alerts;
  - creating an event with a JSON payload and viewing it.

  The browser console showed no errors.
- **Authentication review:** `review-agent` returned APPROVED WITH WARNINGS. Both required actions are fixed (see below).
- **Authentication smoke test:** run with `mvn spring-boot:run` and real environment variables. It checked:
  - the startup admin is created;
  - anonymous user listing returns `401`;
  - sign-up returns `201` with role `USER`;
  - a user reading their own record returns `200`;
  - a wrong password returns `401`;
  - a `USER` listing users returns `403`;
  - an `ADMIN` listing users returns `200`.
- **TDD:** each phase's tests were written and run first. The production code came after. The review marked TDD "unverified", because there is no per-step git history.
- **Live smoke test:** run with `mvn spring-boot:run` and Docker Compose, before the simplification. It checked users, alerts, events, the page shape, a `400` for an unknown type and for an oversized `size`, and deleting a user.

## Changes during the stage

| Change | Reason |
|---|---|
| `UserRequest` trims the email in its constructor. | An integration test found that an email with surrounding spaces failed `@Email` validation. |
| Lists sort by `createdAt`, then `id`. | Review recommendation: a stable order across pages. |
| Changeset `004` adds `CHECK (email = lower(email))`. | Review recommendation: case-insensitive uniqueness enforced by the database. |
| Tests reorganized into `unit/`, `web/`, `integration/` and `support/`. Methods are named `should...`. Integration tests are `*IT`, use real HTTP on a random port, and run under Failsafe. `TestDataFactory` and `AbstractIntegrationTest` hold the shared setup. The empty context test is replaced by `SchemaConstraintsIT.shouldApplyAllMigrations`. | New Testing Conventions in `CLAUDE.md`. |
| `UserRequest` checks that the password is at most 72 bytes in UTF-8, not only 72 characters. | Review: a non-ASCII password could pass `@Size` and then fail in BCrypt with a `500`. |
| Access tests added: a `USER` updating another user or their alert (`403`), an `ADMIN` on another user and their alerts, anonymous writes (`401`), and a `USER` with their own `userId` but another user's `alertId` (`404`). | Review: these rows of the access table were untested. |
| `AdminBootstrap` logs a warning when the admin email is already taken. | Review recommendation: a user who signed up with that email would otherwise block the admin silently. |
| Services return entities. Each controller converts them to response records. | New `CLAUDE.md` rule: services never return DTOs. |
| Removed the custom `ApiExceptionHandler`, the `409 Conflict` handling (`ConflictException`, email-taken checks, the database-conflict mapping), the catch-all `500` handler and the `errors` list. | User request: a simple MVP without extra error code. Spring's built-in Problem Details covers the `400` and `404` cases. |

## Known deviations and open issues

- A duplicate email (ignoring case) is still blocked by the database unique constraint. Without the `409` handling, it now returns Spring Boot's default `500` error. With public sign-up this is easier to hit.
- Unexpected errors return Spring Boot's default error response, not Problem Details.
- `AlertService.create` reads `userId` from the request DTO. The controller's `@PreAuthorize` checks that this `userId` belongs to the caller unless the caller is an `ADMIN`.
- There is no API to change a user's role. A second admin needs a database change.
- If someone signs up with the `ADMIN_EMAIL` address before the admin is created, no admin is created. A warning is logged.
- The owner checks assume the logged-in principal is an `AuthenticatedUser`. That is always true with HTTP Basic today, but another login mechanism would need the same principal type.
- HTTP Basic sends the password with every request, so any deployment must use HTTPS.
- `docs/architecture/` is not updated, because changes to `system-overview.puml` are on hold.
- **Frontend: no event list.** Events can only be opened by id, for example right after an admin creates one. The backend has no endpoint for listing events.
- **Frontend: password on every account change.** The account page needs a password for every change, because `PUT /users/{id}` replaces the whole user, password included.
- **Frontend: component tests.** Frontend unit tests cover services, guards, the interceptor and helpers. Components are covered only by the browser check, with no component tests.
- **Frontend: Angular version.** The frontend uses Angular 21.2 rather than 22, because Angular 22 needs Node ≥ 22.22.3.
