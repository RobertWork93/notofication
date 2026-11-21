# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current phase: implementation (started 2026-11-21)

Quoted project description: "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later."
The project moved from planning to early implementation (see D-007 in `docs/decision-log.md`). The Spring Boot skeleton exists. Feature code is not written yet. The requirements in `docs/plan.md` and the decisions in `docs/decision-log.md` still govern what gets built.

## Conversation History

Maintain a chronological, append-only conversation log in `docs/prompts.md`.

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
- Create `docs/prompts.md` if it doesn't exist.
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

Commands (run from the repository root):
- Build and run all tests: `mvn verify`
- Compile only: `mvn compile`
- Run the app: `mvn spring-boot:run` (no database is configured yet)
- Run a single test class: `mvn test -Dtest=ClassName`
- Run a single test method: `mvn test -Dtest=ClassName#methodName`

Still open:
- Queue implementation: asynchronous delivery is confirmed (D-004), but the technology is not chosen.

## Architecture

Single Maven module, not modular for now. Code is split into layers by package under `com.notificationapp`:

- `controller`: HTTP endpoints. Calls services only.
- `service`: business logic. Calls repositories and other services.
- `repository`: persistence access. Not used yet.
- `NotificationAppApplication` (root package): Spring Boot entry point.

Dependencies point one way: controller → service → repository. Controllers must not call repositories. Tests mirror the main package layout.

Modularising (for example into `app`, `alert`, `event`, `notification` Maven modules) is deferred. The `docs/` planning files describe the intended domains.
