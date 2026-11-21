---
name: spring-boot-testing
description: Testing standards for this Spring Boot project, covering JUnit 5 and Mockito unit tests, Web MVC tests and Testcontainers integration tests. Use when writing or reviewing tests.
---

# Spring Boot Testing Standards

The **Testing Conventions in `CLAUDE.md`** are mandatory and take precedence over this skill. They define the folder structure, naming and Maven execution. This skill adds guidance on what each kind of test should check.

## Which test for which component

| Component | Test type | Location and name |
|---|---|---|
| Services, helpers, utilities, business logic | Unit tests: JUnit 5 and Mockito, no Spring context | `unit/service/`, `unit/mapper/`, `*Test` |
| Controllers | `@WebMvcTest` with MockMvc, services mocked with `@MockitoBean` | `web/controller/`, `*Test` |
| Complete API flows on real infrastructure | Extend `support/AbstractIntegrationTest`: `RANDOM_PORT`, real HTTP with `RestTestClient`, Testcontainers PostgreSQL | `integration/api/`, `*IT` |
| Schema and repository behavior | Extend `AbstractIntegrationTest`, or use `@DataJpaTest` where it fits | `integration/repository/`, `*IT` |
| Configuration, DTOs | No routine tests | |

Shared test code (container configuration, base classes, `TestDataFactory`) lives in `support/`, never next to the tests.

## Unit tests

- Keep them lightweight, with no Spring context.
- Do not write unit tests for controllers, configuration or DTOs.

## Web MVC tests

Verify each of these, for both successful and failing requests:

- HTTP status;
- request validation;
- serialization and response content;
- exception handling, including Problem Details error bodies;
- security behavior, where security applies.

## Integration tests

- Use them when behavior depends on real infrastructure, such as PostgreSQL (the project's database).
- Use production-compatible infrastructure. Do not mock away important database behavior.
- They run under Failsafe on `mvn verify` and need Docker.

## Quality

Every test needs a clear purpose, a `should...` name and meaningful assertions.

Avoid:

- redundant tests;
- excessive mocking;
- arbitrary sleeps;
- testing private methods;
- tests that exist only for coverage.
