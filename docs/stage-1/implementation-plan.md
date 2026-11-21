# Stage 1 Implementation Plan

**Status:** Proposed (2026-11-21). Not started.

This plan turns [plan.md](plan.md) into implementation work: users, alerts and events backed by PostgreSQL. `plan.md` is the contract. If this plan and `plan.md` disagree, `plan.md` wins.

## 1. Starting point

What exists today:

- A single Maven module on Spring Boot 4.2.0 and Java 21, with Lombok.
- The `controller`, `service` and `repository` packages.
- The test endpoint `GET /hello` and its tests.
- No database dependencies and no configuration for them. They were removed earlier on purpose.
- No Spring Security.
- One `@SpringBootTest` context test that currently runs without a database.

## 2. No authentication: consequences (superseded)

This section described the stage before authentication was added. [auth-implementation-plan.md](auth-implementation-plan.md) replaces it.

Authentication is out of scope for this stage. This shapes the implementation as follows:

1. **Callers are trusted.** The `userId` in a path, query or body is taken as given. Anyone who can reach the API can create, read, change or delete any user and any alert. The application must only run locally or on a trusted network in this stage.
2. **Ownership is data scoping, not security.** "A user can only read, edit or delete their own alerts" is enforced by looking alerts up with both `alertId` and `userId`, and returning `404` when they do not match. This stops one user's alert being changed through another user's URL by mistake. It does not stop someone who uses the right `userId`.
3. **Prepare for authentication later.** Services take `userId` as an explicit parameter and never read it from the request themselves. When authentication arrives, the controllers can pass the authenticated user's id instead, and the service rules and tests stay the same.
4. **Personal data is exposed.** `GET /api/v1/users` returns every user's email to any caller. This is accepted for this stage and must be fixed before any shared deployment.
5. **Event creation is open.** Decision D-005 in stage-0 calls event ingestion "an authenticated endpoint". In this stage `POST /api/v1/events` has no authentication. This gap needs to be closed when authentication is added.
6. **No security tests.** With no security in place, there are no `401` or `403` cases to test.

## 3. Technical decisions

### Dependencies

| Change | Purpose | Approval needed |
|---|---|---|
| Add `spring-boot-starter-data-jpa` and `org.postgresql:postgresql` (runtime) | Persistence | No. Part of the chosen stack. |
| Add `spring-boot-starter-liquibase` | Schema migrations | No. Part of the chosen stack. |
| Add Spring Boot Testcontainers support and the Testcontainers PostgreSQL module (test scope) | Integration tests against real PostgreSQL | Approved (2026-11-21) |
| Add `spring-boot-docker-compose` (optional, development only) | Starts PostgreSQL automatically on `mvn spring-boot:run` | Approved (2026-11-21) |

### Database and migrations

- **Local database:** a `compose.yaml` at the repository root runs PostgreSQL. Use the same image version as the Testcontainers tests.
- **Datasource settings:** `application.yml` reads them from environment variables, with local defaults. Set `spring.jpa.hibernate.ddl-auto: validate` and `spring.jpa.open-in-view: false`.
- **Migrations:** Liquibase, with one master changelog that includes one formatted-SQL changeset per table: `users`, then `alerts`, then `events`. Formatted SQL is used because the `CHECK` constraints, `JSONB` and `ON DELETE CASCADE` are written most clearly as plain SQL.
- **Cascade:** `ON DELETE CASCADE` on `alerts.user_id` deletes a user's alerts. `User` has no alerts collection in JPA, so deleting a user never loads their alerts.

### Packages

The packages follow the Architecture section of `CLAUDE.md`:

| Package | Contents |
|---|---|
| `model` | JPA entities `User`, `Alert`, `Event`; enums `EventType`, `Channel` |
| `dto` | Request and response records |
| `configuration` | The `Clock` bean and any other Spring configuration |
| `controller` | `UserController`, `AlertController`, `EventController` |
| `service` | `UserService`, `AlertService`, `EventService`, and `NotFoundException` |
| `repository` | `UserRepository`, `AlertRepository`, `EventRepository` |

Services return entities, never DTOs. Each controller converts entities to response records before returning them.

### Domain mapping

- **IDs:** `@Id @GeneratedValue(strategy = GenerationType.UUID)`, so Hibernate generates the UUIDs.
- **Timestamps:** set by the services from an injected `java.time.Clock` bean, so tests can fix the time.
- **Enum values:** `EventType` and `Channel` each hold their lowercase value (`breaking_news`, `email`).
  - JSON uses that value through `@JsonValue` and a `@JsonCreator` factory. The factory throws on an unknown value, so the request becomes a `400`.
  - The database uses that value through an auto-applied JPA `AttributeConverter` for each enum.
- **Event payload:** `Map<String, Object>`, mapped with `@JdbcTypeCode(SqlTypes.JSON)` to `JSONB`. Because the type is a `Map`, a JSON array or a plain value is rejected with `400`.
- **Alert to user:** `Alert.user` is a lazy `@ManyToOne`. Responses take `userId` from the foreign key, so the user is never loaded just to build a response.
- **Lombok:** entities use `@Getter`, `@Setter` and `@NoArgsConstructor(access = PROTECTED)`, never `@Data`. DTOs are records.

### Requests and validation

- **Bean Validation:** request records use `@NotNull`, `@NotBlank`, `@Size`, `@Email`, and controllers use `@Valid`.
- **Two alert request records.** The contract's single `AlertRequest` is split into two:
  - `CreateAlertRequest`: `userId` required, `enabled` optional and defaulting to `true`.
  - `UpdateAlertRequest`: no `userId`, `enabled` required.

  The JSON contract is unchanged.
- **Email:** trimmed and lower-cased in `UserService` before the uniqueness check and the save.
- **Unknown JSON fields** are ignored, which is Spring Boot's default.

### Pagination

- **Explicit parameters.** List endpoints take `page` and `size` as validated `@RequestParam`s (`@Min(0)`, and `@Min(1) @Max(100)` for size) and build the `PageRequest` themselves. Spring's `Pageable` resolver is not used directly, because it silently clamps bad values, while the contract requires `400`.
- **Fixed sort.** Users are sorted by `createdAt`. Alerts are sorted by `createdAt` ascending, oldest first.
- **Response shape.** The body follows the contract: `content` plus a `page` object. This is Spring Data's `PagedModel` serialization. If the Spring Data version in use does not produce that shape by default, enable it with `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`.

### Errors

There is no custom exception handler. `spring.mvc.problemdetails.enabled: true` makes Spring return Problem Details for its own errors:

| Cause | Status |
|---|---|
| Bean Validation failure, malformed JSON, unknown enum value, wrong type | `400` |
| Missing `userId` on the alerts list, invalid `page` or `size` | `400` |
| `NotFoundException` (user, alert or event). It extends Spring's `ErrorResponseException`. | `404` |

Alert lookups use `findByIdAndUserId`. A missing alert and an alert owned by another user produce the same `404`.

### Endpoint behavior

| Endpoint | Service behavior |
|---|---|
| `POST /users` | Normalize the email, save, and return `201` with a `Location` header. |
| `PUT /users/{userId}` | `404` if the user is missing. Otherwise update and set `updatedAt`. |
| `DELETE /users/{userId}` | `404` if the user is missing. Otherwise delete; the database cascade removes their alerts. |
| `POST /alerts` | `404` if the user is missing. Otherwise save and return `201` with `Location: /api/v1/alerts/{id}/users/{userId}`. |
| `GET /alerts?userId=` | `404` if the user is missing. Otherwise return a page of their alerts. |
| `GET`, `PUT`, `DELETE /alerts/{alertId}/users/{userId}` | Look up by alert and user. `404` if there is no match. |
| `POST /events` | Set `receivedAt` from the `Clock`, save, return `201` with a `Location` header. No matching and no notifications. |

## 4. Testing strategy

All work follows TDD with the project agents and skills:

1. `test-agent` writes the failing tests (RED).
2. `implementation-agent` makes them pass and refactors (GREEN, REFACTOR).
3. `review-agent` checks each phase.

| Level | Tool | What it covers |
|---|---|---|
| Unit | JUnit 5, Mockito, AssertJ | Services: email normalization, not-found paths, `enabled` defaulting to `true` on create, ownership through `findByIdAndUserId`, timestamps from the `Clock`. Enums: mapping known values and rejecting unknown ones. |
| Controller | `@WebMvcTest`, MockMvc, mocked services | Per endpoint: success status, `Location` header, response JSON, validation `400`s, unknown enum and malformed JSON `400`s, `page` and `size` limits, missing `userId`, and the `404` mapping. |
| Integration (`*IT`, Failsafe) | `@SpringBootTest` on `RANDOM_PORT` with real HTTP (`RestTestClient`), Testcontainers PostgreSQL, through `support/AbstractIntegrationTest` | Liquibase migrations run and Hibernate validation passes. Full flows: create user, create alert, list, edit, delete. Deleting a user removes their alerts. Email uniqueness ignores case. The event payload survives a round trip through `JSONB`. `CHECK` constraints hold. |

**Existing tests:**
- `HelloControllerTest` stays as it is.
- `NotificationAppApplicationTests` will no longer start without a database once JPA is added. It was first converted to a Testcontainers context test. After the move to the Testing Conventions in `CLAUDE.md`, it was replaced by `integration/repository/SchemaConstraintsIT.shouldApplyAllMigrations`.
- Test layout and naming follow the Testing Conventions in `CLAUDE.md`: `unit/`, `web/`, `integration/` and `support/`. Surefire runs `*Test`, and Failsafe runs `*IT`.

**Docker requirement:** integration tests and `mvn verify` will need Docker running. `CLAUDE.md` must say so.

## 5. Work breakdown

Each phase ends with `mvn verify` passing and a `review-agent` review.

**Phase 0: infrastructure**
1. Add the dependencies and `compose.yaml`, and add the datasource and JPA settings to `application.yml`.
2. Write the Liquibase master changelog and the three changesets.
3. Add a shared Testcontainers PostgreSQL configuration for tests.
4. Convert the context test to Testcontainers. It passes once migrations and validation work.

**Phase 1: shared building blocks**
1. Add the `EventType` and `Channel` enums with their JSON and database mapping.
2. Add `NotFoundException` and enable Spring's Problem Details.
3. Add the `Clock` bean and the pagination parameter handling.

**Phase 2: users.** Build the entity, repository, service, DTOs and controller, covering all five user endpoints.

**Phase 3: alerts.** Build the alerts endpoints the same way. This phase depends on phase 2.

**Phase 4: events.** Build the events endpoints the same way. This phase depends only on phase 1, so it can run in parallel with phases 2 and 3.

**Phase 5: wrap-up**
1. Run a full `review-agent` review.
2. Update `CLAUDE.md`: the commands, and the note that Docker is required.
3. Write `progress.md` for stage 1.

`docs/architecture/` is not updated in this stage, because changes to `system-overview.puml` are on hold.

## 6. Approvals

All approvals are given (2026-11-21):

1. The Testcontainers test dependencies.
2. The optional `spring-boot-docker-compose` dependency.

## 7. Risks

| Risk | Mitigation |
|---|---|
| The API has no authentication, so all data is exposed (section 2). | Run only locally. Design services so authentication can be added later without changing service code. |
| Spring Boot 4 moved several test annotations and modules, such as the `@WebMvcTest` package. | Check imports against the Boot 4.2 artifacts already in use, as the existing `HelloControllerTest` does. |
| Default pagination behavior does not match the contract. | Validate `page` and `size` explicitly, and test the response shape. |
| Docker is not available on a machine. | Integration tests fail there. Document Docker as a requirement. |
