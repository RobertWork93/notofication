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