# Notification App

Users set up alerts for important events (breaking news and natural disasters) and choose to be notified by email or Slack.

The repository has two applications:

| Folder | Application | Stack |
|---|---|---|
| `/` (root) | Backend REST API | Java 21, Spring Boot 4.2, PostgreSQL, Liquibase, Spring Security (HTTP Basic) |
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

To use a different database, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. The local defaults are in `src/main/resources/application.yml`.

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

## Project documentation

- `CLAUDE.md`: architecture, conventions and commands.
- `docs/stage-n/`: plans, decisions, progress and the conversation log for each project stage.
- `docs/architecture/`: the architecture diagram.
