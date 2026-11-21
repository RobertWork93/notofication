# Notification App

Users set up alerts for important events (breaking news and natural disasters) and choose to be notified by email or Slack.

The repository has two applications:

| Folder | Application | Stack |
|---|---|---|
| `/` (root) | Backend REST API: a modular monolith built from the Maven modules `event`, `notification` and `app` | Java 21, Spring Boot 4.2, PostgreSQL, Liquibase, Spring Security (HTTP Basic) |
| `frontend/` | Web frontend | Angular 21.2, Angular Material 21 |

## Prerequisites

- **Java 21**
- **Maven 3.9+**
- **Docker**, running. The backend starts its PostgreSQL database with Docker Compose, and the integration tests use Testcontainers.
- **Node.js 22**, version 22.12 or later.
- **npm 11**, run through `npx`. npm 10 fails to install the frontend dependencies, as described below.

## 1. Start the backend

1. Create a `.env` file in the repository root with the first admin account. The file is gitignored, so it stays on your machine.

   ```properties
   ADMIN_EMAIL=admin@example.com
   ADMIN_PASSWORD=change-me
   ```

   The backend creates this admin at startup if it does not exist yet. Without these values there is no admin user.

2. Start the backend from the repository root:

   ```bash
   mvn spring-boot:run
   ```

   - The API runs on http://localhost:8080 (base path `/api/v1`).
   - `compose.yaml` starts PostgreSQL 18 on port 5432 automatically.
   - Liquibase creates the database schema.

To use a different database, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. The local defaults are in `app/src/main/resources/application.yml`.

### Starting the backend from IntelliJ IDEA

1. Open the repository root and reload the Maven project (Maven tool window → **Reload All Maven Projects**). IntelliJ then shows the modules `event`, `notification` and `app`.
2. Run the shared run configuration **Backend (app)** (stored in `.run/`). It runs `com.notificationapp.NotificationAppApplication` from the `app` module, with the repository root as the working directory.

The working directory matters: the backend reads `.env` and `compose.yaml` from it. A run configuration that uses another module (for example the old root module `notification-app`, which no longer has sources) or another working directory fails at startup or starts without an admin.

## 2. Start the frontend

The backend must be running first.

1. Install the dependencies (first time only):

   ```bash
   cd frontend
   npx -y npm@11 install
   ```

   This uses npm 11 through `npx`. npm 10.9 fails on this project with an `edgesOut` error.

2. Start the dev server:

   ```bash
   npm start
   ```

3. Open http://localhost:4200.

The dev server forwards every `/api` request to the backend on http://localhost:8080 (see `frontend/proxy.conf.json`), so no CORS setup is needed.

## 3. Log in

- **Admin:** log in with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` from your `.env`. Admins can list and delete users, manage every user's alerts, and create events.
- **User:** sign up at http://localhost:4200/register. New accounts get the `USER` role and can manage only their own alerts.

## Running the tests

| What | Command | Where |
|---|---|---|
| Backend unit and Web MVC tests (no Docker needed) | `mvn test` | repository root |
| All backend tests, including integration tests (needs Docker) | `mvn verify` | repository root |
| Frontend unit tests | `npm test -- --watch=false` | `frontend/` |
| Frontend production build | `npm run build` | `frontend/` |

- Unit and Web MVC tests are named `*Test` and run with Surefire. Integration tests are named `*IT` and run with Failsafe during `mvn verify` only. They start their own PostgreSQL container with Testcontainers, so they do not touch the Compose database.
- Unit tests live in the module of the class they test. Web MVC and integration tests live in `app`.

Run the tests of one module (`event`, `notification` or `app`):

```bash
mvn test -pl notification -am
```

Run a single unit or Web MVC test class (add `#methodName` for one method):

```bash
mvn test -pl app -am -Dtest=UserServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Run a single integration test class:

```bash
mvn verify -pl app -am -Dit.test=OutboxProcessingIT -Dtest=NoMatch -Dsurefire.failIfNoSpecifiedTests=false -Dfailsafe.failIfNoSpecifiedTests=false
```

In IntelliJ, run a test class with the gutter icon next to it. Unit and Web MVC tests need no Docker; integration tests need Docker running.

## Trying out the notification flow

1. Log in as a user and create an alert for an event type, for example natural disasters, with the email or Slack channel.
2. Log in as the admin and create an event of that type.
3. Within a few seconds the backend log shows a line such as `Notification [email] to <user's email> — …`. The email and Slack senders only log for now; nothing is actually sent.

## Backend modules

| Module | Responsibility |
|---|---|
| `event` | Receives and stores events (`/api/v1/events`) and publishes `EventCreated`. Depends on no other module. |
| `notification` | Writes the outbox for each new event, matches alerts, creates delivery jobs, and sends them through the email and Slack senders (which only log for now). Depends on `event`. |
| `app` | The runnable application: users, alerts, security, the database migrations and configuration. Depends on `event` and `notification`. |

Run all Maven commands from the repository root. `mvn spring-boot:run` starts only the `app` module.

## Project documentation

- `CLAUDE.md`: architecture, conventions and commands.
- `docs/stage-n/`: plans, decisions, progress and the conversation log for each project stage.
- `docs/architecture/`: the architecture diagram.
