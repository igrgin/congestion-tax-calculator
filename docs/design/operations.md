# Operations Design

Docker Compose runs PostgreSQL 18.4 as a separate runtime service. A named volume retains development data. `docker compose down -v` removes that data when a clean start is necessary. Environment variables configure the database URL, user, and password. Flyway creates the schema and the initial rule data supplied by the assignment.

The Maven Wrapper builds and runs an executable Spring Boot JAR with Java 17. Springdoc exposes OpenAPI JSON and Swagger UI. Spring Boot Actuator exposes only `/actuator/health` and `/actuator/prometheus` over HTTP. Health includes database status. It shows component names and statuses and hides component details. `/actuator/info` and `/actuator/metrics` are not available over HTTP.

One GitHub Actions workflow uses Java 17 and runs `./mvnw verify` for pushes and pull requests. This runs unit and Testcontainers integration tests. The first delivery has no deployment workflow or application container image.

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

These values are local development credentials. The production profile requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SERVER_PORT`, and `LOGGING_LEVEL_ROOT`. It provides no defaults for these values. Common static settings stay in `application.yaml`. The committed `.env.example` lists these variable names without production values.

Pure unit and Mockito tests start no Spring context and load no profile. Tests that start a small Spring context without PostgreSQL use `test`. Full Spring Boot, HTTP, JPA, Flyway, and PostgreSQL Testcontainers tests use `itest`. Tests that load Spring select their profile explicitly.

The `test` and `itest` profiles enable `DEBUG` logging for the application package when diagnostic application messages are useful. They keep framework logging at a quieter level. Test classes can use `@Slf4j` for meaningful scenario and failure context.

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

Application-specific settings use validated `@ConfigurationProperties` records kept near their consuming package and registered through configuration-properties scanning. Standard Spring Boot settings, such as datasource properties, use Spring Boot's existing property types.

## Metrics

Spring Boot Actuator and Micrometer supply standard JVM, process, HTTP, and database-pool metrics. The Prometheus registry publishes them at `/actuator/prometheus`. The first delivery does not run a Prometheus server and does not supply dashboards, alerts, or deployment configuration. A deployment must restrict access to the scrape endpoint at its network boundary because application authentication is outside the project scope.

The application adds two focused meters:

- `congestion.tax.calculation` is a timer around the Calculation Service. It records the bounded `outcome` values `success`, `rejected`, and `failed`. Its Prometheus histogram supports aggregate latency percentiles.
- `congestion.tax.calculation.passages` is a distribution summary for the number of accepted Passages in a calculation. Its configured boundaries are 1, 10, 100, 1,000, and 10,000.

The meters do not use tax amounts, timestamps, identifiers, city codes, Vehicle Type codes, or exception text as tags. Standard HTTP metrics supply request count, duration, outcome, and status. The application does not duplicate those signals with a custom request or error counter.

## Implementation Time Plan

Planning time is outside the assignment's six-hour implementation limit. The initial implementation budget is:

| Work area | Minutes |
|---|---:|
| Project foundation, dependencies, Docker Compose, metrics, and CI | 35 |
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

The first implementation has no shared cache and does not cache complete HTTP responses. The provider bulk-loads the required data once and reuses it in immutable maps that exist only for the current request.

Complete requests are likely to be unique and can become stale when stored content changes. Add a shared cache only after measurements show a need. Measure database time, query count, p50, p95, and p99 response time, requests per second, connection-pool wait, CPU, memory, repeated rule use, expected hit ratio, and stale-data behavior. If justified, cache immutable rule sets by city and version instead of complete responses.

The first delivery does not include authentication, rate limiting, custom CORS behavior, an administration endpoint, a Prometheus server, metric dashboards, alerts, or production deployment because the assignment does not define those requirements.

## README Requirements

The root README explains the assignment scope, architecture, prerequisites, database startup and reset, application startup, profiles, health, metrics, tests, API examples, error format, supplied-data result, known limits, time spent, and additional work. It links to every design document, architecture decision record, and `questions.md`.
