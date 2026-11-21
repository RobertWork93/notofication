---
name: myproject-core-review
description: Evidence-based review checklist for Java and Spring Boot changes, covering implementation, tests, regressions and conventions, with a verdict. Use before merging or approving a change.
---

# Code Review Checklist

## Implementation

- Is the business logic correct?
- Are responsibilities separated properly?
- Are exceptions handled correctly?
- Are there security issues?
- Is anything more complex than it needs to be?

## Testing

See the `spring-boot-testing` skill.

- Do relevant services have meaningful unit tests?
- Do controllers have MockMvc tests?
- Do infrastructure-dependent flows have Testcontainers tests where needed?
- Are failure scenarios covered?
- Are the assertions meaningful?
- Are any tests duplicated or meaningless?

## Regression

- Were existing tests kept?
- Do the older tests still pass? Run them (`mvn verify`) rather than trusting a claim.
- Are API contracts unchanged?
- Are database changes compatible?

## Conventions

- Does the code follow `CLAUDE.md`?
- Is the package structure kept?
- Are established project patterns respected?

## Verdict

Return one of: **APPROVED**, **APPROVED WITH WARNINGS**, **REJECTED**.

Every finding must include evidence and a file reference.
