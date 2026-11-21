---
name: java-development
description: Java and Spring Boot coding standards for this project, covering principles, Spring conventions, code quality and architecture. Use when implementing or refactoring Java code.
---

# Java Development Standards

## Principles

- Follow SOLID, DRY, KISS and clean code.
- Keep concerns separate.
- Prefer composition to unnecessary inheritance.

## Spring Boot

- **Injection:** use constructor injection only.
- **Controllers:** keep them thin. They accept and return DTOs, never entities.
- **Business logic:** put it in services.
- **Repositories:** give each one a clear responsibility.
- **Transactions:** make transaction boundaries explicit.
- **Exceptions:** handle them consistently, through the project's error-handling approach.

## Code quality

- Use meaningful names and small, focused methods.
- Avoid unnecessary abstractions, duplicated logic and magic values.
- Do not optimize prematurely.
- Prefer immutable types, such as records, where they fit.

## Architecture

- Follow the package structure and REST conventions in `CLAUDE.md`.
- Reuse established patterns.
- Do not make architectural changes without a stated reason.
- Do not add dependencies that are not needed.
