# Operations Design

Docker Compose runs PostgreSQL 18.4 as a separate runtime service. A named volume retains development data. `docker compose down -v` removes that data when a clean start is necessary. Environment variables configure the database URL, user, and password. Flyway creates the schema and the current initial seed data.

The Maven project compiles for Java 17. The Maven Wrapper uses the active compatible JDK. Spring Boot Actuator exposes only `/actuator/health` and `/actuator/prometheus` over HTTP. Health includes database status. It shows component names and statuses and hides component details. `/actuator/info` and `/actuator/metrics` are not available over HTTP.

The repository does not yet contain a GitHub Actions workflow, deployment workflow, or application container image. A later delivery can add a workflow that uses Java 17 and runs `./mvnw verify` for pull requests and pushes to `main`.

## Configuration Profiles

The application uses these files:

```text
src/main/resources/
├── application.yaml
├── application-dev.yaml
└── application-prod.yaml

src/test/resources/
├── application-test.yaml
└── application-itest.yaml
```

`application.yaml` contains settings shared by normal runtime profiles and makes `dev` the default profile. Production explicitly sets `SPRING_PROFILES_ACTIVE=prod`. The `prod` file reads environment-dependent and sensitive values from environment variables. The `dev` file contains convenient local values that match Docker Compose. Static values shared by both profiles stay in the common file.

The development datasource is declarative and matches Docker Compose:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/congestion_tax
    username: congestion_tax
    password: congestion_tax
```

These values are local development credentials. The production profile requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SERVER_PORT`, `LOGGING_LEVEL_ROOT`, and `LOGGING_LEVEL_APPLICATION`. It provides no defaults for these values. Common static settings stay in `application.yaml`. The committed `.env.example` lists these variable names without production values.

Pure unit and Mockito tests start no Spring context and load no profile. Tests that start a small Spring context without PostgreSQL use `test`. Full Spring Boot, HTTP, JPA, Flyway, and PostgreSQL Testcontainers tests use `itest`. Tests that load Spring select their profile explicitly.

The `test` and `itest` profiles enable `DEBUG` logging for the application package. They keep framework logging at a quieter level. Test code does not write its own log messages.

The `itest` datasource uses a declarative Testcontainers JDBC URL:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:18.4:///congestion_tax
    username: test
    password: test
```

This URL starts the temporary PostgreSQL container without a Java dynamic-property method. Use programmatic dynamic properties only when an integration cannot use a declarative connection.

There is no unqualified `src/test/resources/application.yaml`, because it would override main configuration for all tests.

## Logging

The common profile sets the root level to `INFO`. The `dev`, `test`, and `itest` profiles enable `DEBUG` for `io.github.igrgin.congestiontax`. Production reads its root and application-package levels from separate environment variables. This lets an operator enable application diagnostics without enabling verbose framework diagnostics.

The application uses plain parameterized SLF4J messages. It does not add structured JSON output, a correlation identifier, or `TRACE` events. `DEBUG` records diagnostic decisions. `INFO` records successful calculation completion. At the HTTP exception boundary, `WARN` records a handled `4xx` response and `ERROR` records a handled `5xx` response. `WARN` also records a failure that the application suppresses while it continues.

Each feature issue owns the events required by that feature. One boundary logs each event or exception. `CONTRIBUTING.md` defines the safe context and prohibited data.

## Metrics

Spring Boot Actuator and Micrometer supply standard JVM, process, HTTP, and database-pool metrics. The Prometheus registry publishes them at `/actuator/prometheus`. The current application does not run a Prometheus server and does not supply dashboards, alerts, or deployment configuration.

A deployment must restrict access to the scrape endpoint at its network boundary because application authentication is outside the project scope.

The application adds these custom meters:

```text
congestion.tax.calculation
congestion.tax.calculation.passages
```

The timer surrounds the Calculation Service operation. It records these bounded `outcome` tag values:

```text
success
rejected
failed
```

The configured Prometheus histogram supports aggregate latency analysis.

The timer does not use Tax Amounts, Passage timestamps, City codes, Vehicle Type codes, exception messages, or other unbounded values as tags. Standard HTTP metrics supply request count, duration, outcome, and status.

The `congestion.tax.calculation.passages` distribution records the Passage count once for each request that passes HTTP validation and reaches the Calculation Service. It records the count even when later lookup, stored-content, or calculation behavior fails. It has no tags and publishes these boundaries:

```text
1
10
100
1000
10000
```

A metrics failure cannot change the calculation result. `CalculationMetrics` catches failures that occur when it records the Passage count or starts or stops the timer. It logs each suppressed failure at `WARN`.

Each feature issue owns any metric required by its behavior. It adds a custom meter only when standard meters and existing custom meters cannot answer the operational question.

## Implementation Time Plan

Planning time is outside the assignment's six-hour implementation limit. The initial implementation budget is:

| Work area | Minutes |
|---|---:|
| Project foundation, dependencies, Docker Compose, logging, metrics, and CI | 35 |
| Database schema and Flyway | 60 |
| Calculation code, parameterized tests, and focused metrics | 80 |
| JPA loading and application coordination | 50 |
| HTTP API, validation, errors, and OpenAPI | 45 |
| Full-path and error integration tests | 45 |
| README, questions, design review, and final verification | 35 |
| Reserve | 10 |
| **Total** | **360** |

The plan includes all agreed behavior. During implementation, simplify implementation details when this saves time without changing required behavior. Target the six-hour limit. If actual work runs over, record the real time and explain the overrun honestly.

## Caching Decision

The current implementation has no shared cache and does not cache HTTP responses. The Tax Rule Service loads the required stored content for each calculation.

Complete requests are likely to be unique and can become stale when stored content changes. Add a shared cache only after measurements show a need. Measure database time, query count, p50, p95, and p99 response time, requests per second, connection-pool wait, CPU, memory, repeated rule use, expected hit ratio, and stale-data behavior. If justified, cache immutable Tax Rule Sets by city and effective date instead of complete responses.

The first delivery does not include authentication, rate limiting, custom CORS behavior, an administration endpoint, a Prometheus server, metric dashboards, alerts, or production deployment because the assignment does not define those requirements.

## Planned README requirements

The planned root README will explain the assignment scope, architecture, prerequisites, database startup and reset, application startup, profiles, logging, health, metrics, tests, API examples, error format, supplied-data result, known limits, time spent, and additional work. It will link to every design document, architecture decision record, and `questions.md`.
