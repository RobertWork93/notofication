---
name: tdd-workflow
description: Strict Red-Green-Refactor workflow. Use whenever you implement a feature, fix a bug or change existing behavior.
---

# TDD Workflow

Follow these three phases for every change to application behavior.

## 1. RED

1. Understand the requirement from the latest `docs/stage-n/plan.md` and define the expected behavior.
2. Write meaningful tests first (see the `spring-boot-testing` skill).
3. Run them before writing any production code (`mvn test -Dtest=ClassName`).
4. Confirm they fail for the expected reason: the behavior is missing, not the test setup broken.

## 2. GREEN

1. Write the minimum production code that makes the tests pass.
2. Run the tests again and confirm they pass.
3. Never weaken a test to fit the implementation.

## 3. REFACTOR

1. Improve readability and remove duplication, within the existing architecture.
2. Run the tests again, then the full suite (`mvn verify`).

## Rules

- Never skip a phase silently.
- If RED cannot be verified, report that limitation.
