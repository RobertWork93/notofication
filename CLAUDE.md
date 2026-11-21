# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current phase: planning

Quoted project description: "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later."
The project is in discovery and architecture planning for an alerts feature. No application code is written!

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

Still open:
- Build tool: Maven or Gradle (not chosen).
- Queue implementation: asynchronous delivery is confirmed (D-004), but the technology is not chosen.

Once the build tool and project files exist, replace this section's open items with the real build, test, and run commands, including how to run a single test. Then describe the high-level architecture, covering only what takes reading several files to understand.
