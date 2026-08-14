# Contributing

Follow this guide for code, configuration, tests, and documentation.

## Writing and Project Language

Read `CONTEXT.md` before work and use its project language. Follow the writing rules and document pointers in `AGENTS.md`. Use ASD-STE100 Simplified Technical English in documentation, issue and pull-request text, commit messages, code comments, logs, validation messages, and error messages.

## Read the Design

Start at `docs/design/README.md`. Read each document that controls the area you will change. A change is ready only when it agrees with the applicable design and architecture decision records.

## GitHub Workflow

For specifications, issues, branches, pull requests, and time records, follow `docs/agents/issue-tracker.md`. That document is the only source for the GitHub workflow.

## Java and Spring

- Use Java 17 language features when they make the code clear.
- Keep the calculation model independent of Spring, HTTP, JPA, logging, and metrics.
- Use immutable values at boundaries between application areas.
- Prefer Java records for immutable request, response, configuration, and calculation values.
- Make defensive copies when a record receives a mutable collection.
- Keep JPA entities inside the persistence area and map them to immutable calculation values before data leaves that area.
- Inject a Spring service through its interface.

### Spring Services

Every class annotated with Spring `@Service` has a matching interface:

```text
XService
XServiceImpl
```

For example, `CongestionTaxCalculationService` is the interface and `CongestionTaxCalculationServiceImpl` is its `@Service` implementation. Consumers inject `CongestionTaxCalculationService`.

This convention applies to Spring services. It does not rename controllers, Spring Data repositories, configuration classes, the pure calculator, or provider adapters that are not Spring services.

### Lombok

Use the smallest Lombok annotation set that removes mechanical code and keeps behavior clear. The evidence for these rules is in `docs/research/lombok-guidance.md`.

- Use `@Slf4j` on a class that writes log messages.
- Prefer `@RequiredArgsConstructor` when a Spring-managed class only needs a constructor for required final dependencies. A single generated constructor does not need `@Autowired`.
- Write an explicit constructor when it validates, converts, or makes a non-obvious choice.
- Use `@Getter` only for JPA entity values that callers must read.
- Use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` when JPA requires a protected no-argument constructor.
- Use narrow setters only when direct entity mutation is necessary. Prefer named methods when a state change has a rule.
- Use `@Builder` only for a value with many independent optional inputs. Put it on the approved constructor or factory and declare required builder defaults.
- Use `@AllArgsConstructor` only when every field is intentionally part of construction.
- Keep checked exceptions visible. Do not use `@SneakyThrows` in application code.
- Keep entity identity explicit. Do not use `@Data`, `@Value`, or default `@EqualsAndHashCode` on a JPA entity.
- Use normal object identity for a JPA entity unless an approved stable natural key requires explicit equality.
- Keep entity text output explicit. If Lombok generates it, include only approved safe scalar fields. Exclude associations, mutable values, and sensitive values.
- Use a protected normal JPA constructor. Do not use a forced no-argument constructor as a routine solution.
- Review an experimental Lombok annotation before use. It is not a project default.

Let Spring Boot manage the Lombok version. Configure Lombok as a compile-time annotation processor and a provided dependency. It is not an application runtime dependency.

### Local Variable Types

Use `var` when the initializer makes the type clear and the explicit type only repeats it. Use an explicit type when it communicates important project meaning or the initializer hides the result type. Readability controls the choice.

### Formatting

Use Spotless Maven Plugin 3.9.0 with Palantir Java Format. Bind the formatting check to Maven verification and provide the Spotless apply goal for automatic correction. The formatter controls layout. Review controls names, boundaries, and design.

### Logging

- Use `@Slf4j` instead of declaring an SLF4J logger manually.
- Select `INFO`, `WARN`, or `ERROR` from the event's operational meaning. Use diagnostic levels only for diagnostic detail.
- The completed application has at least one natural use of `INFO`, `WARN`, `ERROR`, and `DEBUG`. Do not create an artificial event only to satisfy this list.
- Use parameterized messages for variable values.
- Keep normal successful calculations quiet unless diagnostic logging is active.
- Keep database passwords, authorization values, and complete request bodies out of logs.
- Include safe context, such as city code and calculation date, when it helps diagnose an error.
- Keep stack traces in internal error logs and out of HTTP responses.
- Use `@Slf4j` in a test class when scenario progress or failure context helps the person who runs the tests.
- Keep test log messages parameterized. Log meaningful scenario boundaries and useful failure context, not each assertion.

### Metrics

Follow the metric inventory and exposure rules in `docs/design/operations.md`. Follow the instrumentation boundary in `docs/design/architecture.md`.

- Start with the operational question that the metric must answer.
- Prefer a standard Spring Boot meter when it answers the question.
- Add a custom meter only when the standard meters do not supply the required signal.
- Use a timer for duration, a distribution summary for a quantity in one event, and a counter for a total that has no existing count.
- Use bounded, low-cardinality tags. Keep request values, identifiers, timestamps, monetary amounts, free text, and exception messages out of tags.
- Do not add a counter when a timer or distribution summary already supplies the required count.
- Keep metric collection independent of the calculation result. A metric failure must not change a Congestion Tax Calculation.
- Test the required meter name, tag set, and boundaries through the highest practical public seam. Do not compare a complete metrics scrape.

### Comments and Javadoc

Use comments and Javadoc sparingly. Explain only a non-obvious reason, assignment ambiguity, important boundary, or public contract that names and types cannot make clear. Do not restate code. Private helpers normally need no Javadoc.

### Invalid State

- Reject invalid transport data at the API boundary.
- Construct calculation values in a valid state.
- Use empty collections instead of null collections.
- Use `Optional` for a result that can validly be absent. Do not use it for fields or method parameters.
- Use explicit validation when an error needs a project-specific message. Use Lombok `@NonNull` only when its immediate null failure is the complete required behavior.

### Exceptions

- Keep an exception type near the package where it is primarily used.
- Use specific application exceptions for expected conditions.
- Translate API exceptions to Problem Details in the controller layer.
- Let unexpected exceptions reach the central handler. It logs the failure and returns a safe response.
- Catch a broad exception only at a boundary where the code can add useful context or translate it. Every caught exception must have an explicit outcome.

## Configuration

Follow the profile design in `docs/design/operations.md`.

```text
src/main/resources/
├── application.yaml
├── application-dev.yaml
└── application-prod.yaml

src/test/resources/
├── application-test.yaml
└── application-itest.yaml
```

- Put static settings shared by normal runtime profiles in `application.yaml`.
- Use `dev` as the default profile and keep its local settings consistent with Docker Compose.
- Select `prod` explicitly with `SPRING_PROFILES_ACTIVE=prod`.
- Require the production database URL, user, and password from environment variables. Give safe defaults only to non-sensitive values such as the server port and root log level.
- Use `test` for a small Spring test context without PostgreSQL.
- Use `itest` for full Spring Boot, HTTP, JPA, Flyway, and PostgreSQL Testcontainers tests.
- Keep the Testcontainers JDBC connection in `application-itest.yaml`.
- Use a programmatic dynamic property only when a declarative connection cannot supply the required value.
- Keep `src/test/resources` free of an unqualified `application.yaml`; tests select their named profile explicitly.
- Use validated `@ConfigurationProperties` records for grouped application-specific settings. Keep each type near the package that consumes it and register it through configuration-properties scanning.
- Use Spring Boot's property types for standard framework configuration such as the datasource.
- Commit development-only Docker Compose credentials and an `.env.example` that contains variable names but no production values.
- Keep local `.env` files ignored and keep production secrets in environment variables.

## Tests

Follow `docs/design/testing.md` for test levels, seams, and required behavior.

- Pure unit and Mockito tests start no Spring context and load no profile.
- Tests that load Spring select `test` or `itest` explicitly.
- Prefer parameterized tests when many inputs prove the same rule.
- Test public behavior. Do not bind a test to a private method, Hibernate implementation detail, or exact generated SQL text.
- Cover each distinct successful path, common path, boundary, and error path. Add no repeated case only to increase a coverage number.

## Documentation

Update documentation in the same change when behavior, configuration, the HTTP contract, stored content, project language, or an important design decision changes.

- Update `CONTEXT.md` only for project language.
- Update `questions.md` when an assignment ambiguity or working assumption changes.
- Update the applicable document under `docs/design/` when implementation changes an agreed design.
- Add or change an architecture decision record only for a costly, non-obvious trade-off.
- Keep the root `README.md` focused on understanding, running, testing, and calling the application. Link it to every design document and architecture decision record.

## Required Verification

Run focused tests during development. Run `./mvnw verify` before work is declared complete.

Work is complete when focused verification and `./mvnw verify` pass, the changed behavior matches its issue acceptance criteria, and every affected document is current.
