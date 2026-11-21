# Stage 1 Authentication Implementation Plan

**Status:** Implemented (2026-11-21). See [progress.md](progress.md).

Adds HTTP Basic authentication and two roles, `ADMIN` and `USER`, to the stage-1 API. It builds on [implementation-plan.md](implementation-plan.md) and replaces its section 2 ("No authentication"). Every rule in `CLAUDE.md` still applies:
- services return entities, not DTOs;
- Spring's built-in Problem Details, with no custom error handlers;
- the Testing Conventions and the TDD workflow.

## 1. Goals

1. Every API request is authenticated with HTTP Basic, except signing up and `/hello`.
2. Each user has exactly one role: `ADMIN` or `USER`.
3. A `USER` can only see and change their own user record and their own alerts.
4. An `ADMIN` can see and change every user and every alert, and is the only role that can create events.

## 2. Decisions

| Topic | Decision |
|---|---|
| Mechanism | HTTP Basic over Spring Security. Stateless: no session, no CSRF token, because clients send credentials on every request. |
| Username | The user's email, compared in lower case like the stored email. |
| Password storage | Spring's delegating password encoder with BCrypt (`{bcrypt}...`). The hash is never returned by the API. |
| Roles | The enum `Role` with `ADMIN` and `USER`, stored in a `role` column. Spring authorities are `ROLE_ADMIN` and `ROLE_USER`. |
| Sign-up | `POST /api/v1/users` is public and always creates a `USER`. |
| First admin | Created at startup from the `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables, if both are set and that email does not exist yet. No password is stored in the repository. |
| Role changes | Not part of this plan. A role is set only at sign-up (`USER`) or by the startup admin (`ADMIN`). |
| Ownership checks | Method security (`@PreAuthorize`) on controller methods compares the `userId` in the request with the authenticated user's id. `ADMIN` always passes. |
| Errors | Spring Security's defaults: `401` with `WWW-Authenticate: Basic` when credentials are missing or wrong, and `403` when the role or owner check fails. No custom handlers. |

## 3. Access rules

| Endpoint | Anonymous | `USER` | `ADMIN` |
|---|---|---|---|
| `GET /hello` | yes | yes | yes |
| `POST /api/v1/users` (sign-up) | yes | yes | yes |
| `GET /api/v1/users` | `401` | `403` | yes |
| `GET /api/v1/users/me` | `401` | yes (themselves) | yes (themselves) |
| `GET`, `PUT`, `DELETE /api/v1/users/{userId}` | `401` | only own `userId`, otherwise `403` | yes |
| `POST /api/v1/alerts` | `401` | only with own `userId` in the body, otherwise `403` | yes |
| `GET /api/v1/alerts?userId=` | `401` | only own `userId`, otherwise `403` | yes |
| `GET`, `PUT`, `DELETE /api/v1/alerts/{alertId}/users/{userId}` | `401` | only own `userId`, otherwise `403` | yes |
| `POST /api/v1/events` | `401` | `403` | yes |
| `GET /api/v1/events/{eventId}` | `401` | yes | yes |

The existing ownership lookup (`findByIdAndUserId`) stays. A user who passes the owner check but uses another user's `alertId` still gets `404`.

## 4. API contract changes

These changes need to be added to [plan.md](plan.md) when this plan is approved:

- **`UserRequest`** gets `password`: required, 8 to 72 characters and at most 72 bytes in UTF-8. BCrypt accepts at most 72 bytes, and non-ASCII characters take more than one byte. `PUT` replaces the whole user, so it also requires a password.
- **`UserResponse`** gets `role` (`"ADMIN"` or `"USER"`). It never contains the password or its hash.
- **New status codes:** `401` and `403`, on the endpoints marked in section 3.
- **Scope:** authentication moves from "Out of scope" to "In scope" in `plan.md`.

## 5. Database

Changeset `005-users-authentication.sql`:

| Column | Type | Constraints |
|---|---|---|
| `users.password_hash` | `VARCHAR(100)` | Not null |
| `users.role` | `VARCHAR(16)` | Not null, `CHECK (role IN ('ADMIN', 'USER'))` |

Both columns are `NOT NULL`, so the changeset fails on a database that already has users. There is no production data. The local Compose database keeps its data across application restarts in an anonymous Docker volume, because Spring Boot only runs `docker compose stop` on shutdown. To start with an empty database, run `docker compose down` before the next start.

## 6. Code changes

| Package | Change |
|---|---|
| `pom.xml` | Add `spring-boot-starter-security`, and `spring-security-test` with test scope. |
| `model` | New enum `Role`. `User` gets `passwordHash` and `role`. |
| `dto` | `UserRequest` gets `password`. `UserResponse` gets `role`. |
| `repository` | `UserRepository.findByEmail(String)` is added back, for login and for the startup admin. |
| `service` | `UserService.create` hashes the password and sets `USER`. `UserService.update` hashes the new password. New `DatabaseUserDetailsService` loads a user by email and returns an `AuthenticatedUser`: a `UserDetails` that carries the user id. |
| `configuration` | New `SecurityConfiguration`: the filter chain from section 3, HTTP Basic, stateless, CSRF off, `@EnableMethodSecurity`, and the `PasswordEncoder` bean. New `AdminBootstrap`: an `ApplicationRunner` that creates the first admin from the environment. |
| `controller` | `@PreAuthorize` owner checks, for example `hasRole('ADMIN') or #userId == authentication.principal.id`. `UserController.toResponse` adds `role`. |
| `application.yml` | `app.admin.email: ${ADMIN_EMAIL:}` and `app.admin.password: ${ADMIN_PASSWORD:}`. |

## 7. Testing

Follows the Testing Conventions in `CLAUDE.md`, test-first.

| Level | Location | What it covers |
|---|---|---|
| Unit | `unit/service/` | `UserService` hashes the password and sets `USER`, and never stores the plain password. `DatabaseUserDetailsService` finds a user by lower-cased email and fails for an unknown email. `AdminBootstrap` creates the admin once, and does nothing if the variables are empty or the email already exists. |
| Web MVC | `web/security/` | The access rules table, one row at a time: `401` without credentials, `403` for a `USER` on admin-only or another user's resources, success for the owner and for `ADMIN`, public sign-up. Tests import `SecurityConfiguration` and use Spring Security's test support. |
| Web MVC | `web/controller/` | The existing tests run as an authenticated `ADMIN`, so they keep testing controller behavior. New validation tests for `password`. |
| Integration | `integration/api/` | A new `AuthenticationApiIT`: sign up, then log in with HTTP Basic. Wrong passwords get `401`. A `USER` cannot reach another user's alerts. The startup admin can list users and create events. The existing `*IT` tests send admin credentials through `AbstractIntegrationTest`. |
| Integration | `integration/repository/` | `SchemaConstraintsIT`: rejects an unknown `role` and a missing `password_hash`, and the migration count becomes 5. |

## 8. Work breakdown

Each step ends with `mvn verify` passing.

1. **Schema:** changeset `005`, `Role`, `User` fields, and the `SchemaConstraintsIT` updates.
2. **Security foundation:**
   - dependencies, `PasswordEncoder`, `DatabaseUserDetailsService`, `SecurityConfiguration` with the URL rules;
   - update the existing web and integration tests so they authenticate.
3. **Sign-up and users:** password handling, `role` in the response, the `web/security` user rules.
4. **Ownership:** `@PreAuthorize` on the user and alert endpoints, and the `web/security` alert rules.
5. **Events:** admin-only `POST /api/v1/events`.
6. **Startup admin:** `AdminBootstrap`, and `AuthenticationApiIT`.
7. **Docs and review:**
   - `review-agent` review;
   - update `plan.md` (section 4), `CLAUDE.md` (security in Architecture, `401`/`403` in the status codes, `ADMIN_EMAIL`/`ADMIN_PASSWORD` in the commands) and `progress.md`.

## 9. Risks

| Risk | Mitigation |
|---|---|
| Basic authentication sends the password with every request. | Acceptable only over HTTPS. HTTPS is a deployment concern and outside this plan. |
| A duplicate email at sign-up still returns `500`, because the `409` handling was removed. With public sign-up this is easier to hit. | Accepted for the MVP. Revisit if it matters. |
| There is no way to promote a user to `ADMIN` through the API. | Accepted for the MVP. A later plan can add role management. |
| BCrypt hashing makes every request slower, by a few milliseconds per request. | Acceptable at MVP scale. |
| If the admin environment variables are missing, there is no admin. | `AdminBootstrap` logs a warning at startup when they are missing. |
