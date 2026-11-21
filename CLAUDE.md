# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current phase: implementation (started 2026-11-21)

Quoted project description: "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later."
The project moved from planning to early implementation (see D-007 in `docs/stage-0/decision-log.md`). The Spring Boot skeleton exists. Feature code is not written yet. The requirements in `plan.md` and the decisions in `decision-log.md` in the latest stage folder still govern what gets built.

## Project Stages and Documentation

The project covers the full software development life cycle, so work is organized into small, documented stages. Each stage is one project iteration with its own documentation. Always work in the latest stage, and keep earlier stages unchanged as historical records.

### Stage structure
- All project documentation lives in `docs/`, in folders named `stage-n`: `stage-0`, `stage-1`, `stage-2`, and so on. The one exception is `docs/architecture/` (see below).
- The latest stage is the folder with the highest number.

### Architecture folder
- `docs/architecture/` sits directly in `docs/`, outside every `stage-n` folder.
- It must always describe the current state of the application, not a past or planned state.
- Update it in the same change whenever the application's architecture changes. Unlike stage folders, it is edited over time and is not a historical record.

### Working rules
- Create and update documentation only in the latest stage folder.
- Never modify or overwrite documentation in older stages.
- Only the user decides when a new stage starts. Never create a stage on your own.

### Before starting any work
1. Check `docs/` and identify the latest stage folder.
2. Read its relevant documentation before making changes.
3. If no stage folder exists, ask the user before creating `stage-0`.

### Stage documentation
Typical files in a stage folder (add others when needed):
- `decision-log.md`: architecture decisions and their rationale.
- `plan.md`: the stage's goals, tasks and implementation plan.
- `prompts.md`: the conversation log (see Conversation History).
- `requirements.md`: functional and non-functional requirements.
- `architecture.md`: architecture and design work for this stage. The current-state architecture belongs in `docs/architecture/`.
- `progress.md`: completed work, pending tasks and current status.

### Completing a stage
- Update the stage's documentation with its final status: completed tasks, important decisions and outstanding issues.
- After that, do not modify the stage again.
- Wait for the user to explicitly request the next stage.

## Conversation History

Maintain a chronological, append-only conversation log in `prompts.md` in the latest stage folder (`docs/stage-n/prompts.md`).

After every interaction, record the exact user prompt and your complete response using this format:

```text
---
Date: YYYY-MM-DD HH:mm

Prompt:
<user prompt>

Answer:
<Claude response>
---
```

### Rules
- Record every user message and corresponding Claude response.
- Preserve original wording. Never summarize or paraphrase.
- Append new entries; never modify or delete previous entries.
- Use the actual date and time.
- Create `prompts.md` in the latest stage folder if it doesn't exist.
- Never fabricate missing conversation history.
- Update the log continuously, not just at the end of a session.
- If unable to update the file, inform the user.

## Stack

Chosen tech stack (proposed by the user, not yet implemented):

- Java 21
- Spring Boot, as a modular monolith
- Angular with TypeScript, using Angular Material for UI components
- PostgreSQL
- Spring Data JPA / Hibernate
- Liquibase for schema migrations (`ddl-auto` should be `validate`)
- Spring Security
- JUnit 5 and Mockito for tests
- Docker and Docker Compose

Build tool: Maven (`pom.xml`, Spring Boot 4.2.0 parent, Java 21 with Lombok as an annotation processor).

Commands (run from the repository root). Docker must be running: the integration tests use Testcontainers, and `spring-boot:run` starts PostgreSQL from `compose.yaml`.
- Unit and Web MVC tests only (no Docker needed): `mvn test`
- All tests, including integration tests: `mvn verify`
- Compile only: `mvn compile`
- Run the app: `mvn spring-boot:run`
  - Starts the `compose.yaml` PostgreSQL automatically.
  - `ADMIN_EMAIL` and `ADMIN_PASSWORD` create the first admin at startup. Without them there is no admin.
  - Locally they come from the gitignored `.env` file in the repository root (`admin` / `admin`), which `application.yml` imports. Environment variables of the same name take precedence.
  - Datasource settings come from `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, with local defaults in `application.yml`.
- Run a single unit or Web MVC test: `mvn test -Dtest=ClassName` or `mvn test -Dtest=ClassName#methodName`
- Run a single integration test: `mvn verify -Dit.test=ClassName -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false`

Queue: asynchronous delivery (D-004) uses a transactional outbox plus database-backed delivery jobs in PostgreSQL, with no message broker (D-010 in `docs/stage-2/decision-log.md`).

## Architecture

Single Maven module, not modular for now. Code is split into layers by package under `com.notificationapp`:

- `controller`: HTTP endpoints. Calls services only.
- `service`: business logic. Calls repositories and other services.
- `repository`: Spring Data JPA repositories.
- `model`: domain model: JPA entities and domain enums. Used by every layer; never returned from controllers.
- `dto`: request and response objects exchanged through the API. Implement every DTO as a Java `record`.
- `configuration`: Spring `@Configuration` classes and beans.
- `NotificationAppApplication` (root package): Spring Boot entry point.

Dependencies point one way: controller → service → repository. Controllers must not call repositories. Test layout follows the Testing Conventions below.

**Services never return DTOs.** A service returns domain objects from `model`. Converting them to response DTOs happens only in the controller.

How the pieces fit together:
- **Mapping:** services return entities. The controller converts them to response records before returning them, so no entity leaves the controller.
- **Errors:** services throw `NotFoundException` (404). Spring's built-in Problem Details (`spring.mvc.problemdetails.enabled`) handles it and the `400` validation errors. There is no custom exception handler.
- **Security:** HTTP Basic with the `ADMIN` and `USER` roles, stateless.
  - `configuration/SecurityConfiguration` holds the URL rules: public sign-up, and admin-only user listing and event creation.
  - Ownership checks are `@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")` on controller methods.
  - The principal is `service/AuthenticatedUser`, loaded by `service/DatabaseUserDetailsService`, with the email as the username.
  - Passwords are BCrypt hashes.
  - `configuration/AdminBootstrap` creates the first admin from `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
  - The access table is in `docs/stage-1/auth-implementation-plan.md`.
- **Enums:** `EventType` and `Channel` hold their lowercase wire value. Jackson uses `@JsonValue`/`@JsonCreator`, and the database uses the auto-applied converters in `model`.
- **Schema:** Liquibase owns it (`src/main/resources/db/changelog`, formatted SQL), and Hibernate only validates it.
- **Time:** timestamps come from the `Clock` bean in `configuration`, so tests can fix the time.
- **Notifications:** the design and rules are in `docs/stage-2/`.
  - **Event and outbox:** `EventService.create` saves the event and an `event_outbox` record in one transaction.
  - **Outbox worker:** `OutboxWorker` (`@Scheduled`) calls `OutboxProcessor.processNext()`. In one transaction, the processor:
    1. claims a record with `FOR UPDATE SKIP LOCKED`;
    2. finds the enabled alerts of the event type;
    3. inserts one `notification_deliveries` job per alert (`ON CONFLICT (event_id, alert_id) DO NOTHING`);
    4. marks the record `PROCESSED`.
  - **Delivery worker:** `DeliveryWorker` calls `DeliveryProcessor`, which claims a job the same way and passes a `NotificationMessage` to the `NotificationSender` for the job's channel.
  - **Senders:** `EmailNotificationSender` and `SlackNotificationSender` only log for now. A new channel is one more `NotificationSender`.
  - **Failures:** a failure rolls back and marks the record or job `FAILED` in a second transaction. There are no retries.
  - **Settings:** `app.notifications.*` holds the batch size and poll intervals. `app.notifications.scheduling.enabled=false` turns the workers off; integration tests do this and call the processors directly.
- **Tests:**
  - Integration tests extend `support/AbstractIntegrationTest`. It starts the app on a random port with Testcontainers PostgreSQL and a startup admin. Before each test it deletes everything except that admin, and its `client` sends the admin's credentials.
  - Web MVC tests `@Import(SecurityConfiguration.class)`. They run as `@WithMockUser(roles = "ADMIN")`, or as a specific user through `support/SecurityTestSupport.as(user)`.

## Testing Conventions

These rules are mandatory for every new test and every test refactoring. Do not introduce other folder structures or naming conventions without explicit permission.

### Structure

Under `src/test/java/com/notificationapp/`, every test class sits in the folder that matches its package. Keep `unit`, `web`, `integration` and `support` as separate top-level packages, and never put all tests in one package.

```text
unit/
  service/       service unit tests
  mapper/        mapping tests (enum JSON values, JPA converters)
web/
  controller/    @WebMvcTest tests
  security/      authentication and authorization tests
integration/
  api/           full API tests over real HTTP
  service/       service tests against real infrastructure
  repository/    repository and schema tests
support/
  TestcontainersConfiguration.java
  AbstractIntegrationTest.java
  TestDataFactory.java
```

For example, `unit/service/UserServiceTest.java` is in package `com.notificationapp.unit.service`.

### Naming

- **Unit and Web MVC tests:** `*Test.java`, for example `UserServiceTest` or `UserControllerTest`.
- **Integration tests:** `*IT.java`, for example `UserApiIT` or `UserRepositoryIT`.
- **End-to-end tests:** `*E2ETest.java`.
- **Test methods:** describe the behavior, for example `shouldReturnUserWhenUserExists`.

### Rules per test type

- **Unit (`unit/`):** JUnit 5 and Mockito, with `@Mock` and `@InjectMocks` where appropriate. No Spring context, no database, no Testcontainers. Test business logic in isolation.
- **Web MVC (`web/`):** `@WebMvcTest` and `MockMvc`, with services mocked by `@MockitoBean`. Test HTTP responses, JSON, validation, exceptions, and (once it exists) authentication and authorization. No full context and no Testcontainers.
- **Integration (`integration/`):**
  - Use `@SpringBootTest` with Testcontainers for real infrastructure, through `AbstractIntegrationTest`.
  - Use `@DataJpaTest` for repository-only tests where it fits.
  - Test the complete API with real HTTP requests on `RANDOM_PORT`.
  - Keep tests independent and deterministic.
- **Support (`support/`):** reusable Testcontainers configuration, base classes, test data factories, builders and fixtures. No actual test classes.

### General principles

- **Before changing tests:** inspect the existing code and tests, reuse the existing test infrastructure, and follow the project's existing dependencies and versions.
- **Writing tests:**
  - Follow Arrange / Act / Assert.
  - Cover positive and negative scenarios.
  - Avoid unnecessary mocking and duplicated tests.
  - Keep tests isolated and deterministic.
- **Production code:** never change it just to suit a test, unless there is a genuine issue.

### Maven execution

- **Surefire** runs `*Test.java` on `mvn test`.
- **Failsafe** runs `*IT.java` on `mvn verify`.
- Integration tests never run during `mvn test`.

## REST API Conventions

Expose all functionality as REST APIs and follow REST best practices:

- **Resources:** URLs name resources with plural nouns, never verbs (`/alerts`, not `/createAlert`). Users, alerts and events are each top-level resources (`/users`, `/alerts`, `/events`). A single alert is addressed with its owner as `/alerts/{alertId}/users/{userId}`. Alerts are created with `POST /alerts` and listed per user with `GET /alerts?userId={userId}`.
- **Versioning:** prefix every path with `/api/v1`.
- **Methods:** `GET` reads, `POST` creates, `PUT` replaces a whole resource, `PATCH` changes part of one, `DELETE` removes. `GET`, `PUT` and `DELETE` must be idempotent.
- **Status codes:** `200 OK` for reads and updates, `201 Created` with a `Location` header for creates, `204 No Content` for deletes, `400` for invalid input, `401` for missing or wrong credentials, `403` when the role or ownership check fails, `404` for a missing resource. Never return `200` with an error body.
- **Payloads:** JSON with camelCase field names. Timestamps are ISO-8601 in UTC (`2026-11-21T14:52:00Z`). IDs are UUIDs.
- **Errors:** use Spring's built-in Problem Details. Keep error handling minimal: no custom exception handlers or extra error codes unless the plan asks for them.
- **Validation:** validate request bodies with Bean Validation (`@Valid`) at the controller.
- **DTOs:** controllers accept and return DTOs, never JPA entities.
- **Collections:** paginate list endpoints with `page` and `size` query parameters.
## Frontend

The Angular frontend lives in `frontend/`, next to the Spring Boot backend. It is a separate npm project, not a Maven module.

- **Versions:** Angular 21.2 with Angular Material 21 (prebuilt `azure-blue` CSS theme). Angular 22 needs Node ≥ 22.22.3, and this machine has Node 22.21.1.
- **npm:** npm 10.9 fails on this project with an `edgesOut` error. Use npm 11 through npx: `npx -y npm@11 install`.
- **Commands (run in `frontend/`):**
  - Dev server: `npm start`. It serves on http://localhost:4200 and proxies `/api` to the backend on http://localhost:8080 (`proxy.conf.json`). Start the backend first with `mvn spring-boot:run`.
  - Unit tests: `npm test -- --watch=false` (Vitest).
  - Production build: `npm run build`.
- **Backend calls:**
  - Every request goes through `core/interceptors/auth.interceptor.ts`.
  - It adds the HTTP Basic header for the logged-in user, and `X-Requested-With: XMLHttpRequest` so Spring Security does not trigger the browser's login popup.
  - Credentials are kept in `sessionStorage` until logout.

### Angular Development Guidelines

Mandatory for all frontend work. Act as a senior Angular developer and follow modern Angular best practices.

**1. Architecture**

Use a modular, feature-based structure with standalone components and lazy-loaded routes:

```text
frontend/src/app/
  core/       singleton services, authentication, interceptors, guards, layout
  shared/     reusable components, directives, pipes, shared models
  features/   one folder per business feature: components, services, models, routes
```

**2. Strict file separation**

Every component has three files:
- `name.component.ts` (logic);
- `name.component.html` (template, referenced with `templateUrl`);
- `name.component.css` (styles, referenced with `styleUrl`).

Never:
- use inline templates or inline styles;
- put HTML or CSS inside TypeScript;
- mix API calls with presentation logic.

**3. Components**
- Small, single-purpose, standalone components with `inject()`-based dependency injection.
- Signals for local state, and RxJS for asynchronous streams.
- `ChangeDetectionStrategy.OnPush` where it fits.
- Typed `input()`/`output()`.
- No unnecessary complexity.

**4. Services**
- **Files:** service classes live in their own `.service.ts` files.
- **HTTP:** every HTTP call is inside a service and uses `HttpClient`. Components never call `HttpClient` directly.
- **Typing:** use `providedIn: 'root'` where appropriate, with strong typing and generics.
- **No duplication:** never duplicate business logic across services.

**5. HTML**
- Semantic, accessible HTML, with ARIA attributes where needed.
- Clean templates without complex expressions.
- The modern control flow: `@if`, `@for`, `@switch`.
- No unnecessary DOM manipulation.

**6. CSS**
- Component styles only in external CSS files, using reusable classes and CSS variables for shared values (defined in `src/styles.css`).
- Responsive layout, no inline styles, and a minimum of global styles.

**7. TypeScript**
- Strict mode, and never `any`.
- Interfaces for data structures, and union types or enums where they fit.
- Small, focused methods, and no unnecessary dependencies.

**8. Code quality:** SOLID, DRY, separation of concerns, appropriate error handling, no unnecessary abstractions.

**9. Output requirements.** When implementing a feature:
1. Show the proposed folder structure first.
2. List every file that is created or modified, naming each file clearly.
3. Keep HTML, CSS and TypeScript separate, including when code is shown in chat.
4. Include all imports and dependencies, and make sure the files work together.
5. Never leave placeholders such as "implement this later".

When working directly in the repository, the files themselves hold the complete code. Respect the existing architecture and conventions instead of restructuring without need.

**10. Angular version:** use the version above (21.2). Ask before moving to another major version or relying on APIs from a different version.
