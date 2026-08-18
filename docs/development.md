# Development and operations

## Test structure

Regular tests use the `Test` suffix. They cover the pure calculator, rule values, service coordination, repository mapping with mocks, and the HTTP adapter.

Integration tests use the `ITest` suffix and PostgreSQL through Testcontainers. They verify the Flyway schema, stored rules, complete HTTP path, health endpoint, metrics, OpenAPI output, and Swagger UI. London is used only in tests to show that the application supports rules for multiple cities.

Run regular tests:

```bash
./mvnw test
```

Run integration tests without regular tests:

```bash
./mvnw verify -Dskip.regular.tests=true
```

Run all tests, integration tests, and formatting checks:

```bash
./mvnw verify
```

The integration tests need Docker. GitHub Actions uses Java 17 and runs `./mvnw verify` for pull requests and pushes to `main`.

## Profiles

| Profile | Use |
|---|---|
| `dev` | Default local profile. Connects to the PostgreSQL service in `docker-compose.yaml`. |
| `prod` | Reads the server port, database connection, and log levels from environment variables. |
| `test` | Runs a small Spring test context without PostgreSQL. |
| `itest` | Runs the complete application with PostgreSQL Testcontainers. |

The `prod` profile requires these variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SERVER_PORT
LOGGING_LEVEL_ROOT
LOGGING_LEVEL_APPLICATION
```

The root `.env.example` lists the same names without values.

## Logging

The application uses `DEBUG` for calculation start details, loaded rule options, and applied exemption summaries. It uses `INFO` when a calculation completes.

The HTTP error boundary uses `WARN` for handled client errors and `ERROR` for server errors. A suppressed metrics failure also uses `WARN`. Each error is logged at the boundary that knows its final result.

Logs can contain the city code, vehicle type code, passage count, calculation date, result count, and a stable failure category. They do not contain request bodies, raw passage times, tax amounts, credentials, SQL, or exception details in HTTP responses.

## Health and metrics

The application exposes only these Actuator endpoints:

- `/actuator/health`
- `/actuator/prometheus`

Health includes the database component but hides component details.

Micrometer publishes standard JVM, process, HTTP, and Hikari connection-pool metrics. It also publishes:

- `congestion.tax.calculation`, a timer tagged with `success`, `rejected`, or `failed`;
- `congestion.tax.calculation.passages`, a distribution of accepted passage counts.

The custom metrics do not use city codes, vehicle types, timestamps, amounts, or other request values as tags. A metrics failure does not change a calculation result.
