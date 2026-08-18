# Contributing

Use this guide for code, configuration, tests, and documentation.

## Before a change

1. Read `CONTEXT.md` and use the project terms.
2. Read the document named for the part of the application that will change.
3. Follow `docs/agents/issue-tracker.md` for GitHub work.
4. Confirm that all declared blocking issues are closed.

The change is ready when its behavior, tests, and documentation agree.

## Code boundaries

- Use Java 17 language features when they make the code clear.
- Keep `domain` independent of Spring, HTTP, JPA, logging, metrics, and the system clock.
- Keep HTTP DTOs inside `calculation.http` and map them to application values at the boundary.
- Keep JPA entities and repositories inside `taxrule.persistence`.
- Map persistence rows to immutable values before they leave the tax rule service.
- Use records for immutable request, response, configuration, and calculation values when they fit.
- Give each Spring service an interface named `XService` and an implementation named `XServiceImpl`.
- Return unmodifiable collections across application boundaries when later mutation can change behavior.

Use these dependency directions:

```text
calculation.http -> calculation + domain
calculation -> taxrule + domain + metrics
taxrule -> taxrule.persistence + domain
taxrule.persistence -> domain
metrics -> taxrule
domain -> JDK + compile-time Lombok annotations
```

## Persistence

Flyway owns the schema and initial content. Hibernate validates mapped objects. It does not update the schema.

Use scalar foreign-key fields in JPA entities and explicit repository reads. Add a JPA association only when the application needs object navigation, an association fetch plan, or cascade behavior.

Use Spring Data method queries for simple reads. Use JPQL or native SQL when it makes an important query clearer. Verify PostgreSQL-specific behavior with an integration test.

## Values and failures

- Reject invalid transport data at the HTTP boundary.
- Construct domain values in a valid state.
- Use empty collections instead of null collections.
- Use `Optional` for a result that can be absent, not for fields or parameters.
- Use a standard Java exception for a basic value error.
- Use a specific custom exception when a caller must identify the failure.
- Translate failures at the boundary that owns the caller contract.
- Return safe Problem Details from the HTTP boundary.

## Logging and metrics

Follow `docs/development.md` for event levels, safe log data, and metric names.

Log an error once at the boundary that knows its final result. Keep request bodies, raw passage times, amounts, credentials, SQL, and exception details out of HTTP responses.

Metric failures must not change a calculation result. Use bounded tags and do not put request values in tags.

## Tests

Test public behavior. Use parameterized tests when several inputs prove the same rule.

- Name regular tests `*Test`.
- Name integration tests `*ITest`.
- Keep pure tests free of Spring profiles.
- Use `test` for a small Spring context.
- Use `itest` for full Spring and PostgreSQL tests.

Run focused tests while you work. Run `./mvnw verify` before the change is complete.

## Documentation

Write direct explanations of the current application. Keep one source for each fact and link to it from other documents.

- Update `CONTEXT.md` only for project terms.
- Update `docs/calculation.md` for calculation behavior.
- Update `docs/api.md` for the HTTP contract.
- Update `docs/architecture.md` for application boundaries.
- Update `docs/persistence.md` for stored rules and data access.
- Update `docs/development.md` for tests and operations.
- Keep `README.md` as the short path to understand, run, call, and inspect the application.
- Keep `questions.md` as questions only.

Use Mermaid for architecture, flow, and entity-relationship diagrams.
