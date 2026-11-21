---
name: spring-boot-testing
description: Testing standards for this Spring Boot project, covering JUnit 5 and Mockito unit tests, MockMvc controller tests and Testcontainers integration tests. Use when writing or reviewing tests.
---

# Spring Boot Testing Standards

## Which test for which component

| Component | Test type |
|---|---|
| Services, helpers, utilities, business logic | Unit tests: JUnit 5 and Mockito, no Spring context |
| Controllers | MockMvc with `@WebMvcTest` |
| Flows that depend on real infrastructure | Testcontainers with `@SpringBootTest` and `@AutoConfigureMockMvc` |
| Configuration, DTOs | No routine tests |

## Unit tests

- Keep them lightweight, with no Spring context.
- Do not write unit tests for controllers, configuration or DTOs.

## MockMvc tests

Verify each of these, for both successful and failing requests:

- HTTP status;
- request validation;
- serialization and response content;
- exception handling, including Problem Details error bodies;
- security behavior, where security applies.

## Testcontainers tests

- Use them when behavior depends on real infrastructure, such as PostgreSQL (the project's database).
- Use production-compatible infrastructure. Do not mock away important database behavior.
- Testcontainers is not in `pom.xml` yet. Ask the user before adding it.

## Quality

Every test needs a clear purpose and meaningful assertions.

Avoid:

- redundant tests;
- excessive mocking;
- arbitrary sleeps;
- testing private methods;
- tests that exist only for coverage.
