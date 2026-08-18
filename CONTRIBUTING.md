# Contributing

Follow this guide for code, configuration, tests, and documentation.

## Writing and Project Language

Read `CONTEXT.md` before work and use its project language. Follow the writing rules and document pointers in `AGENTS.md`. Use ASD-STE100 Simplified Technical English in documentation, issue and pull-request text, commit messages, code comments, logs, validation messages, and error messages.

## Read the Design

Start at `docs/design/README.md`. Read each document that controls the area you will change. A change is ready only when it agrees with the applicable design and architecture decision records.

## GitHub Workflow

For specifications, issues, branches, and pull requests, follow `docs/agents/issue-tracker.md`. That document is the only source for the GitHub workflow.

## Java and Spring

- Use Java 17 language features when they make the code clear.
- Keep the calculation model independent of Spring, HTTP, JPA, logging, and metrics.
- Use immutable values at boundaries between application areas.
- Prefer Java records for immutable request, response, configuration, and calculation values.
- Return unmodifiable collections across application-area boundaries.
- The producer owns each collection that crosses an application-area boundary. It must make the collection unmodifiable and must not retain a mutable reference. A receiving record stores the supplied collection without making another copy.
- Confine mutable transport collections to the HTTP adapter. Map them to unmodifiable application values before they cross into another application area. Do not retain the transport value after mapping.
- Declare JPA entities and repositories in the persistence package. They can be public when the implementation in the owning application area needs cross-package access. Do not return them through the area's service interface. Map entities to calculation values and create unmodifiable collections before values cross that interface.
- Inject a Spring service through its interface.
- Declare access explicitly when it communicates an important interface or implementation limit. Interface methods can omit the redundant `public` modifier. Fields are private unless a supported interface requires wider access.
- Use `private final` for a field whose reference must not change after construction. Use `final` for a local variable when it makes an important invariant clear.

### Packages and Dependencies

- Group types by the durable responsibility that owns them.
- Create a subpackage for several related types or for an important adapter seam.
- Name packages for project concepts and responsibilities that remain stable when a library or storage technology changes.
- Keep transport DTOs and exceptions inside their owning area.
- Group each stored concept's entity and repository in one package under `persistence`.
- Mirror production packages in tests by default and test behavior through the area's supported interface. A test for an internal adapter can stay in the adapter package when it needs package access to test data constructors or helpers.

Use these dependency directions:

```text
calculation.http -> calculation + domain
calculation -> taxrule + domain + metrics
taxrule -> taxrule.persistence + domain
taxrule.persistence -> domain
metrics -> taxrule
domain -> JDK + compile-time Lombok annotations
```

### Spring Services

Every class annotated with Spring `@Service` has a matching interface:

```text
XService
XServiceImpl
```

For example, `CalculationService` is the interface and `CalculationServiceImpl` is its `@Service` implementation. Consumers inject `CalculationService`.

This convention applies to Spring services. It does not rename controllers, Spring Data repositories, configuration classes, the pure calculator, or provider adapters that are not Spring services.

### Lombok

Use the smallest Lombok annotation set that removes mechanical code and keeps behavior clear. The evidence for these rules is in `docs/research/lombok-guidance.md`.

- Use `@Slf4j` on a class that writes log messages.
- Use `@NonNull` when an immediate null failure is the complete required behavior. The domain area can use this compile-time annotation.
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
- Each feature change owns the logs required by its behavior. Add or change the event in the same issue as the feature.
- Use `DEBUG` for diagnostic detail and `INFO` for successful calculation completion. At an HTTP exception boundary, use `WARN` for a handled `4xx` response and `ERROR` for a handled `5xx` response. Use `WARN` for a failure that the application suppresses while it continues.
- Use each level only for its operational meaning. Do not use `TRACE`.
- Use parameterized messages for variable values.
- Log each event or exception once at the boundary that knows its final outcome.
- Safe context includes city code, Vehicle Type code, Passage count, City Local Time calculation date, Tax Rule Set effective date, result counts, and a stable failure category.
- Keep complete request bodies, raw Passage timestamps, Tax Amounts, authorization values, database credentials, SQL, and expected exception messages out of logs.
- Omit stack traces from handled `4xx` logs. Include the stack trace when an internal failure is suppressed or the exception handler returns a `5xx` response.
- Keep stack traces and other internal failure data out of HTTP responses.
- Test code does not write log messages. Tests do not assert log output.

### Metrics

Follow the metric inventory and exposure rules in `docs/design/operations.md`. Follow the instrumentation boundary in `docs/design/architecture.md`.

- Each feature change owns any metric required by its behavior. Add or change the meter in the same issue as the feature.
- Start with the operational question that the metric must answer.
- Prefer a standard Spring Boot meter when it answers the question.
- Add a custom meter only when the standard meters do not supply the required signal.
- Use a timer for duration, a distribution summary for a quantity in one event, and a counter for a total that has no existing count.
- Use bounded, low-cardinality tags. Keep request values, identifiers, timestamps, monetary amounts, free text, and exception messages out of tags.
- Do not add a counter when a timer or distribution summary already supplies the required count.
- Keep metric collection independent of the calculation result. A metric failure must not change a Congestion Tax Calculation.
- Do not add automated tests for metrics.

### Comments and Javadoc

Use comments and Javadoc sparingly. Explain only a non-obvious reason, assignment ambiguity, important boundary, or public contract that names and types cannot make clear. Do not restate code. Private helpers normally need no Javadoc.

### Invalid State

- Reject invalid transport data at the API boundary.
- Construct calculation values in a valid state.
- Use empty collections instead of null collections.
- Use `Optional` for a result that can validly be absent. Do not use it for fields or method parameters.
- Use explicit validation when an error needs a project-specific message. Use Lombok `@NonNull` only when its immediate null failure is the complete required behavior.

### Exceptions

Choose the exception type from the failed rule and the caller response. The choice is complete when no caller reads an exception message to select a response.

- Use a standard Java exception for a basic value-construction failure, such as a required null value or a scalar value outside its allowed range.
- Use a custom exception for a failure detected by application or domain logic, including an invalid relation between otherwise valid values.
- Use a specific custom exception for each condition that a caller must identify separately.
- Extend the closest standard Java exception when it accurately describes the custom failure.
- Put each custom exception in the `exception` subpackage of the area that owns the failure.
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
- Require the production database URL, user, password, server port, root log level, and application log level from environment variables. Do not give these production values defaults.
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
- Name full integration test classes with the suffix `ITest`. Name unit tests and small Spring context test classes with the suffix `Test`.
- Run regular tests with `./mvnw test`.
- Run integration tests without regular tests with `./mvnw verify -Dskip.regular.tests=true`.
- Run all tests and build checks with `./mvnw verify`.
- Use `./mvnw verify -DskipITs` when full build checks are required without integration tests.

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
